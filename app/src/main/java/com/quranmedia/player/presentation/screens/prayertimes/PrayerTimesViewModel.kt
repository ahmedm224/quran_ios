package com.quranmedia.player.presentation.screens.prayertimes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.data.location.LocationHelper
import com.quranmedia.player.data.notification.PrayerNotificationScheduler
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.data.repository.UserSettings
import com.quranmedia.player.domain.model.AsrJuristicMethod
import com.quranmedia.player.domain.model.CalculationMethod
import com.quranmedia.player.domain.model.PrayerTimes
import com.quranmedia.player.domain.model.PrayerType
import com.quranmedia.player.domain.util.Resource
import com.quranmedia.player.domain.model.UserLocation
import com.quranmedia.player.domain.repository.PrayerTimesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class PrayerTimesUiState(
    val prayerTimes: PrayerTimes? = null,
    val tomorrowPrayerTimes: PrayerTimes? = null, // For accurate Fajr countdown after Isha
    val location: UserLocation? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val nextPrayer: PrayerType? = null,
    val timeToNextPrayer: String = "",
    val hoursRemaining: Int = 0,
    val minutesRemaining: Int = 0,
    val hasLocationPermission: Boolean = false,
    val calculationMethod: CalculationMethod = CalculationMethod.MWL,
    val offlineWarning: String? = null, // Non-blocking warning shown as snackbar
    val currentDate: LocalDate = LocalDate.now() // Track current date for day change detection
)

