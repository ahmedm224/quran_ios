package com.quranmedia.player.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// App language enum
enum class AppLanguage(val code: String) {
    ARABIC("ar"),
    ENGLISH("en")
}

// Reading theme options
enum class ReadingTheme(
    val id: String,
    val arabicLabel: String,
    val englishLabel: String
) {
    LIGHT("light", "فاتح", "Light"),
    SEPIA("sepia", "بني فاتح", "Sepia"),
    NIGHT("night", "ليلي", "Night"),
    PAPER("paper", "ورقي", "Paper"),
    OCEAN("ocean", "محيط", "Ocean"),
    TAJWEED("tajweed", "تجويد", "Tajweed"),
    CUSTOM("custom", "مخصص", "Custom");

    fun getLabel(language: AppLanguage): String = when (language) {
        AppLanguage.ARABIC -> arabicLabel
        AppLanguage.ENGLISH -> englishLabel
    }
}

// Reading reminder interval options (in hours)
enum class ReminderInterval(val hours: Int, val arabicLabel: String, val englishLabel: String) {
    OFF(0, "إيقاف", "Off"),
    ONE_HOUR(1, "ساعة واحدة", "1 hour"),
    TWO_HOURS(2, "ساعتان", "2 hours"),
    THREE_HOURS(3, "3 ساعات", "3 hours"),
    FOUR_HOURS(4, "4 ساعات", "4 hours"),
    FIVE_HOURS(5, "5 ساعات", "5 hours"),
    SIX_HOURS(6, "6 ساعات", "6 hours");

    fun getLabel(language: AppLanguage): String = when (language) {
        AppLanguage.ARABIC -> arabicLabel
        AppLanguage.ENGLISH -> englishLabel
    }
}

// Prayer notification mode options
enum class PrayerNotificationMode(val arabicLabel: String, val englishLabel: String) {
    ATHAN("أذان", "Athan"),
    NOTIFICATION("إشعار", "Notification"),
    SILENT("صامت", "Silent");

    fun getLabel(language: AppLanguage): String = when (language) {
        AppLanguage.ARABIC -> arabicLabel
        AppLanguage.ENGLISH -> englishLabel
    }
}

