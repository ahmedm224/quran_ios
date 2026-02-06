package com.quranmedia.player.presentation.screens.imsakiya

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.domain.model.AsrJuristicMethod
import com.quranmedia.player.domain.model.CalculationMethod
import com.quranmedia.player.domain.repository.PrayerTimesRepository
import com.quranmedia.player.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Cache key for Imsakiya data - invalidate cache when any of these change
 */
private data class ImsakiyaCacheKey(
    val latitude: Double,
    val longitude: Double,
    val calculationMethodId: Int,
    val asrMethodId: Int,
    val hijriDateAdjustment: Int,
    val hijriYear: Int
)

/**
 * Cached Imsakiya data with its cache key
 */
private data class ImsakiyaCache(
    val key: ImsakiyaCacheKey,
    val data: ImskaiyaMonth,
    val currentDayIndex: Int,
    val currentHijriDate: String,
    val currentHijriDateArabic: String,
    val daysUntilRamadan: Int
)

/**
 * ViewModel for the Imsakiya (Ramadan prayer times calendar) screen.
 * Uses the same settings as PrayerTimesViewModel (location, calculation method, Asr method).
 * Caches loaded data to avoid API calls on every screen open.
 *
 * TODO: After Ramadan, this file can be removed along with the entire imsakiya package.
 */
