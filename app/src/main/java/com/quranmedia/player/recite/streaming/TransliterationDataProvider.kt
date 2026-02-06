package com.quranmedia.player.recite.streaming

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber

/**
 * Provider for Quran transliteration data.
 * Loads word-level transliterations from assets for romanized matching.
 */
class TransliterationDataProvider(private val context: Context) {

    // Cache of transliteration data: surahNumber -> ayahNumber -> list of word transliterations
    private var transliterationCache: Map<Int, SurahTransliteration>? = null

    data class WordData(
        val index: Int,
        val arabic: String,
        val transliteration: String,
        val normalized: String // Pre-normalized for matching
    )

    data class AyahTransliteration(
        val ayahArabic: String,
        val words: List<WordData>
    )

    data class SurahTransliteration(
        val surahNumber: Int,
        val surahNameAr: String,
        val surahNameEn: String,
        val ayahs: Map<Int, AyahTransliteration>
    )

    /**
     * Load transliteration data from assets.
     * Call this once at startup or when needed.
     */
    fun loadData() {
        if (transliterationCache != null) return

        try {
            val jsonString = context.assets.open("quran_words_all.json")
                .bufferedReader()
                .use { it.readText() }

            val gson = Gson()
            val type = object : TypeToken<Map<String, RawSurahData>>() {}.type
            val rawData: Map<String, RawSurahData> = gson.fromJson(jsonString, type)

            // Convert to our data structure with pre-normalized transliterations
            transliterationCache = rawData.mapKeys { it.key.toInt() }.mapValues { (surahNum, rawSurah) ->
                SurahTransliteration(
                    surahNumber = rawSurah.surah_number,
                    surahNameAr = rawSurah.surah_name_ar,
                    surahNameEn = rawSurah.surah_name_en,
                    ayahs = rawSurah.ayahs.mapKeys { it.key.toInt() }.mapValues { (_, rawAyah) ->
                        AyahTransliteration(
                            ayahArabic = rawAyah.ayah_arabic,
                            words = rawAyah.words.map { rawWord ->
                                WordData(
                                    index = rawWord.index,
                                    arabic = rawWord.arabic,
                                    transliteration = rawWord.transliteration,
                                    normalized = normalizeTransliteration(rawWord.transliteration)
                                )
                            }
                        )
                    }
                )
            }

            Timber.d("Loaded transliteration data for ${transliterationCache?.size} surahs")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load transliteration data")
        }
    }

    /**
     * Get romanized text for a range of ayahs (for prompt conditioning).
     */
    fun getRomanizedText(surahNumber: Int, startAyah: Int, endAyah: Int): String {
        ensureLoaded()

        val surah = transliterationCache?.get(surahNumber) ?: return ""
        val words = mutableListOf<String>()

        for (ayahNum in startAyah..endAyah) {
            val ayah = surah.ayahs[ayahNum] ?: continue
            words.addAll(ayah.words.map { it.transliteration })
        }

        return words.joinToString(" ")
    }

    /**
     * Get transliteration data for a specific ayah.
     */
    fun getAyahTransliteration(surahNumber: Int, ayahNumber: Int): AyahTransliteration? {
        ensureLoaded()
        return transliterationCache?.get(surahNumber)?.ayahs?.get(ayahNumber)
    }

    /**
     * Get all word transliterations for a range of ayahs.
     * Returns list of (ayahNumber, list of WordData).
     */
    fun getWordsForRange(surahNumber: Int, startAyah: Int, endAyah: Int): List<Pair<Int, List<WordData>>> {
        ensureLoaded()

        val surah = transliterationCache?.get(surahNumber) ?: return emptyList()
        val result = mutableListOf<Pair<Int, List<WordData>>>()

        for (ayahNum in startAyah..endAyah) {
            val ayah = surah.ayahs[ayahNum] ?: continue
            result.add(ayahNum to ayah.words)
        }

        return result
    }

    private fun ensureLoaded() {
        if (transliterationCache == null) {
            loadData()
        }
    }

    companion object {
        /**
         * Normalize transliteration for fuzzy matching.
         * Removes diacritical marks, converts to lowercase, removes special chars.
         */
        fun normalizeTransliteration(text: String): String {
            var result = text.lowercase()

            // Replace diacritical variants
            val replacements = mapOf(
                'ā' to 'a', 'ī' to 'i', 'ū' to 'u',
                'ṣ' to 's', 'ḍ' to 'd', 'ṭ' to 't', 'ẓ' to 'z',
                'ḥ' to 'h', 'ġ' to 'g', 'ḫ' to 'k',
                'ʿ' to ' ', 'ʾ' to ' ', '\'' to ' ',
                '-' to ' ', '_' to ' '
            )

            for ((old, new) in replacements) {
                result = result.replace(old, new)
            }

            // Remove extra spaces and trim
            return result.replace(Regex("\\s+"), "").trim()
        }
    }

    // Raw JSON data classes for parsing
    private data class RawWordData(
        val index: Int,
        val arabic: String,
        val transliteration: String,
        val translation: String
    )

    private data class RawAyahData(
        val ayah_arabic: String,
        val words: List<RawWordData>
    )

    private data class RawSurahData(
        val surah_number: Int,
        val surah_name_ar: String,
        val surah_name_en: String,
        val ayahs: Map<String, RawAyahData>
    )
}
