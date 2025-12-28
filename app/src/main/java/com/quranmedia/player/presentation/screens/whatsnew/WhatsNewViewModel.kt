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
import com.quranmedia.player.data.repository.SettingsRepository
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
)

@HiltViewModel
class WhatsNewViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val locationHelper: LocationHelper,
    private val prayerTimesRepository: PrayerTimesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhatsNewUiState())
    val uiState: StateFlow<WhatsNewUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
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
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
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
        Timber.d("Location permission granted: $granted")

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

    fun markAsComplete() {
        viewModelScope.launch {
            settingsRepository.setLastSeenVersionCode(BuildConfig.VERSION_CODE)
            settingsRepository.setCompletedInitialSetup(true)
            Timber.d("What's New marked as complete for version ${BuildConfig.VERSION_CODE}")
        }
    }
}
