# Quran Media Player - Developer Documentation (Part 2)

**Continued from DEVELOPER_PART1.md**

---

## Table of Contents (Part 2)

11. [Prayer Times System](#11-prayer-times-system)
12. [Settings Management](#12-settings-management)
13. [Background Jobs](#13-background-jobs-workmanager)
14. [Technology Stack](#14-technology-stack)
15. [Build Configuration](#15-build-configuration)
16. [Android Manifest](#16-android-manifest)
17. [Critical Implementation Details](#17-critical-implementation-details)
18. [Data Flow Examples](#18-data-flow-examples)
19. [Common Development Tasks](#19-common-development-tasks)
20. [Troubleshooting Guide](#20-troubleshooting-guide)

---

## 11. Prayer Times System

### 11.1 Prayer Times Models

**File:** `domain/model/PrayerTimes.kt`

```kotlin
data class PrayerTimes(
    val date: LocalDate,
    val fajr: LocalTime,
    val sunrise: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime,
    val locationName: String,
    val calculationMethod: CalculationMethod,
    val hijriDate: HijriDate
)

data class HijriDate(
    val day: Int,
    val month: String,              // English name
    val monthArabic: String,
    val year: Int,
    val monthNumber: Int = 1
)

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String? = null,
    val countryName: String? = null,
    val isAutoDetected: Boolean = true
)
```

### 11.2 Prayer Enums

```kotlin
enum class PrayerType(val nameArabic: String, val nameEnglish: String) {
    FAJR("الفجر", "Fajr"),
    SUNRISE("الشروق", "Sunrise"),
    DHUHR("الظهر", "Dhuhr"),
    ASR("العصر", "Asr"),
    MAGHRIB("المغرب", "Maghrib"),
    ISHA("العشاء", "Isha")
}

enum class CalculationMethod(val id: Int, val nameArabic: String, val nameEnglish: String) {
    MAKKAH(4, "أم القرى - مكة", "Umm Al-Qura, Makkah"),
    MWL(3, "رابطة العالم الإسلامي", "Muslim World League"),
    EGYPT(5, "الهيئة المصرية", "Egyptian General Authority"),
    KARACHI(1, "جامعة كراتشي", "University of Karachi"),
    ISNA(2, "أمريكا الشمالية", "ISNA, North America"),
    TEHRAN(7, "طهران", "Tehran"),
    GULF(8, "الخليج", "Gulf Region"),
    KUWAIT(9, "الكويت", "Kuwait"),
    QATAR(10, "قطر", "Qatar"),
    DUBAI(16, "دبي", "Dubai"),
    SINGAPORE(11, "سنغافورة", "Singapore"),
    FRANCE(12, "فرنسا", "France"),
    TURKEY(13, "تركيا", "Turkey"),
    RUSSIA(14, "روسيا", "Russia");

    companion object {
        fun fromId(id: Int): CalculationMethod? = values().find { it.id == id }
    }
}

enum class AsrJuristicMethod(val id: Int, val nameArabic: String, val nameEnglish: String) {
    SHAFI(0, "الشافعي / المالكي / الحنبلي", "Shafi'i, Maliki, Hanbali"),
    HANAFI(1, "الحنفي", "Hanafi");

    companion object {
        fun fromId(id: Int): AsrJuristicMethod? = values().find { it.id == id }
    }
}
```

### 11.3 Prayer Notification Scheduler

**File:** `data/notification/PrayerNotificationScheduler.kt`

```kotlin
@Singleton
class PrayerNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedulePrayerNotifications(prayerTimes: PrayerTimes) {
        // Schedule for each prayer
        scheduleNotification(PrayerType.FAJR, prayerTimes.fajr, minutesBefore = 0)
        scheduleNotification(PrayerType.DHUHR, prayerTimes.dhuhr, minutesBefore = 0)
        scheduleNotification(PrayerType.ASR, prayerTimes.asr, minutesBefore = 0)
        scheduleNotification(PrayerType.MAGHRIB, prayerTimes.maghrib, minutesBefore = 0)
        scheduleNotification(PrayerType.ISHA, prayerTimes.isha, minutesBefore = 0)
    }

    private fun scheduleNotification(
        prayerType: PrayerType,
        prayerTime: LocalTime,
        minutesBefore: Int
    ) {
        val settings = settingsRepository.getCurrentSettings()
        val notificationMode = settingsRepository.getPrayerNotificationMode(prayerType.name)

        if (notificationMode == PrayerNotificationMode.SILENT) return

        // Calculate trigger time
        val adjustedTime = prayerTime.minusMinutes(minutesBefore.toLong())
        val triggerAtMillis = calculateTriggerTimeMillis(adjustedTime)

        // Create intent
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = "com.quranmedia.player.PRAYER_ALARM"
            putExtra("PRAYER_TYPE", prayerType.name)
            putExtra("NOTIFICATION_MODE", notificationMode.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            3000 + prayerType.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule exact alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                // Fallback to inexact alarm
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelAllNotifications() {
        PrayerType.values().forEach { cancelNotification(it) }
    }

    fun cancelNotification(prayerType: PrayerType) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            3000 + prayerType.ordinal,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }
}
```

**Key Features:**
- Uses `setExactAndAllowWhileIdle()` for precise timing even during Doze mode
- Android 12+ checks `canScheduleExactAlarms()` permission
- Unique request codes: `3000 + PrayerType.ordinal`

### 11.4 Athan Service

**File:** `media/service/AthanService.kt`

```kotlin
@AndroidEntryPoint
class AthanService : Service() {
    @Inject lateinit var athanPlayer: AthanPlayer
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var athanRepository: AthanRepository

    companion object {
        const val CHANNEL_ID = "athan_playback_channel"
        const val NOTIFICATION_ID = 3000

        const val ACTION_PLAY = "com.quranmedia.player.ACTION_PLAY_ATHAN"
        const val ACTION_STOP = "com.quranmedia.player.ACTION_STOP_ATHAN"

        fun startAthan(context: Context, athanId: String, prayerType: PrayerType) {
            val intent = Intent(context, AthanService::class.java).apply {
                action = ACTION_PLAY
                putExtra("ATHAN_ID", athanId)
                putExtra("PRAYER_TYPE", prayerType.name)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopAthan(context: Context) {
            val intent = Intent(context, AthanService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val athanId = intent.getStringExtra("ATHAN_ID")
                val prayerType = intent.getStringExtra("PRAYER_TYPE")?.let {
                    PrayerType.valueOf(it)
                }

                if (athanId != null && prayerType != null) {
                    playAthan(athanId, prayerType)
                }
            }
            ACTION_STOP -> {
                stopAthanPlayback()
            }
        }
        return START_NOT_STICKY
    }

    private fun playAthan(athanId: String, prayerType: PrayerType) {
        // Get local path
        val localPath = runBlocking {
            athanRepository.getDownloadedAthanLocalPath(athanId)
        }

        if (localPath != null) {
            // Start foreground service
            val notification = createPlayingNotification(prayerType, getAppLanguage())
            startForeground(NOTIFICATION_ID, notification)

            // Register flip-to-silence
            registerFlipToSilence()

            // Play athan
            athanPlayer.playAthan(
                path = localPath,
                athanId = athanId,
                maximizeVolume = settingsRepository.getCurrentSettings().athanMaxVolume,
                onCompletion = { stopSelf() }
            )
        } else {
            stopSelf()
        }
    }

    private fun registerFlipToSilence() {
        // Sensor-based flip detection
        if (settingsRepository.getCurrentSettings().flipToSilenceAthan) {
            // Register accelerometer + proximity sensors
            // On face-down flip, call stopAthanPlayback()
        }
    }

    private fun stopAthanPlayback() {
        athanPlayer.stop()
        unregisterFlipToSilence()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

**Features:**
- **Flip-to-Silence:** Accelerometer + proximity sensors detect face-down flip
- **Volume Control:** Maximizes alarm volume, restores after playback
- **Foreground Service:** Prevents killing during playback
- **Local File Requirement:** Athan MUST be downloaded (no streaming for notifications)

---

## 12. Settings Management

### 12.1 Proto DataStore Schema

**File:** `app/src/main/proto/settings.proto`

```protobuf
syntax = "proto3";

option java_package = "com.quranmedia.player.data.datastore";
option java_multiple_files = true;

message UserSettings {
    // Playback
    float playback_speed = 1;
    bool pitch_lock_enabled = 2;
    bool gapless_playback = 3;
    int32 volume_level = 4;
    bool normalize_audio = 5;

    // Seeking
    int32 small_seek_increment_ms = 6;
    int32 large_seek_increment_ms = 7;
    bool snap_to_ayah_enabled = 8;

    // Loop
    bool auto_loop_enabled = 9;
    int32 loop_count = 10;
    int32 ayah_repeat_count = 11;

    // UI
    bool waveform_enabled = 12;
    bool show_translation = 13;
    string translation_language = 14;
    bool dark_mode = 15;
    bool dynamic_colors = 16;
    string reading_theme = 17;

    // Download
    bool wifi_only_downloads = 18;
    bool auto_delete_after_playback = 19;
    int32 preferred_bitrate = 20;
    string preferred_audio_format = 21;

    // Notification
    bool show_notification = 22;
    bool show_playback_position = 23;
    bool android_auto_enabled = 24;
    int32 recent_bookmarks_count = 25;

    // Playback State
    string last_reciter_id = 26;
    int32 last_surah_number = 27;
    int64 last_position_ms = 28;
    bool resume_on_startup = 29;
    string selected_reciter_id = 30;

    // Accessibility
    bool large_text = 31;
    bool high_contrast = 32;
    int32 haptic_feedback_intensity = 33;
    bool keep_screen_on = 34;

    // Prayer Times
    bool prayer_notification_enabled = 35;
    int32 prayer_notification_minutes_before = 36;
    bool notify_fajr = 37;
    bool notify_dhuhr = 38;
    bool notify_asr = 39;
    bool notify_maghrib = 40;
    bool notify_isha = 41;
    string prayer_notification_sound = 42;
    bool prayer_notification_vibrate = 43;
    int32 prayer_calculation_method = 44;
    int32 asr_juristic_method = 45;

    // Athan Settings (per prayer)
    string fajr_notification_mode = 46;
    string dhuhr_notification_mode = 47;
    string asr_notification_mode = 48;
    string maghrib_notification_mode = 49;
    string isha_notification_mode = 50;

    string fajr_athan_id = 51;
    string dhuhr_athan_id = 52;
    string asr_athan_id = 53;
    string maghrib_athan_id = 54;
    string isha_athan_id = 55;

    bool athan_max_volume = 56;
    bool flip_to_silence_athan = 57;

    // Reading Reminders
    bool reading_reminder_enabled = 58;
    string reading_reminder_interval = 59;
    string quiet_hours_start = 60;
    string quiet_hours_end = 61;

    // Theme Colors
    int32 custom_background_color = 62;
    int32 custom_text_color = 63;
    int32 custom_header_color = 64;

    // App Language
    string app_language = 65;

    // Version Tracking
    int32 last_seen_version_code = 66;
    bool has_completed_initial_setup = 67;
}
```

### 12.2 Settings Serializer

**File:** `data/datastore/SettingsSerializer.kt`

```kotlin
object SettingsSerializer : Serializer<UserSettings> {
    override val defaultValue: UserSettings = UserSettings.getDefaultInstance()
        .toBuilder()
        .setPlaybackSpeed(1.0f)
        .setPitchLockEnabled(true)
        .setSmallSeekIncrementMs(250)
        .setLargeSeekIncrementMs(30000)
        .setSnapToAyahEnabled(true)
        .setGaplessPlayback(true)
        .setVolumeLevel(100)
        .setNormalizeAudio(true)
        .setAutoLoopEnabled(false)
        .setLoopCount(1)
        .setAyahRepeatCount(1)
        .setWaveformEnabled(true)
        .setShowTranslation(false)
        .setTranslationLanguage("en")
        .setDarkMode(false)
        .setDynamicColors(true)
        .setReadingTheme("LIGHT")
        .setWifiOnlyDownloads(true)
        .setPreferredBitrate(128)
        .setPreferredAudioFormat("MP3")
        .setPrayerCalculationMethod(4) // Umm Al-Qura
        .setAsrJuristicMethod(0) // Shafi'i
        .setAthanMaxVolume(true)
        .setFlipToSilenceAthan(true)
        .setAppLanguage("ar")
        .build()

    override suspend fun readFrom(input: InputStream): UserSettings {
        try {
            return UserSettings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: UserSettings, output: OutputStream) = t.writeTo(output)
}
```

### 12.3 Settings Repository

**File:** `data/repository/SettingsRepository.kt`

Uses **SharedPreferences** (not Proto DataStore in current implementation)

```kotlin
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("quran_settings", Context.MODE_PRIVATE)

    // Playback Settings
    fun setPlaybackSpeed(speed: Float) {
        prefs.edit().putFloat("playback_speed", speed).apply()
    }

    fun getPlaybackSpeed(): Float = prefs.getFloat("playback_speed", 1.0f)

    // App Language
    fun setAppLanguage(language: AppLanguage) {
        prefs.edit().putString("app_language", language.name).apply()
    }

    fun getAppLanguage(): AppLanguage {
        val languageStr = prefs.getString("app_language", "ARABIC")
        return AppLanguage.valueOf(languageStr ?: "ARABIC")
    }

    // Prayer Notification Mode
    fun getPrayerNotificationMode(prayerName: String): PrayerNotificationMode {
        val key = "${prayerName.lowercase()}_notification_mode"
        val modeStr = prefs.getString(key, "ATHAN")
        return PrayerNotificationMode.valueOf(modeStr ?: "ATHAN")
    }

    fun setPrayerNotificationMode(prayerName: String, mode: PrayerNotificationMode) {
        val key = "${prayerName.lowercase()}_notification_mode"
        prefs.edit().putString(key, mode.name).apply()
    }

    // Version Tracking
    fun shouldShowWhatsNew(currentVersionCode: Int): Boolean {
        val lastSeenVersion = prefs.getInt("last_seen_version_code", 0)
        val hasCompletedSetup = prefs.getBoolean("has_completed_initial_setup", false)
        return !hasCompletedSetup || lastSeenVersion < currentVersionCode
    }

    fun setLastSeenVersionCode(versionCode: Int) {
        prefs.edit().putInt("last_seen_version_code", versionCode).apply()
    }

    fun setCompletedInitialSetup(completed: Boolean) {
        prefs.edit().putBoolean("has_completed_initial_setup", completed).apply()
    }

    // [... 60+ more settings methods ...]
}
```

**Setting Enums:**

```kotlin
enum class AppLanguage { ARABIC, ENGLISH }

enum class ReadingTheme {
    LIGHT, SEPIA, NIGHT, PAPER, OCEAN, TAJWEED, CUSTOM
}

enum class ReminderInterval {
    OFF, ONE_HOUR, TWO_HOURS, THREE_HOURS, FOUR_HOURS, FIVE_HOURS, SIX_HOURS
}

enum class PrayerNotificationMode {
    ATHAN, NOTIFICATION, SILENT
}
```

---

## 13. Background Jobs (WorkManager)

### 13.1 QuranDataPopulatorWorker

**File:** `data/worker/QuranDataPopulatorWorker.kt`

**Purpose:** One-time population of 6,236 ayahs from bundled JSON on first app launch

```kotlin
@HiltWorker
class QuranDataPopulatorWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val surahDao: SurahDao,
    private val ayahDao: AyahDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Check if already populated
            val ayahCount = ayahDao.getAyahCount()
            if (ayahCount >= 6236) {
                Timber.d("Quran data already populated ($ayahCount ayahs)")
                return Result.success()
            }

            Timber.d("Starting Quran data population...")

            // Load metadata from XML
            val metadata = parseQuranMetadata()

            // Load Tanzil JSON from assets
            val quranJson = loadTanzilJson()

            // Populate surahs and ayahs
            populateDatabase(quranJson, metadata)

            Timber.d("Quran data population completed (${ayahDao.getAyahCount()} ayahs)")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error populating Quran data")
            Result.retry()
        }
    }

    private fun parseQuranMetadata(): QuranMetadata {
        val xmlParser = applicationContext.resources.openRawResource(R.raw.quran_metadata)
        // Parse XML with XmlPullParser to extract:
        // - Page divisions (604 pages)
        // - Juz divisions (30 juz)
        // - Manzil divisions (7 manzils)
        // - Ruku divisions (~556 rukus)
        // - Hizb quarter divisions (240 quarters)
        // - Sajda ayahs
        // Returns QuranMetadata with all division info
    }

    private fun loadTanzilJson(): JsonObject {
        val jsonStream = applicationContext.assets.open("tanzil_quran.json")
        // Parse JSON with Gson
        // Returns full Quran text organized by surah and ayah
    }

    private suspend fun populateDatabase(quranJson: JsonObject, metadata: QuranMetadata) {
        val surahs = mutableListOf<SurahEntity>()
        val ayahs = mutableListOf<AyahEntity>()

        var globalAyahNumber = 1

        for (surahNumber in 1..114) {
            // Create SurahEntity
            val surah = createSurahEntity(surahNumber, quranJson)
            surahs.add(surah)

            // Create AyahEntity for each ayah
            for (ayahNumber in 1..surah.ayahCount) {
                val ayah = createAyahEntity(
                    surahNumber,
                    ayahNumber,
                    globalAyahNumber,
                    quranJson,
                    metadata
                )
                ayahs.add(ayah)
                globalAyahNumber++
            }
        }

        // Batch insert
        surahDao.insertSurahs(surahs)
        ayahDao.insertAyahs(ayahs)
    }
}
```

**Work Configuration:**
- **Name:** `"quran_data_populator"`
- **Type:** OneTimeWorkRequest
- **Constraints:** CONNECTED network required
- **Runs:** Once on app startup via `QuranMediaApplication.onCreate()`
- **Skips:** If `ayahCount >= 6236`

### 13.2 ReciterDataPopulatorWorker

**File:** `data/worker/ReciterDataPopulatorWorker.kt`

**Purpose:** Fetch reciters from CloudLinqed API on first app launch

```kotlin
@HiltWorker
class ReciterDataPopulatorWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reciterDao: ReciterDao,
    private val audioVariantDao: AudioVariantDao,
    private val quranApi: QuranApi
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Check if reciters already exist
            val reciterCount = reciterDao.getReciterCount()
            if (reciterCount > 0) {
                Timber.d("Reciters already populated ($reciterCount reciters)")
                return Result.success()
            }

            // Fetch reciters from API
            val response = quranApi.getReciters()

            // Process each reciter
            val reciters = response.reciters.map { apiReciter ->
                ReciterEntity(
                    id = apiReciter.id,
                    name = apiReciter.name,
                    nameArabic = apiReciter.arabicName,
                    style = extractStyle(apiReciter.name),
                    version = "1.0",
                    imageUrl = null
                )
            }

            // Insert reciters
            reciterDao.insertReciters(reciters)

            // Create AudioVariant for each reciter × surah
            val audioVariants = mutableListOf<AudioVariantEntity>()
            for (reciter in response.reciters) {
                for (surahNumber in 1..114) {
                    val variant = AudioVariantEntity(
                        id = UUID.randomUUID().toString(),
                        reciterId = reciter.id,
                        surahNumber = surahNumber,
                        bitrate = 128,
                        format = "MP3",
                        url = "quranapi:${reciter.r2Path}",  // Special prefix for CloudLinqed API
                        localPath = null,
                        durationMs = 0,
                        fileSizeBytes = null,
                        hash = null
                    )
                    audioVariants.add(variant)
                }
            }

            // Batch insert audio variants
            audioVariantDao.insertAudioVariants(audioVariants)

            Timber.d("Reciter data population completed (${reciters.size} reciters)")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error populating reciter data")
            Result.retry()
        }
    }

    private fun extractStyle(name: String): String {
        return when {
            name.contains("Murattal", ignoreCase = true) -> "Murattal"
            name.contains("Mujawwad", ignoreCase = true) -> "Mujawwad"
            name.contains("Warsh", ignoreCase = true) -> "Warsh"
            else -> "Murattal"
        }
    }
}
```

**Work Configuration:**
- **Name:** `"reciter_data_populator"`
- **Type:** OneTimeWorkRequest
- **Constraints:** CONNECTED network
- **URL Handling:** Uses `"quranapi:"` prefix to denote CloudLinqed API

### 13.3 PrayerNotificationWorker

**File:** `data/worker/PrayerNotificationWorker.kt`

**Purpose:** Scheduled for each prayer time to trigger PrayerAlarmReceiver

**Work Configuration:**
- **Type:** OneTimeWorkRequest (scheduled for each prayer)
- **Triggered by:** AlarmManager via PrayerAlarmReceiver

### 13.4 ReadingReminderWorker

**File:** `data/worker/ReadingReminderWorker.kt`

**Purpose:** Periodic reminder to read Quran

**Work Configuration:**
- **Type:** PeriodicWorkRequest
- **Interval:** Based on user's `readingReminderInterval` setting (1-6 hours)
- **Quiet Hours:** Skips if current time is within `quietHoursStart` - `quietHoursEnd`

---

## 14. Technology Stack

### 14.1 Core Technologies

**Language & Tooling:**
- **Kotlin:** 1.5.8
- **Java Version:** 17
- **Gradle:** Latest (Kotlin DSL)
- **Android Gradle Plugin:** Compatible with SDK 35

### 14.2 Android SDK

- **Minimum SDK:** 27 (Android 8.1 Oreo)
- **Target SDK:** 35 (Android 15)
- **Compile SDK:** 35 (Android 15)

### 14.3 UI Framework

**Jetpack Compose:**
- **Compose BOM:** 2024.02.00
- **Compose Compiler Extension:** 1.5.8
- **Material Design 3:** Latest via BOM
- **Material Icons Extended:** For icon library
- **Navigation Compose:** 2.7.7

**Compose Libraries:**
```gradle
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-graphics")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")
implementation("androidx.navigation:navigation-compose:2.7.7")
```

### 14.4 Dependency Injection

**Hilt (Dagger):**
- **Version:** 2.50
- **KSP Processor:** Compatible with Kotlin 1.5.8

```gradle
implementation("com.google.dagger:hilt-android:2.50")
ksp("com.google.dagger:hilt-compiler:2.50")
implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
implementation("androidx.hilt:hilt-work:1.1.0")
ksp("androidx.hilt:hilt-compiler:1.1.0")
```

### 14.5 Database

**Room:**
- **Version:** 2.6.1
- **Room Runtime:** SQLite database wrapper
- **Room KTX:** Kotlin extensions + Coroutines
- **Room Compiler:** KSP-based annotation processor

```gradle
val roomVersion = "2.6.1"
implementation("androidx.room:room-runtime:$roomVersion")
implementation("androidx.room:room-ktx:$roomVersion")
ksp("androidx.room:room-compiler:$roomVersion")
```

### 14.6 Settings Storage

**Proto DataStore:**
- **DataStore:** 1.0.0
- **Protobuf:** 3.25.2 (Kotlin Lite)

```gradle
implementation("androidx.datastore:datastore:1.0.0")
implementation("com.google.protobuf:protobuf-kotlin-lite:3.25.2")
```

### 14.7 Networking

**Retrofit:**
- **Retrofit:** 2.9.0
- **Converter Gson:** 2.9.0
- **OkHttp:** 4.12.0
- **Logging Interceptor:** 4.12.0

```gradle
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
```

**JSON Parsing:**
- **Gson:** 2.10.1

### 14.8 Media Playback

**ExoPlayer (Media3):**
- **Version:** 1.2.1
- **ExoPlayer Core:** Audio/video playback
- **Media3 Session:** MediaSession integration
- **Media3 UI:** Player UI components

```gradle
implementation("androidx.media3:media3-exoplayer:1.2.1")
implementation("androidx.media3:media3-session:1.2.1")
implementation("androidx.media3:media3-ui:1.2.1")
```

**Android Media:**
- **androidx.media:** 1.7.0 (MediaBrowserService for Android Auto)

### 14.9 Background Processing

**WorkManager:**
- **Version:** 2.9.0

```gradle
implementation("androidx.work:work-runtime-ktx:2.9.0")
```

### 14.10 Image Loading

**Coil:**
- **Version:** 2.5.0
- **Coil Compose:** Async image loading
- **Coil SVG:** SVG support

```gradle
implementation("io.coil-kt:coil-compose:2.5.0")
implementation("io.coil-kt:coil-svg:2.5.0")
```

### 14.11 Lifecycle & Coroutines

**Lifecycle:**
- **Version:** 2.7.0
- **Runtime KTX:** Lifecycle-aware coroutines
- **ViewModel Compose:** Compose-integrated ViewModels
- **Runtime Compose:** Lifecycle effects

```gradle
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
```

**Coroutines:**
- **Version:** 1.7.3

```gradle
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
```

### 14.12 Location Services

**Google Play Services:**
- **Location:** 21.0.1

```gradle
implementation("com.google.android.gms:play-services-location:21.0.1")
```

### 14.13 Logging

**Timber:**
- **Version:** 5.0.1

```gradle
implementation("com.jakewharton.timber:timber:5.0.1")
```

### 14.14 Testing

**Unit Testing:**
- **JUnit:** 4.13.2
- **Coroutines Test:** 1.7.3
- **Room Testing:** 2.6.1

**Android Testing:**
- **Espresso:** 3.5.1
- **Compose UI Test:** From BOM

---

## 15. Build Configuration

### 15.1 Version Information

**File:** `app/build.gradle.kts`

```kotlin
android {
    namespace = "com.quranmedia.player"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.quranmedia.player"
        minSdk = 27
        targetSdk = 35
        versionCode = 16
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
}
```

### 15.2 Build Types

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true              // ProGuard obfuscation
        isShrinkResources = true            // Remove unused resources
        proguardFiles(
            getDefaultProguardFile("proguard-android.txt"),
            "proguard-rules.pro"
        )
    }
    debug {
        isMinifyEnabled = false             // Faster builds, easier debugging
    }
}
```

### 15.3 Compile Options

```kotlin
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlinOptions {
    jvmTarget = "17"
}
```

### 15.4 Build Features

```kotlin
buildFeatures {
    compose = true                          // Enable Jetpack Compose
    buildConfig = true                      // Generate BuildConfig class
}

composeOptions {
    kotlinCompilerExtensionVersion = "1.5.8"
}
```

### 15.5 Protobuf Configuration

```kotlin
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.2"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

// Ensure protobuf generates before KSP
tasks.withType<org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile<*>>().configureEach {
    dependsOn("generateDebugProto", "generateReleaseProto")
}

afterEvaluate {
    tasks.named("kspDebugKotlin") {
        dependsOn("generateDebugProto")
    }
    tasks.named("kspReleaseKotlin") {
        dependsOn("generateReleaseProto")
    }
}
```

### 15.6 Build Commands

**Debug Build:**
```bash
cmd.exe /c "gradlew.bat assembleDebug"
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

**Release Build:**
```bash
cmd.exe /c "gradlew.bat assembleRelease"
```
Output: `app/build/outputs/apk/release/app-release.apk`

**Clean Build:**
```bash
cmd.exe /c "gradlew.bat clean assembleDebug"
```

**Install Debug:**
```bash
cmd.exe /c "gradlew.bat installDebug"
```

**Run Tests:**
```bash
# Unit tests
cmd.exe /c "gradlew.bat test"

# Instrumentation tests (requires emulator/device)
cmd.exe /c "gradlew.bat connectedAndroidTest"
```

**Generate Proto:**
```bash
cmd.exe /c "gradlew.bat generateDebugProto"
```
(Use if you get "Unresolved reference: Settings" error)

---

## 16. Android Manifest

### 16.1 Permissions

**File:** `app/src/main/AndroidManifest.xml`

```xml
<!-- Audio & Download Permissions -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Location for Prayer Times -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Prayer Notifications -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

### 16.2 Application Configuration

```xml
<application
    android:name=".QuranMediaApplication"
    android:allowBackup="false"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="false"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@style/Theme.QuranMediaPlayer"
    android:enableOnBackInvokedCallback="true"
    android:localeConfig="@xml/locales_config"
    tools:targetApi="34">

    <!-- Android Auto metadata -->
    <meta-data
        android:name="com.google.android.gms.car.application"
        android:resource="@xml/automotive_app_desc" />

    <!-- ... Activities, Services, Receivers ... -->
</application>
```

### 16.3 Activities

```xml
<!-- Splash Activity (Launcher) -->
<activity
    android:name=".presentation.SplashActivity"
    android:exported="true"
    android:screenOrientation="portrait"
    android:theme="@style/Theme.Splash">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<!-- Main Activity -->
<activity
    android:name=".presentation.MainActivity"
    android:exported="false"
    android:screenOrientation="portrait"
    android:configChanges="screenSize|screenLayout|keyboardHidden"
    android:theme="@style/Theme.QuranMediaPlayer"
    android:windowSoftInputMode="adjustResize" />
```

### 16.4 Services

```xml
<!-- Media Playback Service -->
<service
    android:name=".media.service.QuranMediaService"
    android:enabled="true"
    android:exported="true"
    android:foregroundServiceType="mediaPlayback">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaSessionService" />
    </intent-filter>
</service>

<!-- Android Auto MediaBrowser Service -->
<service
    android:name=".media.auto.QuranMediaBrowserService"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.media.browse.MediaBrowserService" />
    </intent-filter>
    <meta-data
        android:name="android.media.session"
        android:value="true" />
</service>

<!-- Athan Playback Service -->
<service
    android:name=".media.service.AthanService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="mediaPlayback" />
```

### 16.5 Receivers

```xml
<!-- MediaButtonReceiver -->
<receiver
    android:name="androidx.media.session.MediaButtonReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MEDIA_BUTTON" />
    </intent-filter>
</receiver>

<!-- Prayer Alarm Receiver -->
<receiver
    android:name=".data.notification.PrayerAlarmReceiver"
    android:enabled="true"
    android:exported="false">
    <intent-filter>
        <action android:name="com.quranmedia.player.PRAYER_ALARM" />
        <action android:name="com.quranmedia.player.STOP_ATHAN" />
    </intent-filter>
</receiver>

<!-- Boot Completed Receiver -->
<receiver
    android:name=".data.notification.BootCompletedReceiver"
    android:enabled="true"
    android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

---

## 17. Critical Implementation Details

### 17.1 ExoPlayer Exact Seeking (CRITICAL)

**Location:** `media/player/QuranPlayer.kt:76`

```kotlin
private fun createPlayer(): ExoPlayer {
    return ExoPlayer.Builder(context)
        .setSeekParameters(SeekParameters.EXACT)  // NEVER change to approximate!
        .setHandleAudioBecomingNoisy(true)
        .build()
}
```

**Why CRITICAL:**
- **Precise Ayah Navigation:** Without exact seeking, ayah boundaries are inaccurate
- **AyahIndex Dependency:** Relies on millisecond timestamps in `ayah_index` table
- **User Experience:** Users expect to jump to exact ayah start/end

**Requirements:**
1. `ayah_index` table MUST be populated with millisecond timestamps
2. Audio files MUST support precise seeking (constant bitrate preferred)
3. `SeekParameters.EXACT` MUST be set (never use `CLOSEST`, `PREVIOUS`, or `NEXT`)

---

### 17.2 AyahIndex Table Required

**Table:** `ayah_index`

**Purpose:** Provides millisecond-precise timestamps for ayah-level seeking

**Schema:**
```kotlin
@Entity(
    tableName = "ayah_index",
    primaryKeys = ["reciterId", "surahNumber", "ayahNumber"]
)
data class AyahIndexEntity(
    val reciterId: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val startMs: Long,      // Millisecond timestamp (start)
    val endMs: Long         // Millisecond timestamp (end)
)
```

**Without this table:**
- Ayah detection won't work
- "Next Ayah" / "Previous Ayah" buttons won't function
- Ayah highlighting during playback won't work

---

### 17.3 Audio URL Pattern

**Primary Pattern (Al-Quran Cloud):**
```
https://cdn.islamic.network/quran/audio-surah/{bitrate}/{reciter}/{surah}.mp3
```

**Example:**
```
https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/1.mp3
```

**CloudLinqed API Pattern:**
```
quranapi:{r2Path}/{surah}.mp3
```

Prefix `"quranapi:"` is used in `AudioVariantEntity.url` to denote CloudLinqed API source.

---

### 17.4 Android Auto Requirements

**Critical Requirements:**

1. **All media items MUST have `isPlayable = true`:**
```kotlin
MediaBrowserCompat.MediaItem(
    MediaDescriptionCompat.Builder()
        .setMediaId(mediaId)
        .setTitle(title)
        .setSubtitle(subtitle)
        .setMediaUri(Uri.parse(audioUrl))  // REQUIRED for playable items
        .build(),
    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE  // REQUIRED
)
```

2. **Metadata must include mediaUri:**
```kotlin
.setMediaUri(Uri.parse(audioUrl))
```

3. **Browse hierarchy max depth:** 4 levels recommended

4. **automotive_app_desc.xml must declare media:**
```xml
<automotiveApp>
    <uses name="media" />
</automotiveApp>
```

5. **Session token MUST be set:**
```kotlin
override fun onCreate() {
    super.onCreate()
    sessionToken = mediaSession?.sessionCompatToken
}
```

---

### 17.5 RTL Support (Arabic-first)

**All Compose layouts use RTL:**
```kotlin
CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    // All UI content
}
```

**HorizontalPager for right-to-left swiping:**
```kotlin
HorizontalPager(
    state = pagerState,
    reverseLayout = true  // RTL: swipe right to go forward
)
```

**BiasAlignment for RTL mirror:**
```kotlin
.align(BiasAlignment(-1f, 0f))  // Start alignment (RTL-aware)
```

**Arabic strings:**
- Located in `res/values-ar/strings.xml`
- Automatic language switching based on `appLanguage` setting

---

### 17.6 Proto DataStore Settings

**Type-safe settings storage with Protocol Buffers:**

**Schema:** `app/src/main/proto/settings.proto`

**Serializer:** `data/datastore/SettingsSerializer.kt`

**Default values defined in serializer** (not in proto file)

**Access via SettingsRepository:**
```kotlin
settingsRepository.setPlaybackSpeed(1.5f)
val speed = settingsRepository.getPlaybackSpeed()
```

---

### 17.7 Database Migrations

**Current Version:** 7

**Always increment version for schema changes:**

```kotlin
@Database(
    entities = [/* ... */],
    version = 7  // Increment this
)
abstract class QuranDatabase : RoomDatabase()
```

**Provide migration in DatabaseModule.kt:**

```kotlin
.addMigrations(object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE ayahs ADD COLUMN textTajweed TEXT")
    }
})
```

**Recent Migrations:**
- **v6 → v7:** Added `textTajweed` column to `ayahs` table for Tajweed feature
- **v5 → v6:** Added daily tracker tables (`daily_activities`, `quran_progress`, `khatmah_goals`)

---

### 17.8 Athan Requirements

**Files MUST be pre-downloaded (no streaming for notifications):**

1. User downloads athan via `AthanSettingsScreen`
2. File stored in `DownloadedAthanEntity` with `localPath`
3. `AthanService` reads from local file only
4. If file doesn't exist, notification shows without audio

**Alarm permissions required on Android 12+:**
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    if (!alarmManager.canScheduleExactAlarms()) {
        // Request permission or show settings
    }
}
```

