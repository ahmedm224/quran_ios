# Privacy Policy for Alfurqan (الفرقان)

**Effective Date:** January 26, 2025
**Last Updated:** December 30, 2025

## Introduction

Alfurqan (الفرقان) - Quran Media Player ("we," "our," or "the app") is committed to protecting your privacy. This Privacy Policy explains how we handle information when you use our Android application.

**Developer:** cloudlinqed.com
**App Name:** Alfurqan (الفرقان)
**Package Name:** com.quranmedia.player

## Privacy Summary

**What We Collect:**
- ✅ **Approximate location** (optional, city-level only) - for prayer times calculation
- ✅ **App preferences** - stored locally on your device
- ✅ **Personal progress data** - your Quran reading/listening tracking (local only)

**What We DON'T Collect:**
- ❌ Personal information (name, email, phone)
- ❌ Precise GPS location
- ❌ User behavior analytics or tracking
- ❌ Device identifiers or advertising IDs
- ❌ Any data for advertising or marketing purposes

**Key Privacy Points:**
- 🔒 **All data stored locally** on your device
- 🔒 **No cloud storage** or data transmission to our servers
- 🔒 **Location is optional** - manual city entry available
- 🔒 **All network traffic encrypted** with HTTPS/TLS
- 🔒 **No user accounts** or login required
- 🔒 **No tracking or analytics** services
- 🔒 **Daily Tracker** is for your personal progress, NOT behavior monitoring

## Information We Collect

### Information We DO NOT Collect or Transmit to Servers

Alfurqan does NOT collect, store, or transmit any personal information to external servers, including:

- Personal identification information (name, email, phone number)
- Device identifiers or advertising IDs
- User behavior analytics or tracking
- Crash reports or diagnostics
- Advertising data
- User accounts or profiles

**Important:** We do NOT track user behavior, app usage patterns, or interaction analytics. Our "Daily Tracker" feature is solely for tracking your personal Quran reading and listening progress on your device—not for monitoring or analyzing your behavior.

### Information Collected and Stored Locally on Your Device

The app collects and stores the following data **locally on your device only**:

#### Required for App Functionality:
1. **Quran Text Data**: High-quality Quranic text from Tanzil Project bundled with the app for offline access
2. **Audio Playback State**: Your last listening position, selected reciter, and current surah/ayah
3. **Downloaded Audio Files**: Quran recitation audio files you choose to download for offline listening

#### User-Created Data:
4. **Bookmarks**: Saved positions you create for both reading and listening
5. **App Settings & Preferences**: Language, theme, playback speed, audio quality, reading preferences
6. **Location Data (Approximate)**: City-level location (latitude/longitude) used to calculate accurate prayer times for your area
7. **Prayer Times Cache**: Calculated prayer times for your location (refreshed daily)

#### Personal Progress Tracking (NOT Behavior Analytics):
8. **Daily Tracker Data**: Your personal Quran reading/listening progress including:
   - Pages read and pages listened to
   - Time spent reading and listening
   - Last page/position accessed
   - Daily/weekly progress for your own tracking
9. **Athkar Completion**: Which Islamic remembrances (athkar) you've completed each day
10. **Khatmah Goals**: Your personal Quran completion goals and progress

**Important Notes:**
- **All data remains on your device** and is never transmitted to external servers
- **Location data** is only used for prayer time calculations and stored locally
- **Daily Tracker** is for your personal progress tracking, NOT for behavior analytics or monitoring
- **No analytics or tracking** of how you use the app features

## Permissions Required

The app requests the following Android permissions:

### Core Functionality:
1. **INTERNET**: Required to stream Quran recitations, fetch prayer times, download audio files, and retrieve athkar content
2. **FOREGROUND_SERVICE**: Allows audio playback to continue when the app is in the background
3. **FOREGROUND_SERVICE_MEDIA_PLAYBACK**: Required for Android 14+ to provide media playback controls
4. **WAKE_LOCK**: Prevents the device from sleeping during audio playback
5. **POST_NOTIFICATIONS**: Shows playback notifications with media controls (Android 13+)

### Prayer Times Feature:
6. **ACCESS_COARSE_LOCATION**: Optional permission to detect your approximate location (city-level) for accurate prayer times. You can also manually enter your city.
7. **SCHEDULE_EXACT_ALARM**: Optional permission to schedule exact prayer time notifications/athan. Required for time-sensitive religious obligations.
8. **RECEIVE_BOOT_COMPLETED**: Re-schedules prayer notifications after device restart (only if you enable prayer notifications)

**Note:** Location permission is optional. If not granted, you can manually select your city for prayer times.

## Third-Party Services

The app connects to the following third-party APIs over **encrypted HTTPS connections**:

### 1. Tanzil Project
**Purpose:** Quranic text display
**Website:** https://tanzil.net

- Quran text is bundled with the app (no internet connection required)
- No data is transmitted to or from Tanzil servers
- Text is stored locally on your device

### 2. Al-Quran Cloud API
**Purpose:** Quran metadata and some audio recitations
**Website:** https://alquran.cloud/api
**Base URL:** https://api.alquran.cloud/v1/

The app connects to this API to:
- Retrieve Quran metadata (surah names, ayah counts)
- Stream select audio recitations

**Data Shared:**
- Your device's IP address (standard for any internet connection)
- Requested surah/ayah numbers and edition identifiers
- No personal information or location data

