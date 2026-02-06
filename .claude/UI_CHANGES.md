# Quran Media App - UI Changes Documentation

This document captures the UI changes made to the Android app for implementation reference on iOS.

## 1. Color Scheme Update

The app uses a warm, natural color palette with green accents for headers and action buttons.

### Main Color Palette

| UI Element | Color Name | Hex Code | Usage |
|------------|------------|----------|-------|
| Background | Warm Beige/Cream | `#FDFBF7` | Main app background |
| Header/Nav Bar | Islamic Green | `#2E7D32` | Headers, navigation bars |
| Main Action Button | Islamic Green | `#2E7D32` | Primary buttons, selected items |
| Dark Accent | Dark Green | `#1B5E20` | Emphasis, titles |
| Light Accent | Light Green | `#4CAF50` | Secondary highlights |
| Text (Body) | Dark Coffee Brown | `#3E2723` | Body text, labels |
| Dividers/Borders | Soft Wood Brown | `#A1887F` | Dividers, borders, separators |
| Special Accent | Gold | `#D4AF37` | Bismillah, special highlights |

### Card Colors (Feature Grid)

| Color Name | Hex Code | Usage |
|------------|----------|-------|
| Teal | `#00897B` | Athkar card |
| Purple | `#7E57C2` | Prayer Times card |
| Orange | `#FF8A65` | Bookmarks card |
| Blue | `#42A5F5` | Downloads card |
| Grey | `#78909C` | Misc cards |

---

## 2. Unified Settings Screen

The Settings and Prayer Settings screens were unified into a single tabbed settings screen.

### Structure

```
UnifiedSettingsScreen
├── Header (Green gradient with back button and title)
├── Tab Row
│   ├── Reading Tab (book icon)
│   └── Prayer Tab (clock icon)
├── Reading Tab Content
│   ├── Reading Theme Section
│   │   ├── Theme chips (Cream, White, Dark, Sepia, Night, Tajweed, Custom)
│   │   ├── Custom color pickers (if Custom selected)
│   │   └── Theme preview
│   ├── Reading Reminder Section
│   │   ├── Enable/Disable toggle
│   │   ├── Reminder interval (if enabled)
│   │   └── Quiet hours (if enabled)
│   ├── Display Settings Section
│   │   ├── Mushaf Font toggle
│   │   ├── Font downloads (V2/V4)
│   │   ├── Bold Font toggle (if not using Mushaf)
│   │   └── Keep Screen On toggle
│   ├── Recite Settings Section
│   │   ├── Real-Time Assessment toggle
│   │   └── Vibrate on Mistake (if real-time enabled)
│   └── Language Section
│       ├── Arabic option
│       └── English option
└── Prayer Tab Content
    ├── Calculation Method dropdown
    ├── Asr Calculation chips (Shafi/Hanafi)
    ├── Hijri Date Adjustment chips (-2 to +2)
    ├── Muezzin selector (if any Athan enabled)
    │   ├── Global muezzin selector
    │   └── Per-prayer customization (expandable)
    ├── Prayer Notifications card
    │   └── 5 prayers, each with Athan/Alert/Off chips
    ├── Notify Before timing (if notifications enabled)
    ├── Athan Options (if Athan enabled)
    │   ├── Max Volume toggle
    │   └── Flip to Silence toggle
    ├── Notification Options (if Alert enabled)
    │   ├── Sound toggle
    │   └── Vibration toggle
    └── Downloaded Athans list (if any)
```

### File Location
- `UnifiedSettingsScreen.kt` in `presentation/screens/settings/`

---

## 3. Compact Single-Line Header

The main screen header was compacted from multi-line to a single line.

### Old Structure
```
┌─────────────────────────────────┐
│  App Name              [Lang]   │
│                                 │
│    بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ    │
│                                 │
└─────────────────────────────────┘
```

### New Structure
```
┌───────────────────────────────────────────────────────┐
│  App Name    بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ    [Lang] [⋮] │
└───────────────────────────────────────────────────────┘
```

### Changes
- Header height reduced
- Full Bismillah "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ" centered inline (16sp)
- Language toggle made smaller (32dp vs 40dp)
- Added 3-dot context menu (⋮)
- Bottom corners rounded (16dp vs 24dp)

---

## 4. 3-Dot Contextual Menu

The settings gear icon was replaced with a 3-dot vertical menu (⋮) that provides contextual navigation.

### Menu Structure

```
┌─────────────────────┐
│ 📖  Reading         │  (القرآن)
│ ⚙️  Settings        │
│ 🕐  Prayer Times    │
│ ☀️  Athkar          │
│ ✓  Daily Tracker    │
│ ☁️  Downloads       │
│ ℹ️  About           │
└─────────────────────┘
```