---

## 18. Data Flow Examples

### 18.1 Audio Playback Flow

```
1. User taps surah in SurahsScreen
   ↓
2. Navigate to PlayerScreenNew with (reciterId, surahNumber)
   PlayerScreenNew(reciterId = "ar.alafasy", surahNumber = 1)
   ↓
3. PlayerViewModel.loadAudio() is called
   ↓
4. PlayerViewModel queries QuranRepository:
   - getSurahByNumber(1) → Returns Surah("Al-Fatihah", 7 ayahs)
   - getReciterById("ar.alafasy") → Returns Reciter info
   - getAudioVariants("ar.alafasy", 1) → Returns AudioVariant with URL
   - getAyahIndices("ar.alafasy", 1) → Returns timestamps for ayahs 1-7
   ↓
5. PlayerViewModel calls PlaybackController.playAudio(
       reciterId = "ar.alafasy",
       surahNumber = 1,
       audioUrl = "https://cdn.islamic.network/...",
       startFromAyah = null
   )
   ↓
6. PlaybackController prepares MediaItem:
   MediaItem.Builder()
       .setUri(audioUrl)
       .setMediaMetadata(...)
       .build()
   ↓
7. QuranPlayer (ExoPlayer) loads audio:
   player.setMediaItem(mediaItem)
   player.prepare()
   player.play()
   ↓
8. Position updates trigger ayah boundary checks:
   - Every 100ms, check currentPosition against ayahIndices
   - If position >= startMs && position < endMs, update currentAyah
   ↓
9. UI recomposes with updated state:
   - Current ayah number
   - Current ayah text (auto-scrolling)
   - Progress bar shows ayah boundaries
```

