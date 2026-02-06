package com.quranmedia.player.wear.domain.model

/**
 * Represents a Surah (chapter) of the Quran
 */
data class Surah(
    val id: Int,
    val name: String,
    val transliteration: String,
    val type: String,
    val totalVerses: Int,
    val verses: List<Verse>
)

/**
 * Represents an Ayah (verse) of the Quran
 */
data class Verse(
    val id: Int,
    val text: String
)

/**
 * Reading position to save/restore
 */
data class QuranReadingPosition(
    val surahNumber: Int = 1,
    val verseNumber: Int = 1
)

/**
 * Page to Surah mapping (first ayah of each page)
 * Standard Madani Mushaf has 604 pages
 */
object QuranPageMapping {
    // Map of page number to (surahNumber, ayahNumber)
    // This is the standard Madani mushaf page mapping
    private val pageToSurah = mapOf(
        1 to Pair(1, 1), 2 to Pair(2, 1), 3 to Pair(2, 6), 4 to Pair(2, 17), 5 to Pair(2, 25),
        6 to Pair(2, 30), 7 to Pair(2, 38), 8 to Pair(2, 49), 9 to Pair(2, 58), 10 to Pair(2, 62),
        11 to Pair(2, 70), 12 to Pair(2, 77), 13 to Pair(2, 84), 14 to Pair(2, 89), 15 to Pair(2, 94),
        16 to Pair(2, 102), 17 to Pair(2, 106), 18 to Pair(2, 113), 19 to Pair(2, 120), 20 to Pair(2, 127),
        21 to Pair(2, 135), 22 to Pair(2, 142), 23 to Pair(2, 146), 24 to Pair(2, 154), 25 to Pair(2, 164),
        26 to Pair(2, 170), 27 to Pair(2, 177), 28 to Pair(2, 182), 29 to Pair(2, 187), 30 to Pair(2, 191),
        31 to Pair(2, 197), 32 to Pair(2, 203), 33 to Pair(2, 211), 34 to Pair(2, 216), 35 to Pair(2, 220),
        36 to Pair(2, 225), 37 to Pair(2, 231), 38 to Pair(2, 234), 39 to Pair(2, 238), 40 to Pair(2, 246),
        41 to Pair(2, 249), 42 to Pair(2, 253), 43 to Pair(2, 257), 44 to Pair(2, 260), 45 to Pair(2, 265),
        46 to Pair(2, 270), 47 to Pair(2, 275), 48 to Pair(2, 282), 49 to Pair(2, 283), 50 to Pair(3, 1),
        // Simplified: For pages 50-604, map to approximate surah starts
    )

    // Surah start pages (simplified mapping)
    val surahStartPages = listOf(
        1, 2, 50, 77, 106, 128, 151, 177, 187, 208,
        221, 235, 249, 255, 262, 267, 282, 293, 305, 312,
        322, 332, 342, 350, 359, 367, 377, 385, 396, 404,
        411, 415, 418, 428, 434, 440, 446, 453, 458, 467,
        477, 483, 489, 496, 499, 502, 507, 511, 515, 518,
        520, 523, 526, 528, 531, 534, 537, 542, 545, 549,
        551, 553, 554, 556, 558, 560, 562, 564, 566, 568,
        570, 572, 574, 575, 577, 578, 580, 582, 583, 585,
        586, 587, 587, 589, 590, 591, 591, 592, 593, 594,
        595, 595, 596, 596, 597, 597, 598, 598, 599, 599,
        600, 600, 601, 601, 601, 602, 602, 602, 603, 603,
        603, 604, 604, 604
    )

    fun getSurahForPage(page: Int): Int {
        if (page < 1 || page > 604) return 1
        // Find the surah that starts at or before this page
        for (i in surahStartPages.indices.reversed()) {
            if (surahStartPages[i] <= page) {
                return i + 1
            }
        }
        return 1
    }

    fun getPageForSurah(surahNumber: Int): Int {
        if (surahNumber < 1 || surahNumber > 114) return 1
        return surahStartPages.getOrElse(surahNumber - 1) { 1 }
    }
}
