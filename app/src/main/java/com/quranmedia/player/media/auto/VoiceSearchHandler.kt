package com.quranmedia.player.media.auto

import com.quranmedia.player.domain.model.Surah
import com.quranmedia.player.domain.repository.QuranRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceSearchHandler @Inject constructor(
    private val quranRepository: QuranRepository
) {
    /**
     * Parse voice search query and find matching surah
     * Examples:
     * - "Play Surah Al-Baqarah"
     * - "Play Surah 2"
     * - "Play Al-Fatiha"
     */
    suspend fun parseAndFindSurah(query: String): Surah? {
        val normalizedQuery = query.lowercase().trim()

        Timber.d("Parsing voice query: $query")

        // Try to extract surah number
        val numberMatch = Regex("surah\\s+(\\d+)").find(normalizedQuery)
        if (numberMatch != null) {
            val surahNumber = numberMatch.groupValues[1].toIntOrNull()
            surahNumber?.let {
                return quranRepository.getSurahByNumber(it)
            }
        }

        // Try to match surah name
        val surahs = quranRepository.getAllSurahs().first()

        // Try exact match first
        val exactMatch = surahs.find { surah ->
            normalizedQuery.contains(surah.nameEnglish.lowercase()) ||
            normalizedQuery.contains(surah.nameTransliteration.lowercase())
        }
        if (exactMatch != null) return exactMatch

        // Try fuzzy match
        val fuzzyMatch = surahs.find { surah ->
            val englishName = surah.nameEnglish.lowercase().replace("-", " ")
            val transliteration = surah.nameTransliteration.lowercase().replace("-", " ")

            normalizedQuery.contains(englishName) || normalizedQuery.contains(transliteration)
        }
        if (fuzzyMatch != null) return fuzzyMatch

        Timber.w("No surah found for query: $query")
        return null
    }

    /**
     * Common surah name variations and aliases
     */
    private fun getSurahAliases(): Map<String, Int> {
        return mapOf(
            "fatiha" to 1,
            "fatihah" to 1,
            "opening" to 1,
            "baqarah" to 2,
            "cow" to 2,
            "yasin" to 36,
            "yaseen" to 36,
            "rahman" to 55,
            "waqiah" to 56,
            "mulk" to 67,
            "kahf" to 18,
            "maryam" to 19,
            "yusuf" to 12,
            "joseph" to 12
        )
    }
}