---

### 18.2 Bookmark Creation Flow

```
1. User taps "Bookmark" button during playback
   ↓
2. PlayerViewModel.addBookmark() called
   bookmarkLabel = "Al-Fatihah, Ayah 3"
   ↓
3. ViewModel creates Bookmark domain model:
   Bookmark(
       id = UUID.randomUUID().toString(),
       reciterId = "ar.alafasy",
       surahNumber = 1,
       ayahNumber = 3,
       positionMs = 12350,
       label = "Al-Fatihah, Ayah 3"
   )
   ↓
4. ViewModel calls BookmarkRepository.insertBookmark(bookmark)
   ↓
5. BookmarkRepository maps to BookmarkEntity:
   BookmarkEntity(
       id = bookmark.id,
       reciterId = bookmark.reciterId,
       ...
       createdAt = System.currentTimeMillis()
   )
   ↓
6. BookmarkDao.insertBookmark(entity)
   @Insert(onConflict = OnConflictStrategy.REPLACE)
   ↓
7. Room inserts into bookmarks table
   ↓
8. UI shows "Bookmark saved" confirmation
   ↓
9. Later: User taps bookmark in BookmarksScreen
   ↓
10. Navigate to PlayerScreen with:
    reciterId = bookmark.reciterId
    surahNumber = bookmark.surahNumber
    startAyah = bookmark.ayahNumber
    ↓
11. PlaybackController seeks to saved position:
    player.seekTo(bookmark.positionMs)
```