### 3. Alfurqan API
**Purpose:** Quran audio recitations and athan recordings
**Base URL:** https://api.alfurqan.online/

The app connects to this API to:
- Stream audio recitations from 50+ reciters
- Download athan (call to prayer) recordings
- Retrieve reciter metadata

**Data Shared:**
- Your device's IP address (standard for any internet connection)
- Requested reciter IDs and surah/ayah numbers
- No personal information or location data

### 4. Aladhan API
**Purpose:** Prayer times calculation
**Website:** https://aladhan.com
**Base URL:** https://api.aladhan.com/

The app connects to this API to:
- Calculate accurate prayer times based on your location
- Retrieve Hijri (Islamic) calendar dates

**Data Shared:**
- Your approximate location (latitude/longitude) - only when you use prayer times feature
- Date and calculation method preferences
- Your device's IP address (standard for any internet connection)
- No personal identification information

**Data Encryption:** All location data sent to Aladhan API is encrypted in transit using HTTPS/TLS.

### 5. HisnMuslim API
**Purpose:** Islamic remembrances (Athkar)
**Website:** https://hisnmuslim.com
**Base URL:** https://hisnmuslim.com/api/

The app connects to this API to:
- Retrieve authentic Islamic remembrances (morning/evening/after-prayer athkar)
- Get Arabic text with translations and references

**Data Shared:**
- Your device's IP address (standard for any internet connection)
- No personal information or location data

### Data Security
- All API connections use **HTTPS encryption** (TLS/SSL)
- No personal information is shared with any third-party service
- Location data (when used for prayer times) is sent securely and not stored by external servers

## Data Storage and Security

### Local Storage
- All user data is stored locally using Android's Room Database and DataStore
- Data is stored in your app's private storage directory, inaccessible to other apps
- No cloud backup is enabled (android:allowBackup="false")

### Optional Features and Data Privacy

**Location and Prayer Times:**
- Location permission is **completely optional**
- You can use the app's full Quran features without granting location permission
- If you don't grant location permission, you can manually enter your city for prayer times
- Location data is **NEVER tracked, shared, or transmitted to any analytics service**
- Location is only used locally to calculate prayer times and is **NOT** sent to external servers for tracking purposes
- The only time location data leaves your device is when sent encrypted (HTTPS) to Aladhan API solely for prayer time calculation—this API does not track or store your location

**Notifications:**
- All notification permissions are **optional**
- You can choose to enable/disable prayer time notifications
- Choose notification mode per prayer: Silent, Notification, or Athan
- Notification preferences are stored locally and **NEVER** shared with third parties
- We do **NOT** track when or how you use notifications

**Privacy Guarantee:**
- We do **NOT** sell, share, or transmit your location data to advertisers or analytics services
- We do **NOT** track your behavior, movements, or app usage patterns
- We do **NOT** build user profiles or share data with data brokers
- Your data stays on your device under your complete control

### Data Deletion
You can delete all app data at any time by:
1. Going to Android Settings → Apps → Alfurqan → Storage → Clear Data
2. Uninstalling the app (removes all data permanently)

## Children's Privacy

Alfurqan does not collect any personal information from anyone, including children under the age of 13. The app is safe for users of all ages.

## Android Auto Integration

When using Android Auto:
- The app integrates with your vehicle's infotainment system
- No additional data is collected or transmitted
- Audio playback and browsing functionality remain the same as mobile usage

## Changes to This Privacy Policy

We may update this Privacy Policy from time to time. Any changes will be posted in this document with an updated "Last Updated" date. Continued use of the app after changes constitutes acceptance of the revised policy.

## Your Rights

Since we do not collect any personal information, there is no personal data to:
- Access
- Correct
- Delete
- Export
- Restrict processing

All data remains under your control on your device.

## Contact Us

If you have questions about this Privacy Policy or the app's data practices, please contact:

**Developer:** cloudlinqed.com
**Email:** info@cloudlinqed.com
**Website:** https://cloudlinqed.com

## Data Protection Compliance

### GDPR Compliance (Europe)
- No personal data is collected or processed
- No data transfers occur outside your device
- No profiling or automated decision-making

### CCPA Compliance (California)
- No personal information is sold or shared
- No personal information is collected

### General Data Protection
This app follows privacy-by-design principles:
- **Data minimization**: Only essential data collected (location only for prayer times)
- **Purpose limitation**: Data used only for app functionality (prayer times, progress tracking)
- **Local storage**: All data stored on device, not transmitted to servers
- **User control**: Location permission is optional; manual city entry available
- **Data deletion**: Users can delete all data anytime via Android Settings
- **Encryption in transit**: All network communication uses HTTPS/TLS encryption
- **No behavior tracking**: Daily tracker is for personal progress, not analytics
- **Confidentiality**: Data stored in app's private storage, inaccessible to other apps

## Open Source Acknowledgments

This app uses the following open-source components:
- **Jetpack Compose** - UI framework (Apache 2.0)
- **ExoPlayer (Media3)** - Audio playback (Apache 2.0)
- **Room Database** - Local data storage (Apache 2.0)
- **Hilt** - Dependency injection (Apache 2.0)
- **Retrofit** - Network communication (Apache 2.0)

## Consent

By installing and using Alfurqan, you consent to this Privacy Policy.

---

**© 2025 cloudlinqed.com. All rights reserved.**
