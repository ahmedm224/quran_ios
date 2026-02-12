# Tafseer Settings & View Modal - iOS Implementation Guide

## Overview

Two related features for managing and viewing Tafseer (Quran interpretation):
1. **Settings**: Dropdown-based tafseer download manager
2. **View Modal**: In-reader tafseer viewer with inline download capability

---

## Part 1: Tafseer Downloads in Settings

### Layout

```
Tafseer Downloads
Download tafseer for offline reading

┌──────────────────────────────────┐  ┌──────┐
│  [✓] تفسير ابن كثير          ▼  │  │  🗑️  │   ← downloaded: delete button
└──────────────────────────────────┘  └──────┘

┌──────────────────────────────────┐  ┌──────┐
│  Al-Tafsir Al-Muyassar       ▼  │  │  ⬇️  │   ← not downloaded: green download button
└──────────────────────────────────┘  └──────┘
```

### Dropdown Contents

The dropdown shows ALL available tafseers in a single list, divided into two visual groups:

**Group 1 — Tafseers** (normal background):
- Each item shows: `[Lang Badge] [Name] [✓ if downloaded]`
- Tapping a downloaded tafseer selects it
- Tapping a not-downloaded tafseer triggers download

**Divider line**

**Group 2 — Word Meanings & Grammar** (shaded background):
- Same item format but with distinct background tint
- Grammar items get a special badge ("نحو" / "Gram") instead of language badge

### Language Badges