---

### 18.3 Prayer Notification Flow

```
1. App startup → QuranMediaApplication.onCreate()
   ↓
2. User opens PrayerTimesScreen
   - Detects location (GPS or manual entry)
   - Selects calculation method (e.g., Umm Al-Qura)
   ↓
3. PrayerTimesViewModel.fetchPrayerTimes()
   ↓
4. Calls PrayerTimesRepository.getPrayerTimes(
       latitude, longitude, date, method
   )
   ↓
5. Repository queries AladhanApi:
   GET https://api.aladhan.com/v1/timings/28-12-2025
       ?latitude=21.4225&longitude=39.8262&method=4
   ↓
6. API returns PrayerTimesApiResponse with times:
   {
       "fajr": "05:47",
       "dhuhr": "12:22",
       "asr": "15:35",
       ...
   }
   ↓
7. Repository caches in PrayerTimesEntity (prayer_times_cache table)
   ↓
8. User enables notifications in PrayerTimesScreen
   ↓
9. PrayerNotificationScheduler.schedulePrayerNotifications(prayerTimes)
   ↓
10. For each prayer (Fajr, Dhuhr, Asr, Maghrib, Isha):
    - Get notification mode from settings (ATHAN/NOTIFICATION/SILENT)
    - Calculate trigger time (prayerTime - minutesBefore)
    - Create PendingIntent for PrayerAlarmReceiver
    - Schedule with AlarmManager.setExactAndAllowWhileIdle()
    ↓
11. At prayer time:
    AlarmManager triggers PrayerAlarmReceiver.onReceive()
    ↓
12. PrayerAlarmReceiver checks notification mode:
    - If ATHAN: Start AthanService with athanId
    - If NOTIFICATION: Show notification
    - If SILENT: Do nothing
    ↓
13. If Athan mode:
    AthanService.playAthan(athanId, prayerType)
    ↓
14. AthanService:
    - Starts foreground service with notification
    - Gets athan local path from DownloadedAthanDao
    - AthanPlayer plays audio
    - Registers flip-to-silence sensor
    - Auto-stops when athan completes
    ↓
15. Device reboot:
    BootCompletedReceiver.onReceive()
    - Fetches cached prayer times
    - Reschedules all alarms
```

