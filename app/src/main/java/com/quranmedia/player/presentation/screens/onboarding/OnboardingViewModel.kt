package com.quranmedia.player.presentation.screens.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.BuildConfig
import com.quranmedia.player.data.location.LocationHelper
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.data.repository.AthanRepository
import com.quranmedia.player.data.repository.PrayerNotificationMode
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.data.repository.TafseerRepository
import com.quranmedia.player.data.source.FontDownloadProgress
import com.quranmedia.player.data.source.FontDownloadState
import com.quranmedia.player.data.source.QCFFontDownloadManager
import com.quranmedia.player.data.notification.PrayerNotificationScheduler
import com.quranmedia.player.domain.model.AsrJuristicMethod
import com.quranmedia.player.domain.model.CalculationMethod
import com.quranmedia.player.domain.repository.PrayerTimesRepository
import com.quranmedia.player.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Onboarding stages
 */
enum class OnboardingStage {
    DOWNLOADS,      // Stage 1: Download fonts and tafseer
    PERMISSIONS     // Stage 2: Request permissions
}

/**
 * Download item state
 */
data class DownloadItemState(
    val id: String,
    val nameArabic: String,
    val nameEnglish: String,
    val description: String,
    val descriptionArabic: String,
    val sizeInfo: String,
    val isRequired: Boolean = false,  // V2 is required for Mushaf font
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null
)

/**
 * UI state for onboarding
 */
