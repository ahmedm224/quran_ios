package com.quranmedia.player.presentation.screens.whatsnew

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
import com.quranmedia.player.data.repository.PrayerNotificationMode
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.data.source.QCFFontDownloadManager
import com.quranmedia.player.domain.repository.PrayerTimesRepository
import com.quranmedia.player.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class WhatsNewUiState(
    val language: AppLanguage = AppLanguage.ARABIC,
    val isFirstInstall: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val selectedPrayerMethod: Int = 4, // Default: Umm Al-Qura (Makkah)
    val selectedPrayerNotificationMode: PrayerNotificationMode = PrayerNotificationMode.SILENT
)

@HiltViewModel
class WhatsNewViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val locationHelper: LocationHelper,
    private val prayerTimesRepository: PrayerTimesRepository,
    private val fontDownloadManager: QCFFontDownloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhatsNewUiState())
    val uiState: StateFlow<WhatsNewUiState> = _uiState.asStateFlow()

    // Expose font download progress
    val svgDownloadProgress = fontDownloadManager.svgDownloadProgress
    val v4DownloadProgress = fontDownloadManager.v4DownloadProgress

    init {
        loadInitialState()
    }

    fun toggleLanguage() {
        val newLanguage = if (_uiState.value.language == AppLanguage.ARABIC) {
            AppLanguage.ENGLISH
        } else {
            AppLanguage.ARABIC
        }
        _uiState.value = _uiState.value.copy(language = newLanguage)
    }

    fun isSVGDownloaded(): Boolean = fontDownloadManager.isSVGDownloaded()
    fun isV4Downloaded(): Boolean = fontDownloadManager.isV4Downloaded()

    fun downloadSVGFonts() {
        viewModelScope.launch {
            val success = fontDownloadManager.downloadSVGFonts()
            if (success) {
                // Enable Mushaf mode with SVG rendering
                settingsRepository.setUseQCFFont(true)
                settingsRepository.setQCFTajweedMode(false)
                Timber.d("SVG fonts downloaded - Mushaf mode enabled")
            }
        }
    }

    fun downloadV4Fonts() {
        viewModelScope.launch {
            val success = fontDownloadManager.downloadV4Fonts()
            if (success) {
                // Enable QCF mode with Tajweed (V4) fonts
                settingsRepository.setUseQCFFont(true)
                settingsRepository.setQCFTajweedMode(true)
                Timber.d("V4 fonts downloaded - QCF mode enabled with Tajweed fonts")
            }
        }
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            // Get current settings
            settingsRepository.settings.collect { settings ->
                _uiState.value = _uiState.value.copy(
                    language = settings.appLanguage,
                    isFirstInstall = !settings.hasCompletedInitialSetup,
                    locationPermissionGranted = checkLocationPermission(),
                    notificationPermissionGranted = checkNotificationPermission(),
                    selectedPrayerMethod = settings.prayerCalculationMethod
                )
            }
        }
    }

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
            true // Not required on older versions
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(locationPermissionGranted = granted)
        Timber.d("Approximate location permission granted: $granted")

        // If permission granted, detect and save location
        if (granted) {
            detectLocation()
        }
    }

    private fun detectLocation() {
        viewModelScope.launch {
            Timber.d("Detecting location...")
            when (val result = locationHelper.getCurrentLocation()) {
                is Resource.Success -> {
                    result.data?.let { location ->
                        prayerTimesRepository.saveLocation(location)
                        Timber.d("Location detected and saved: ${location.cityName}")
                    } ?: run {
                        Timber.e("Location data is null")
                    }
                }
                is Resource.Error -> {
                    Timber.e("Failed to detect location: ${result.message}")
                }
                is Resource.Loading -> {
                    // Not used for this operation
                }
            }
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(notificationPermissionGranted = granted)
        Timber.d("Notification permission granted: $granted")
    }

    fun onPrayerMethodSelected(methodId: Int) {
        viewModelScope.launch {
            settingsRepository.setPrayerCalculationMethod(methodId)
            _uiState.value = _uiState.value.copy(selectedPrayerMethod = methodId)
            Timber.d("Prayer calculation method set to: $methodId")
        }
    }

    fun onPrayerNotificationModeSelected(mode: PrayerNotificationMode) {
        _uiState.value = _uiState.value.copy(selectedPrayerNotificationMode = mode)
        Timber.d("Prayer notification mode selected: $mode")
    }

    fun markAsComplete() {
        viewModelScope.launch {
            // Apply selected prayer notification mode to all prayers
            val selectedMode = _uiState.value.selectedPrayerNotificationMode
            settingsRepository.setFajrNotificationMode(selectedMode)
            settingsRepository.setDhuhrNotificationMode(selectedMode)
            settingsRepository.setAsrNotificationMode(selectedMode)
            settingsRepository.setMaghribNotificationMode(selectedMode)
            settingsRepository.setIshaNotificationMode(selectedMode)

            // Enable prayer notifications if mode is not SILENT
            if (selectedMode != PrayerNotificationMode.SILENT) {
                settingsRepository.setPrayerNotificationEnabled(true)
            }

            Timber.d("Applied $selectedMode to all prayers")

            settingsRepository.setLastSeenVersionCode(BuildConfig.VERSION_CODE)
            settingsRepository.setCompletedInitialSetup(true)
            Timber.d("What's New marked as complete for version ${BuildConfig.VERSION_CODE}")
        }
    }
}