---

### 18.4 Search Flow

```
1. User types "الله" in SearchScreen
   ↓
2. SearchViewModel.searchQuran("الله")
   ↓
3. ViewModel calls SearchRepository.searchQuran(
       query = "الله",
       surahNumber = null  // Search all surahs
   )
   ↓
4. Repository calls AyahDao.searchAyahs("الله")
   ↓
5. DAO executes SQL:
   SELECT * FROM ayahs
   WHERE textArabic LIKE '%الله%'
   ↓
6. Returns ~2,700 ayahs containing "الله"
   ↓
7. Repository maps to List<SearchResult>:
   SearchResult(
       surahNumber = 1,
       ayahNumber = 1,
       surahName = "Al-Fatihah",
       ayahText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
       page = 1
   )
   ↓
8. ViewModel updates state:
   _searchResults.value = results
   ↓
9. UI displays results in LazyColumn:
   - Surah name + ayah number
   - Ayah text (excerpt with query highlighted)
   - Page number
   ↓
10. User taps result
   ↓
11. Navigate to QuranReaderScreen with:
    page = result.page
    highlightSurah = result.surahNumber
    highlightAyah = result.ayahNumber
    ↓
12. QuranReaderScreen:
    - Loads page 1
    - Highlights ayah 1 in green
    - Scrolls to ayah position
```

