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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class PrayerTimesUiState(
    val prayerTimes: PrayerTimes? = null,
    val location: UserLocation? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val nextPrayer: PrayerType? = null,
    val timeToNextPrayer: String = "",
    val hasLocationPermission: Boolean = false,
    val calculationMethod: CalculationMethod = CalculationMethod.MWL
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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = locationHelper.getCurrentLocation()) {
                is Resource.Success -> {
                    result.data?.let { location ->
                        _uiState.value = _uiState.value.copy(location = location)
                        prayerTimesRepository.saveLocation(location)
                        fetchPrayerTimes(location)
                    } ?: run {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Could not get location data"
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> {
                    // Already showing loading
                }
            }
        }
    }

    fun setManualLocation(cityName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = locationHelper.getLocationFromCityName(cityName)) {
                is Resource.Success -> {
                    result.data?.let { location ->
                        _uiState.value = _uiState.value.copy(location = location)
                        prayerTimesRepository.saveLocation(location)
                        fetchPrayerTimes(location)
                    } ?: run {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "City not found"
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val method = prayerTimesRepository.getSavedCalculationMethod()
            val settings = settingsRepository.getCurrentSettings()
            val asrMethod = AsrJuristicMethod.fromId(settings.asrJuristicMethod)
            _uiState.value = _uiState.value.copy(calculationMethod = method)

            when (val result = prayerTimesRepository.getPrayerTimes(
                latitude = location.latitude,
                longitude = location.longitude,
                date = LocalDate.now(),
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
                            error = null
                        )
                        updateTimeToNextPrayer()
                        // Schedule prayer notifications
                        prayerNotificationScheduler.schedulePrayerNotifications(prayerTimes)
                    } ?: run {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to get prayer times"
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> {
                    // Already showing loading
                }
            }
        }
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
                updateTimeToNextPrayer()
                delay(1000) // Update every second
            }
        }
    }

    private fun updateTimeToNextPrayer() {
        val prayerTimes = _uiState.value.prayerTimes ?: return
        val nextPrayer = _uiState.value.nextPrayer ?: return

        val now = LocalTime.now()
        val nextPrayerTime = when (nextPrayer) {
            PrayerType.FAJR -> prayerTimes.fajr
            PrayerType.SUNRISE -> prayerTimes.sunrise
            PrayerType.DHUHR -> prayerTimes.dhuhr
            PrayerType.ASR -> prayerTimes.asr
            PrayerType.MAGHRIB -> prayerTimes.maghrib
            PrayerType.ISHA -> prayerTimes.isha
        }

        val minutesUntil = if (now.isBefore(nextPrayerTime)) {
            now.until(nextPrayerTime, ChronoUnit.MINUTES)
        } else {
            // Next day's prayer (for Fajr after Isha)
            now.until(LocalTime.MAX, ChronoUnit.MINUTES) +
                    LocalTime.MIN.until(nextPrayerTime, ChronoUnit.MINUTES) + 1
        }

        val hours = minutesUntil / 60
        val minutes = minutesUntil % 60

        _uiState.value = _uiState.value.copy(
            nextPrayer = calculateNextPrayer(prayerTimes),
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