// Temporary data class to replace protobuf UserSettings
data class UserSettings(
    val playbackSpeed: Float = 1.0f,
    val pitchLockEnabled: Boolean = true,
    val smallSeekIncrementMs: Int = 250,
    val largeSeekIncrementMs: Int = 30000,
    val snapToAyahEnabled: Boolean = true,
    val gaplessPlayback: Boolean = true,
    val volumeLevel: Int = 100,
    val normalizeAudio: Boolean = false,
    val autoLoopEnabled: Boolean = false,
    val loopCount: Int = 1,
    val waveformEnabled: Boolean = true,
    val showTranslation: Boolean = false,
    val translationLanguage: String = "en",
    val darkMode: Boolean = false,
    val dynamicColors: Boolean = true,
    val wifiOnlyDownloads: Boolean = false,
    val autoDeleteAfterPlayback: Boolean = false,
    val preferredBitrate: Int = 128,
    val preferredAudioFormat: String = "mp3",
    val lastReciterId: String = "",
    val lastSurahNumber: Int = 1,
    val lastPositionMs: Long = 0L,
    val selectedReciterId: String = "minshawy-murattal",  // Default reciter (CloudLinqed API format)
    val resumeOnStartup: Boolean = false,
    val largeText: Boolean = false,
    val highContrast: Boolean = false,
    val hapticFeedbackIntensity: Int = 50,
    val continuousPlaybackEnabled: Boolean = true,
    val appLanguage: AppLanguage = AppLanguage.ARABIC,  // Default to Arabic
    val readingTheme: ReadingTheme = ReadingTheme.LIGHT,  // Reading theme
    // Custom theme colors (ARGB format)
    val customBackgroundColor: Long = 0xFFFFFFF5,  // Default cream
    val customTextColor: Long = 0xFF000000,  // Default black
    val customHeaderColor: Long = 0xFF2E7D32,  // Default Islamic green
    // Reading reminder settings
    val readingReminderEnabled: Boolean = false,
    val readingReminderInterval: ReminderInterval = ReminderInterval.TWO_HOURS,
    val quietHoursStart: Int = 22,  // 10 PM (24-hour format)
    val quietHoursEnd: Int = 7,     // 7 AM (24-hour format)
    val lastReadingTimestamp: Long = 0L,  // Track when user last read
    // Ayah repeat setting (1 = normal, 2 = repeat twice, 3 = repeat three times)
    val ayahRepeatCount: Int = 1,
    // Keep screen on during reading/playback
    val keepScreenOn: Boolean = true,  // Enabled by default to prevent screen from sleeping during recitation
    // Prayer times settings
    val prayerCalculationMethod: Int = 4,  // Umm Al-Qura (Makkah) default
    val asrJuristicMethod: Int = 0,  // 0 = Shafi (standard), 1 = Hanafi
    // Prayer notification settings
    val prayerNotificationEnabled: Boolean = false,
    val prayerNotificationMinutesBefore: Int = 0,  // 0, 5, 10, 15, 30 minutes before
    // Per-prayer enable flags (synced with notification modes - false when SILENT)
    val notifyFajr: Boolean = false,
    val notifyDhuhr: Boolean = false,
    val notifyAsr: Boolean = false,
    val notifyMaghrib: Boolean = false,
    val notifyIsha: Boolean = false,
    val prayerNotificationSound: Boolean = true,
    val prayerNotificationVibrate: Boolean = true,
    // Hijri date adjustment (-3 to +3 days)
    val hijriDateAdjustment: Int = 0,
    // Per-prayer athan settings
    // Notification mode for each prayer (ATHAN, NOTIFICATION, SILENT)
    // Default to SILENT - user must explicitly enable (triggers permission request)
    val fajrNotificationMode: PrayerNotificationMode = PrayerNotificationMode.SILENT,
    val dhuhrNotificationMode: PrayerNotificationMode = PrayerNotificationMode.SILENT,
    val asrNotificationMode: PrayerNotificationMode = PrayerNotificationMode.SILENT,
    val maghribNotificationMode: PrayerNotificationMode = PrayerNotificationMode.SILENT,
    val ishaNotificationMode: PrayerNotificationMode = PrayerNotificationMode.SILENT,
    // Selected athan ID for each prayer (empty = use default)
    val fajrAthanId: String = "1a014366658c",  // Abdulbasit Abdusamad (Egypt) - default
    val dhuhrAthanId: String = "1a014366658c",
    val asrAthanId: String = "1a014366658c",
    val maghribAthanId: String = "1a014366658c",
    val ishaAthanId: String = "1a014366658c",
    // Athan playback settings
    val athanMaxVolume: Boolean = false,  // Play athan at maximum volume - default OFF
    val flipToSilenceAthan: Boolean = false,  // Flip phone face-down to stop athan - default OFF
    // Version tracking and first-run
    val lastSeenVersionCode: Int = 0,  // Last version code that showed What's New screen
    val hasCompletedInitialSetup: Boolean = false  // Whether user completed first-run setup
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("quran_settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(loadFromPrefs())

    val settings: Flow<UserSettings> = _settings

    private fun loadFromPrefs(): UserSettings {
        return UserSettings(
            playbackSpeed = prefs.getFloat("playbackSpeed", 1.0f),
            pitchLockEnabled = prefs.getBoolean("pitchLockEnabled", true),
            smallSeekIncrementMs = prefs.getInt("smallSeekIncrementMs", 250),
            largeSeekIncrementMs = prefs.getInt("largeSeekIncrementMs", 30000),
            snapToAyahEnabled = prefs.getBoolean("snapToAyahEnabled", true),
            gaplessPlayback = prefs.getBoolean("gaplessPlayback", true),
            volumeLevel = prefs.getInt("volumeLevel", 100),
            normalizeAudio = prefs.getBoolean("normalizeAudio", false),
            autoLoopEnabled = prefs.getBoolean("autoLoopEnabled", false),
            loopCount = prefs.getInt("loopCount", 1),
            waveformEnabled = prefs.getBoolean("waveformEnabled", true),
            showTranslation = prefs.getBoolean("showTranslation", false),
            translationLanguage = prefs.getString("translationLanguage", "en") ?: "en",
            darkMode = prefs.getBoolean("darkMode", false),
            dynamicColors = prefs.getBoolean("dynamicColors", true),
            wifiOnlyDownloads = prefs.getBoolean("wifiOnlyDownloads", false),
            autoDeleteAfterPlayback = prefs.getBoolean("autoDeleteAfterPlayback", false),
            preferredBitrate = prefs.getInt("preferredBitrate", 128),
            preferredAudioFormat = prefs.getString("preferredAudioFormat", "mp3") ?: "mp3",
            lastReciterId = prefs.getString("lastReciterId", "") ?: "",
            lastSurahNumber = prefs.getInt("lastSurahNumber", 1),
            lastPositionMs = prefs.getLong("lastPositionMs", 0L),
            selectedReciterId = prefs.getString("selectedReciterId", "minshawy-murattal") ?: "minshawy-murattal",
            resumeOnStartup = prefs.getBoolean("resumeOnStartup", false),
            largeText = prefs.getBoolean("largeText", false),
            highContrast = prefs.getBoolean("highContrast", false),
            hapticFeedbackIntensity = prefs.getInt("hapticFeedbackIntensity", 50),
            continuousPlaybackEnabled = prefs.getBoolean("continuousPlaybackEnabled", true),
            appLanguage = AppLanguage.entries.find { it.code == prefs.getString("appLanguage", "ar") } ?: AppLanguage.ARABIC,
            readingTheme = ReadingTheme.entries.find { it.id == prefs.getString("readingTheme", "light") } ?: ReadingTheme.LIGHT,
            readingReminderEnabled = prefs.getBoolean("readingReminderEnabled", false),
            readingReminderInterval = ReminderInterval.entries.find { it.hours == prefs.getInt("readingReminderInterval", 2) } ?: ReminderInterval.TWO_HOURS,
            quietHoursStart = prefs.getInt("quietHoursStart", 22),
            quietHoursEnd = prefs.getInt("quietHoursEnd", 7),
            lastReadingTimestamp = prefs.getLong("lastReadingTimestamp", 0L),
            ayahRepeatCount = prefs.getInt("ayahRepeatCount", 1),
            keepScreenOn = prefs.getBoolean("keepScreenOn", true),
            prayerCalculationMethod = prefs.getInt("prayerCalculationMethod", 4),
            asrJuristicMethod = prefs.getInt("asrJuristicMethod", 0),
            prayerNotificationEnabled = prefs.getBoolean("prayerNotificationEnabled", false),
            prayerNotificationMinutesBefore = prefs.getInt("prayerNotificationMinutesBefore", 0),
            notifyFajr = prefs.getBoolean("notifyFajr", false),
            notifyDhuhr = prefs.getBoolean("notifyDhuhr", false),
            notifyAsr = prefs.getBoolean("notifyAsr", false),
            notifyMaghrib = prefs.getBoolean("notifyMaghrib", false),
            notifyIsha = prefs.getBoolean("notifyIsha", false),
            prayerNotificationSound = prefs.getBoolean("prayerNotificationSound", true),
            prayerNotificationVibrate = prefs.getBoolean("prayerNotificationVibrate", true),
            hijriDateAdjustment = prefs.getInt("hijriDateAdjustment", 0),
            fajrNotificationMode = PrayerNotificationMode.entries.find { it.name == prefs.getString("fajrNotificationMode", "SILENT") } ?: PrayerNotificationMode.SILENT,
            dhuhrNotificationMode = PrayerNotificationMode.entries.find { it.name == prefs.getString("dhuhrNotificationMode", "SILENT") } ?: PrayerNotificationMode.SILENT,
            asrNotificationMode = PrayerNotificationMode.entries.find { it.name == prefs.getString("asrNotificationMode", "SILENT") } ?: PrayerNotificationMode.SILENT,
            maghribNotificationMode = PrayerNotificationMode.entries.find { it.name == prefs.getString("maghribNotificationMode", "SILENT") } ?: PrayerNotificationMode.SILENT,
            ishaNotificationMode = PrayerNotificationMode.entries.find { it.name == prefs.getString("ishaNotificationMode", "SILENT") } ?: PrayerNotificationMode.SILENT,
            fajrAthanId = prefs.getString("fajrAthanId", "1a014366658c") ?: "1a014366658c",
            dhuhrAthanId = prefs.getString("dhuhrAthanId", "1a014366658c") ?: "1a014366658c",
            asrAthanId = prefs.getString("asrAthanId", "1a014366658c") ?: "1a014366658c",
            maghribAthanId = prefs.getString("maghribAthanId", "1a014366658c") ?: "1a014366658c",
            ishaAthanId = prefs.getString("ishaAthanId", "1a014366658c") ?: "1a014366658c",
            athanMaxVolume = prefs.getBoolean("athanMaxVolume", false),
            flipToSilenceAthan = prefs.getBoolean("flipToSilenceAthan", false),
            lastSeenVersionCode = prefs.getInt("lastSeenVersionCode", 0),
            hasCompletedInitialSetup = prefs.getBoolean("hasCompletedInitialSetup", false)
        )
    }

    private fun saveToPrefs(settings: UserSettings) {
        prefs.edit().apply {
            putFloat("playbackSpeed", settings.playbackSpeed)
            putBoolean("pitchLockEnabled", settings.pitchLockEnabled)
            putInt("smallSeekIncrementMs", settings.smallSeekIncrementMs)
            putInt("largeSeekIncrementMs", settings.largeSeekIncrementMs)
            putBoolean("snapToAyahEnabled", settings.snapToAyahEnabled)
            putBoolean("gaplessPlayback", settings.gaplessPlayback)
            putInt("volumeLevel", settings.volumeLevel)
            putBoolean("normalizeAudio", settings.normalizeAudio)
            putBoolean("autoLoopEnabled", settings.autoLoopEnabled)
            putInt("loopCount", settings.loopCount)
            putBoolean("waveformEnabled", settings.waveformEnabled)
            putBoolean("showTranslation", settings.showTranslation)
            putString("translationLanguage", settings.translationLanguage)
            putBoolean("darkMode", settings.darkMode)
            putBoolean("dynamicColors", settings.dynamicColors)
            putBoolean("wifiOnlyDownloads", settings.wifiOnlyDownloads)
            putBoolean("autoDeleteAfterPlayback", settings.autoDeleteAfterPlayback)
            putInt("preferredBitrate", settings.preferredBitrate)
            putString("preferredAudioFormat", settings.preferredAudioFormat)
            putString("lastReciterId", settings.lastReciterId)
            putInt("lastSurahNumber", settings.lastSurahNumber)
            putLong("lastPositionMs", settings.lastPositionMs)
            putString("selectedReciterId", settings.selectedReciterId)
            putBoolean("resumeOnStartup", settings.resumeOnStartup)
            putBoolean("largeText", settings.largeText)
            putBoolean("highContrast", settings.highContrast)
            putInt("hapticFeedbackIntensity", settings.hapticFeedbackIntensity)
            putBoolean("continuousPlaybackEnabled", settings.continuousPlaybackEnabled)
            putString("appLanguage", settings.appLanguage.code)
            putString("readingTheme", settings.readingTheme.id)
            putBoolean("readingReminderEnabled", settings.readingReminderEnabled)
            putInt("readingReminderInterval", settings.readingReminderInterval.hours)
            putInt("quietHoursStart", settings.quietHoursStart)
            putInt("quietHoursEnd", settings.quietHoursEnd)
            putLong("lastReadingTimestamp", settings.lastReadingTimestamp)
            putInt("ayahRepeatCount", settings.ayahRepeatCount)
            putBoolean("keepScreenOn", settings.keepScreenOn)
            putInt("prayerCalculationMethod", settings.prayerCalculationMethod)
            putInt("asrJuristicMethod", settings.asrJuristicMethod)
            putBoolean("prayerNotificationEnabled", settings.prayerNotificationEnabled)
            putInt("prayerNotificationMinutesBefore", settings.prayerNotificationMinutesBefore)
            putBoolean("notifyFajr", settings.notifyFajr)
            putBoolean("notifyDhuhr", settings.notifyDhuhr)
            putBoolean("notifyAsr", settings.notifyAsr)
            putBoolean("notifyMaghrib", settings.notifyMaghrib)
            putBoolean("notifyIsha", settings.notifyIsha)
            putBoolean("prayerNotificationSound", settings.prayerNotificationSound)
            putBoolean("prayerNotificationVibrate", settings.prayerNotificationVibrate)
            putInt("hijriDateAdjustment", settings.hijriDateAdjustment)
            putString("fajrNotificationMode", settings.fajrNotificationMode.name)
            putString("dhuhrNotificationMode", settings.dhuhrNotificationMode.name)
            putString("asrNotificationMode", settings.asrNotificationMode.name)
            putString("maghribNotificationMode", settings.maghribNotificationMode.name)
            putString("ishaNotificationMode", settings.ishaNotificationMode.name)
            putString("fajrAthanId", settings.fajrAthanId)
            putString("dhuhrAthanId", settings.dhuhrAthanId)
            putString("asrAthanId", settings.asrAthanId)
            putString("maghribAthanId", settings.maghribAthanId)
            putString("ishaAthanId", settings.ishaAthanId)
            putBoolean("athanMaxVolume", settings.athanMaxVolume)
            putBoolean("flipToSilenceAthan", settings.flipToSilenceAthan)
            putInt("lastSeenVersionCode", settings.lastSeenVersionCode)
            putBoolean("hasCompletedInitialSetup", settings.hasCompletedInitialSetup)
            apply()
        }
    }

    private fun updateSettings(update: UserSettings.() -> UserSettings) {
        val newSettings = _settings.value.update()
        _settings.value = newSettings
        saveToPrefs(newSettings)
    }

    // Playback settings
    suspend fun setPlaybackSpeed(speed: Float) {
        updateSettings { copy(playbackSpeed = speed) }
    }

    suspend fun setPitchLockEnabled(enabled: Boolean) {
        updateSettings { copy(pitchLockEnabled = enabled) }
    }

    // Seeking settings
    suspend fun setSmallSeekIncrement(incrementMs: Int) {
        updateSettings { copy(smallSeekIncrementMs = incrementMs) }
    }

    suspend fun setLargeSeekIncrement(incrementMs: Int) {
        updateSettings { copy(largeSeekIncrementMs = incrementMs) }
    }

    suspend fun setSnapToAyahEnabled(enabled: Boolean) {
        updateSettings { copy(snapToAyahEnabled = enabled) }
    }

    // Audio settings
    suspend fun setGaplessPlayback(enabled: Boolean) {
        updateSettings { copy(gaplessPlayback = enabled) }
    }

    suspend fun setVolumeLevel(level: Int) {
        updateSettings { copy(volumeLevel = level) }
    }

    suspend fun setNormalizeAudio(enabled: Boolean) {
        updateSettings { copy(normalizeAudio = enabled) }
    }

    // Loop settings
    suspend fun setAutoLoopEnabled(enabled: Boolean) {
        updateSettings { copy(autoLoopEnabled = enabled) }
    }

    suspend fun setLoopCount(count: Int) {
        updateSettings { copy(loopCount = count) }
    }

    // UI settings
    suspend fun setWaveformEnabled(enabled: Boolean) {
        updateSettings { copy(waveformEnabled = enabled) }
    }

    suspend fun setShowTranslation(show: Boolean) {
        updateSettings { copy(showTranslation = show) }
    }

    suspend fun setTranslationLanguage(language: String) {
        updateSettings { copy(translationLanguage = language) }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        updateSettings { copy(darkMode = enabled) }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        updateSettings { copy(dynamicColors = enabled) }
    }

    // Download settings
    suspend fun setWifiOnlyDownloads(enabled: Boolean) {
        updateSettings { copy(wifiOnlyDownloads = enabled) }
    }

    suspend fun setAutoDeleteAfterPlayback(enabled: Boolean) {
        updateSettings { copy(autoDeleteAfterPlayback = enabled) }
    }

    suspend fun setPreferredBitrate(bitrate: Int) {
        updateSettings { copy(preferredBitrate = bitrate) }
    }

    suspend fun setPreferredAudioFormat(format: String) {
        updateSettings { copy(preferredAudioFormat = format) }
    }

    // Last playback state
    suspend fun updateLastPlaybackState(reciterId: String, surahNumber: Int, positionMs: Long) {
        updateSettings { copy(lastReciterId = reciterId, lastSurahNumber = surahNumber, lastPositionMs = positionMs) }
    }

    // Selected reciter preference
    suspend fun setSelectedReciterId(reciterId: String) {
        updateSettings { copy(selectedReciterId = reciterId) }
    }

    fun getSelectedReciterId(): String = _settings.value.selectedReciterId

    suspend fun setResumeOnStartup(enabled: Boolean) {
        updateSettings { copy(resumeOnStartup = enabled) }
    }

    // Accessibility
    suspend fun setLargeText(enabled: Boolean) {
        updateSettings { copy(largeText = enabled) }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        updateSettings { copy(highContrast = enabled) }
    }

    suspend fun setHapticFeedbackIntensity(intensity: Int) {
        updateSettings { copy(hapticFeedbackIntensity = intensity) }
    }

    // Continuous playback
    suspend fun setContinuousPlaybackEnabled(enabled: Boolean) {
        updateSettings { copy(continuousPlaybackEnabled = enabled) }
    }

    // Language settings
    suspend fun setAppLanguage(language: AppLanguage) {
        updateSettings { copy(appLanguage = language) }
    }

    fun getAppLanguage(): AppLanguage = _settings.value.appLanguage

    // Reading theme settings
    suspend fun setReadingTheme(theme: ReadingTheme) {
        updateSettings { copy(readingTheme = theme) }
    }

    suspend fun setCustomBackgroundColor(color: Long) {
        updateSettings { copy(customBackgroundColor = color) }
    }

    suspend fun setCustomTextColor(color: Long) {
        updateSettings { copy(customTextColor = color) }
    }

    suspend fun setCustomHeaderColor(color: Long) {
        updateSettings { copy(customHeaderColor = color) }
    }

    // Reading reminder settings
    suspend fun setReadingReminderEnabled(enabled: Boolean) {
        updateSettings { copy(readingReminderEnabled = enabled) }
    }

    suspend fun setReadingReminderInterval(interval: ReminderInterval) {
        updateSettings { copy(readingReminderInterval = interval) }
    }

    suspend fun setQuietHoursStart(hour: Int) {
        updateSettings { copy(quietHoursStart = hour.coerceIn(0, 23)) }
    }

    suspend fun setQuietHoursEnd(hour: Int) {
        updateSettings { copy(quietHoursEnd = hour.coerceIn(0, 23)) }
    }

    suspend fun updateLastReadingTimestamp() {
        updateSettings { copy(lastReadingTimestamp = System.currentTimeMillis()) }
    }

    // Ayah repeat setting
    suspend fun setAyahRepeatCount(count: Int) {
        updateSettings { copy(ayahRepeatCount = count.coerceIn(1, 3)) }
    }

    // Keep screen on setting
    suspend fun setKeepScreenOn(enabled: Boolean) {
        updateSettings { copy(keepScreenOn = enabled) }
    }

    // Prayer times settings
    suspend fun setPrayerCalculationMethod(methodId: Int) {
        updateSettings { copy(prayerCalculationMethod = methodId) }
    }

    suspend fun setAsrJuristicMethod(methodId: Int) {
        updateSettings { copy(asrJuristicMethod = methodId) }
    }

    // Prayer notification settings
    suspend fun setPrayerNotificationEnabled(enabled: Boolean) {
        updateSettings { copy(prayerNotificationEnabled = enabled) }
    }

    suspend fun setPrayerNotificationMinutesBefore(minutes: Int) {
        updateSettings { copy(prayerNotificationMinutesBefore = minutes) }
    }

    suspend fun setNotifyFajr(enabled: Boolean) {
        updateSettings { copy(notifyFajr = enabled) }
    }

    suspend fun setNotifyDhuhr(enabled: Boolean) {
        updateSettings { copy(notifyDhuhr = enabled) }
    }

    suspend fun setNotifyAsr(enabled: Boolean) {
        updateSettings { copy(notifyAsr = enabled) }
    }

    suspend fun setNotifyMaghrib(enabled: Boolean) {
        updateSettings { copy(notifyMaghrib = enabled) }
    }

    suspend fun setNotifyIsha(enabled: Boolean) {
        updateSettings { copy(notifyIsha = enabled) }
    }

    suspend fun setPrayerNotificationSound(enabled: Boolean) {
        updateSettings { copy(prayerNotificationSound = enabled) }
    }

    suspend fun setPrayerNotificationVibrate(enabled: Boolean) {
        updateSettings { copy(prayerNotificationVibrate = enabled) }
    }

    suspend fun setHijriDateAdjustment(days: Int) {
        updateSettings { copy(hijriDateAdjustment = days.coerceIn(-3, 3)) }
    }

    // Per-prayer athan settings
    suspend fun setFajrNotificationMode(mode: PrayerNotificationMode) {
        updateSettings { copy(fajrNotificationMode = mode) }
    }

    suspend fun setDhuhrNotificationMode(mode: PrayerNotificationMode) {
        updateSettings { copy(dhuhrNotificationMode = mode) }
    }

    suspend fun setAsrNotificationMode(mode: PrayerNotificationMode) {
        updateSettings { copy(asrNotificationMode = mode) }
    }

    suspend fun setMaghribNotificationMode(mode: PrayerNotificationMode) {
        updateSettings { copy(maghribNotificationMode = mode) }
    }

    suspend fun setIshaNotificationMode(mode: PrayerNotificationMode) {
        updateSettings { copy(ishaNotificationMode = mode) }
    }

    suspend fun setFajrAthanId(athanId: String) {
        updateSettings { copy(fajrAthanId = athanId) }
    }

    suspend fun setDhuhrAthanId(athanId: String) {
        updateSettings { copy(dhuhrAthanId = athanId) }
    }

    suspend fun setAsrAthanId(athanId: String) {
        updateSettings { copy(asrAthanId = athanId) }
    }

    suspend fun setMaghribAthanId(athanId: String) {
        updateSettings { copy(maghribAthanId = athanId) }
    }

    suspend fun setIshaAthanId(athanId: String) {
        updateSettings { copy(ishaAthanId = athanId) }
    }

    suspend fun setAthanMaxVolume(enabled: Boolean) {
        updateSettings { copy(athanMaxVolume = enabled) }
    }

    suspend fun setFlipToSilenceAthan(enabled: Boolean) {
        updateSettings { copy(flipToSilenceAthan = enabled) }
    }

    /**
     * Get the notification mode for a specific prayer
     */
    fun getPrayerNotificationMode(prayerName: String): PrayerNotificationMode {
        val settings = _settings.value
        return when (prayerName.uppercase()) {
            "FAJR" -> settings.fajrNotificationMode
            "DHUHR" -> settings.dhuhrNotificationMode
            "ASR" -> settings.asrNotificationMode
            "MAGHRIB" -> settings.maghribNotificationMode
            "ISHA" -> settings.ishaNotificationMode
            else -> PrayerNotificationMode.NOTIFICATION
        }
    }

    /**
     * Get the athan ID for a specific prayer
     */
    fun getPrayerAthanId(prayerName: String): String {
        val settings = _settings.value
        return when (prayerName.uppercase()) {
            "FAJR" -> settings.fajrAthanId
            "DHUHR" -> settings.dhuhrAthanId
            "ASR" -> settings.asrAthanId
            "MAGHRIB" -> settings.maghribAthanId
            "ISHA" -> settings.ishaAthanId
            else -> settings.fajrAthanId  // Default
        }
    }

    fun getCurrentSettings(): UserSettings = _settings.value

    /**
     * Save a custom recitation preset.
     * Maximum of 3 presets allowed. Stored in SharedPreferences as JSON.
     */
    fun saveRecitationPreset(name: String, settings: com.quranmedia.player.domain.model.CustomRecitationSettings) {
        val presets = getRecitationPresetsSync().toMutableList()

        // Check if preset with same name exists
        val existingIndex = presets.indexOfFirst { it.name == name }
        if (existingIndex >= 0) {
            // Update existing preset
            presets[existingIndex] = com.quranmedia.player.domain.model.RecitationPreset(name, settings)
        } else {
            // Add new preset, keeping max 3
            if (presets.size >= 3) {
                presets.removeAt(0) // Remove oldest
            }
            presets.add(com.quranmedia.player.domain.model.RecitationPreset(name, settings))
        }

        // Save to SharedPreferences as JSON strings
        prefs.edit().apply {
            presets.forEachIndexed { index, preset ->
                putString("recitation_preset_${index}_name", preset.name)
                putInt("recitation_preset_${index}_start_surah", preset.settings.startSurahNumber)
                putInt("recitation_preset_${index}_start_ayah", preset.settings.startAyahNumber)
                putInt("recitation_preset_${index}_end_surah", preset.settings.endSurahNumber)
                putInt("recitation_preset_${index}_end_ayah", preset.settings.endAyahNumber)
                putInt("recitation_preset_${index}_ayah_repeat", preset.settings.ayahRepeatCount)
                putInt("recitation_preset_${index}_group_repeat", preset.settings.groupRepeatCount)
                putFloat("recitation_preset_${index}_speed", preset.settings.speed)
            }
            // Clear any presets beyond the count
            for (i in presets.size until 3) {
                remove("recitation_preset_${i}_name")
            }
            apply()
        }
    }

    /**
     * Get all saved custom recitation presets synchronously.
     */
    private fun getRecitationPresetsSync(): List<com.quranmedia.player.domain.model.RecitationPreset> {
        val presets = mutableListOf<com.quranmedia.player.domain.model.RecitationPreset>()
        for (i in 0 until 3) {
            val name = prefs.getString("recitation_preset_${i}_name", null) ?: continue
            val settings = com.quranmedia.player.domain.model.CustomRecitationSettings(
                startSurahNumber = prefs.getInt("recitation_preset_${i}_start_surah", 1),
                startAyahNumber = prefs.getInt("recitation_preset_${i}_start_ayah", 1),
                endSurahNumber = prefs.getInt("recitation_preset_${i}_end_surah", 1),
                endAyahNumber = prefs.getInt("recitation_preset_${i}_end_ayah", 1),
                ayahRepeatCount = prefs.getInt("recitation_preset_${i}_ayah_repeat", 1),
                groupRepeatCount = prefs.getInt("recitation_preset_${i}_group_repeat", 1),
                speed = prefs.getFloat("recitation_preset_${i}_speed", 1.0f)
            )
            presets.add(com.quranmedia.player.domain.model.RecitationPreset(name, settings))
        }
        return presets
    }

    /**
     * Get all saved custom recitation presets as Flow.
     */
    fun getRecitationPresets(): Flow<List<com.quranmedia.player.domain.model.RecitationPreset>> {
        return kotlinx.coroutines.flow.flow {
            emit(getRecitationPresetsSync())
        }
    }

    /**
     * Delete a custom recitation preset by name.
     */
    fun deleteRecitationPreset(name: String) {
        val presets = getRecitationPresetsSync().filter { it.name != name }

        prefs.edit().apply {
            // Clear all presets first
            for (i in 0 until 3) {
                remove("recitation_preset_${i}_name")
                remove("recitation_preset_${i}_start_surah")
                remove("recitation_preset_${i}_start_ayah")
                remove("recitation_preset_${i}_end_surah")
                remove("recitation_preset_${i}_end_ayah")
                remove("recitation_preset_${i}_ayah_repeat")
                remove("recitation_preset_${i}_group_repeat")
                remove("recitation_preset_${i}_speed")
            }

            // Re-save remaining presets
            presets.forEachIndexed { index, preset ->
                putString("recitation_preset_${index}_name", preset.name)
                putInt("recitation_preset_${index}_start_surah", preset.settings.startSurahNumber)
                putInt("recitation_preset_${index}_start_ayah", preset.settings.startAyahNumber)
                putInt("recitation_preset_${index}_end_surah", preset.settings.endSurahNumber)
                putInt("recitation_preset_${index}_end_ayah", preset.settings.endAyahNumber)
                putInt("recitation_preset_${index}_ayah_repeat", preset.settings.ayahRepeatCount)
                putInt("recitation_preset_${index}_group_repeat", preset.settings.groupRepeatCount)
                putFloat("recitation_preset_${index}_speed", preset.settings.speed)
            }
            apply()
        }
    }

    // Version tracking and first-run setup
    suspend fun setLastSeenVersionCode(versionCode: Int) {
        updateSettings { copy(lastSeenVersionCode = versionCode) }
    }

    suspend fun setCompletedInitialSetup(completed: Boolean) {
        updateSettings { copy(hasCompletedInitialSetup = completed) }
    }

    /**
     * Determines if the What's New screen should be shown.
     * Returns true if this is the first install OR if the app has been upgraded to a new version.
     */
    fun shouldShowWhatsNew(currentVersionCode: Int): Boolean {
        val settings = _settings.value
        // Show on first install OR version upgrade
        return !settings.hasCompletedInitialSetup ||
               settings.lastSeenVersionCode < currentVersionCode
    }
}