---

### 18.5 Download Flow

```
1. User taps "Download" on surah in SurahsScreen
   ↓
2. DownloadsViewModel.downloadAudio(
       reciterId = "ar.alafasy",
       surahNumber = 1
   )
   ↓
3. ViewModel calls DownloadManager.downloadAudio(...)
   ↓
4. DownloadManager:
   - Checks if already downloaded (localPath != null)
   - If downloaded: return
   - Creates DownloadTaskEntity:
     DownloadTaskEntity(
         id = UUID.randomUUID().toString(),
         audioVariantId = variantId,
         reciterId = "ar.alafasy",
         surahNumber = 1,
         status = "PENDING",
         progress = 0.0f,
         bytesDownloaded = 0,
         bytesTotal = estimatedSize
     )
   - Inserts into download_tasks table
   ↓
5. Enqueues DownloadWorker via WorkManager:
   val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
       .setInputData(workDataOf("TASK_ID" to taskId))
       .setConstraints(...)
       .build()
   workManager.enqueue(workRequest)
   ↓
6. DownloadWorker (background job):
   - Fetches audio file from URL using OkHttp
   - Updates progress in database every 1 MB:
     downloadTaskDao.updateProgress(taskId, progress)
   - Saves to local storage:
     /data/data/com.quranmedia.player/files/audio/ar.alafasy/1.mp3
   - Updates AudioVariantEntity with localPath
   - Sets status to "COMPLETED"
   ↓
7. UI observes downloadState StateFlow:
   - Shows progress bar while downloading (0-100%)
   - Shows "Downloaded" icon when complete
   ↓
8. Subsequent playback:
   - AudioVariantDao.getAudioVariant() returns entity with localPath
   - PlaybackController checks if localPath exists:
     if (localPath != null && File(localPath).exists()) {
         // Play from local file
         Uri.fromFile(File(localPath))
     } else {
         // Stream from URL
         Uri.parse(audioUrl)
     }
```