data class OnboardingUiState(
    val language: AppLanguage = AppLanguage.ARABIC,
    val currentStage: OnboardingStage = OnboardingStage.DOWNLOADS,

    // Download states
    val svgState: DownloadItemState = DownloadItemState(
        id = "svg",
        nameArabic = "خطوط المصحف",
        nameEnglish = "Mushaf Fonts",
        description = "Required for authentic Quran page display",
        descriptionArabic = "مطلوب لعرض صفحات المصحف بشكل أصيل",
        sizeInfo = "~100 MB",
        isRequired = true
    ),
    val v4State: DownloadItemState = DownloadItemState(
        id = "v4",
        nameArabic = "خطوط التجويد",
        nameEnglish = "Tajweed Fonts",
        description = "Color-coded Tajweed rules (optional)",
        descriptionArabic = "قواعد التجويد الملونة (اختياري)",
        sizeInfo = "~159 MB"
    ),
    val tafseerArabicState: DownloadItemState = DownloadItemState(
        id = "ibn-kathir",
        nameArabic = "تفسير ابن كثير",
        nameEnglish = "Tafsir Ibn Kathir (Arabic)",
        description = "Arabic Quran interpretation (optional)",
        descriptionArabic = "تفسير عربي للقرآن (اختياري)",
        sizeInfo = "~25 MB"
    ),
    val tafseerEnglishState: DownloadItemState = DownloadItemState(
        id = "ibn-kathir-english",
        nameArabic = "تفسير ابن كثير",
        nameEnglish = "Tafsir Ibn Kathir",
        description = "English Quran interpretation (optional)",
        descriptionArabic = "تفسير إنجليزي للقرآن (اختياري)",
        sizeInfo = "~15 MB"
    ),

    // Prayer calculation method
    val selectedCalculationMethod: Int = 4,  // Default: Umm Al-Qura (Makkah)

    // Permission states
    val locationPermissionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val batteryOptimizationDisabled: Boolean = false,
    val isDetectingLocation: Boolean = false,
    val detectedLocationName: String? = null,

    // General
    val showSkipWarning: Boolean = false,
    val isCompleting: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val fontDownloadManager: QCFFontDownloadManager,
    private val tafseerRepository: TafseerRepository,
    private val locationHelper: LocationHelper,
    private val prayerTimesRepository: PrayerTimesRepository,
    private val athanRepository: AthanRepository,
    private val prayerNotificationScheduler: PrayerNotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // Expose font download progress
    val svgDownloadProgress = fontDownloadManager.svgDownloadProgress
    val v4DownloadProgress = fontDownloadManager.v4DownloadProgress

    // Tafseer download progress
    private val _tafseerDownloadProgress = MutableStateFlow(0f)
    val tafseerDownloadProgress: StateFlow<Float> = _tafseerDownloadProgress.asStateFlow()

    private val _mufradatDownloadProgress = MutableStateFlow(0f)
    val mufradatDownloadProgress: StateFlow<Float> = _mufradatDownloadProgress.asStateFlow()

    init {
        loadInitialState()
        observeFontDownloads()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            _uiState.value = _uiState.value.copy(
                language = settings.appLanguage,
                selectedCalculationMethod = settings.prayerCalculationMethod,
                locationPermissionGranted = checkLocationPermission(),
                notificationPermissionGranted = checkNotificationPermission(),
                batteryOptimizationDisabled = checkBatteryOptimization()
            )

            // Check existing downloads
            checkExistingDownloads()
        }
    }

    private fun observeFontDownloads() {
        viewModelScope.launch {
            fontDownloadManager.svgDownloadProgress.collect { progress ->
                _uiState.value = _uiState.value.copy(
                    svgState = _uiState.value.svgState.copy(
                        isDownloaded = progress.state == FontDownloadState.DOWNLOADED,
                        isDownloading = progress.state == FontDownloadState.DOWNLOADING,
                        progress = progress.progress,
                        error = progress.errorMessage
                    )
                )
            }
        }

        viewModelScope.launch {
            fontDownloadManager.v4DownloadProgress.collect { progress ->
                _uiState.value = _uiState.value.copy(
                    v4State = _uiState.value.v4State.copy(
                        isDownloaded = progress.state == FontDownloadState.DOWNLOADED,
                        isDownloading = progress.state == FontDownloadState.DOWNLOADING,
                        progress = progress.progress,
                        error = progress.errorMessage
                    )
                )
            }
        }
    }

    private suspend fun checkExistingDownloads() {
        // Check SVG
        val svgDownloaded = fontDownloadManager.isSVGDownloaded()
        _uiState.value = _uiState.value.copy(
            svgState = _uiState.value.svgState.copy(isDownloaded = svgDownloaded)
        )

        // Check V4
        val v4Downloaded = fontDownloadManager.isV4Downloaded()
        _uiState.value = _uiState.value.copy(
            v4State = _uiState.value.v4State.copy(isDownloaded = v4Downloaded)
        )

        // Check Arabic Tafseer (Ibn Kathir)
        val tafseerArabicDownloaded = tafseerRepository.isDownloaded("ibn-kathir")
        _uiState.value = _uiState.value.copy(
            tafseerArabicState = _uiState.value.tafseerArabicState.copy(isDownloaded = tafseerArabicDownloaded)
        )

        // Check English Tafseer (Ibn Kathir)
        val tafseerEnglishDownloaded = tafseerRepository.isDownloaded("ibn-kathir-english")
        _uiState.value = _uiState.value.copy(
            tafseerEnglishState = _uiState.value.tafseerEnglishState.copy(isDownloaded = tafseerEnglishDownloaded)
        )
    }

    fun toggleLanguage() {
        val newLanguage = if (_uiState.value.language == AppLanguage.ARABIC) {
            AppLanguage.ENGLISH
        } else {
            AppLanguage.ARABIC
        }
        _uiState.value = _uiState.value.copy(language = newLanguage)

        viewModelScope.launch {
            settingsRepository.setAppLanguage(newLanguage)
        }
    }

    // ==================== Download Functions ====================

    fun downloadSVG() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                svgState = _uiState.value.svgState.copy(isDownloading = true, error = null)
            )
            val success = fontDownloadManager.downloadSVGFonts()
            if (success) {
                // Enable Mushaf mode with SVG rendering
                settingsRepository.setUseQCFFont(true)
                settingsRepository.setQCFTajweedMode(false)
                Timber.d("SVG fonts downloaded - Mushaf mode enabled")
            }
        }
    }

    fun downloadV4() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                v4State = _uiState.value.v4State.copy(isDownloading = true, error = null)
            )
            val success = fontDownloadManager.downloadV4Fonts()
            if (success) {
                // Enable Tajweed mode if SVG is also downloaded
                if (fontDownloadManager.isSVGDownloaded()) {
                    settingsRepository.setQCFTajweedMode(true)
                }
                Timber.d("V4 fonts downloaded")
            }
        }
    }

    fun downloadTafseerArabic() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                tafseerArabicState = _uiState.value.tafseerArabicState.copy(isDownloading = true, error = null)
            )
            val success = tafseerRepository.downloadTafseer("ibn-kathir") { progress ->
                _tafseerDownloadProgress.value = progress
                _uiState.value = _uiState.value.copy(
                    tafseerArabicState = _uiState.value.tafseerArabicState.copy(progress = progress)
                )
            }
            _uiState.value = _uiState.value.copy(
                tafseerArabicState = _uiState.value.tafseerArabicState.copy(
                    isDownloaded = success,
                    isDownloading = false,
                    error = if (!success) "Download failed" else null
                )
            )
        }
    }

    fun downloadTafseerEnglish() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                tafseerEnglishState = _uiState.value.tafseerEnglishState.copy(isDownloading = true, error = null)
            )
            val success = tafseerRepository.downloadTafseer("ibn-kathir-english") { progress ->
                _mufradatDownloadProgress.value = progress
                _uiState.value = _uiState.value.copy(
                    tafseerEnglishState = _uiState.value.tafseerEnglishState.copy(progress = progress)
                )
            }
            _uiState.value = _uiState.value.copy(
                tafseerEnglishState = _uiState.value.tafseerEnglishState.copy(
                    isDownloaded = success,
                    isDownloading = false,
                    error = if (!success) "Download failed" else null
                )
            )
        }
    }

    // ==================== Prayer Calculation Method ====================

    fun setCalculationMethod(methodId: Int) {
        viewModelScope.launch {
            settingsRepository.setPrayerCalculationMethod(methodId)
            _uiState.value = _uiState.value.copy(selectedCalculationMethod = methodId)
            Timber.d("Prayer calculation method set to: $methodId")
        }
    }

    // ==================== Navigation ====================

    fun goToNextStage() {
        when (_uiState.value.currentStage) {
            OnboardingStage.DOWNLOADS -> {
                // Check if SVG Mushaf font is skipped without download
                if (!_uiState.value.svgState.isDownloaded && !_uiState.value.showSkipWarning) {
                    _uiState.value = _uiState.value.copy(showSkipWarning = true)
                    return
                }
                _uiState.value = _uiState.value.copy(
                    currentStage = OnboardingStage.PERMISSIONS,
                    showSkipWarning = false
                )

                // Pre-cache Makkah prayer times as fallback (in background)
                // This ensures prayer times work even without location permission
                viewModelScope.launch {
                    cacheFallbackPrayerTimes()
                }
            }
            OnboardingStage.PERMISSIONS -> {
                // Complete onboarding
                completeOnboarding()
            }
        }
    }

    fun dismissSkipWarning() {
        _uiState.value = _uiState.value.copy(showSkipWarning = false)
    }

    fun confirmSkip() {
        _uiState.value = _uiState.value.copy(
            currentStage = OnboardingStage.PERMISSIONS,
            showSkipWarning = false
        )
    }

    // ==================== Permissions ====================

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(locationPermissionGranted = granted)
        Timber.d("Location permission granted: $granted")

        if (granted) {
            detectLocation()
        }
    }

    private fun detectLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDetectingLocation = true)

            when (val result = locationHelper.getCurrentLocation()) {
                is Resource.Success -> {
                    result.data?.let { location ->
                        prayerTimesRepository.saveLocation(location)
                        _uiState.value = _uiState.value.copy(
                            isDetectingLocation = false,
                            detectedLocationName = location.cityName ?: "${location.latitude}, ${location.longitude}"
                        )
                        Timber.d("Location detected and saved: ${location.cityName}")

                        // Immediately fetch and cache prayer times for offline use
                        cachePrayerTimesForLocation(location)
                    } ?: run {
                        _uiState.value = _uiState.value.copy(isDetectingLocation = false)
                        Timber.e("Location data is null")
                    }
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isDetectingLocation = false)
                    Timber.e("Failed to detect location: ${result.message}")
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * Fetch and cache prayer times immediately after location is detected.
     * This ensures prayer times work offline from the start.
     * Also caches Imsakiya (Ramadan) data for the full month.
     */
    private suspend fun cachePrayerTimesForLocation(location: com.quranmedia.player.domain.model.UserLocation) {
        try {
            val settings = settingsRepository.getCurrentSettings()
            val calculationMethod = CalculationMethod.fromId(settings.prayerCalculationMethod)
            val asrMethod = AsrJuristicMethod.fromId(settings.asrJuristicMethod)

            Timber.d("Caching prayer times for: ${location.cityName}, method: ${calculationMethod.name}")

            // Fetch today's prayer times - this will cache them
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)

            prayerTimesRepository.getPrayerTimes(
                latitude = location.latitude,
                longitude = location.longitude,
                date = today,
                method = calculationMethod,
                asrMethod = asrMethod
            )

            // Also cache tomorrow's prayer times
            prayerTimesRepository.getPrayerTimes(
                latitude = location.latitude,
                longitude = location.longitude,
                date = tomorrow,
                method = calculationMethod,
                asrMethod = asrMethod
            )

            Timber.d("Prayer times cached for today and tomorrow")

            // Also cache Imsakiya (Ramadan prayer times for the full month)
            cacheImsakiyaForLocation(location, calculationMethod, asrMethod)

        } catch (e: Exception) {
            Timber.e(e, "Error caching prayer times during onboarding")
        }
    }

    /**
     * Cache Makkah (default fallback) prayer times when entering permissions stage.
     * This ensures prayer times work even without location permission.
     */
    private suspend fun cacheFallbackPrayerTimes() {
        try {
            // Use Makkah as default location
            val makkahLocation = com.quranmedia.player.domain.model.UserLocation(
                latitude = 21.4225,
                longitude = 39.8262,
                cityName = "Makkah",
                countryName = "Saudi Arabia",
                isAutoDetected = false
            )

            val settings = settingsRepository.getCurrentSettings()
            val calculationMethod = CalculationMethod.fromId(settings.prayerCalculationMethod)
            val asrMethod = AsrJuristicMethod.fromId(settings.asrJuristicMethod)

            Timber.d("Pre-caching Makkah prayer times as fallback")

            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)

            // Fetch today's prayer times for Makkah
            prayerTimesRepository.getPrayerTimes(
                latitude = makkahLocation.latitude,
                longitude = makkahLocation.longitude,
                date = today,
                method = calculationMethod,
                asrMethod = asrMethod
            )

            // Also cache tomorrow's prayer times for Makkah
            prayerTimesRepository.getPrayerTimes(
                latitude = makkahLocation.latitude,
                longitude = makkahLocation.longitude,
                date = tomorrow,
                method = calculationMethod,
                asrMethod = asrMethod
            )

            // Also cache Imsakiya for Makkah
            cacheImsakiyaForLocation(makkahLocation, calculationMethod, asrMethod)

            Timber.d("Makkah fallback prayer times cached (today + tomorrow)")
        } catch (e: Exception) {
            Timber.e(e, "Error caching fallback prayer times")
        }
    }

    /**
     * Cache Imsakiya (Ramadan prayer times) for the full month.
     * This fetches all 29-30 days of Ramadan prayer times.
     */
    private suspend fun cacheImsakiyaForLocation(
        location: com.quranmedia.player.domain.model.UserLocation,
        calculationMethod: CalculationMethod,
        asrMethod: AsrJuristicMethod
    ) {
        try {
            // Get current or upcoming Ramadan dates
            val ramadanInfo = com.quranmedia.player.presentation.screens.imsakiya.RamadanDateUtil.getCurrentOrUpcomingRamadan()
            if (ramadanInfo == null) {
                Timber.d("Ramadan dates not available for caching")
                return
            }

            val (baseRamadanStart, _) = ramadanInfo
            val settings = settingsRepository.getCurrentSettings()
            val hijriDateAdjustment = settings.hijriDateAdjustment

            // Apply Hijri date adjustment
            val adjustedRamadanStart = baseRamadanStart.plusDays(hijriDateAdjustment.toLong())
            val ramadanDates = com.quranmedia.player.presentation.screens.imsakiya.RamadanDateUtil.getRamadanDates(adjustedRamadanStart)

            Timber.d("Caching Imsakiya for ${location.cityName}: ${ramadanDates.size} days")

            // Fetch prayer times for each day of Ramadan (this caches them)
            var cachedCount = 0
            for (date in ramadanDates) {
                val result = prayerTimesRepository.getPrayerTimes(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    date = date,
                    method = calculationMethod,
                    asrMethod = asrMethod
                )
                if (result is Resource.Success) {
                    cachedCount++
                }
            }

            Timber.d("Imsakiya cached: $cachedCount/${ramadanDates.size} days for ${location.cityName}")
        } catch (e: Exception) {
            Timber.e(e, "Error caching Imsakiya")
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(notificationPermissionGranted = granted)
        Timber.d("Notification permission granted: $granted")
    }

    private fun checkBatteryOptimization(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun refreshBatteryOptimizationStatus() {
        _uiState.value = _uiState.value.copy(batteryOptimizationDisabled = checkBatteryOptimization())
        Timber.d("Battery optimization disabled: ${_uiState.value.batteryOptimizationDisabled}")
    }

    // ==================== Complete Onboarding ====================

    fun completeOnboarding() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCompleting = true)

            // Enable Mushaf font if SVG is downloaded
            if (_uiState.value.svgState.isDownloaded) {
                settingsRepository.setUseQCFFont(true)
                // Enable Tajweed mode if V4 is also downloaded
                if (_uiState.value.v4State.isDownloaded) {
                    settingsRepository.setQCFTajweedMode(true)
                }
            }

            // Enable Athan notifications for all prayers (already defaulted, but ensure)
            settingsRepository.setPrayerNotificationEnabled(true)
            settingsRepository.setFajrNotificationMode(PrayerNotificationMode.ATHAN)
            settingsRepository.setDhuhrNotificationMode(PrayerNotificationMode.ATHAN)
            settingsRepository.setAsrNotificationMode(PrayerNotificationMode.ATHAN)
            settingsRepository.setMaghribNotificationMode(PrayerNotificationMode.ATHAN)
            settingsRepository.setIshaNotificationMode(PrayerNotificationMode.ATHAN)

            // Enable per-prayer notification flags
            settingsRepository.setNotifyFajr(true)
            settingsRepository.setNotifyDhuhr(true)
            settingsRepository.setNotifyAsr(true)
            settingsRepository.setNotifyMaghrib(true)
            settingsRepository.setNotifyIsha(true)

            // Enable max volume and flip to silence (already defaulted, but ensure)
            settingsRepository.setAthanMaxVolume(true)
            settingsRepository.setFlipToSilenceAthan(true)

            // Ensure default athan is available
            athanRepository.ensureDefaultAthanAvailable()

            // CRITICAL: Schedule prayer notifications if we have a location
            // Without this, alarms are never created even though settings are enabled!
            schedulePrayerNotifications()

            // Mark onboarding as complete
            settingsRepository.setLastSeenVersionCode(BuildConfig.VERSION_CODE)
            settingsRepository.setCompletedInitialSetup(true)

            Timber.d("Onboarding completed")
            _uiState.value = _uiState.value.copy(isCompleting = false)
        }
    }

    /**
     * Fetch prayer times and schedule notifications.
     * This is critical - without this, no Athan alarms are created!
     * Uses saved location if available, otherwise falls back to Makkah (default location).
     */
    private suspend fun schedulePrayerNotifications() {
        try {
            // Get location - repository provides DEFAULT_LOCATION (Makkah) if none saved
            // This ensures Athan works even without location permission
            val location = prayerTimesRepository.getSavedLocation().first()
                ?: return  // Should never happen as repository has fallback, but be safe

            // Get calculation method and Asr method from settings
            val settings = settingsRepository.getCurrentSettings()
            val calculationMethod = CalculationMethod.fromId(settings.prayerCalculationMethod)
            val asrMethod = AsrJuristicMethod.fromId(settings.asrJuristicMethod)

            Timber.d("Fetching prayer times for scheduling: ${location.cityName ?: "Default"}, method: ${calculationMethod.name}")

            // Fetch today's prayer times
            when (val result = prayerTimesRepository.getPrayerTimes(
                latitude = location.latitude,
                longitude = location.longitude,
                date = LocalDate.now(),
                method = calculationMethod,
                asrMethod = asrMethod
            )) {
                is Resource.Success -> {
                    result.data?.let { prayerTimes ->
                        // Schedule the notifications
                        prayerNotificationScheduler.schedulePrayerNotifications(prayerTimes)
                        Timber.d("Prayer notifications scheduled successfully after onboarding")
                    }
                }
                is Resource.Error -> {
                    Timber.e("Failed to fetch prayer times for notification scheduling: ${result.message}")
                }
                is Resource.Loading -> { /* ignore */ }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error scheduling prayer notifications after onboarding")
        }
    }

    fun isOnboardingComplete(): Boolean {
        return !_uiState.value.isCompleting
    }
}