@HiltViewModel
class ImskaiyaViewModel @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImskaiyaUiState())
    val uiState: StateFlow<ImskaiyaUiState> = _uiState.asStateFlow()

    // In-memory cache for Imsakiya data
    private var cache: ImsakiyaCache? = null

    val settings: StateFlow<com.quranmedia.player.data.repository.UserSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.quranmedia.player.data.repository.UserSettings()
        )

    init {
        loadImsakiya()

        // Observe settings changes that affect Imsakiya and reload when they change
        viewModelScope.launch {
            settingsRepository.settings
                .map { Triple(it.hijriDateAdjustment, it.asrJuristicMethod, it.appLanguage) }
                .distinctUntilChanged()
                .collect { (hijriAdj, asrMethod, _) ->
                    // Check if cache is invalidated by settings change
                    val currentCache = cache
                    if (currentCache != null &&
                        (currentCache.key.hijriDateAdjustment != hijriAdj ||
                         currentCache.key.asrMethodId != asrMethod)) {
                        Timber.d("Imsakiya cache invalidated due to settings change")
                        cache = null
                        loadImsakiya(forceRefresh = true)
                    }
                }
        }
    }

    fun loadImsakiya(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                // Get current or upcoming Ramadan dates
                val ramadanInfo = RamadanDateUtil.getCurrentOrUpcomingRamadan()
                if (ramadanInfo == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Ramadan dates not available"
                    )
                    return@launch
                }

                val (baseRamadanStart, hijriYear) = ramadanInfo

                // Get saved location from prayer times settings
                val location = prayerTimesRepository.getSavedLocationSync()
                if (location == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Location not set. Please set your location in Prayer Times."
                    )
                    return@launch
                }

                // Get calculation settings (same as prayer times)
                val calculationMethod = prayerTimesRepository.getSavedCalculationMethod()
                val settings = settingsRepository.getCurrentSettings()
                val asrMethod = AsrJuristicMethod.fromId(settings.asrJuristicMethod)
                val isArabic = settings.appLanguage.code == "ar"
                val hijriDateAdjustment = settings.hijriDateAdjustment

                // Build cache key
                val cacheKey = ImsakiyaCacheKey(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    calculationMethodId = calculationMethod.id,
                    asrMethodId = asrMethod.id,
                    hijriDateAdjustment = hijriDateAdjustment,
                    hijriYear = hijriYear
                )

                // Check if we have valid cached data
                val currentCache = cache
                if (!forceRefresh && currentCache != null && currentCache.key == cacheKey) {
                    // Cache hit! Update current day index (may change if day changed)
                    val today = LocalDate.now()
                    val currentDayIndex = currentCache.data.days.indexOfFirst { it.gregorianDate == today }

                    // Recalculate days until Ramadan and current date display
                    val adjustedRamadanStart = baseRamadanStart.plusDays(hijriDateAdjustment.toLong())
                    val (currentHijriDate, currentHijriDateArabic, daysUntilRamadan) = if (currentDayIndex >= 0) {
                        val dayNumber = currentDayIndex + 1
                        Triple(
                            RamadanDateUtil.formatHijriDate(dayNumber, hijriYear, false),
                            RamadanDateUtil.formatHijriDate(dayNumber, hijriYear, true),
                            0
                        )
                    } else {
                        val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, adjustedRamadanStart).toInt()
                        Triple(
                            "Ramadan $hijriYear",
                            "رمضان $hijriYear",
                            if (daysLeft > 0) daysLeft else 0
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        imsakiya = currentCache.data,
                        currentDayIndex = currentDayIndex,
                        currentHijriDate = currentHijriDate,
                        currentHijriDateArabic = currentHijriDateArabic,
                        daysUntilRamadan = daysUntilRamadan,
                        isLoading = false,
                        error = null
                    )
                    Timber.d("Imsakiya loaded from cache")
                    return@launch
                }

                // Cache miss - calculate prayer times (offline)
                // Only show loading if we don't have existing data
                val hasExistingData = _uiState.value.imsakiya != null
                _uiState.value = _uiState.value.copy(
                    isLoading = !hasExistingData,
                    error = null,
                    offlineWarning = null
                )

                // Apply Hijri date adjustment to Ramadan start date
                val adjustedRamadanStart = baseRamadanStart.plusDays(hijriDateAdjustment.toLong())
                val ramadanDates = RamadanDateUtil.getRamadanDates(adjustedRamadanStart)

                // Fetch prayer times for each day of Ramadan
                val days = mutableListOf<ImskaiyaDay>()

                for ((index, date) in ramadanDates.withIndex()) {
                    val result = prayerTimesRepository.getPrayerTimes(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        date = date,
                        method = calculationMethod,
                        asrMethod = asrMethod
                    )

                    when (result) {
                        is Resource.Success -> {
                            result.data?.let { prayerTimes ->
                                days.add(
                                    ImskaiyaDay(
                                        dayNumber = index + 1,
                                        gregorianDate = date,
                                        hijriDate = RamadanDateUtil.formatHijriDate(
                                            index + 1,
                                            hijriYear,
                                            isArabic
                                        ),
                                        fajr = prayerTimes.fajr,
                                        sunrise = prayerTimes.sunrise,
                                        dhuhr = prayerTimes.dhuhr,
                                        asr = prayerTimes.asr,
                                        maghrib = prayerTimes.maghrib,
                                        isha = prayerTimes.isha
                                    )
                                )
                            }
                        }
                        is Resource.Error -> {
                            Timber.e("Failed to fetch prayer times for $date: ${result.message}")
                        }
                        else -> {}
                    }
                }

                if (days.isEmpty()) {
                    // If we have existing data, keep it and show warning
                    val hasExistingData = _uiState.value.imsakiya != null
                    if (hasExistingData) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            offlineWarning = "Using cached Imsakiya data"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to calculate Imsakiya. Please check your location settings."
                        )
                    }
                    return@launch
                }

                // Calculate current day index
                val today = LocalDate.now()
                val currentDayIndex = days.indexOfFirst { it.gregorianDate == today }

                // Get calculation method name for display
                val methodName = getCalculationMethodName(calculationMethod, isArabic)

                val imsakiya = ImskaiyaMonth(
                    days = days,
                    hijriYear = hijriYear,
                    gregorianYear = adjustedRamadanStart.year,
                    locationName = location.cityName ?: "${location.latitude}, ${location.longitude}",
                    calculationMethod = methodName
                )

                // Get today's Hijri date for the title and calculate days until Ramadan
                val (currentHijriDate, currentHijriDateArabic, daysUntilRamadan) = if (currentDayIndex >= 0) {
                    val dayNumber = currentDayIndex + 1
                    Triple(
                        RamadanDateUtil.formatHijriDate(dayNumber, hijriYear, false),
                        RamadanDateUtil.formatHijriDate(dayNumber, hijriYear, true),
                        0
                    )
                } else {
                    val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, adjustedRamadanStart).toInt()
                    Triple(
                        "Ramadan $hijriYear",
                        "رمضان $hijriYear",
                        if (daysLeft > 0) daysLeft else 0
                    )
                }

                // Store in cache
                cache = ImsakiyaCache(
                    key = cacheKey,
                    data = imsakiya,
                    currentDayIndex = currentDayIndex,
                    currentHijriDate = currentHijriDate,
                    currentHijriDateArabic = currentHijriDateArabic,
                    daysUntilRamadan = daysUntilRamadan
                )

                _uiState.value = _uiState.value.copy(
                    imsakiya = imsakiya,
                    currentDayIndex = currentDayIndex,
                    currentHijriDate = currentHijriDate,
                    currentHijriDateArabic = currentHijriDateArabic,
                    daysUntilRamadan = daysUntilRamadan,
                    isLoading = false,
                    error = null
                )

                Timber.d("Imsakiya calculated offline: ${days.size} days, current day index: $currentDayIndex")

            } catch (e: Exception) {
                Timber.e(e, "Error calculating Imsakiya")
                // If we have existing data, keep it and show warning
                val hasExistingData = _uiState.value.imsakiya != null
                if (hasExistingData) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        offlineWarning = "Could not refresh. Using cached data."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to calculate Imsakiya: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Force refresh the Imsakiya data from API, ignoring cache
     */
    fun refreshImsakiya() {
        loadImsakiya(forceRefresh = true)
    }

    /**
     * Clear the offline warning (called after user dismisses snackbar)
     */
    fun clearOfflineWarning() {
        _uiState.value = _uiState.value.copy(offlineWarning = null)
    }

    private fun getCalculationMethodName(method: CalculationMethod, isArabic: Boolean): String {
        // Use the built-in names from the enum
        return if (isArabic) method.nameArabic else method.nameEnglish
    }
}