---

## 19. Common Development Tasks

### 19.1 Adding a New Reciter

**Steps:**

1. **Insert ReciterEntity:**
```kotlin
val reciter = ReciterEntity(
    id = "ar.newreciter",
    name = "New Reciter Name",
    nameArabic = "اسم القارئ الجديد",
    style = "Murattal",
    version = "1.0",
    imageUrl = null
)
reciterDao.insertReciter(reciter)
```

2. **Create AudioVariant for each surah (1-114):**
```kotlin
for (surahNumber in 1..114) {
    val variant = AudioVariantEntity(
        id = UUID.randomUUID().toString(),
        reciterId = "ar.newreciter",
        surahNumber = surahNumber,
        bitrate = 128,
        format = "MP3",
        url = "https://cdn.example.com/audio/${surahNumber}.mp3",
        localPath = null,
        durationMs = 0,
        fileSizeBytes = null,
        hash = null
    )
    audioVariantDao.insertAudioVariant(variant)
}
```

3. **(Optional) Populate AyahIndex for precise seeking:**
```kotlin
// Requires millisecond timestamps for each ayah
// Usually obtained from API or manual timing
for (ayahNumber in 1..ayahCount) {
    val index = AyahIndexEntity(
        reciterId = "ar.newreciter",
        surahNumber = surahNumber,
        ayahNumber = ayahNumber,
        startMs = calculateStartMs(ayahNumber),
        endMs = calculateEndMs(ayahNumber)
    )
    ayahIndexDao.insertAyahIndex(index)
}
```

---

### 19.2 Adding a New Screen with ViewModel

**Steps:**

1. **Create Screen route in `presentation/navigation/Screen.kt`:**
```kotlin
sealed class Screen(val route: String) {
    // ... existing routes ...

    object NewFeature : Screen("newFeature/{param}") {
        fun createRoute(param: String) = "newFeature/$param"
    }
}
```

2. **Create ViewModel in `presentation/screens/newfeature/NewFeatureViewModel.kt`:**
```kotlin
@HiltViewModel
class NewFeatureViewModel @Inject constructor(
    private val repository: SomeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val param: String = checkNotNull(savedStateHandle["param"])

    private val _uiState = MutableStateFlow(NewFeatureUiState())
    val uiState: StateFlow<NewFeatureUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getData(param).collect { data ->
                _uiState.value = _uiState.value.copy(data = data)
            }
        }
    }

    fun onAction(action: NewFeatureAction) {
        // Handle user actions
    }
}

data class NewFeatureUiState(
    val data: SomeData? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

3. **Create Composable in `presentation/screens/newfeature/NewFeatureScreen.kt`:**
```kotlin
@Composable
fun NewFeatureScreen(
    param: String,
    viewModel: NewFeatureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("New Feature") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()
                uiState.error != null -> Text("Error: ${uiState.error}")
                else -> {
                    // Display data
                    uiState.data?.let { data ->
                        Text(data.toString())
                    }
                }
            }
        }
    }
}
```

4. **Add route to `presentation/navigation/QuranNavGraph.kt`:**
```kotlin
composable(
    route = Screen.NewFeature.route,
    arguments = listOf(navArgument("param") { type = NavType.StringType })
) { backStackEntry ->
    val param = backStackEntry.arguments?.getString("param") ?: ""
    NewFeatureScreen(param = param)
}
```

5. **Navigate to screen:**
```kotlin
navController.navigate(Screen.NewFeature.createRoute("someValue"))
```

---

### 19.3 Modifying Database Schema (Migration)

**Example: Adding a new column to `ayahs` table**

1. **Update entity in `data/database/entity/AyahEntity.kt`:**
```kotlin
@Entity(tableName = "ayahs")
data class AyahEntity(
    // ... existing fields ...
    val newField: String? = null  // Add new field
)
```

2. **Increment database version in `QuranDatabase.kt`:**
```kotlin
@Database(
    entities = [/* ... */],
    version = 8  // Was 7, now 8
)
abstract class QuranDatabase : RoomDatabase()
```

3. **Provide migration in `di/DatabaseModule.kt`:**
```kotlin
private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE ayahs ADD COLUMN newField TEXT")
    }
}

