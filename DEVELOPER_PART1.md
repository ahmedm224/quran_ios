# Quran Media Player - Developer Documentation (Part 1)

**Version:** 2.0.0 (Build 16)
**Last Updated:** December 2025
**Architecture:** Clean Architecture + MVVM

---

## Table of Contents (Part 1)

1. [Project Overview](#1-project-overview)
2. [Complete Feature List](#2-complete-feature-list)
3. [Architecture Overview](#3-architecture-overview)
4. [Complete File Structure](#4-complete-file-structure)
5. [Database Schema](#5-database-schema-room-v7)
6. [DAOs (Data Access Objects)](#6-daos-data-access-objects)
7. [Repositories](#7-repositories)
8. [API Integration](#8-api-integration)
9. [Media Playback System](#9-media-playback-system)
10. [UI/Presentation Layer](#10-uipresentation-layer)

---

## 1. Project Overview

### Application Details
- **Name:** Quran Media Player (Alfurqan - الفرقان)
- **Package:** `com.quranmedia.player`
- **Version:** 2.0.0
- **Version Code:** 16
- **Platform:** Android
- **Language:** Kotlin 1.5.8
- **Java Version:** 17

### SDK Requirements
- **Minimum SDK:** 27 (Android 8.1 Oreo)
- **Target SDK:** 35 (Android 15)
- **Compile SDK:** 35

### Architecture
- **Pattern:** Clean Architecture with MVVM
- **UI Framework:** Jetpack Compose (Material Design 3)
- **Dependency Injection:** Hilt (Dagger)
- **Database:** Room 2.6.1 (SQLite)
- **Settings:** Proto DataStore
- **Reactive:** Kotlin Coroutines + Flow

---

## 2. Complete Feature List

### 2.1 Listening Mode (Audio Playback)

**Core Playback Features:**
- Stream Quran recitations from 50+ reciters
- Download complete surahs for offline playback
- **Precise ayah-level seeking** with millisecond accuracy using ExoPlayer
- Playback speed control (1x, 1.25x, 1.5x, 2x)
- Pitch lock to maintain original reciter's tone
- Gapless playback for smooth transitions
- Background playback with media notifications

**Navigation & Control:**
- Ayah-by-ayah navigation (next/previous ayah buttons)
- Nudge controls (±250ms, ±1s for fine-tuning)
- Snap-to-ayah feature (auto-align to nearest ayah boundary)
- Auto-scrolling Arabic text display with marquee for long ayahs
- Ayah end marker (۝) to indicate verse completion

**Repeat & Loop Features:**
- Single ayah repeat (1-99 times or infinite)
- Multiple ayah loops (select start/end ayah)
- A-B Loop functionality (set custom start/end points)
- Group repetition (repeat groups of ayahs with custom settings)

**Bookmarks & Downloads:**
- Save listening positions at any ayah
- Custom labels for bookmarks
- Quick resume from bookmark
- Background download queue with progress tracking
- Pause/resume/cancel downloads

**Display:**
- Compact fixed bottom control bar
- Current ayah text with Arabic diacritics
- Surah and reciter metadata
- Playback progress with ayah boundaries marked

### 2.2 Reading Mode (Quran Reader)

**Page Display:**
- Full 604-page Mushaf-style Quran display
- Page-by-page horizontal swipe navigation (RTL-native)
- High-quality page rendering with proper Arabic typography
- Scheherazade font for authentic Quranic text

**Display Modes:**
- **Fit Screen:** Compact view, entire page visible
- **Current Size:** Larger text with scrolling
- **Split View:** Extra large text, 50% page per swipe

**Reading Themes:**
- Light theme (white background)
- Sepia theme (cream background)
- Night theme (dark background)
- Paper theme (textured background)
- Ocean theme (blue tones)
- **Tajweed theme** (color-coded rules of recitation)
- Custom theme (user-defined colors)

**Interactive Features:**
- **Tap-to-play:** Tap any ayah to start audio playback from that verse
- **Ayah highlighting:** Green text color during audio playback
- **Auto-follow:** Page automatically turns during audio playback
- **Multiple page bookmarks:** Save and return to any page
- Zoom controls for text size adjustment

**Navigation:**
- Index by Pages (1-604)
- Index by Surahs (1-114)
- Index by Juz (1-30)
- Direct page input
- Quick jump to surah start pages

### 2.3 Android Auto Integration

**Features:**
- Browse surahs and reciters hands-free
- Voice search support:
  - "Play Surah Al-Baqarah"
  - "Play Surah 2"
  - "Play Fatiha by Afasy"
- Media notifications with playback controls
- Metadata display (surah name, ayah, reciter)
- Background playback with system integration
- Custom commands (next ayah, previous ayah, etc.)

**Browse Hierarchy:**
```
Root
├── Browse by Reciter
│   └── [Reciter Names]
│       └── [Surahs 1-114]
├── Browse by Surah
│   └── [All 114 Surahs]
└── Bookmarks
    └── [Saved Positions]
```

### 2.4 Prayer Times & Athan

**Prayer Time Calculation:**
- Real-time prayer times for current location
- 14 calculation methods:
  - Umm Al-Qura (Makkah) - Default
  - Muslim World League (MWL)
  - Egyptian General Authority
  - University of Karachi
  - ISNA (North America)
  - Tehran, Gulf, Kuwait, Qatar, Dubai
  - Singapore, France, Turkey, Russia
- Asr juristic method selection (Shafi'i or Hanafi)
- Manual location override with city search
- Automatic location detection via GPS

**Prayer Notifications:**
- Customizable notification mode per prayer:
  - **Athan:** Full call to prayer audio
  - **Notification:** Silent notification
  - **Silent:** No notification
- 5-minute advance notification option
- Exact alarm scheduling (even during Doze mode)
- Hijri date display with prayer times

**Athan Playback:**
- Multiple athan recordings available
- Download and store athan audio locally
- **Flip-to-silence:** Face-down gesture to stop athan
- Volume control (maximizes during playback)
- Foreground service for uninterrupted playback
- Auto-rescheduling after device reboot

### 2.5 Athkar (Islamic Remembrances)

**Categories:**
- Morning remembrances (أذكار الصباح)
- Evening remembrances (أذكار المساء)
- After-prayer remembrances
- General supplications

**Display:**
- Arabic text with proper diacritics
- English transliteration
- English translation
- Hadith reference attribution
- Repeat counter for each supplication
- Audio playback support (when available)

### 2.6 Daily Tracking & Goals

**Activity Tracking:**
- Morning azkar completion
- Evening azkar completion
- After-prayer azkar completion
- Daily completion status indicators

**Quran Progress Tracking:**
- Pages read in reader mode
- Pages listened to (audio equivalents)
- Reading duration (milliseconds)
- Listening duration (milliseconds)
- Last page viewed
- Last surah/ayah listened
- Historical data for progress charts

**Khatmah Goals (Quran Completion):**
- Create monthly or custom khatmah goals
- Track pages completed toward goal
- Progress percentage calculation
- Daily target pages calculation
- On-track status indicator (ahead/behind schedule)
- Hijri calendar integration
- Multiple simultaneous goals support
- Goal completion notifications

### 2.7 Search Functionality

**Search Features:**
- Full-text search across all 6,236 ayahs
- Filter results by specific surah
- Display ayah text with context (surrounding verses)
- Navigate directly to search results in reader
- Highlight found ayahs in green
- Search by Arabic text (with/without diacritics)

### 2.8 Download Management

**Background Downloads:**
- WorkManager-based background queue
- Download progress tracking (percentage, bytes)
- Automatic retry on network failure
- Network-aware (WiFi-only option)
- Pause/resume/cancel individual downloads
- Download history with status
- Storage location management
- Cancelled/failed download cleanup

**Download States:**
- PENDING: Queued for download
- IN_PROGRESS: Currently downloading
- COMPLETED: Successfully downloaded
- FAILED: Download error occurred
- PAUSED: User paused download
- CANCELLED: User cancelled download

### 2.9 Settings & Customization

**App Settings:**
- Language selection (Arabic/English)
- Dark mode with dynamic colors
- App theme customization

**Playback Preferences:**
- Default playback speed (1x - 2x)
- Pitch lock enable/disable
- Small seek increment (default 250ms)
- Large seek increment (default 30s)
- Snap to ayah enable/disable
- Gapless playback toggle
- Audio normalization
- Volume level

**Reading Preferences:**
- Default reading theme
- Text size adjustment
- Auto-follow during playback
- Reading reminder interval (1-6 hours or off)
- Quiet hours for reminders

**Prayer Settings:**
- Calculation method selection
- Asr juristic method (Shafi'i/Hanafi)
- Per-prayer notification modes
- Per-prayer athan selection
- Athan volume (max volume option)
- Flip-to-silence gesture enable/disable

**Download Preferences:**
- WiFi-only downloads
- Preferred audio bitrate (64/128/192/320 kbps)
- Preferred audio format (MP3/M4A/FLAC)
- Auto-delete after playback

**Accessibility:**
- Large text option
- High contrast mode
- Haptic feedback intensity (0-100)
- Keep screen on during reading
- TalkBack support

---

## 3. Architecture Overview

### 3.1 Clean Architecture Layers

The app follows **Clean Architecture** principles with three distinct layers:

```
┌─────────────────────────────────────────────────────┐
│         PRESENTATION LAYER (UI)                      │
│  • Jetpack Compose UI (@Composable functions)       │
│  • ViewModels (State Management with StateFlow)     │
│  • Navigation (Type-safe routing)                   │
│  • Theme & Reusable Components                      │
│  • No direct database or API access                 │
├─────────────────────────────────────────────────────┤
│         DOMAIN LAYER (Business Logic)               │
│  • Repository Interfaces (contracts)                │
│  • Domain Models (Pure Kotlin, no Android deps)     │
│  • Use Cases & Business Rules                       │
│  • Resource Wrapper (Success/Error/Loading)         │
│  • No Android framework dependencies                │
├─────────────────────────────────────────────────────┤
│         DATA LAYER (Data Management)                │
│  • Local Database (Room with DAOs)                  │
│  • Remote APIs (Retrofit interfaces)                │
│  • DataStore (Proto - Type-safe settings)           │
│  • Repository Implementations                       │
│  • Background Workers (WorkManager)                 │
│  • Download Management                              │
└─────────────────────────────────────────────────────┘
```

### 3.2 Design Patterns

**MVVM (Model-View-ViewModel):**
- ViewModels handle business logic and state management
- UI observes StateFlow for reactive updates
- Clear separation between UI and business logic

**Repository Pattern:**
- Abstract data sources (database, API, settings)
- Single source of truth for data access
- Implements domain layer interfaces

**Dependency Injection (Hilt):**
- Constructor injection for loose coupling
- Singleton instances for shared resources
- Module-based dependency provision

**Singleton Pattern:**
- QuranPlayer (ExoPlayer wrapper)
- PlaybackController (playback orchestration)
- DownloadManager (download coordination)

**Flow-based Reactive Programming:**
- StateFlow for state management
- Flow for database queries
- Coroutines for asynchronous operations

### 3.3 Data Flow

```
User Interaction (UI)
        ↓
    ViewModel
        ↓
   Repository ← Domain Interface
        ↓
   DAO / API
        ↓
Database / Network
```

**Example: Playing a Surah**
1. User taps surah in `SurahsScreen`
2. Navigation passes `reciterId` and `surahNumber` to `PlayerScreen`
3. `PlayerViewModel.loadAudio()` is called
4. ViewModel calls `QuranRepository.getSurahWithAudio()`
5. Repository queries `AyahDao`, `ReciterDao`, `AudioVariantDao`, `AyahIndexDao`
6. Repository maps entities to domain models
7. ViewModel updates `playbackState: StateFlow`
8. UI recomposes with new state
9. ViewModel calls `PlaybackController.playAudio()`
10. PlaybackController prepares `MediaItem` and starts ExoPlayer

---

## 4. Complete File Structure

### 4.1 Package Organization

```
app/src/main/java/com/quranmedia/player/
│
├── QuranMediaApplication.kt              # App entry point (Hilt setup, workers)
│
├── presentation/                         # UI Layer
│   ├── MainActivity.kt                   # Main entry activity
│   ├── SplashActivity.kt                 # Splash screen with Bismillah
│   │
│   ├── navigation/                       # Navigation configuration
│   │   ├── QuranNavGraph.kt              # Compose Navigation graph
│   │   └── Screen.kt                     # Route definitions (sealed class)
│   │
│   ├── screens/                          # All feature screens
│   │   ├── home/                         # Home screen
│   │   │   ├── HomeScreenNew.kt
│   │   │   └── HomeViewModel.kt
│   │   ├── player/                       # Audio playback screen
│   │   │   ├── PlayerScreenNew.kt
│   │   │   └── PlayerViewModel.kt
│   │   ├── reader/                       # Quran reading mode
│   │   │   ├── QuranReaderScreen.kt
│   │   │   ├── QuranReaderViewModel.kt
│   │   │   ├── QuranIndexScreen.kt
│   │   │   ├── QuranIndexViewModel.kt
│   │   │   └── components/
│   │   │       ├── QuranPageComposable.kt
│   │   │       ├── QuranPageLayoutData.kt
│   │   │       └── CustomRecitationDialog.kt
│   │   ├── reciters/                     # Reciter selection
│   │   │   ├── RecitersScreenNew.kt
│   │   │   └── RecitersViewModel.kt
│   │   ├── surahs/                       # Surah listing
│   │   │   ├── SurahsScreenNew.kt
│   │   │   └── SurahsViewModel.kt
│   │   ├── search/                       # Quran text search
│   │   │   ├── SearchScreen.kt
│   │   │   └── SearchViewModel.kt
│   │   ├── bookmarks/                    # Bookmark management
│   │   │   ├── BookmarksScreen.kt
│   │   │   └── BookmarksViewModel.kt
│   │   ├── downloads/                    # Download queue
│   │   │   ├── DownloadsScreen.kt
│   │   │   └── DownloadsViewModel.kt
│   │   ├── prayertimes/                  # Prayer times
│   │   │   ├── PrayerTimesScreen.kt
│   │   │   ├── PrayerTimesViewModel.kt
│   │   │   ├── AthanSettingsScreen.kt
│   │   │   └── AthanSettingsViewModel.kt
│   │   ├── athkar/                       # Islamic remembrances
│   │   │   ├── AthkarCategoriesScreen.kt
│   │   │   ├── AthkarCategoriesViewModel.kt
│   │   │   ├── AthkarListScreen.kt
│   │   │   └── AthkarListViewModel.kt
│   │   ├── tracker/                      # Daily tracking
│   │   │   ├── TrackerScreen.kt
│   │   │   └── TrackerViewModel.kt
│   │   ├── settings/                     # App settings
│   │   │   ├── SettingsScreen.kt
│   │   │   └── SettingsViewModel.kt
│   │   ├── about/                        # About screen
│   │   │   ├── AboutScreen.kt
│   │   │   └── AboutViewModel.kt
│   │   └── whatsnew/                     # Version changelog
│   │       ├── WhatsNewScreen.kt
│   │       └── WhatsNewViewModel.kt
│   │
│   ├── components/                       # Reusable UI components
│   │   └── AccessibleComponents.kt
│   │
│   ├── theme/                            # Material Design 3 theme
│   │   ├── Theme.kt                      # Main theme configuration
│   │   ├── Color.kt                      # Color palette
│   │   ├── Type.kt                       # Typography
│   │   └── ReadingThemes.kt              # Reading mode themes
│   │
│   └── util/                             # Presentation utilities
│       ├── LocalizationHelper.kt
│       ├── Strings.kt
│       └── layoutDirection.kt
│
├── domain/                               # Business Logic Layer
│   ├── model/                            # Domain models (Pure Kotlin)
│   │   ├── Surah.kt
│   │   ├── Ayah.kt
│   │   ├── Reciter.kt
│   │   ├── AudioVariant.kt
│   │   ├── AyahIndex.kt
│   │   ├── Bookmark.kt
│   │   ├── ReadingBookmark.kt
│   │   ├── PrayerTimes.kt
│   │   ├── Athan.kt
│   │   ├── Athkar.kt
│   │   ├── DailyActivity.kt
│   │   ├── QuranProgress.kt
│   │   ├── KhatmahGoal.kt
│   │   ├── SearchResult.kt
│   │   └── CustomRecitationSettings.kt
│   │
│   ├── repository/                       # Repository interfaces
│   │   ├── QuranRepository.kt
│   │   ├── AthkarRepository.kt
│   │   ├── PrayerTimesRepository.kt
│   │   └── SearchRepository.kt
│   │
│   └── util/                             # Domain utilities
│       ├── Resource.kt                   # Success/Error/Loading wrapper
│       └── TajweedDataLoader.kt
│
├── data/                                 # Data Layer
│   ├── database/                         # Room database
│   │   ├── QuranDatabase.kt              # Database definition (v7)
│   │   │
│   │   ├── entity/                       # Database entities
│   │   │   ├── ReciterEntity.kt
│   │   │   ├── SurahEntity.kt
│   │   │   ├── AyahEntity.kt
│   │   │   ├── AyahIndexEntity.kt
│   │   │   ├── AudioVariantEntity.kt
│   │   │   ├── BookmarkEntity.kt
│   │   │   ├── ReadingBookmarkEntity.kt
│   │   │   ├── DownloadTaskEntity.kt
│   │   │   ├── AthkarCategoryEntity.kt
│   │   │   ├── ThikrEntity.kt
│   │   │   ├── PrayerTimesEntity.kt
│   │   │   ├── UserLocationEntity.kt
│   │   │   ├── DownloadedAthanEntity.kt
│   │   │   ├── DailyActivityEntity.kt
│   │   │   ├── QuranProgressEntity.kt
│   │   │   └── KhatmahGoalEntity.kt
│   │   │
│   │   └── dao/                          # Data Access Objects
│   │       ├── ReciterDao.kt
│   │       ├── SurahDao.kt
│   │       ├── AyahDao.kt
│   │       ├── AyahIndexDao.kt
│   │       ├── AudioVariantDao.kt
│   │       ├── BookmarkDao.kt
│   │       ├── ReadingBookmarkDao.kt
│   │       ├── DownloadTaskDao.kt
│   │       ├── AthkarDao.kt
│   │       ├── PrayerTimesDao.kt
│   │       ├── DownloadedAthanDao.kt
│   │       ├── DailyActivityDao.kt
│   │       ├── QuranProgressDao.kt
│   │       └── KhatmahGoalDao.kt
│   │
│   ├── api/                              # Retrofit API interfaces
│   │   ├── AlQuranCloudApi.kt            # Primary Quran API
│   │   ├── AladhanApi.kt                 # Prayer times API
│   │   ├── AthkarApi.kt                  # Supplications API
│   │   ├── AthanApi.kt                   # Athan recordings API
│   │   ├── QuranApi.kt                   # CloudLinqed API
│   │   └── model/                        # API response models
│   │       ├── QuranApiResponse.kt
│   │       ├── PrayerTimesApiModels.kt
│   │       ├── AthkarApiModels.kt
│   │       ├── AthanModels.kt
│   │       └── TanzilModels.kt
│   │
│   ├── repository/                       # Repository implementations
│   │   ├── QuranRepositoryImpl.kt
│   │   ├── SearchRepositoryImpl.kt
│   │   ├── BookmarkRepository.kt
│   │   ├── SettingsRepository.kt
│   │   ├── PrayerTimesRepositoryImpl.kt
│   │   ├── AthkarRepositoryImpl.kt
│   │   ├── TrackerRepository.kt
│   │   ├── AthanRepository.kt
│   │   └── QuranDataRepository.kt
│   │
│   ├── datastore/                        # Proto DataStore
│   │   └── SettingsSerializer.kt
│   │
│   ├── worker/                           # WorkManager background jobs
│   │   ├── QuranDataPopulatorWorker.kt
│   │   ├── QuranMetadataPopulatorWorker.kt
│   │   ├── ReciterDataPopulatorWorker.kt
│   │   ├── PrayerNotificationWorker.kt
│   │   └── ReadingReminderWorker.kt
│   │
│   ├── notification/                     # Notification system
│   │   ├── PrayerNotificationScheduler.kt
│   │   ├── PrayerAlarmReceiver.kt
│   │   └── BootCompletedReceiver.kt
│   │
│   ├── location/                         # Location services
│   │   └── LocationHelper.kt
│   │
│   └── QuranMetadata.kt                  # Static Quran metadata
│
├── media/                                # Media Playback System
│   ├── player/                           # Media players
│   │   ├── QuranPlayer.kt                # ExoPlayer wrapper (EXACT seeking)
│   │   └── AthanPlayer.kt                # Athan playback player
│   │
│   ├── service/                          # Media services
│   │   ├── QuranMediaService.kt          # MediaSessionService
│   │   └── AthanService.kt               # Athan foreground service
│   │
│   ├── controller/                       # Playback controllers
│   │   └── PlaybackController.kt         # High-level orchestration
│   │
│   ├── auto/                             # Android Auto integration
│   │   ├── QuranMediaBrowserService.kt   # MediaBrowserService
│   │   └── VoiceSearchHandler.kt         # Voice query parser
│   │
│   └── model/                            # Media models
│       └── PlaybackState.kt
│
├── download/                             # Download System
│   ├── DownloadManager.kt                # Download orchestration
│   ├── DownloadWorker.kt                 # Audio download worker
│   ├── AyahDownloadWorker.kt             # Ayah-by-ayah download
│   └── EveryAyahMapping.kt               # EveryAyah provider mapping
│
└── di/                                   # Dependency Injection (Hilt)
    ├── AppModule.kt                      # Core dependencies
    ├── DatabaseModule.kt                 # Room database
    ├── DataStoreModule.kt                # Proto DataStore
    ├── NetworkModule.kt                  # Retrofit & OkHttp
    ├── RepositoryModule.kt               # Repository bindings
    ├── MediaModule.kt                    # Media system
    └── WorkManagerModule.kt              # WorkManager
```

### 4.2 Resource Files

```
app/src/main/res/
├── drawable/                             # Icons and images
│   ├── bismillah.png                     # Splash screen image
│   └── quran_logo.png                    # App logo
│
├── values/                               # Default resources (English)
│   ├── strings.xml                       # String resources
│   ├── colors.xml                        # Color definitions
│   └── themes.xml                        # App themes
│
├── values-ar/                            # Arabic resources
│   ├── strings.xml                       # Arabic strings
│   └── themes.xml                        # RTL themes
│
├── raw/                                  # Bundled data files
│   ├── tajweed_colors.json               # Tajweed color mappings
│   ├── tanzil_quran.json                 # Full Quran text
│   └── quran_metadata.xml                # Page/Juz/Manzil divisions
│
└── xml/                                  # Configuration files
    ├── automotive_app_desc.xml           # Android Auto metadata
    └── locales_config.xml                # Supported languages
```

### 4.3 Proto Files

```
app/src/main/proto/
└── settings.proto                        # Proto DataStore schema
```

---

## 5. Database Schema (Room v7)

### 5.1 Database Overview

- **Database Class:** `QuranDatabase`
- **Current Version:** 7
- **Total Entities:** 17 tables
- **Total DAOs:** 14 interfaces
- **Total Ayahs:** 6,236 (fixed across all tables)
- **Total Surahs:** 114 (fixed)

### 5.2 Core Quran Entities

#### ReciterEntity

**Table:** `reciters`
**File:** `data/database/entity/ReciterEntity.kt`

```kotlin
@Entity(tableName = "reciters")
data class ReciterEntity(
    @PrimaryKey
    val id: String,                     // e.g., "ar.alafasy"
    val name: String,                   // e.g., "Mishari Rashid al-Afasy"
    val nameArabic: String?,            // Arabic name
    val style: String?,                 // "Murattal", "Mujawwad", "Warsh"
    val version: String,                // Version code
    val imageUrl: String?               // Profile image URL
)
```

**Purpose:** Stores metadata for Quran reciters

**Indices:** None (primary key only)

---

#### SurahEntity

**Table:** `surahs`
**File:** `data/database/entity/SurahEntity.kt`

```kotlin
@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey
    val number: Int,                    // 1-114
    val nameArabic: String,             // e.g., "الفاتحة"
    val nameEnglish: String,            // e.g., "Al-Fatihah"
    val nameTransliteration: String,    // e.g., "Al-Fatiha"
    val ayahCount: Int,                 // Number of verses
    val revelationType: String          // "MECCAN" or "MEDINAN"
)
```

**Purpose:** Stores metadata for all 114 surahs

**Indices:** None (primary key only)

---

#### AyahEntity

**Table:** `ayahs`
**File:** `data/database/entity/AyahEntity.kt`

```kotlin
@Entity(
    tableName = "ayahs",
    primaryKeys = ["surahNumber", "ayahNumber"],
    foreignKeys = [
        ForeignKey(
            entity = SurahEntity::class,
            parentColumns = ["number"],
            childColumns = ["surahNumber"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("surahNumber"),
        Index("globalAyahNumber"),
        Index("page")
    ]
)
data class AyahEntity(
    val surahNumber: Int,               // 1-114
    val ayahNumber: Int,                // 1-286 (varies by surah)
    val globalAyahNumber: Int,          // 1-6236 (unique global index)
    val textArabic: String,             // Verse text with diacritics
    val textTajweed: String?,           // Tajweed-encoded text (v7 addition)
    val juz: Int,                       // 1-30
    val manzil: Int,                    // 1-7
    val page: Int,                      // 1-604 (Mushaf page)
    val ruku: Int,                      // Subdivision
    val hizbQuarter: Int,               // Quarter division (1-240)
    val sajda: Boolean                  // Prostration ayah flag
)
```

**Purpose:** Stores all 6,236 ayahs with Mushaf metadata

**Foreign Keys:** `surahNumber` → surahs.number

**Indices:**
- `surahNumber` (for surah-specific queries)
- `globalAyahNumber` (for sequential access)
- `page` (for page-based reading mode)

**Special Features:**
- `textTajweed` field added in v7 for color-coded Tajweed display
- Full-text search supported via LIKE queries

---

#### AudioVariantEntity

**Table:** `audio_variants`
**File:** `data/database/entity/AudioVariantEntity.kt`

```kotlin
@Entity(
    tableName = "audio_variants",
    foreignKeys = [
        ForeignKey(
            entity = ReciterEntity::class,
            parentColumns = ["id"],
            childColumns = ["reciterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["reciterId", "surahNumber"], unique = false)
    ]
)
data class AudioVariantEntity(
    @PrimaryKey
    val id: String,                     // UUID
    val reciterId: String,              // Foreign key to reciters
    val surahNumber: Int,               // 1-114
    val bitrate: Int,                   // 64, 128, 192, 320
    val format: String,                 // "MP3", "M4A", "FLAC"
    val url: String,                    // Streaming URL or base path
    val localPath: String?,             // Local file path (if downloaded)
    val durationMs: Long,               // Audio duration in milliseconds
    val fileSizeBytes: Long?,           // File size
    val hash: String?                   // File hash for integrity
)
```

**Purpose:** Stores audio file metadata for each reciter × surah combination

**Foreign Keys:** `reciterId` → reciters.id

**Indices:** Composite index on `(reciterId, surahNumber)` for fast lookup

---

#### AyahIndexEntity

**Table:** `ayah_index`
**File:** `data/database/entity/AyahIndexEntity.kt`

```kotlin
@Entity(
    tableName = "ayah_index",
    primaryKeys = ["reciterId", "surahNumber", "ayahNumber"],
    indices = [
        Index(value = ["reciterId", "surahNumber"]),
        Index(value = ["startMs", "endMs"])
    ]
)
data class AyahIndexEntity(
    val reciterId: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val startMs: Long,                  // Millisecond timestamp (start)
    val endMs: Long                     // Millisecond timestamp (end)
)
```

**Purpose:** **CRITICAL** - Provides millisecond-precise timestamps for ayah-level seeking

**Composite Primary Key:** `(reciterId, surahNumber, ayahNumber)`

**Indices:**
- `(reciterId, surahNumber)` for loading all indices for a surah
- `(startMs, endMs)` for timestamp-based lookups

**Usage:** ExoPlayer uses these timestamps with `SeekParameters.EXACT` for precise ayah navigation

---

### 5.3 Bookmark Entities

#### BookmarkEntity

**Table:** `bookmarks`
**File:** `data/database/entity/BookmarkEntity.kt`

```kotlin
@Entity(
    tableName = "bookmarks",
    indices = [
        Index(value = ["reciterId", "surahNumber", "ayahNumber"])
    ]
)
data class BookmarkEntity(
    @PrimaryKey
    val id: String,                     // UUID
    val reciterId: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val positionMs: Long,               // Playback position
    val label: String?,                 // User-defined label
    val loopEndMs: Long?,               // A-B loop end position
    val createdAt: Long,                // Unix timestamp
    val updatedAt: Long                 // Unix timestamp
)
```

**Purpose:** Stores user-saved listening positions (ayah bookmarks)

**Indices:** Composite index on `(reciterId, surahNumber, ayahNumber)`

---

#### ReadingBookmarkEntity

**Table:** `reading_bookmarks`
**File:** `data/database/entity/ReadingBookmarkEntity.kt`

```kotlin
@Entity(tableName = "reading_bookmarks")
data class ReadingBookmarkEntity(
    @PrimaryKey
    val id: String,                     // UUID
    val pageNumber: Int,                // 1-604
    val surahNumber: Int?,              // Optional surah reference
    val surahName: String?,             // Optional surah name
    val label: String?,                 // User-defined label
    val createdAt: Long                 // Unix timestamp
)
```

**Purpose:** Stores user-saved reading mode page positions

**Indices:** None (primary key only)

---

### 5.4 Download Entities

#### DownloadTaskEntity

**Table:** `download_tasks`
**File:** `data/database/entity/DownloadTaskEntity.kt`

```kotlin
@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey
    val id: String,                     // UUID
    val audioVariantId: String,         // Reference to audio_variants
    val reciterId: String,
    val surahNumber: Int,
    val status: String,                 // "PENDING", "IN_PROGRESS", "COMPLETED", "FAILED", "PAUSED"
    val progress: Float,                // 0.0 - 100.0
    val bytesDownloaded: Long,
    val bytesTotal: Long,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long
)
```

**Purpose:** Tracks download queue and progress

**Status Values:**
- `PENDING`: Queued for download
- `IN_PROGRESS`: Currently downloading
- `COMPLETED`: Successfully downloaded
- `FAILED`: Download error
- `PAUSED`: User paused
- `CANCELLED`: User cancelled

---

#### DownloadedAthanEntity

**Table:** `downloaded_athans`
**File:** `data/database/entity/DownloadedAthanEntity.kt`

```kotlin
@Entity(tableName = "downloaded_athans")
data class DownloadedAthanEntity(
    @PrimaryKey
    val id: String,                     // Athan ID from API
    val name: String,                   // Athan name
    val muezzin: String,                // Muezzin name
    val location: String,               // Location info
    val localPath: String,              // Local file path
    val fileSize: Long,                 // File size in bytes
    val downloadedAt: Long              // Download timestamp
)
```

**Purpose:** Tracks downloaded athan recordings

---

### 5.5 Prayer Times Entities

#### PrayerTimesEntity

**Table:** `prayer_times_cache`
**File:** `data/database/entity/PrayerTimesEntity.kt`

```kotlin
@Entity(
    tableName = "prayer_times_cache",
    primaryKeys = ["date", "latitude", "longitude"]
)
data class PrayerTimesEntity(
    val date: String,                   // ISO format (YYYY-MM-DD)
    val latitude: Double,
    val longitude: Double,
    val fajr: String,                   // Time in HH:mm format
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val locationName: String,           // City/timezone name
    val calculationMethod: Int,         // Method ID (4 = Umm Al-Qura)
    val hijriDay: Int,
    val hijriMonth: String,             // English name
    val hijriMonthArabic: String,
    val hijriYear: Int,
    val cachedAt: Long                  // Cache timestamp
)
```

**Purpose:** Caches fetched prayer times to reduce API calls

**Composite Primary Key:** `(date, latitude, longitude)`

---

#### UserLocationEntity

**Table:** `user_location`
**File:** `data/database/entity/UserLocationEntity.kt`

```kotlin
@Entity(tableName = "user_location")
data class UserLocationEntity(
    @PrimaryKey
    val id: Int = 1,                    // Always 1 (single row table)
    val latitude: Double,
    val longitude: Double,
    val cityName: String?,
    val countryName: String?,
    val isAutoDetected: Boolean,        // GPS vs manual entry
    val updatedAt: Long
)
```

**Purpose:** Stores user's location for prayer time calculation (single row)

---

### 5.6 Athkar Entities

#### AthkarCategoryEntity

**Table:** `athkar_categories`
**File:** `data/database/entity/AthkarCategoryEntity.kt`

```kotlin
@Entity(tableName = "athkar_categories")
data class AthkarCategoryEntity(
    @PrimaryKey
    val id: String,                     // e.g., "api_morning_azkar"
    val nameArabic: String,
    val nameEnglish: String,
    val iconName: String,               // Material Design icon name
    val order: Int,                     // Display order
    val updatedAt: Long
)
```

**Purpose:** Categorizes athkar/supplications

---

#### ThikrEntity

**Table:** `athkar`
**File:** `data/database/entity/ThikrEntity.kt`

```kotlin
@Entity(
    tableName = "athkar",
    foreignKeys = [
        ForeignKey(
            entity = AthkarCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class ThikrEntity(
    @PrimaryKey
    val id: String,
    val categoryId: String,             // Foreign key to athkar_categories
    val textArabic: String,             // Supplication text
    val transliteration: String?,
    val translation: String?,           // English translation
    val repeatCount: Int,               // How many times to recite
    val reference: String?,             // Hadith reference
    val audioUrl: String?,              // Audio file URL
    val order: Int                      // Display order within category
)
```

**Purpose:** Individual supplications/remembrances

**Foreign Keys:** `categoryId` → athkar_categories.id

---

### 5.7 Tracking Entities

#### DailyActivityEntity

**Table:** `daily_activities`
**File:** `data/database/entity/DailyActivityEntity.kt`

```kotlin
@Entity(
    tableName = "daily_activities",
    primaryKeys = ["date", "activityType"]
)
data class DailyActivityEntity(
    val date: String,                   // ISO format (YYYY-MM-DD)
    val activityType: String,           // "MORNING_AZKAR", "EVENING_AZKAR", "AFTER_PRAYER_AZKAR"
    val completed: Boolean,
    val completedAt: Long?,             // Completion timestamp
    val createdAt: Long
)
```

**Purpose:** Tracks daily activity completion (azkar, etc.)

**Composite Primary Key:** `(date, activityType)`

---

#### QuranProgressEntity

**Table:** `quran_progress`
**File:** `data/database/entity/QuranProgressEntity.kt`

```kotlin
@Entity(tableName = "quran_progress")
data class QuranProgressEntity(
    @PrimaryKey
    val date: String,                   // ISO format (YYYY-MM-DD)
    val pagesRead: Int,                 // Pages viewed in reader
    val pagesListened: Int,             // Audio pages equivalent
    val readingDurationMs: Long,        // Time in reader
    val listeningDurationMs: Long,      // Listening time
    val lastPage: Int,                  // Last page viewed
    val lastSurah: Int,                 // Last surah listened
    val lastAyah: Int,                  // Last ayah listened
    val updatedAt: Long
)
```

**Purpose:** Tracks daily Quran progress metrics

---

#### KhatmahGoalEntity

**Table:** `khatmah_goals`
**File:** `data/database/entity/KhatmahGoalEntity.kt`

```kotlin
@Entity(tableName = "khatmah_goals")
data class KhatmahGoalEntity(
    @PrimaryKey
    val id: String,                     // UUID
    val name: String,                   // Goal name (e.g., "Ramadan 1446")
    val startDate: String,              // ISO format
    val endDate: String,                // ISO format
    val startPage: Int,                 // Starting page (default 1)
    val targetPages: Int,               // Pages to complete (default 604)
    val isActive: Boolean,              // Only one active at a time
    val goalType: String,               // "MONTHLY", "RAMADAN", "CUSTOM"
    val createdAt: Long,
    val completedAt: Long?              // Completion timestamp
)
```

**Purpose:** Tracks Quran completion goals (Khatmah)

---

## 6. DAOs (Data Access Objects)

### 6.1 Core Quran DAOs

#### ReciterDao

**File:** `data/database/dao/ReciterDao.kt`

```kotlin
@Dao
interface ReciterDao {
    // Query methods
    @Query("SELECT * FROM reciters ORDER BY name ASC")
    fun getAllReciters(): Flow<List<ReciterEntity>>

    @Query("SELECT * FROM reciters WHERE id = :reciterId")
    suspend fun getReciterById(reciterId: String): ReciterEntity?

    @Query("SELECT COUNT(*) FROM reciters")
    suspend fun getReciterCount(): Int

    @Query("SELECT * FROM reciters WHERE name LIKE '%' || :query || '%' OR nameArabic LIKE '%' || :query || '%'")
    suspend fun searchReciters(query: String): List<ReciterEntity>

    // Insert methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReciter(reciter: ReciterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReciters(reciters: List<ReciterEntity>)

    // Delete methods
    @Query("DELETE FROM reciters WHERE id = :reciterId")
    suspend fun deleteReciterById(reciterId: String)

    @Query("DELETE FROM reciters")
    suspend fun deleteAllReciters()
}
```

**Key Features:**
- Returns `Flow<List>` for reactive UI updates
- REPLACE conflict strategy for upserts
- Search by name (English or Arabic)

---

#### SurahDao

**File:** `data/database/dao/SurahDao.kt`

```kotlin
@Dao
interface SurahDao {
    @Query("SELECT * FROM surahs ORDER BY number ASC")
    fun getAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs WHERE number = :surahNumber")
    suspend fun getSurahByNumber(surahNumber: Int): SurahEntity?

    @Query("""
        SELECT * FROM surahs
        WHERE nameEnglish LIKE '%' || :query || '%'
        OR nameArabic LIKE '%' || :query || '%'
        OR nameTransliteration LIKE '%' || :query || '%'
        OR CAST(number AS TEXT) = :query
    """)
    suspend fun searchSurahs(query: String): List<SurahEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurah(surah: SurahEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahs(surahs: List<SurahEntity>)

    @Query("DELETE FROM surahs")
    suspend fun deleteAllSurahs()
}
```

**Key Features:**
- Multi-field search (English, Arabic, transliteration, number)
- Flow-based reactive queries

---

#### AyahDao

**File:** `data/database/dao/AyahDao.kt`

```kotlin
@Dao
interface AyahDao {
    // Basic queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyah(ayah: AyahEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(ayahs: List<AyahEntity>)

    @Query("SELECT * FROM ayahs WHERE surahNumber = :surahNumber ORDER BY ayahNumber ASC")
    fun getAyahsBySurah(surahNumber: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE surahNumber = :surahNumber ORDER BY ayahNumber ASC")
    suspend fun getAyahsBySurahSync(surahNumber: Int): List<AyahEntity>

    @Query("SELECT * FROM ayahs WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber")
    suspend fun getAyah(surahNumber: Int, ayahNumber: Int): AyahEntity?

    @Query("SELECT * FROM ayahs WHERE globalAyahNumber = :globalNumber")
    suspend fun getAyahByGlobalNumber(globalNumber: Int): AyahEntity?

    // Page-based queries
    @Query("SELECT * FROM ayahs WHERE page = :pageNumber ORDER BY surahNumber, ayahNumber ASC")
    fun getAyahsByPage(pageNumber: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE page = :pageNumber ORDER BY surahNumber, ayahNumber ASC")
    suspend fun getAyahsByPageSync(pageNumber: Int): List<AyahEntity>

    // Juz queries
    @Query("SELECT * FROM ayahs WHERE juz = :juzNumber ORDER BY surahNumber, ayahNumber ASC")
    fun getAyahsByJuz(juzNumber: Int): Flow<List<AyahEntity>>

    // Special ayahs
    @Query("SELECT * FROM ayahs WHERE sajda = 1 ORDER BY surahNumber, ayahNumber ASC")
    fun getSajdaAyahs(): Flow<List<AyahEntity>>

    // Counts
    @Query("SELECT COUNT(*) FROM ayahs")
    suspend fun getAyahCount(): Int

    @Query("SELECT COUNT(*) FROM ayahs WHERE surahNumber = :surahNumber")
    suspend fun getAyahCountForSurah(surahNumber: Int): Int

    // Search
    @Query("SELECT * FROM ayahs WHERE textArabic LIKE '%' || :query || '%'")
    suspend fun searchAyahs(query: String): List<AyahEntity>

    // Navigation helpers
    @Query("SELECT * FROM ayahs WHERE juz = :juzNumber ORDER BY globalAyahNumber ASC LIMIT 1")
    suspend fun getFirstAyahOfJuz(juzNumber: Int): AyahEntity?

    @Query("SELECT MIN(page) FROM ayahs WHERE surahNumber = :surahNumber")
    suspend fun getFirstPageOfSurah(surahNumber: Int): Int?

    @Query("SELECT page FROM ayahs WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber")
    suspend fun getPageForAyah(surahNumber: Int, ayahNumber: Int): Int?

    @Query("SELECT MIN(page) FROM ayahs WHERE juz = :juzNumber")
    suspend fun getFirstPageOfJuz(juzNumber: Int): Int?

    @Query("SELECT DISTINCT juz FROM ayahs ORDER BY juz ASC")
    fun getAllJuzNumbers(): Flow<List<Int>>

    @Query("SELECT MAX(page) FROM ayahs")
    suspend fun getMaxPageNumber(): Int?

    // Data classes for complex queries
    data class SurahPageInfo(val surahNumber: Int, val page: Int)
    data class JuzStartInfo(val juz: Int, val firstSurah: Int, val page: Int)
    data class HizbQuarterInfo(val hizbQuarter: Int, val surah: Int, val ayah: Int, val page: Int)

    @Query("""
        SELECT DISTINCT surahNumber, MIN(page) as page
        FROM ayahs
        GROUP BY surahNumber
        ORDER BY surahNumber ASC
    """)
    suspend fun getAllSurahStartPages(): List<SurahPageInfo>

    @Query("""
        SELECT juz, MIN(surahNumber) as firstSurah, MIN(page) as page
        FROM ayahs
        GROUP BY juz
        ORDER BY juz ASC
    """)
    suspend fun getAllJuzStartInfo(): List<JuzStartInfo>

    @Query("""
        SELECT hizbQuarter, surahNumber as surah, ayahNumber as ayah, page
        FROM (
            SELECT hizbQuarter, surahNumber, MIN(ayahNumber) as ayahNumber, page
            FROM ayahs
            GROUP BY hizbQuarter
        )
        ORDER BY hizbQuarter ASC
    """)
    suspend fun getAllHizbQuartersInfo(): List<HizbQuarterInfo>
}
```

**Key Features:**
- Both Flow and suspend variants for flexibility
- Full-text search via LIKE
- Page-based, Juz-based, and Surah-based queries
- Complex aggregation queries for navigation helpers

---

*(Continuing in next file due to length...)*

**[DEVELOPER_PART1.md continues with remaining DAO documentation, Repositories, API Integration, Media System, and UI Layer...]**
