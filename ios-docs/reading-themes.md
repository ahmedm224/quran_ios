# Reading Themes - iOS Implementation Guide

## Overview

The Quran reader supports 7 reading themes: Default (warm Mushaf paper), Light, Night, Paper, Ocean, Tajweed, and Custom. Users select themes from chip-style buttons in the Reading Settings. The **Default** theme is applied on fresh install.

---

## Theme List & Order

Themes are displayed in this order (first = default for new users):

| # | ID | Arabic Label | English Label |
|---|-----|-------------|---------------|
| 1 | sepia | افتراضي | Default |
| 2 | light | فاتح | Light |
| 3 | night | ليلي | Night |
| 4 | paper | ورقي | Paper |
| 5 | ocean | محيط | Ocean |
| 6 | tajweed | تجويد | Tajweed |
| 7 | custom | مخصص | Custom |

**Fresh install default**: `sepia` (Default theme)

---

## Theme Color Definitions

### Default (sepia)
Warm Mushaf paper — resembles a printed Quran page.

| Property | Hex | Description |
|----------|-----|-------------|
| background | #F0EDE4 | Warm Mushaf ivory |
| surface | #F5F2EB | Slightly lighter |
| textPrimary | #000000 | Black |
| textSecondary | #444444 | Dark gray |
| accent | #D6C4A6 | Warm beige |
| accentLight | #E2D5BD | Lighter beige |
| divider | #D8D4CA | Muted warm gray |
| highlightBackground | #F5EFE0 | Light gold tint |
| highlight | #BF360C | Deep orange (playing ayah) |
| ayahMarker | #D6C4A6 | Warm beige |
| topBarBackground | #D6C4A6 | Warm beige header |
| topBarContent | #000000 | Black (on light header) |
| isDark | false | |

### Light
Classic cream with Islamic green text.

| Property | Hex | Description |
|----------|-----|-------------|
| background | #FAF8F3 | Cream |
| surface | #FFFFFF | White |
| textPrimary | #1B5E20 | Dark green |
| textSecondary | #666666 | Gray |
| accent | #2E7D32 | Islamic green |
| accentLight | #66BB6A | Light green |
| divider | #E0E0E0 | Light gray |
| highlightBackground | #FFF8E1 | Light amber/gold |
| highlight | #FF6F00 | Orange (playing ayah) |
| ayahMarker | #D4AF37 | Gold |
| topBarBackground | #2E7D32 | Green header |
| topBarContent | #FFFFFF | White |
| isDark | false | |

### Night
True dark for OLED — dimmed colors for eye comfort.

| Property | Hex | Description |
|----------|-----|-------------|
| background | #000000 | Pure black |
| surface | #121212 | Near black |
| textPrimary | #B0B0B0 | Dimmed light gray |
| textSecondary | #757575 | Medium gray |
| accent | #4A6B4D | Dimmed muted green |
| accentLight | #3D5C40 | Even more dimmed |
| divider | #2D2D2D | Dark gray |
| highlightBackground | #1B3D1E | Very dark green |
| highlight | #E6A500 | Gold (playing ayah) |
| ayahMarker | #CDAD00 | Dimmed gold |
| topBarBackground | #121212 | Near black |
| topBarContent | #B0B0B0 | Dimmed gray |
| isDark | true | |

### Ocean
Calm blue tones.

| Property | Hex | Description |
|----------|-----|-------------|
| background | #E3F2FD | Light blue |
| surface | #BBDEFB | Slightly deeper blue |
| textPrimary | #0D47A1 | Dark blue |
| textSecondary | #1565C0 | Medium blue |
| accent | #1976D2 | Blue |
| accentLight | #64B5F6 | Light blue |
| divider | #90CAF9 | Soft blue |
| highlightBackground | #B3E5FC | Light cyan |
| highlight | #FF6D00 | Orange (playing ayah) |
| ayahMarker | #0288D1 | Blue marker |
| topBarBackground | #1565C0 | Dark blue header |
| topBarContent | #FFFFFF | White |
| isDark | false | |

### Paper
Classic book feel with blue ink.

| Property | Hex | Description |
|----------|-----|-------------|
| background | #FFFDF7 | Off-white paper |
| surface | #FFFEFA | Near white |
| textPrimary | #2C2C2C | Near black |
| textSecondary | #555555 | Dark gray |
| accent | #1565C0 | Blue ink |
| accentLight | #42A5F5 | Light blue |
| divider | #E8E8E8 | Light gray |
| highlightBackground | #FFF9C4 | Light yellow |
| highlight | #D84315 | Deep orange (playing ayah) |
| ayahMarker | #1565C0 | Blue |
| topBarBackground | #37474F | Blue gray header |
| topBarContent | #FFFDF7 | Off-white |
| isDark | false | |

### Tajweed
Pure white background — text is colored by Tajweed rules (requires Tajweed font).

| Property | Hex | Description |
|----------|-----|-------------|
| background | #FFFFFF | Pure white |
| surface | #FFFFFF | White |
| textPrimary | #000000 | Black (colored by font) |
| textSecondary | #666666 | Gray |
| accent | #2E7D32 | Islamic green |
| accentLight | #66BB6A | Light green |
| divider | #E0E0E0 | Light gray |
| highlightBackground | #FFF8E1 | Light amber |
| highlight | #FF6F00 | Orange (playing ayah) |
| ayahMarker | #D4AF37 | Gold |
| topBarBackground | #2E7D32 | Green header |
| topBarContent | #FFFFFF | White |
| isDark | false | |

