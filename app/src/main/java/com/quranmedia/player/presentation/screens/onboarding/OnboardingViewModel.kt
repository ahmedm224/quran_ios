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
 * UI state for onboarding (single page)
 */
data class OnboardingUiState(
    val language: AppLanguage = AppLanguage.ARABIC,

    // Prayer calculation method
    val selectedCalculationMethod: Int = 4,  // Default: Umm Al-Qura (Makkah)

    // Permission states
    val locationPermissionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val batteryOptimizationDisabled: Boolean = false,
    val isDetectingLocation: Boolean = false,
    val detectedLocationName: String? = null,

    // General
    val isCompleting: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val locationHelper: LocationHelper,
    private val prayerTimesRepository: PrayerTimesRepository,
    private val athanRepository: AthanRepository,
    private val prayerNotificationScheduler: PrayerNotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
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

            // Pre-cache Makkah prayer times as fallback
            cacheFallbackPrayerTimes()
        }
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

    // ==================== Prayer Calculation Method ====================

    fun setCalculationMethod(methodId: Int) {
        viewModelScope.launch {
            settingsRepository.setPrayerCalculationMethod(methodId)
            _uiState.value = _uiState.value.copy(selectedCalculationMethod = methodId)
            Timber.d("Prayer calculation method set to: $methodId")
        }
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

    private fun checkBatteryOptimization(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(locationPermissionGranted = granted)
        Timber.d("Location permission granted: $granted")

        if (granted) {
            detectLocation()
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(notificationPermissionGranted = granted)
        Timber.d("Notification permission granted: $granted")
    }

    fun refreshBatteryOptimizationStatus() {
        _uiState.value = _uiState.value.copy(batteryOptimizationDisabled = checkBatteryOptimization())
        Timber.d("Battery optimization disabled: ${_uiState.value.batteryOptimizationDisabled}")
    }

    /**
     * Called after RequestMultiplePermissions result.
     * Updates both location and notification permission states.
     */
    fun onMultiplePermissionsResult(permissions: Map<String, Boolean>) {
        val locationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else {
            true
        }

        _uiState.value = _uiState.value.copy(
            locationPermissionGranted = locationGranted,
            notificationPermissionGranted = notificationGranted
        )

        if (locationGranted) {
            detectLocation()
        }

        Timber.d("Multiple permissions result: location=$locationGranted, notification=$notificationGranted")
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

    // ==================== Prayer Times Caching ====================

    /**
     * Fetch and cache prayer times immediately after location is detected.
     * Also caches Imsakiya (Ramadan) data for the full month.
     */
    private suspend fun cachePrayerTimesForLocation(location: com.quranmedia.player.domain.model.UserLocation) {
        try {
            val settings = settingsRepository.getCurrentSettings()
            val calculationMethod = CalculationMethod.fromId(settings.prayerCalculationMethod)
            val asrMethod = AsrJuristicMethod.fromId(settings.asrJuristicMethod)

            Timber.d("Caching prayer times for: ${location.cityName}, method: ${calculationMethod.name}")

            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)

            prayerTimesRepository.getPrayerTimes(
                latitude = location.latitude,
                longitude = location.longitude,
                date = today,
                method = calculationMethod,
                asrMethod = asrMethod
            )

            prayerTimesRepository.getPrayerTimes(
                latitude = location.latitude,
                longitude = location.longitude,
                date = tomorrow,
                method = calculationMethod,
                asrMethod = asrMethod
            )

            Timber.d("Prayer times cached for today and tomorrow")

            cacheImsakiyaForLocation(location, calculationMethod, asrMethod)

        } catch (e: Exception) {
            Timber.e(e, "Error caching prayer times during onboarding")
        }
    }

    /**
     * Cache Makkah (default fallback) prayer times.
     * Ensures prayer times work even without location permission.
     */
    private suspend fun cacheFallbackPrayerTimes() {
        try {
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

            prayerTimesRepository.getPrayerTimes(
                latitude = makkahLocation.latitude,
                longitude = makkahLocation.longitude,
                date = today,
                method = calculationMethod,
                asrMethod = asrMethod
            )

            prayerTimesRepository.getPrayerTimes(
                latitude = makkahLocation.latitude,
                longitude = makkahLocation.longitude,
                date = tomorrow,
                method = calculationMethod,
                asrMethod = asrMethod
            )

            cacheImsakiyaForLocation(makkahLocation, calculationMethod, asrMethod)

            Timber.d("Makkah fallback prayer times cached (today + tomorrow)")
        } catch (e: Exception) {
            Timber.e(e, "Error caching fallback prayer times")
        }
    }

    /**
     * Cache Imsakiya (Ramadan prayer times) for the full month.
     */
    private suspend fun cacheImsakiyaForLocation(
        location: com.quranmedia.player.domain.model.UserLocation,
        calculationMethod: CalculationMethod,
        asrMethod: AsrJuristicMethod
    ) {
        try {
            val ramadanInfo = com.quranmedia.player.presentation.screens.imsakiya.RamadanDateUtil.getCurrentOrUpcomingRamadan()
            if (ramadanInfo == null) {
                Timber.d("Ramadan dates not available for caching")
                return
            }

            val (baseRamadanStart, _) = ramadanInfo
            val settings = settingsRepository.getCurrentSettings()
            val hijriDateAdjustment = settings.hijriDateAdjustment

            val adjustedRamadanStart = baseRamadanStart.plusDays(hijriDateAdjustment.toLong())
            val ramadanDates = com.quranmedia.player.presentation.screens.imsakiya.RamadanDateUtil.getRamadanDates(adjustedRamadanStart)

            Timber.d("Caching Imsakiya for ${location.cityName}: ${ramadanDates.size} days")

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

    // ==================== Complete Onboarding ====================

    fun completeOnboarding() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCompleting = true)

            // Always enable Mushaf font (SVG is bundled in assets)
            settingsRepository.setUseQCFFont(true)

            // Enable Athan notifications for all prayers
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

            // Enable max volume and flip to silence
            settingsRepository.setAthanMaxVolume(true)
            settingsRepository.setFlipToSilenceAthan(true)

            // Ensure default athan is available
            athanRepository.ensureDefaultAthanAvailable()

            // Schedule prayer notifications
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
     * Uses saved location if available, otherwise falls back to Makkah.
     */
    private suspend fun schedulePrayerNotifications() {
        try {
            val location = prayerTimesRepository.getSavedLocation().first()
                ?: return

            val settings = settingsRepository.getCurrentSettings()
            val calculationMethod = CalculationMethod.fromId(settings.prayerCalculationMethod)
            val asrMethod = AsrJuristicMethod.fromId(settings.asrJuristicMethod)

            Timber.d("Fetching prayer times for scheduling: ${location.cityName ?: "Default"}, method: ${calculationMethod.name}")

            when (val result = prayerTimesRepository.getPrayerTimes(
                latitude = location.latitude,
                longitude = location.longitude,
                date = LocalDate.now(),
                method = calculationMethod,
                asrMethod = asrMethod
            )) {
                is Resource.Success -> {
                    result.data?.let { prayerTimes ->
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
}