### Menu Items (in order)
1. **Reading** - Opens Quran reading index (القرآن in Arabic)
2. **Settings** - Opens unified settings screen
3. **Prayer Times** - Opens prayer times screen
4. **Athkar** - Opens athkar categories
5. **Daily Tracker** - Opens daily tracking screen
6. **Downloads** - Opens downloads management
7. **About** - Opens about screen

### Context Rules
- All menu items are shown regardless of current screen (consistent navigation)
- Recite is excluded from the menu (still in development)
- Menu appears in ALL screens (Home, QuranIndex, PrayerTimes, Athkar, Bookmarks, Tracker, Downloads)
- Each screen excludes itself from its own menu (e.g., Prayer Times doesn't show Prayer Times in its menu)
- **Note:** iOS implementation may optionally hide current screen from menu if desired

### Screens with 3-Dot Menu
1. **HomeScreenNew** - Full menu (excludes Reading since it has Quran card)
2. **QuranIndexScreen** - Full menu (excludes Reading since user is in reading)
3. **PrayerTimesScreen** - Includes Reading, excludes Prayer Times
4. **AthkarCategoriesScreen** - Includes Reading, excludes Athkar
5. **AthkarListScreen** - Includes Reading, excludes Athkar
6. **BookmarksScreen** - Full menu including Reading
7. **TrackerScreen** - Includes Reading, excludes Daily Tracker
8. **DownloadsScreen** - Includes Reading, excludes Downloads

### Icon Colors
- Menu Icons: Islamic Green (`#2E7D32`)
- Menu Text: Dark Green (`#1B5E20`)
- Menu Background: White

### Menu Icons
- Reading: `Icons.Default.MenuBook`
- Settings: `Icons.Default.Settings`
- Prayer Times: `Icons.Default.Schedule`
- Athkar: `Icons.Default.WbSunny`
- Daily Tracker: `Icons.Default.CheckCircle`
- Downloads: `Icons.Default.CloudDownload`
- About: `Icons.Default.Info`

---

## 5. Athkar Screens Color Theme

The Athkar screens now use the unified Islamic Green theme instead of teal:

### Changes
- **Header**: Changed from teal (`#00897B`) to Islamic Green (`#2E7D32`)
- **Category Icons**: Changed from teal to Islamic Green
- **Loading Indicator**: Changed from teal to Islamic Green
- **Background**: Uses warm cream (`#FDFBF7`)

---

## 6. Prayer Times Wood Coloring

The Prayer Times 5-prayer list now uses wood/paper coloring for a warmer look:

### Changes
- **Dividers**: Changed from green to Soft Wood Brown (`#A1887F`)
- **Prayer Text**: Changed from dark green to Coffee Brown (`#3E2723`)
- **Prayer Icons**: Changed from gray to Soft Wood Brown for inactive prayers
- **Next Prayer Highlight**: Changed from light green to gold accent (`#D4AF37` at 15% opacity)
- **Next Prayer Text/Icon**: Remains Islamic Green for emphasis

---

## 7. Home Screen Card Layout

The following cards were removed from the home screen grid as they're now in the 3-dot menu:
- Settings card
- About card

The home screen now shows (in order):
1. **Quran** (main card - large, prominent)
2. **Prayer Times** (long card - full width, for prominence)
3. **Recite** (full width)
4. **Athkar** + **Daily Tracker** (side by side)
5. **Bookmarks** + **Downloads** (side by side)

### Card Colors
| Card | Color |
|------|-------|
| Prayer Times | Purple (`#7E57C2`) |
| Recite | Purple (`#7B1FA2`) |
| Athkar | Teal (`#00897B`) |
| Daily Tracker | Teal (`#00897B`) |
| Bookmarks | Orange (`#FF8A65`) |
| Downloads | Light Green (`#4CAF50`) |

---

## 7.1. 3-Dot Menu Navigation Behavior

When navigating via the 3-dot menu, the back stack is cleared up to the home screen. This means:
- Pressing back after using the 3-dot menu goes directly to Home
- The navigation uses `popUpTo(Screen.Home.route)` to clear intermediate screens
- This prevents the user from having to press back multiple times through visited screens

---

## 8. Header Gradient

The header uses a vertical gradient with the green theme:

```kotlin
Brush.verticalGradient(
    colors = listOf(
        darkGreen,      // #1B5E20 (top)
        islamicGreen,   // #2E7D32 (middle)
        lightGreen      // #4CAF50 (bottom)
    )
)
```

---

## 9. Navigation Updates

### QuranNavGraph Changes
- Settings route now uses `UnifiedSettingsScreen`
- QuranIndexScreen now receives additional navigation callbacks:
  - `onNavigateToPrayerTimes`
  - `onNavigateToAthkar`
  - `onNavigateToTracker`
  - `onNavigateToDownloads`
  - `onNavigateToAbout`

---

## 10. Files Modified

| File | Changes |
|------|---------|
| `HomeScreenNew.kt` | Updated colors, compact header, 3-dot menu |
| `QuranPageComposable.kt` | Shared color definitions (single source of truth) |
| `QuranIndexScreen.kt` | 3-dot menu added, navigation callbacks |
| `QuranNavGraph.kt` | Uses UnifiedSettingsScreen, passes 3-dot menu navigation callbacks to all screens |
| `UnifiedSettingsScreen.kt` | NEW - Tabbed settings screen |
| `PrayerTimesScreen.kt` | Imports shared colors, 3-dot menu added |
| `AthanSettingsScreen.kt` | Imports shared colors |
| `AthkarCategoriesScreen.kt` | Imports shared colors, 3-dot menu added (keeps teal for Athkar theme) |
| `AthkarListScreen.kt` | Imports shared colors, 3-dot menu added (keeps teal for Athkar theme) |
| `BookmarksScreen.kt` | Imports shared colors, 3-dot menu added |
| `TrackerScreen.kt` | Imports shared colors, 3-dot menu added |
| `WhatsNewScreen.kt` | Imports shared colors |
| `CreateKhatmahGoalDialog.kt` | Imports shared colors |
| `DownloadsScreen.kt` | Imports shared colors, 3-dot menu added |

### Shared Color Source

All screens import colors from `QuranPageComposable.kt`:

```kotlin
import com.quranmedia.player.presentation.screens.reader.components.islamicGreen
import com.quranmedia.player.presentation.screens.reader.components.darkGreen
import com.quranmedia.player.presentation.screens.reader.components.lightGreen
import com.quranmedia.player.presentation.screens.reader.components.goldAccent
import com.quranmedia.player.presentation.screens.reader.components.creamBackground
import com.quranmedia.player.presentation.screens.reader.components.coffeeBrown
import com.quranmedia.player.presentation.screens.reader.components.softWoodBrown
```

### Screen-Specific Colors

Some screens keep specific colors for their unique themes:
- **Athkar screens**: Use `tealColor = Color(0xFF00897B)` for Athkar-specific teal theme
- **WhatsNew/AthanSettings**: Use `lightGreen = Color(0xFFE8F5E9)` for very light green backgrounds

---

## 11. Typography Notes

- Arabic text uses `scheherazadeFont` (KFGQPC Hafs font)
- English text uses system default font
- Header title: 22sp Bold
- Bismillah: 18sp Medium
- Body text: 14sp Regular (Dark Coffee Brown `#3E2723`)
- Subtitle/secondary: 12sp Regular
- Captions: 11sp Regular

---

## 12. Shadow and Elevation

### Header Shadow
```kotlin
shadow(
    elevation = 10.dp,
    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
    ambientColor = darkGreen.copy(alpha = 0.4f),
    spotColor = darkGreen.copy(alpha = 0.4f)
)
```

### Card Shadow
```kotlin
shadow(
    elevation = 6.dp,
    shape = RoundedCornerShape(14.dp),
    ambientColor = islamicGreen.copy(alpha = 0.2f),
    spotColor = islamicGreen.copy(alpha = 0.2f)
)
```

---

## Implementation Notes for iOS

### Color Definitions (Swift)
```swift
// Main Colors
let warmBeigeBg = UIColor(red: 253/255, green: 251/255, blue: 247/255, alpha: 1)     // #FDFBF7
let islamicGreen = UIColor(red: 46/255, green: 125/255, blue: 50/255, alpha: 1)       // #2E7D32
let darkGreen = UIColor(red: 27/255, green: 94/255, blue: 32/255, alpha: 1)           // #1B5E20
let lightGreen = UIColor(red: 76/255, green: 175/255, blue: 80/255, alpha: 1)         // #4CAF50
let coffeeBrown = UIColor(red: 62/255, green: 39/255, blue: 35/255, alpha: 1)         // #3E2723
let softWoodBrown = UIColor(red: 161/255, green: 136/255, blue: 127/255, alpha: 1)    // #A1887F
let goldAccent = UIColor(red: 212/255, green: 175/255, blue: 55/255, alpha: 1)        // #D4AF37
```

### Implementation Tips
1. Tab bar can use `UISegmentedControl` or custom tabs
2. Context menu can use `UIMenu` with `UIAction` items
3. Gradients can be created with `CAGradientLayer`
4. Shadows use `layer.shadowColor`, `layer.shadowOffset`, `layer.shadowRadius`
5. Use `coffeeBrown` for body text instead of black
6. Use `softWoodBrown` for dividers and borders instead of grey
