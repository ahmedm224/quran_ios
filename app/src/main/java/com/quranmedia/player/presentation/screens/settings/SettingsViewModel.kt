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
import com.quranmedia.player.data.worker.ReadingReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

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
        viewModelScope.launch {
            settingsRepository.setReadingTheme(theme)
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
}