| Badge | Color | Meaning |
|-------|-------|---------|
| ع | Gold (#D4AF37) on gold bg | Arabic content |
| EN | Green (#2E7D32) on green bg | English content |
| نحو | Coffee brown on brown bg | Grammar (Irab) |

### Action Button (next to dropdown)

Depends on the selected tafseer's state:

| State | Button |
|-------|--------|
| Not downloaded | Green download icon button (white icon, green #2E7D32 bg, rounded) |
| Downloading | Circular progress indicator with percentage text inside |
| Downloaded | Red delete icon button |

### Download Progress

When downloading, a linear progress bar appears below the dropdown row.

### Sort Order

**Critical**: Both settings and modal use the same sort order from `AvailableTafseers.getSortedByLanguage(appLanguage)`:

**Arabic app language order**:
1. تفسير ابن كثير (Ibn Kathir Arabic) — priority
2. Other Arabic tafseers: التفسير الميسر, تفسير السعدي, تفسير الطبري
3. English tafseers: Tafsir Ibn Kathir (EN), Ma'ariful Quran
4. إعراب القرآن (Grammar/Irab)
5. مفردات القرآن (Mufradat, Arabic word meanings)
6. Word by Word Translation (English word meanings)

**English app language order**:
1. English tafseers: Tafsir Ibn Kathir (EN), Ma'ariful Quran
2. Arabic tafseers: تفسير ابن كثير, التفسير الميسر, تفسير السعدي, تفسير الطبري
3. إعراب القرآن (Grammar/Irab)
4. Word by Word Translation (English)
5. مفردات القرآن (Mufradat, Arabic)

---

## Part 2: Tafseer View Modal

Shown when user long-presses or taps an ayah in the Quran reader and selects "Tafseer".

### Modal Layout

```
┌──────────────────────────────────────────┐
│  Header (gradient cream background)      │
│  تفسير الآية 5  /  Tafseer - Ayah 5  [✕]│
│  سورة الفاتحة                            │
├──────────────────────────────────────────┤
│  Tafseer Selector (dropdown)             │
│  [📖] [ع] تفسير ابن كثير  [⬇️] [▼]     │
├──────────────────────────────────────────┤
│  Content Area (scrollable)               │
│                                          │
│  ┌─ Ayah Text Card (gold border) ──────┐ │
│  │  بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ │ │
│  └─────────────────────────────────────┘ │
│                                          │
│  ┌─ Tafseer Content Card ──────────────┐ │
│  │  📖 تفسير ابن كثير                  │ │
│  │                                     │ │
│  │  [Tafseer text content...]          │ │
│  └─────────────────────────────────────┘ │
│                                          │
├──────────────────────────────────────────┤
│  Footer (gradient)         [📋 Copy]     │
└──────────────────────────────────────────┘
```

### Tafseer Selector (in Modal)

The selector dropdown shows ALL tafseers (downloaded AND not downloaded):

**Downloaded tafseers**:
- Show check icon (✓)
- Tapping selects and displays content

**Not-downloaded tafseers**:
- Show download icon (⬇️)
- Name appears in muted/secondary color
- Tapping triggers inline download
- After download completes: content loads automatically and tafseer is selected

**Selected tafseer display** (the collapsed row):
- Shows: `[📖] [Lang Badge] [Tafseer Name] [⬇️ if not downloaded] [▼]`
- Download button appears next to dropdown arrow when selected tafseer is not downloaded
- Shows circular progress during download

### Dropdown Item Layout

Same grouping as settings (tafseers first, divider, word meanings & grammar with shaded bg):

```
┌─────────────────────────────────────┐
│  [ع] تفسير ابن كثير           [✓]  │  ← downloaded
│  [ع] التفسير الميسر            [✓]  │  ← downloaded
│  [EN] Tafsir Ibn Kathir        [⬇️] │  ← not downloaded
│  [EN] Ma'ariful Quran          [⬇️] │  ← not downloaded
├─────────────────────────────────────┤
│  [نحو] إعراب القرآن            [✓]  │  ← shaded bg
│  [ع] مفردات القرآن             [⬇️] │  ← shaded bg
│  [EN] Word by Word Translation [⬇️] │  ← shaded bg
└─────────────────────────────────────┘
```

### Empty State

When no tafseers are downloaded, the content area shows:
- Book icon (muted)
- "Select a tafseer from the list above to download" (the selector is still available with all tafseers)

### Content Rendering

**Regular tafseers** (Arabic or English):
- Arabic: Scheherazade font, 17sp, RTL, 30sp line height
- English: System font, 15sp, LTR, 24sp line height
- Text is cleaned of problematic Unicode characters (ornamental marks, isolated diacritics)

**Word meanings**:
- Always RTL (contains Arabic words even in English translation)
- Format: `arabicWord: meaning` (one per paragraph)
- Arabic word rendered **bold** in green accent color
- Meaning rendered in regular weight

**Grammar (Irab)**:
- Always RTL, Arabic font
- Regular text rendering

### Download from Modal - ViewModel Flow

```
downloadTafseerFromModal(tafseerId):
  1. Set downloadingTafseerId = tafseerId, downloadProgress = 0
  2. Download tafseer with progress callback → updates downloadProgress
  3. On success:
     a. Refresh downloadedIds set
     b. Reload ALL tafseer content for current surah:ayah
     c. Re-sort content by language preference
     d. Auto-select the newly downloaded tafseer
     e. Clear downloading state
  4. On failure:
     a. Clear downloading state
```

### Modal State

```
TafseerModalState:
  isVisible: Bool
  surah: Int
  ayah: Int
  surahName: String
  ayahText: String
  availableTafseers: [(TafseerInfo, TafseerContent)]  // downloaded with content
  allTafseers: [TafseerInfo]                           // all (for dropdown)
  downloadedIds: Set<String>
  downloadingTafseerId: String?
  downloadProgress: Float                              // 0.0 to 1.0
  selectedTafseerId: String?
  isLoading: Bool
  error: String?
```

---

## Part 3: Available Tafseers Data Model

```
TafseerInfo:
  id: String           // e.g. "ibn-kathir"
  nameArabic: String?  // e.g. "تفسير ابن كثير"
  nameEnglish: String  // e.g. "Tafsir Ibn Kathir"
  language: String     // "arabic" or "english"
  type: TafseerType    // TAFSEER, WORD_MEANING, or GRAMMAR
  downloadUrl: String  // API path or "bundled:filename.json"

TafseerType:
  TAFSEER       // Full tafseer/interpretation
  WORD_MEANING  // Word-by-word meanings
  GRAMMAR       // Grammatical analysis (Irab)
```

### Complete Tafseer Catalog

| ID | Arabic Name | English Name | Language | Type | Source |
|----|------------|--------------|----------|------|--------|
| quran-irab | إعراب القرآن | Quran Grammar (Irab) | arabic | GRAMMAR | Bundled |
| word-by-word-english | — | Word by Word Translation | english | WORD_MEANING | API |
| mufradat | مفردات القرآن | Quran Mufradat | arabic | WORD_MEANING | API |
| ibn-kathir-english | تفسير ابن كثير | Tafsir Ibn Kathir | english | TAFSEER | API |
| maarif-ul-quran | معارف القرآن | Ma'ariful Quran | english | TAFSEER | API |
| al-saddi | تفسير السعدي | Tafsir Al-Saddi | arabic | TAFSEER | API |
| al-tabari | تفسير الطبري | Tafsir Al-Tabari | arabic | TAFSEER | API |
| ibn-kathir | تفسير ابن كثير | Tafsir Ibn Kathir | arabic | TAFSEER | API |
| muyassar | التفسير الميسر | Al-Tafsir Al-Muyassar | arabic | TAFSEER | Bundled |

### API Endpoints

- **Download**: `GET https://alfurqan.online/api/v1/tafseer/download/{tafseer-id}`
- **Bundled items**: Loaded from app assets (no network needed)
  - `quran_irab_by_surah.json` — Grammar
  - `Tafseer-muyassar.json` — Al-Muyassar

---

## Part 4: Modal Colors

| Name | Hex | Usage |
|------|-----|-------|
| tafseerCardBackground | #FAF8F5 | Modal card bg |
| tafseerHeaderBackground | #F5F0E8 | Header/selector bg |
| tafseerAccent | #2E7D32 | Green accent, selected items |
| tafseerGold | #D4AF37 | Gold accent, Arabic badges |
| tafseerTextPrimary | #3E2723 | Main text color |
| tafseerTextSecondary | #6D4C41 | Muted text, not-downloaded items |

## iOS-Specific Notes

- Use `UIMenu` or a custom picker for the dropdown in settings
- The modal can be presented as a `.sheet` or custom overlay
- For Arabic text rendering, use Scheherazade font (or system Arabic font as fallback)
- RTL layout: Use `environment(\.layoutDirection, .rightToLeft)` for Arabic content sections
- Download progress: Use `URLSession` with delegate for progress tracking
- Bundled JSON files should be included in the app bundle (no download needed)
