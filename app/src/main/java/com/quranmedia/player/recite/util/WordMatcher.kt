package com.quranmedia.player.recite.util

import com.quranmedia.player.recite.domain.model.MismatchType
import com.quranmedia.player.recite.domain.model.ReciteSelection
import com.quranmedia.player.recite.domain.model.WordMismatch
import kotlin.math.min

/**
 * Utility for matching transcribed text against expected Quran text
 * Uses Levenshtein distance for fuzzy matching
 */
object WordMatcher {

    private const val FUZZY_MATCH_THRESHOLD = 0.70f // 70% similarity

    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) {
            dp[i][0] = i
        }

        for (j in 0..len2) {
            dp[0][j] = j
        }

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(
                        dp[i - 1][j] + 1,      // deletion
                        dp[i][j - 1] + 1       // insertion
                    ),
                    dp[i - 1][j - 1] + cost    // substitution
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * Calculate similarity ratio between two strings
     * @return Value between 0.0 and 1.0 (1.0 = perfect match)
     */
    private fun calculateSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f

        val maxLen = kotlin.math.max(s1.length, s2.length)
        val distance = levenshteinDistance(s1, s2)

        return 1.0f - (distance.toFloat() / maxLen.toFloat())
    }

    /**
     * Check if two words match (exact or fuzzy)
     */
    private fun wordsMatch(expected: String, detected: String): Boolean {
        if (expected == detected) return true

        val similarity = calculateSimilarity(expected, detected)
        return similarity >= FUZZY_MATCH_THRESHOLD
    }

    /**
     * Match transcribed text against expected text word-by-word
     * @param expectedWords List of expected normalized words
     * @param transcribedWords List of transcribed normalized words
     * @param selection Recitation selection for ayah numbering
     * @return List of mismatches found
     */
    fun matchWords(
        expectedWords: List<String>,
        transcribedWords: List<String>,
        selection: ReciteSelection
    ): List<WordMismatch> {
        val mismatches = mutableListOf<WordMismatch>()

        var expectedIndex = 0
        var transcribedIndex = 0

        // Calculate approximate words per ayah
        val totalAyahs = selection.endAyah - selection.startAyah + 1
        val wordsPerAyah = if (totalAyahs > 0) expectedWords.size / totalAyahs else expectedWords.size

        while (expectedIndex < expectedWords.size || transcribedIndex < transcribedWords.size) {
            // Calculate current ayah number based on word position
            val currentAyah = selection.startAyah + (expectedIndex / kotlin.math.max(1, wordsPerAyah))

            when {
                // Both lists have words remaining
                expectedIndex < expectedWords.size && transcribedIndex < transcribedWords.size -> {
                    val expectedWord = expectedWords[expectedIndex]
                    val transcribedWord = transcribedWords[transcribedIndex]

                    if (wordsMatch(expectedWord, transcribedWord)) {
                        // Words match - continue
                        expectedIndex++
                        transcribedIndex++
                    } else {
                        // Check if transcribed word matches next expected word (missing word case)
                        val matchesNext = expectedIndex + 1 < expectedWords.size &&
                            wordsMatch(expectedWords[expectedIndex + 1], transcribedWord)

                        if (matchesNext) {
                            // Current expected word was missing
                            mismatches.add(
                                WordMismatch(
                                    surahNumber = selection.surahNumber,
                                    ayahNumber = currentAyah,
                                    wordIndex = expectedIndex,
                                    expectedWord = expectedWord,
                                    detectedWord = null,
                                    type = MismatchType.MISSING
                                )
                            )
                            expectedIndex++
                        } else {
                            // Words don't match - mark as incorrect
                            mismatches.add(
                                WordMismatch(
                                    surahNumber = selection.surahNumber,
                                    ayahNumber = currentAyah,
                                    wordIndex = expectedIndex,
                                    expectedWord = expectedWord,
                                    detectedWord = transcribedWord,
                                    type = MismatchType.INCORRECT
                                )
                            )
                            expectedIndex++
                            transcribedIndex++
                        }
                    }
                }

                // Only expected words remaining - all are missing
                expectedIndex < expectedWords.size -> {
                    mismatches.add(
                        WordMismatch(
                            surahNumber = selection.surahNumber,
                            ayahNumber = currentAyah,
                            wordIndex = expectedIndex,
                            expectedWord = expectedWords[expectedIndex],
                            detectedWord = null,
                            type = MismatchType.MISSING
                        )
                    )
                    expectedIndex++
                }

                // Only transcribed words remaining - all are extra
                transcribedIndex < transcribedWords.size -> {
                    mismatches.add(
                        WordMismatch(
                            surahNumber = selection.surahNumber,
                            ayahNumber = currentAyah,
                            wordIndex = expectedIndex,
                            expectedWord = "",
                            detectedWord = transcribedWords[transcribedIndex],
                            type = MismatchType.EXTRA
                        )
                    )
                    transcribedIndex++
                }
            }
        }

        return mismatches
    }

    /**
     * Calculate accuracy percentage
     * @param totalWords Total number of expected words
     * @param mismatches Number of mismatches found
     * @return Accuracy percentage (0-100)
     */
    fun calculateAccuracy(totalWords: Int, mismatches: Int): Float {
        if (totalWords == 0) return 100f

        val correctWords = kotlin.math.max(0, totalWords - mismatches)
        return (correctWords.toFloat() / totalWords.toFloat()) * 100f
    }
}
