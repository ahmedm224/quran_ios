package com.quranmedia.player.wear.presentation.screens.prayertimes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.wear.data.repository.WearPrayerTimesRepository
import com.quranmedia.player.wear.domain.model.AppLanguage
import com.quranmedia.player.wear.domain.model.AsrJuristicMethod
import com.quranmedia.player.wear.domain.model.CalculationMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrayerSettingsUiState(
    val calculationMethod: CalculationMethod = CalculationMethod.MAKKAH,
    val asrMethod: AsrJuristicMethod = AsrJuristicMethod.SHAFI,
    val locationName: String = "Makkah",
    val notificationsEnabled: Boolean = true,
    val appLanguage: AppLanguage = AppLanguage.ARABIC,
    val isDetectingLocation: Boolean = false,
    val hasLocationPermission: Boolean = false
)

@HiltViewModel
class PrayerSettingsViewModel @Inject constructor(
    private val repository: WearPrayerTimesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrayerSettingsUiState())
    val uiState: StateFlow<PrayerSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = repository.getSettingsSync()
            val location = repository.getSavedLocationSync()
            val hasPermission = repository.hasLocationPermission()

            _uiState.value = PrayerSettingsUiState(
                calculationMethod = CalculationMethod.fromId(settings.calculationMethod),
                asrMethod = AsrJuristicMethod.fromId(settings.asrMethod),
                locationName = location?.displayName ?: "Makkah",
                notificationsEnabled = settings.notificationsEnabled,
                appLanguage = AppLanguage.fromCode(settings.appLanguage),
                hasLocationPermission = hasPermission
            )
        }
    }

    /**
     * Detect and update the current GPS location.
     */
    suspend fun detectLocation() {
        _uiState.value = _uiState.value.copy(isDetectingLocation = true)
        val detectedLocation = repository.detectAndSaveLocation()
        _uiState.value = _uiState.value.copy(
            isDetectingLocation = false,
            locationName = detectedLocation?.displayName ?: _uiState.value.locationName
        )
    }

    /**
     * Called when location permission is granted.
     */
    fun onLocationPermissionGranted() {
        _uiState.value = _uiState.value.copy(hasLocationPermission = true)
        viewModelScope.launch {
            detectLocation()
        }
    }

    suspend fun setCalculationMethod(method: CalculationMethod) {
        repository.updateCalculationMethod(method)
        _uiState.value = _uiState.value.copy(calculationMethod = method)
    }

    suspend fun setAsrMethod(method: AsrJuristicMethod) {
        repository.updateAsrMethod(method)
        _uiState.value = _uiState.value.copy(asrMethod = method)
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        repository.setNotificationsEnabled(enabled)
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        repository.setAppLanguage(language)
        _uiState.value = _uiState.value.copy(appLanguage = language)
    }
}
