package com.quranmedia.player.wear.presentation.screens.prayertimes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.wear.data.notification.PrayerNotificationScheduler
import com.quranmedia.player.wear.data.repository.WearPrayerTimesRepository
import com.quranmedia.player.wear.domain.model.AppLanguage
import com.quranmedia.player.wear.domain.model.PrayerTimes
import com.quranmedia.player.wear.domain.model.PrayerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Duration
import java.time.LocalTime
import javax.inject.Inject

data class PrayerTimesUiState(
    val prayerTimes: PrayerTimes? = null,
    val nextPrayer: PrayerType? = null,
    val nextPrayerTime: LocalTime? = null,
    val countdown: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val appLanguage: AppLanguage = AppLanguage.ARABIC
)

@HiltViewModel
class PrayerTimesViewModel @Inject constructor(
    private val repository: WearPrayerTimesRepository,
    private val notificationScheduler: PrayerNotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrayerTimesUiState())
    val uiState: StateFlow<PrayerTimesUiState> = _uiState.asStateFlow()

    init {
        loadPrayerTimes()
        startCountdownTimer()
    }

    private fun loadPrayerTimes() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val prayerTimes = repository.getPrayerTimesForToday()
                val nextPrayer = repository.getNextPrayer()
                val language = repository.getAppLanguage()

                _uiState.value = _uiState.value.copy(
                    prayerTimes = prayerTimes,
                    nextPrayer = nextPrayer?.first,
                    nextPrayerTime = nextPrayer?.second,
                    isLoading = false,
                    appLanguage = language
                )

                // Schedule notifications silently
                try {
                    notificationScheduler.scheduleAllPrayerNotifications()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to schedule notifications")
                }

                Timber.d("Loaded prayer times for: ${prayerTimes.locationName}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load prayer times")
                // Don't show error to user - just stop loading
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    /**
     * Called when location permission is granted (from settings).
     */
    fun onLocationPermissionGranted() {
        viewModelScope.launch {
            repository.detectAndSaveLocation()
            loadPrayerTimes()
        }
    }

    private fun startCountdownTimer() {
        viewModelScope.launch {
            while (isActive) {
                updateCountdown()
                delay(1000) // Update every second
            }
        }
    }

    private fun updateCountdown() {
        val nextPrayerTime = _uiState.value.nextPrayerTime ?: return
        val now = LocalTime.now()

        if (nextPrayerTime.isBefore(now)) {
            // Prayer time passed, refresh data
            loadPrayerTimes()
            return
        }

        val duration = Duration.between(now, nextPrayerTime)
        val hours = duration.toHours()
        val minutes = duration.toMinutesPart()
        val seconds = duration.toSecondsPart()

        val countdown = if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }

        _uiState.value = _uiState.value.copy(countdown = countdown)
    }

    fun refresh() {
        loadPrayerTimes()
    }
}
