package com.quranmedia.player.presentation.screens.prayertimes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.data.api.AthanApi
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.data.repository.AthanRepository
import com.quranmedia.player.data.repository.PrayerNotificationMode
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.data.notification.PrayerNotificationScheduler
import com.quranmedia.player.domain.model.AsrJuristicMethod
import com.quranmedia.player.domain.model.Athan
import com.quranmedia.player.domain.model.CalculationMethod
import com.quranmedia.player.domain.model.PrayerType
import com.quranmedia.player.domain.repository.PrayerTimesRepository
import com.quranmedia.player.domain.util.Resource
import java.time.LocalDate
import com.quranmedia.player.media.player.AthanPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AthanSettingsUiState(
    val language: AppLanguage = AppLanguage.ARABIC,
    val availableAthans: List<Athan> = emptyList(),
    val downloadedAthans: List<Athan> = emptyList(),
    val isLoadingAthans: Boolean = false,
    val downloadingAthanId: String? = null,
    val previewingAthanId: String? = null,
    val athanMaxVolume: Boolean = true,
    val flipToSilence: Boolean = true,
    // Per-prayer settings (default SILENT - user must enable explicitly)
    val fajrMode: PrayerNotificationMode = PrayerNotificationMode.SILENT,
    val dhuhrMode: PrayerNotificationMode = PrayerNotificationMode.SILENT,
    val asrMode: PrayerNotificationMode = PrayerNotificationMode.SILENT,
    val maghribMode: PrayerNotificationMode = PrayerNotificationMode.SILENT,
    val ishaMode: PrayerNotificationMode = PrayerNotificationMode.SILENT,
    val fajrAthanId: String = "1a014366658c",  // Abdulbasit Abdusamad (Egypt)
    val dhuhrAthanId: String = "1a014366658c",
    val asrAthanId: String = "1a014366658c",
    val maghribAthanId: String = "1a014366658c",
    val ishaAthanId: String = "1a014366658c",
    // Notification settings (consolidated from PrayerNotificationDialog)
    val notifyMinutesBefore: Int = 0,
    val notificationSound: Boolean = true,
    val notificationVibrate: Boolean = true,
    // Calculation settings
    val calculationMethod: CalculationMethod = CalculationMethod.MAKKAH,
    val asrJuristicMethod: AsrJuristicMethod = AsrJuristicMethod.SHAFI,
    val hijriDateAdjustment: Int = 0,
) {
    fun getPrayerMode(prayerType: PrayerType): PrayerNotificationMode {
        return when (prayerType) {
            PrayerType.FAJR -> fajrMode
            PrayerType.DHUHR -> dhuhrMode
            PrayerType.ASR -> asrMode
            PrayerType.MAGHRIB -> maghribMode
            PrayerType.ISHA -> ishaMode
            else -> PrayerNotificationMode.NOTIFICATION
        }
    }

    fun getSelectedAthanId(prayerType: PrayerType): String? {
        return when (prayerType) {
            PrayerType.FAJR -> fajrAthanId
            PrayerType.DHUHR -> dhuhrAthanId
            PrayerType.ASR -> asrAthanId
            PrayerType.MAGHRIB -> maghribAthanId
            PrayerType.ISHA -> ishaAthanId
            else -> null
        }
    }

    fun getSelectedAthanName(prayerType: PrayerType): String? {
        val athanId = getSelectedAthanId(prayerType) ?: return null
        return downloadedAthans.find { it.id == athanId }?.name
            ?: availableAthans.find { it.id == athanId }?.name
    }

    fun isAthanDownloaded(prayerType: PrayerType): Boolean {
        val athanId = getSelectedAthanId(prayerType) ?: return false
        return downloadedAthans.any { it.id == athanId }
    }
}

