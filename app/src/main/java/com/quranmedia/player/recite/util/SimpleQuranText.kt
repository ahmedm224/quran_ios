package com.quranmedia.player.recite.util

import android.content.Context
import timber.log.Timber

/**
 * Utility for loading Tanzil Simple (Plain) Quran text for AI comparison.
 * This text format is closer to what Whisper transcribes.
 *
 * Can be disabled to fall back to normalized database text if needed.
 */
object SimpleQuranText {

    // Cumulative ayah counts for each surah (0-indexed)
    // surahStartIndex[surahNumber - 1] = first ayah's line index (0-based)
    private val AYAH_COUNTS = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109,  // 1-10
        123, 111, 43, 52, 99, 128, 111, 110, 98, 135,   // 11-20
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60,      // 21-30
        34, 30, 73, 54, 45, 83, 182, 88, 75, 85,        // 31-40
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45,         // 41-50
        60, 49, 62, 55, 78, 96, 29, 22, 24, 13,         // 51-60
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44,         // 61-70
        28, 28, 20, 56, 40, 31, 50, 40, 46, 42,         // 71-80
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20,         // 81-90
        15, 21, 11, 8, 8, 19, 5, 8, 8, 11,              // 91-100
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3,                  // 101-110
        5, 4, 5, 6                                       // 111-114
    )

    // Precomputed start indices for each surah
    private val surahStartIndices: IntArray by lazy {
        val indices = IntArray(114)
        var cumulative = 0
        for (i in 0 until 114) {
            indices[i] = cumulative
            cumulative += AYAH_COUNTS[i]
        }
        indices
    }

    // Loaded ayah texts (one per line)
    private var ayahTexts: List<String>? = null
    private var loadError: String? = null

    /**
     * Load the simple Quran text from assets
     * Call this once during app initialization or on first use
     */
    fun load(context: Context): Boolean {
        return try {
            val inputStream = context.assets.open("quran_simple.txt")
            ayahTexts = inputStream.bufferedReader().readLines()
            Timber.d("Loaded ${ayahTexts?.size} ayahs from quran_simple.txt")
            true
        } catch (e: Exception) {
            loadError = e.message
            Timber.e(e, "Failed to load quran_simple.txt")
            false
        }
    }

    /**
     * Check if simple text is loaded and available
     */
    fun isLoaded(): Boolean = ayahTexts != null

    /**
     * Get ayah text by surah and ayah number
     * @return The simple text, or null if not available
     */
    fun getAyah(surahNumber: Int, ayahNumber: Int): String? {
        val texts = ayahTexts ?: return null

        if (surahNumber < 1 || surahNumber > 114) {
            Timber.w("Invalid surah number: $surahNumber")
            return null
        }

        val surahIndex = surahNumber - 1
        val maxAyahs = AYAH_COUNTS[surahIndex]

        if (ayahNumber < 1 || ayahNumber > maxAyahs) {
            Timber.w("Invalid ayah number: $ayahNumber for surah $surahNumber (max: $maxAyahs)")
            return null
        }

        val lineIndex = surahStartIndices[surahIndex] + (ayahNumber - 1)

        return if (lineIndex < texts.size) {
            texts[lineIndex]
        } else {
            Timber.w("Line index $lineIndex out of bounds (total: ${texts.size})")
            null
        }
    }

    /**
     * Get multiple ayahs as a single string
     * @return Combined text of ayahs in the range, or null if not available
     */
    fun getAyahRange(surahNumber: Int, startAyah: Int, endAyah: Int): String? {
        val texts = mutableListOf<String>()

        for (ayahNumber in startAyah..endAyah) {
            val text = getAyah(surahNumber, ayahNumber) ?: return null
            texts.add(text)
        }

        return texts.joinToString(" ")
    }

    /**
     * Get normalized ayah range (removes diacritics for comparison)
     */
    fun getAyahRangeNormalized(surahNumber: Int, startAyah: Int, endAyah: Int): String? {
        val text = getAyahRange(surahNumber, startAyah, endAyah) ?: return null
        return ArabicNormalizer.normalize(text)
    }

    /**
     * Clear loaded data (for memory management if needed)
     */
    fun clear() {
        ayahTexts = null
        loadError = null
    }

    /**
     * Get last load error message
     */
    fun getLoadError(): String? = loadError
}