@Provides
@Singleton
fun provideQuranDatabase(@ApplicationContext context: Context): QuranDatabase {
    return Room.databaseBuilder(context, QuranDatabase::class.java, "quran_database")
        .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
        .build()
}
```

4. **Test migration:**
   - Install old version (v7) on emulator
   - Upgrade to new version (v8)
   - Verify data intact and new column exists

---

### 19.4 Testing Audio Playback (Logcat Filtering)

**Filter for media-related logs:**

```bash
adb logcat | grep -E "QuranPlayer|ExoPlayer|MediaSession|PlaybackController"
```

**Key logs to watch:**

```
QuranPlayer: ExoPlayer created with HTTP streaming support
QuranPlayer: Setting media item: https://cdn.islamic.network/.../1.mp3
QuranPlayer: Seeking to ayah 3 at 12350ms
ExoPlayer: Player state changed: READY
PlaybackController: Current ayah updated: 3
```

**Common errors:**

```
ExoPlayer: Source error
  → Check URL validity, internet permission, network connectivity

QuranPlayer: AyahIndex not found for reciter ar.alafasy, surah 1
  → AyahIndex table not populated

PlaybackController: Audio variant not found
  → AudioVariant missing from database
```

---

### 19.5 Testing Android Auto (DHU Setup)

**Desktop Head Unit (DHU) - Android Auto Emulator**

1. **Enable Developer Mode in Android Auto app:**
   - Open Android Auto app on phone
   - Tap version number 10 times
   - Enable "Developer settings"
   - Enable "Unknown sources"

2. **Forward DHU port:**
```bash
adb forward tcp:5277 tcp:5277
```

3. **Run DHU** (download from Android developer site):
```bash
desktop-head-unit.exe
```

4. **Check MediaBrowser logs:**
```bash
adb logcat | grep -E "MediaBrowser|QuranMediaService|onGetRoot|onLoadChildren"
```

**Key logs:**

```
QuranMediaBrowserService: onGetRoot: clientPackage=com.google.android.projection.gearhead
QuranMediaBrowserService: Loading 50 reciters for Android Auto
QuranMediaBrowserService: Added 114 media items to surah list
```

**Common issues:**

```
App not visible in Auto:
  → Check automotive_app_desc.xml exists
  → Verify manifest has android.gms.car.application metadata

Can't browse:
  → Verify sessionToken is set in onCreate()
  → Check onLoadChildren() returns valid MediaItems

No playback:
  → Ensure mediaUri is set in MediaDescriptionCompat
  → Verify isPlayable flag is set
  → Check audio URL is accessible
```

---

## 20. Troubleshooting Guide

### 20.1 Build Errors

**Error: "Unresolved reference: Settings"**
```
Cause: Proto DataStore classes not generated
Fix: Run: cmd.exe /c "gradlew.bat generateDebugProto"
```

**Error: "Cannot find implementation for QuranDatabase"**
```
Cause: Room annotation processor not running
Fix: Clean and rebuild project
     Ensure KSP is configured correctly
     Check all DAOs are abstract functions
```

**Error: "'B' is not a valid file-based resource name character"**
```
Cause: Resource file has uppercase letter
Fix: Rename to lowercase (e.g., Bismillah.png → bismillah.png)
```

---

### 20.2 Runtime Errors

**Error: "ExoPlayer Source error"**
```
Cause: Invalid audio URL or network issue
Fix: - Check URL in browser
     - Verify INTERNET permission in manifest
     - Check network connectivity
     - Check logcat for HTTP response codes
```

**Error: "Hilt Missing @Inject constructor"**
```
Cause: Dependency not properly provided
Fix: - Verify @Singleton or @ViewModelScoped on class
     - Check @Provides method exists in module
     - Ensure @HiltAndroidApp on Application class
     - Clean and rebuild
```

**Error: "Prayer notifications not firing"**
```
Cause: Alarm permissions or scheduling issue
Fix: - Check SCHEDULE_EXACT_ALARM permission granted (Android 12+)
     - Verify alarmManager.canScheduleExactAlarms() returns true
     - Check PrayerNotificationScheduler logic
     - Verify PrayerAlarmReceiver is registered in manifest
```

---

### 20.3 Android Auto Issues

**Issue: App not showing in Android Auto**
```
Cause: Missing automotive configuration
Fix: - Check automotive_app_desc.xml exists in res/xml/
     - Verify manifest has:
       <meta-data
           android:name="com.google.android.gms.car.application"
           android:resource="@xml/automotive_app_desc" />
     - Restart DHU or reconnect car
```

**Issue: Can browse but can't play**
```
Cause: Missing mediaUri in media items
Fix: - Ensure MediaDescriptionCompat.Builder() has:
       .setMediaUri(Uri.parse(audioUrl))
     - Verify isPlayable flag is set
     - Check audio URL is accessible
```

**Issue: Search not working in Auto**
```
Cause: VoiceSearchHandler not parsing query
Fix: - Check VoiceSearchHandler.parseAndFindSurah() logic
     - Add logging to see incoming queries
     - Test with common queries: "Play Surah Al-Baqarah"
```

---

### 20.4 Database Issues

**Issue: Ayah count is 0 after first launch**
```
Cause: QuranDataPopulatorWorker failed
Fix: - Check WorkManager logs
     - Verify tanzil_quran.json exists in assets/
     - Verify quran_metadata.xml exists in raw/
     - Manually trigger worker:
       workManager.enqueue(QuranDataPopulatorWorker)
```

**Issue: Search returns no results**
```
Cause: Database not populated or query issue
Fix: - Verify ayahCount: ayahDao.getAyahCount()
     - Check if query has Arabic diacritics (may need to strip)
     - Test with simple query: "الله"
```

---

### 20.5 Media Playback Issues

**Issue: Ayah seeking not precise**
```
Cause: SeekParameters not set to EXACT or AyahIndex missing
Fix: - Verify QuranPlayer.kt has: setSeekParameters(SeekParameters.EXACT)
     - Check AyahIndex table is populated:
       ayahIndexDao.getAyahIndices("ar.alafasy", 1)
     - Verify timestamps are in milliseconds (not seconds)
```

**Issue: Audio playback is choppy**
```
Cause: Network buffering or ExoPlayer configuration
Fix: - Check network speed
     - Increase buffer size in ExoPlayer config
     - Use downloaded audio instead of streaming
     - Check for main thread blocking (use Profiler)
```

---

**End of DEVELOPER_PART2.md**

**Continue to DEVELOPER_PART1.md for sections 1-10.**