@HiltViewModel
class PrayerTimesViewModel @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val locationHelper: LocationHelper,
    private val settingsRepository: SettingsRepository,
    private val prayerNotificationScheduler: PrayerNotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrayerTimesUiState())
    val uiState: StateFlow<PrayerTimesUiState> = _uiState.asStateFlow()

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    init {
        checkLocationPermission()
        loadSavedLocation()
        startCountdownTimer()
        observeSettingsChanges()
    }

    /**
     * Observe settings changes that affect prayer times calculation and notifications.
     * Automatically refreshes prayer times when Asr juristic method changes.
     * Reschedules notifications when notification settings change.
     */
    private fun observeSettingsChanges() {
        // Observe Asr juristic method changes - affects prayer time calculation
        viewModelScope.launch {
            settingsRepository.settings
                .map { it.asrJuristicMethod }
                .distinctUntilChanged()
                .collect { asrMethod ->
                    Timber.d("Asr juristic method changed to: $asrMethod")
                    // Only refresh if we already have prayer times (not on initial load)
                    if (_uiState.value.prayerTimes != null) {
                        _uiState.value.location?.let { location ->
                            fetchPrayerTimes(location)
                        }
                    }
                }
        }

        // Observe notification settings changes - reschedule alarms when settings change
        viewModelScope.launch {
            settingsRepository.settings
                .map { settings ->
                    // Create a key from all notification-related settings
                    "${settings.prayerNotificationEnabled}|" +
                    "${settings.notifyFajr}|${settings.notifyDhuhr}|${settings.notifyAsr}|${settings.notifyMaghrib}|${settings.notifyIsha}|" +
                    "${settings.prayerNotificationMinutesBefore}"
                }
                .distinctUntilChanged()
                .collect { notificationKey ->
                    Timber.d("Prayer notification settings changed: $notificationKey")
                    // Reschedule notifications if we have prayer times
                    _uiState.value.prayerTimes?.let { prayerTimes ->
                        prayerNotificationScheduler.schedulePrayerNotifications(prayerTimes)
                    }
                }
        }
    }

    private fun checkLocationPermission() {
        _uiState.value = _uiState.value.copy(
            hasLocationPermission = locationHelper.hasLocationPermission()
        )
    }

    private fun loadSavedLocation() {
        viewModelScope.launch {
            prayerTimesRepository.getSavedLocation().collect { location ->
                if (location != null) {
                    _uiState.value = _uiState.value.copy(location = location)
                    fetchPrayerTimes(location)
                } else if (locationHelper.hasLocationPermission()) {
                    detectLocation()
                }
            }
        }
    }

    fun detectLocation() {
        viewModelScope.launch {
            val hasExistingData = _uiState.value.prayerTimes != null
            _uiState.value = _uiState.value.copy(
                isLoading = !hasExistingData,
                error = null,
                offlineWarning = null
            )

            when (val result = locationHelper.getCurrentLocation()) {
                is Resource.Success -> {
                    result.data?.let { location ->
                        _uiState.value = _uiState.value.copy(location = location)
                        prayerTimesRepository.saveLocation(location)
                        fetchPrayerTimes(location)
                    } ?: run {
                        if (hasExistingData) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                offlineWarning = "Could not detect location. Using previous data."
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Could not get location data"
                            )
                        }
                    }
                }
                is Resource.Error -> {
                    if (hasExistingData) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            offlineWarning = "Location unavailable. Using cached data."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> {
                    // Already showing loading
                }
            }
        }
    }

    fun setManualLocation(cityName: String) {
        viewModelScope.launch {
            val hasExistingData = _uiState.value.prayerTimes != null
            _uiState.value = _uiState.value.copy(
                isLoading = !hasExistingData,
                error = null,
                offlineWarning = null
            )

            when (val result = locationHelper.getLocationFromCityName(cityName)) {
                is Resource.Success -> {
                    result.data?.let { location ->
                        _uiState.value = _uiState.value.copy(location = location)
                        prayerTimesRepository.saveLocation(location)
                        fetchPrayerTimes(location)
                    } ?: run {
                        if (hasExistingData) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                offlineWarning = "City not found. Using previous location."
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "City not found"
                            )
                        }
                    }
                }
                is Resource.Error -> {
                    if (hasExistingData) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            offlineWarning = "Cannot search city offline. Using cached data."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> {
                    // Already showing loading
                }
            }
        }
    }

    fun setCalculationMethod(method: CalculationMethod) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(calculationMethod = method)
            prayerTimesRepository.saveCalculationMethod(method)

            // Refresh prayer times with new method
            _uiState.value.location?.let { location ->
                fetchPrayerTimes(location)
            }
        }
    }

    fun setAsrJuristicMethod(method: AsrJuristicMethod) {
        viewModelScope.launch {
            settingsRepository.setAsrJuristicMethod(method.id)

            // Refresh prayer times with new method
            _uiState.value.location?.let { location ->
                fetchPrayerTimes(location)
            }
        }
    }

    private fun fetchPrayerTimes(location: UserLocation) {
        viewModelScope.launch {
            // Only show loading if we don't have existing data
            val hasExistingData = _uiState.value.prayerTimes != null
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)

            _uiState.value = _uiState.value.copy(
                isLoading = !hasExistingData,
                error = null,
                offlineWarning = null,
                currentDate = today
            )

            val method = prayerTimesRepository.getSavedCalculationMethod()
            val settings = settingsRepository.getCurrentSettings()
            val asrMethod = AsrJuristicMethod.fromId(settings.asrJuristicMethod)
            _uiState.value = _uiState.value.copy(calculationMethod = method)

            // Fetch today's prayer times
            when (val result = prayerTimesRepository.getPrayerTimes(
                latitude = location.latitude,
                longitude = location.longitude,
                date = today,
                method = method,
                asrMethod = asrMethod
            )) {
                is Resource.Success -> {
                    result.data?.let { prayerTimes ->
                        val nextPrayer = calculateNextPrayer(prayerTimes)
                        _uiState.value = _uiState.value.copy(
                            prayerTimes = prayerTimes,
                            isLoading = false,
                            nextPrayer = nextPrayer,
                            error = null,
                            offlineWarning = null
                        )
                        updateTimeToNextPrayer()
                        // Schedule prayer notifications
                        prayerNotificationScheduler.schedulePrayerNotifications(prayerTimes)

                        // Also fetch and cache tomorrow's prayer times (for accurate Fajr countdown after Isha)
                        fetchTomorrowPrayerTimes(location, tomorrow, method, asrMethod)
                    } ?: run {
                        // No data at all - show error only if we have no existing data
                        if (!hasExistingData) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Set your location to see prayer times"
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                offlineWarning = "Could not refresh. Using cached data."
                            )
                        }
                    }
                }
                is Resource.Error -> {
                    // If we have existing data, keep it and show a warning instead of error
                    if (hasExistingData) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            offlineWarning = "Offline - using cached prayer times"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> {
                    // Already showing loading
                }
            }
        }
    }

    /**
     * Fetch tomorrow's prayer times in background for accurate Fajr countdown after Isha.
     */
    private suspend fun fetchTomorrowPrayerTimes(
        location: UserLocation,
        tomorrow: LocalDate,
        method: CalculationMethod,
        asrMethod: AsrJuristicMethod
    ) {
        when (val result = prayerTimesRepository.getPrayerTimes(
            latitude = location.latitude,
            longitude = location.longitude,
            date = tomorrow,
            method = method,
            asrMethod = asrMethod
        )) {
            is Resource.Success -> {
                result.data?.let { tomorrowTimes ->
                    _uiState.value = _uiState.value.copy(tomorrowPrayerTimes = tomorrowTimes)
                    Timber.d("Tomorrow's prayer times cached")
                }
            }
            is Resource.Error -> {
                Timber.w("Could not fetch tomorrow's prayer times: ${result.message}")
            }
            is Resource.Loading -> {}
        }
    }

    /**
     * Clear the offline warning (called after user dismisses snackbar)
     */
    fun clearOfflineWarning() {
        _uiState.value = _uiState.value.copy(offlineWarning = null)
    }

    fun refresh() {
        _uiState.value.location?.let { location ->
            fetchPrayerTimes(location)
        } ?: run {
            if (locationHelper.hasLocationPermission()) {
                detectLocation()
            }
        }
    }

    private fun calculateNextPrayer(prayerTimes: PrayerTimes): PrayerType {
        val now = LocalTime.now()

        return when {
            now.isBefore(prayerTimes.fajr) -> PrayerType.FAJR
            now.isBefore(prayerTimes.sunrise) -> PrayerType.SUNRISE
            now.isBefore(prayerTimes.dhuhr) -> PrayerType.DHUHR
            now.isBefore(prayerTimes.asr) -> PrayerType.ASR
            now.isBefore(prayerTimes.maghrib) -> PrayerType.MAGHRIB
            now.isBefore(prayerTimes.isha) -> PrayerType.ISHA
            else -> PrayerType.FAJR // Next day's Fajr
        }
    }

    private fun startCountdownTimer() {
        viewModelScope.launch {
            while (true) {
                // Check for day change
                val today = LocalDate.now()
                if (_uiState.value.currentDate != today && _uiState.value.prayerTimes != null) {
                    // Day changed - refresh prayer times
                    Timber.d("Day changed, refreshing prayer times")
                    _uiState.value.location?.let { location ->
                        fetchPrayerTimes(location)
                    }
                }

                updateTimeToNextPrayer()
                delay(1000) // Update every second
            }
        }
    }

    private fun updateTimeToNextPrayer() {
        val prayerTimes = _uiState.value.prayerTimes ?: return
        val nextPrayer = _uiState.value.nextPrayer ?: return

        val now = LocalTime.now()

        // Use tomorrow's Fajr time if we're after Isha and have tomorrow's data
        val nextPrayerTime = when (nextPrayer) {
            PrayerType.FAJR -> {
                // After Isha, use tomorrow's Fajr if available
                if (now.isAfter(prayerTimes.isha)) {
                    _uiState.value.tomorrowPrayerTimes?.fajr ?: prayerTimes.fajr
                } else {
                    prayerTimes.fajr
                }
            }
            PrayerType.SUNRISE -> prayerTimes.sunrise
            PrayerType.DHUHR -> prayerTimes.dhuhr
            PrayerType.ASR -> prayerTimes.asr
            PrayerType.MAGHRIB -> prayerTimes.maghrib
            PrayerType.ISHA -> prayerTimes.isha
        }

        val minutesUntil = if (now.isBefore(nextPrayerTime) && nextPrayer != PrayerType.FAJR) {
            now.until(nextPrayerTime, ChronoUnit.MINUTES)
        } else if (nextPrayer == PrayerType.FAJR && now.isAfter(prayerTimes.isha)) {
            // After Isha, counting to tomorrow's Fajr
            now.until(LocalTime.MAX, ChronoUnit.MINUTES) +
                    LocalTime.MIN.until(nextPrayerTime, ChronoUnit.MINUTES) + 1
        } else if (now.isBefore(nextPrayerTime)) {
            now.until(nextPrayerTime, ChronoUnit.MINUTES)
        } else {
            // Next day's prayer
            now.until(LocalTime.MAX, ChronoUnit.MINUTES) +
                    LocalTime.MIN.until(nextPrayerTime, ChronoUnit.MINUTES) + 1
        }

        val hours = (minutesUntil / 60).toInt()
        val minutes = (minutesUntil % 60).toInt()

        _uiState.value = _uiState.value.copy(
            nextPrayer = calculateNextPrayer(prayerTimes),
            hoursRemaining = hours,
            minutesRemaining = minutes,
            timeToNextPrayer = if (hours > 0) {
                "${hours}h ${minutes}m"
            } else {
                "${minutes}m"
            }
        )
    }

    fun onLocationPermissionGranted() {
        checkLocationPermission()
        if (locationHelper.hasLocationPermission()) {
            detectLocation()
        }
    }

    // Prayer notification settings
    fun setPrayerNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPrayerNotificationEnabled(enabled)
            // Reschedule or cancel notifications based on new setting
            _uiState.value.prayerTimes?.let { prayerTimes ->
                if (enabled) {
                    prayerNotificationScheduler.schedulePrayerNotifications(prayerTimes)
                } else {
                    prayerNotificationScheduler.cancelAllNotifications()
                }
            }
        }
    }

    fun setPrayerNotificationMinutesBefore(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setPrayerNotificationMinutesBefore(minutes)
            // Reschedule notifications with new timing
            _uiState.value.prayerTimes?.let { prayerTimes ->
                prayerNotificationScheduler.schedulePrayerNotifications(prayerTimes)
            }
        }
    }

    fun setNotifyFajr(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotifyFajr(enabled)
            rescheduleNotifications()
        }
    }

    fun setNotifyDhuhr(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotifyDhuhr(enabled)
            rescheduleNotifications()
        }
    }

    fun setNotifyAsr(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotifyAsr(enabled)
            rescheduleNotifications()
        }
    }

    fun setNotifyMaghrib(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotifyMaghrib(enabled)
            rescheduleNotifications()
        }
    }

    fun setNotifyIsha(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotifyIsha(enabled)
            rescheduleNotifications()
        }
    }

    fun setPrayerNotificationSound(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPrayerNotificationSound(enabled)
        }
    }

    fun setPrayerNotificationVibrate(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPrayerNotificationVibrate(enabled)
        }
    }

    fun setHijriDateAdjustment(days: Int) {
        viewModelScope.launch {
            settingsRepository.setHijriDateAdjustment(days)
        }
    }

    private fun rescheduleNotifications() {
        _uiState.value.prayerTimes?.let { prayerTimes ->
            // Isha offset is already applied to the stored prayerTimes
            prayerNotificationScheduler.schedulePrayerNotifications(prayerTimes)
        }
    }
}