@HiltViewModel
class AthanSettingsViewModel @Inject constructor(
    private val athanRepository: AthanRepository,
    private val settingsRepository: SettingsRepository,
    private val prayerTimesRepository: PrayerTimesRepository,
    private val prayerNotificationScheduler: PrayerNotificationScheduler,
    private val athanPlayer: AthanPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(AthanSettingsUiState())
    val uiState: StateFlow<AthanSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadAthans()
        observeDownloadedAthans()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val calculationMethod = prayerTimesRepository.getSavedCalculationMethod()
            _uiState.value = _uiState.value.copy(
                language = settings.appLanguage,
                athanMaxVolume = settings.athanMaxVolume,
                flipToSilence = settings.flipToSilenceAthan,
                fajrMode = settings.fajrNotificationMode,
                dhuhrMode = settings.dhuhrNotificationMode,
                asrMode = settings.asrNotificationMode,
                maghribMode = settings.maghribNotificationMode,
                ishaMode = settings.ishaNotificationMode,
                fajrAthanId = settings.fajrAthanId,
                dhuhrAthanId = settings.dhuhrAthanId,
                asrAthanId = settings.asrAthanId,
                maghribAthanId = settings.maghribAthanId,
                ishaAthanId = settings.ishaAthanId,
                notifyMinutesBefore = settings.prayerNotificationMinutesBefore,
                notificationSound = settings.prayerNotificationSound,
                notificationVibrate = settings.prayerNotificationVibrate,
                calculationMethod = calculationMethod,
                asrJuristicMethod = AsrJuristicMethod.fromId(settings.asrJuristicMethod),
                hijriDateAdjustment = settings.hijriDateAdjustment
            )
        }
    }

    private fun loadAthans() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAthans = true)
            try {
                val athans = athanRepository.getAllAthans()
                _uiState.value = _uiState.value.copy(
                    availableAthans = athans,
                    isLoadingAthans = false
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading athans")
                _uiState.value = _uiState.value.copy(isLoadingAthans = false)
            }
        }
    }

    private fun observeDownloadedAthans() {
        viewModelScope.launch {
            athanRepository.getDownloadedAthans().collect { athans ->
                _uiState.value = _uiState.value.copy(downloadedAthans = athans)
            }
        }
    }

    fun setPrayerMode(prayerType: PrayerType, mode: PrayerNotificationMode) {
        viewModelScope.launch {
            // Enable master notification switch when user enables any prayer notification
            if (mode != PrayerNotificationMode.SILENT) {
                settingsRepository.setPrayerNotificationEnabled(true)
            }

            // Also update the per-prayer enable flag to match the mode
            val isEnabled = mode != PrayerNotificationMode.SILENT

            when (prayerType) {
                PrayerType.FAJR -> {
                    settingsRepository.setFajrNotificationMode(mode)
                    settingsRepository.setNotifyFajr(isEnabled)
                    _uiState.value = _uiState.value.copy(fajrMode = mode)
                }
                PrayerType.DHUHR -> {
                    settingsRepository.setDhuhrNotificationMode(mode)
                    settingsRepository.setNotifyDhuhr(isEnabled)
                    _uiState.value = _uiState.value.copy(dhuhrMode = mode)
                }
                PrayerType.ASR -> {
                    settingsRepository.setAsrNotificationMode(mode)
                    settingsRepository.setNotifyAsr(isEnabled)
                    _uiState.value = _uiState.value.copy(asrMode = mode)
                }
                PrayerType.MAGHRIB -> {
                    settingsRepository.setMaghribNotificationMode(mode)
                    settingsRepository.setNotifyMaghrib(isEnabled)
                    _uiState.value = _uiState.value.copy(maghribMode = mode)
                }
                PrayerType.ISHA -> {
                    settingsRepository.setIshaNotificationMode(mode)
                    settingsRepository.setNotifyIsha(isEnabled)
                    _uiState.value = _uiState.value.copy(ishaMode = mode)
                }
                else -> {}
            }

            // Auto-download Athan if user enables ATHAN mode and it's not downloaded
            if (mode == PrayerNotificationMode.ATHAN) {
                val athanId = _uiState.value.getSelectedAthanId(prayerType)
                if (athanId != null && !athanRepository.isAthanDownloaded(athanId)) {
                    // Get the Athan details and download it
                    val athan = athanRepository.getAthanById(athanId)
                    if (athan != null) {
                        Timber.d("Auto-downloading Athan for $prayerType: ${athan.name}")
                        downloadAthan(athan)
                    }
                }
            }
        }
    }

    fun selectAthanForPrayer(prayerType: PrayerType, athanId: String) {
        viewModelScope.launch {
            when (prayerType) {
                PrayerType.FAJR -> {
                    settingsRepository.setFajrAthanId(athanId)
                    _uiState.value = _uiState.value.copy(fajrAthanId = athanId)
                }
                PrayerType.DHUHR -> {
                    settingsRepository.setDhuhrAthanId(athanId)
                    _uiState.value = _uiState.value.copy(dhuhrAthanId = athanId)
                }
                PrayerType.ASR -> {
                    settingsRepository.setAsrAthanId(athanId)
                    _uiState.value = _uiState.value.copy(asrAthanId = athanId)
                }
                PrayerType.MAGHRIB -> {
                    settingsRepository.setMaghribAthanId(athanId)
                    _uiState.value = _uiState.value.copy(maghribAthanId = athanId)
                }
                PrayerType.ISHA -> {
                    settingsRepository.setIshaAthanId(athanId)
                    _uiState.value = _uiState.value.copy(ishaAthanId = athanId)
                }
                else -> {}
            }
        }
    }

    /**
     * Apply global Athan selection to all prayers
     */
    fun setGlobalAthan(athanId: String) {
        viewModelScope.launch {
            // Apply to all prayers
            settingsRepository.setFajrAthanId(athanId)
            settingsRepository.setDhuhrAthanId(athanId)
            settingsRepository.setAsrAthanId(athanId)
            settingsRepository.setMaghribAthanId(athanId)
            settingsRepository.setIshaAthanId(athanId)

            _uiState.value = _uiState.value.copy(
                fajrAthanId = athanId,
                dhuhrAthanId = athanId,
                asrAthanId = athanId,
                maghribAthanId = athanId,
                ishaAthanId = athanId
            )

            Timber.d("Applied global Athan to all prayers: $athanId")
        }
    }

    fun setAthanMaxVolume(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAthanMaxVolume(enabled)
            _uiState.value = _uiState.value.copy(athanMaxVolume = enabled)
        }
    }

    fun setFlipToSilence(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFlipToSilenceAthan(enabled)
            _uiState.value = _uiState.value.copy(flipToSilence = enabled)
        }
    }

    fun downloadAthan(athan: Athan) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(downloadingAthanId = athan.id)
            try {
                val success = athanRepository.downloadAthan(athan)
                if (success) {
                    Timber.d("Athan downloaded: ${athan.name}")
                } else {
                    Timber.e("Failed to download athan: ${athan.name}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error downloading athan")
            } finally {
                _uiState.value = _uiState.value.copy(downloadingAthanId = null)
            }
        }
    }

    fun deleteAthan(athanId: String) {
        viewModelScope.launch {
            try {
                athanRepository.deleteDownloadedAthan(athanId)
                Timber.d("Athan deleted: $athanId")
            } catch (e: Exception) {
                Timber.e(e, "Error deleting athan")
            }
        }
    }

    fun previewAthan(athan: Athan) {
        // For preview, we stream directly from URL
        val url = AthanApi.getAthanAudioUrl(athan.id)
        _uiState.value = _uiState.value.copy(previewingAthanId = athan.id)
        athanPlayer.playAthan(
            path = url,
            athanId = athan.id,
            maximizeVolume = false
        ) {
            _uiState.value = _uiState.value.copy(previewingAthanId = null)
        }
    }

    fun stopPreview() {
        athanPlayer.stop()
        _uiState.value = _uiState.value.copy(previewingAthanId = null)
    }

    fun setNotifyMinutesBefore(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setPrayerNotificationMinutesBefore(minutes)
            _uiState.value = _uiState.value.copy(notifyMinutesBefore = minutes)
        }
    }

    fun setNotificationSound(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPrayerNotificationSound(enabled)
            _uiState.value = _uiState.value.copy(notificationSound = enabled)
        }
    }

    fun setNotificationVibrate(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPrayerNotificationVibrate(enabled)
            _uiState.value = _uiState.value.copy(notificationVibrate = enabled)
        }
    }

    // Calculation settings
    fun setCalculationMethod(method: CalculationMethod) {
        viewModelScope.launch {
            prayerTimesRepository.saveCalculationMethod(method)
            _uiState.value = _uiState.value.copy(calculationMethod = method)
            // Refresh prayer times with new method
            refreshPrayerTimes()
        }
    }

    fun setAsrJuristicMethod(method: AsrJuristicMethod) {
        viewModelScope.launch {
            settingsRepository.setAsrJuristicMethod(method.id)
            _uiState.value = _uiState.value.copy(asrJuristicMethod = method)
            // Refresh prayer times with new Asr method
            refreshPrayerTimes()
        }
    }

    fun setHijriDateAdjustment(adjustment: Int) {
        viewModelScope.launch {
            settingsRepository.setHijriDateAdjustment(adjustment)
            _uiState.value = _uiState.value.copy(hijriDateAdjustment = adjustment)
        }
    }

    /**
     * Refresh prayer times and reschedule notifications
     */
    private suspend fun refreshPrayerTimes() {
        val location = prayerTimesRepository.getSavedLocationSync() ?: return
        val method = prayerTimesRepository.getSavedCalculationMethod()
        val settings = settingsRepository.getCurrentSettings()
        val asrMethod = AsrJuristicMethod.fromId(settings.asrJuristicMethod)

        when (val result = prayerTimesRepository.getPrayerTimes(
            latitude = location.latitude,
            longitude = location.longitude,
            date = LocalDate.now(),
            method = method,
            asrMethod = asrMethod
        )) {
            is Resource.Success -> {
                result.data?.let { prayerTimes ->
                    // Reschedule notifications with updated times
                    prayerNotificationScheduler.schedulePrayerNotifications(prayerTimes)
                }
            }
            else -> { /* Ignore errors */ }
        }
    }

    override fun onCleared() {
        super.onCleared()
        athanPlayer.stop()
    }
}
