# Onboarding Screen - iOS Implementation Guide

## Overview

Single-page onboarding shown on first launch. No downloads required — SVG Mushaf fonts are bundled in app assets. The screen collects permissions and prayer calculation method, then enables Athan with sensible defaults.

## Layout (Top to Bottom)

```
[Language Toggle]              (top-right: "English" / "العربية")

Welcome to Al-Furqan           (title, green #1B5E20)
App Setup                      (subtitle, gray)
Allow permissions and choose your settings  (hint, gray)

┌─────────────────────────────────────────┐
│  Permissions Card                       │
│                                         │
│  📍 Location        [detected city] ✓/○ │
│  🔔 Notifications                  ✓/○ │
│  🔋 Battery                        ✓/○ │
│                                         │
│  [🔒 Allow Permissions]  (green button) │
│  (hidden when all granted)              │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  ⚠️ Warning Card (orange #FFF3E0)       │
│  "Without notification permission..."   │
│  "Without battery exemption..."         │
│  (hidden when both granted)             │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  Prayer Calculation Method              │
│  [Dropdown: Umm Al-Qura, Makkah  ▼]    │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  ℹ️ Info Card (green #E8F5E9)           │
│  "Athan, max volume, and flip-to-       │
│   silence will be enabled automatically"│
└─────────────────────────────────────────┘

[Start]  (full-width green button)
You can change settings later from the menu
```

## UI State

```
OnboardingUiState:
  language: AppLanguage          // ARABIC (default) or ENGLISH
  selectedCalculationMethod: Int // default 4 (Umm Al-Qura)
  locationPermissionGranted: Bool
  notificationPermissionGranted: Bool
  batteryOptimizationDisabled: Bool
  isDetectingLocation: Bool
  detectedLocationName: String?
  isCompleting: Bool
```

## Permissions

### Permission Status Row
Each permission is a compact row: `[Icon] [Label] [Extra Info?] [✓ or ○]`
- Icon color: green if granted, gray if not
- Status icon: `CheckCircle` (green) if granted, `RadioButtonUnchecked` (light gray) if not
- Location row shows detected city name or "Detecting..." as extra info

### "Allow Permissions" Button
Single button requests all permissions at once:
1. **Location + Notifications**: Requested together via multi-permission request
   - iOS: `ACCESS_COARSE_LOCATION` → `CLLocationManager.requestWhenInUseAuthorization()`
   - iOS: `POST_NOTIFICATIONS` → `UNUserNotificationCenter.requestAuthorization()`
2. **Battery optimization**: Separate system dialog (Android-specific)
   - iOS equivalent: Not applicable. iOS handles background execution differently. Consider informing users about enabling Background App Refresh instead.

### Permissions Card
- Background turns light green (#E8F5E9) when ALL permissions are granted
- Button is hidden when all permissions are granted

### Warning Card
- Orange background (#FFF3E0) with warning icon (#FF9800)
- Shown when notification OR battery permission is NOT granted
- Two warning messages (shown conditionally):
  - Notification: "Without notification permission, you won't receive Athan alerts"
  - Battery: "Without battery exemption, Athan may not play on time"

## Prayer Calculation Methods

Dropdown with these methods (id → Arabic name / English name):

| ID | Arabic | English |
|----|--------|---------|
| 4  | أم القرى - مكة | Umm Al-Qura, Makkah |
| 3  | رابطة العالم الإسلامي | Muslim World League |
| 5  | الهيئة المصرية | Egyptian General Authority |
| 1  | جامعة كراتشي | Univ. of Karachi |
| 2  | أمريكا الشمالية | ISNA, North America |
| 7  | طهران | Tehran |
| 8  | الخليج | Gulf Region |
| 9  | الكويت | Kuwait |
| 10 | قطر | Qatar |
| 16 | دبي | Dubai |
| 11 | سنغافورة | Singapore |
| 12 | فرنسا | France |
| 13 | تركيا | Turkey |
| 14 | روسيا | Russia |

Default: **4** (Umm Al-Qura, Makkah)

## Completion Logic

When user taps "Start" (`completeOnboarding()`):

1. **Enable Mushaf font**: `useQCFFont = true` (SVG bundled in assets, always available)
2. **Enable Athan for all prayers**: Fajr, Dhuhr, Asr, Maghrib, Isha — all set to ATHAN mode
3. **Enable per-prayer notification flags**: All 5 prayers enabled
4. **Enable max volume**: `athanMaxVolume = true`
5. **Enable flip-to-silence**: `flipToSilenceAthan = true`
6. **Ensure default Athan audio** is available locally
7. **Schedule prayer notifications** using saved location (or Makkah fallback)
8. **Mark onboarding complete**: `completedInitialSetup = true`

## Background Tasks on Init

- **Pre-cache Makkah prayer times** as fallback (today + tomorrow + Ramadan Imsakiya)
- **On location granted**: Detect city, cache prayer times for detected location

## Bilingual Support

All text has Arabic/English variants. Layout direction flips to RTL when Arabic is selected. Language toggle at top-right switches between Arabic/English.

## Colors

| Name | Hex | Usage |
|------|-----|-------|
| PrimaryGreen | #1B5E20 | Title, buttons, icons, info card |
| LightGreen | #E8F5E9 | Permissions card (all granted), info card bg |
| WarningOrange | #FF9800 | Warning icon |
| LightOrange | #FFF3E0 | Warning card background |

## iOS-Specific Notes

- No battery optimization dialog on iOS. Consider a tip about enabling Background App Refresh.
- Notification permission on iOS is a single request (`UNUserNotificationCenter`), not per-type.
- Location permission: Use `requestWhenInUseAuthorization()` — coarse location is sufficient for prayer times.
- iOS does not have a multi-permission request API like Android. Request each permission sequentially or use a custom flow.
