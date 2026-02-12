package com.quranmedia.player.presentation.screens.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.data.repository.ReadingTheme
import com.quranmedia.player.data.repository.ReminderInterval
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.data.repository.UserSettings
import com.quranmedia.player.data.source.FontDownloadProgress
import com.quranmedia.player.data.source.QCFFontDownloadManager
import com.quranmedia.player.data.worker.ReadingReminderWorker
import com.quranmedia.player.data.repository.TafseerRepository
import com.quranmedia.player.domain.model.AvailableTafseers
import com.quranmedia.player.domain.model.TafseerDownload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val fontDownloadManager: QCFFontDownloadManager,
    private val tafseerRepository: TafseerRepository
) : ViewModel() {

    val settings = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    // Font download progress states
    val v4DownloadProgress: StateFlow<FontDownloadProgress> = fontDownloadManager.v4DownloadProgress

    // Check if fonts are downloaded
    fun isV4Downloaded(): Boolean = fontDownloadManager.isV4Downloaded()

    // Get downloaded font sizes
    fun getV4FontsSize(): Long = fontDownloadManager.getV4FontsSize()
    fun formatSize(bytes: Long): String = fontDownloadManager.formatSize(bytes)

    // V4 download prompt for Tajweed theme
    private val _showV4DownloadPrompt = MutableStateFlow(false)
    val showV4DownloadPrompt: StateFlow<Boolean> = _showV4DownloadPrompt.asStateFlow()

    fun dismissV4DownloadPrompt() {
        _showV4DownloadPrompt.value = false
    }

    fun downloadV4AndApplyTajweed(baseUrl: String = "https://alfurqan.online/api/v1/fonts") {
        _showV4DownloadPrompt.value = false
        viewModelScope.launch {
            fontDownloadManager.downloadV4Fonts(baseUrl)
            // After download completes, apply Tajweed theme
            if (fontDownloadManager.isV4Downloaded()) {
                settingsRepository.setReadingTheme(ReadingTheme.TAJWEED)
                val currentSettings = settingsRepository.getCurrentSettings()
                if (currentSettings.useQCFFont) {
                    settingsRepository.setQCFTajweedMode(true)
                }
            }
        }
    }

    // Tafseer download states
    val downloadedTafseers = tafseerRepository.getDownloadedTafseers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _tafseerDownloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val tafseerDownloadProgress = _tafseerDownloadProgress.asStateFlow()

    private val _downloadingTafseerId = MutableStateFlow<String?>(null)
    val downloadingTafseerId = _downloadingTafseerId.asStateFlow()

    // Get all available tafseers
    fun getAvailableTafseers() = AvailableTafseers.tafseers

    // Check if a tafseer is downloaded
    suspend fun isTafseerDownloaded(tafseerId: String): Boolean {
        return tafseerRepository.isDownloaded(tafseerId)
    }

    // Download a tafseer
    fun downloadTafseer(tafseerId: String) {
        viewModelScope.launch {
            _downloadingTafseerId.value = tafseerId
            _tafseerDownloadProgress.value = _tafseerDownloadProgress.value + (tafseerId to 0f)

            val success = tafseerRepository.downloadTafseer(tafseerId) { progress ->
                _tafseerDownloadProgress.value = _tafseerDownloadProgress.value + (tafseerId to progress)
            }

            _downloadingTafseerId.value = null
            _tafseerDownloadProgress.value = _tafseerDownloadProgress.value - tafseerId
        }
    }

    // Delete a tafseer
    fun deleteTafseer(tafseerId: String) {
        viewModelScope.launch {
            tafseerRepository.deleteTafseer(tafseerId)
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReadingReminderEnabled(enabled)

            if (enabled) {
                // Schedule the reminder with current interval
                val interval = settingsRepository.getCurrentSettings().readingReminderInterval
                ReadingReminderWorker.scheduleReminder(context, interval)
            } else {
                // Cancel reminders
                ReadingReminderWorker.cancelReminder(context)
            }
        }
    }

    fun setReminderInterval(interval: ReminderInterval) {
        viewModelScope.launch {
            settingsRepository.setReadingReminderInterval(interval)

            // Reschedule with new interval
            if (settingsRepository.getCurrentSettings().readingReminderEnabled) {
                ReadingReminderWorker.scheduleReminder(context, interval)
            }
        }
    }

    fun setQuietHours(startHour: Int, endHour: Int) {
        viewModelScope.launch {
            settingsRepository.setQuietHoursStart(startHour)
            settingsRepository.setQuietHoursEnd(endHour)
        }
    }

    fun setReadingTheme(theme: ReadingTheme) {
        // Block Tajweed theme if V4 fonts not downloaded — show download prompt instead
        if (theme == ReadingTheme.TAJWEED && !fontDownloadManager.isV4Downloaded()) {
            _showV4DownloadPrompt.value = true
            return
        }

        viewModelScope.launch {
            settingsRepository.setReadingTheme(theme)

            // If TAJWEED theme selected and Mushaf mode is active, enable V4 (Tajweed) font
            // If other theme selected, disable V4 mode
            val currentSettings = settingsRepository.getCurrentSettings()
            if (currentSettings.useQCFFont) {
                settingsRepository.setQCFTajweedMode(theme == ReadingTheme.TAJWEED)
            }
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeepScreenOn(enabled)
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            // Save language preference
            settingsRepository.setAppLanguage(language)

            // Apply language change using AppCompat
            // This works on all API levels 27+ and uses native API on Android 13+
            val localeList = LocaleListCompat.forLanguageTags(language.code)
            AppCompatDelegate.setApplicationLocales(localeList)
        }
    }

    fun setCustomBackgroundColor(color: Long) {
        viewModelScope.launch {
            settingsRepository.setCustomBackgroundColor(color)
        }
    }

    fun setCustomTextColor(color: Long) {
        viewModelScope.launch {
            settingsRepository.setCustomTextColor(color)
        }
    }

    fun setCustomHeaderColor(color: Long) {
        viewModelScope.launch {
            settingsRepository.setCustomHeaderColor(color)
        }
    }

    fun setReciteRealTimeAssessment(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReciteRealTimeAssessment(enabled)
        }
    }

    fun setReciteHapticOnMistake(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReciteHapticOnMistake(enabled)
        }
    }

    fun setUseBoldFont(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUseBoldFont(enabled)
        }
    }

    fun setQCFTajweedMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setQCFTajweedMode(enabled)
        }
    }

    // Font download methods
    fun downloadV4Fonts(baseUrl: String = "https://alfurqan.online/api/v1/fonts") {
        viewModelScope.launch {
            fontDownloadManager.downloadV4Fonts(baseUrl)
        }
    }

    fun deleteV4Fonts() {
        viewModelScope.launch {
            fontDownloadManager.deleteV4Fonts()
        }
    }
}