### Custom
User picks 3 colors; all other colors are derived automatically.

**User-configurable:**
- `backgroundColor` — page background
- `textColor` — primary text
- `headerColor` — top bar, accent, ayah marker

**Derived colors:**
- `isDark`: based on background luminance (`0.299*R + 0.587*G + 0.114*B < 0.5`)
- `textSecondary`: textColor at 70% alpha
- `accentLight`: headerColor at 70% alpha
- `divider`: white at 12% alpha (dark bg) or black at 12% alpha (light bg)
- `highlightBackground`: headerColor at 10% alpha
- `highlight`: Gold #FFB300 (dark bg) or Orange #FF6F00 (light bg)
- `topBarContent`: White (dark header) or Black (light header), based on header luminance

---

## SVG Mushaf Recoloring

The SVG Quran pages contain 3 fill colors that get replaced at render time:

| Original SVG Color | Target | Description |
|--------------------|--------|-------------|
| `#231f20` | `textPrimary` | Quran text and ayah number digits |
| `#bfe8c1` | ornament blend | Ayah ornament circles |
| `#ffffff` | `background` | Page background fill |

### Ornament Color Blending

Ornament circles are blended with the background to keep them lighter than the text (so ayah numbers inside remain readable):

- **Regular pages**: `40% accent + 60% background`
- **Pages 1-2 (muted)**: `20% accent + 80% background`

This ensures:
- Light themes: ornaments are a subtle tint
- Dark themes: ornaments contrast with light-colored text
- Custom themes with bright colors: ornaments remain visible (40% strength)

### Surah Header SVG

Each theme maps to a surah header SVG variant:

| Theme | Header SVG |
|-------|-----------|
| Night | `surah_header_dark.svg` |
| Ocean | `surah_header_blue.svg` |
| Default, Light, Paper, Tajweed, Custom | `surah_header_green.svg` |

The surah name text color uses `ayahMarker` from the theme.

---

## Tajweed Theme — Font Download Gate

The Tajweed theme requires the Tajweed font to be downloaded. If not downloaded:

1. User taps Tajweed chip → theme does **NOT** switch
2. A dialog appears:
   - **Title**: "Tajweed Font Required" / "خط التجويد مطلوب"
   - **Message**: "The Tajweed font must be downloaded to use the Tajweed theme" / "يجب تحميل خط التجويد لاستخدام مظهر التجويد"
   - **Download button** (green): starts download, shows progress bar with percentage
   - **Cancel button**: dismisses dialog, theme stays unchanged
3. After download completes → Tajweed theme is auto-applied

### Download API

- **Endpoint**: `GET https://alfurqan.online/api/v1/fonts` (Tajweed font files)
- Track progress and show `LinearProgressIndicator` with percentage text

### ViewModel Flow

```
setReadingTheme(TAJWEED):
  if tajweedFontDownloaded:
    apply theme
    enable tajweed mode
  else:
    show download prompt dialog

downloadAndApplyTajweed():
  dismiss dialog
  download tajweed font with progress
  if download success:
    apply TAJWEED theme
    enable tajweed mode
```

---

## Theme Settings UI

### Layout

```
Reading Theme
┌─────────────────────────────────────────┐
│  [Default] [Light] [Night] [Paper]      │  ← chip-style buttons
│  [Ocean] [Tajweed] [Custom]             │     (FlowRow layout)
├─────────────────────────────────────────┤
│  [Custom color pickers - if Custom]     │
│  Background | Text | Header             │
├─────────────────────────────────────────┤
│  Theme Preview                          │
│  ┌───────────────────────────────────┐  │
│  │  بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ  │  │
│  │  (sample text in theme colors)    │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

### Theme Chip Behavior

- Selected chip is visually highlighted
- Tapping applies the theme immediately (except Tajweed without font → shows download dialog)
- Selecting Custom reveals 3 color pickers below the chips

---

## ThemeColors Data Model

```
ReadingThemeColors:
  background: Color        // Page background
  surface: Color           // Card/elevated surfaces
  textPrimary: Color       // Main Quran text
  textSecondary: Color     // Secondary/muted text
  accent: Color            // Primary accent (header, ornaments base)
  accentLight: Color       // Lighter accent variant
  divider: Color           // Separator lines
  cardBackground: Color    // Card backgrounds
  highlightBackground: Color // Selected ayah background
  highlight: Color         // Currently playing ayah text color
  ayahMarker: Color        // Ayah number marker / surah header text
  topBarBackground: Color  // Navigation bar background
  topBarContent: Color     // Navigation bar text/icons
  bottomBarBackground: Color // Bottom bar background
  isDark: Bool             // Whether this is a dark theme
```

---

## iOS-Specific Notes

- Use `@AppStorage` or `UserDefaults` to persist the selected theme ID (string)
- Default value for fresh install: `"sepia"`
- For custom theme colors, store as hex strings or integer ARGB values
- Night theme: consider matching iOS system dark mode preference
- Tajweed font download: use `URLSession` with progress delegate
- SVG recoloring: apply string replacements before rendering with SVG library (e.g., SVGKit or Macaw)
- Theme chip layout: use SwiftUI `LazyVGrid` or `FlowLayout` equivalent
- Custom color pickers: use `ColorPicker` (iOS 14+)
