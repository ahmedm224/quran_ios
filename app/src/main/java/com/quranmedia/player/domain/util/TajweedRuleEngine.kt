package com.quranmedia.player.domain.util

import timber.log.Timber

/**
 * Tajweed Rule Engine - Analyzes Arabic Quranic text and generates Tajweed annotations
 *
 * This engine applies actual Tajweed rules to detect:
 * - Ghunnah (nasal sound)
 * - Idgham (merging) with/without Ghunnah
 * - Ikhfa (hiding)
 * - Iqlab (conversion)
 * - Qalqalah (echoing)
 * - Madd (prolongation) - various types
 * - Hamzat Wasl (connecting hamza)
 * - Lam Shamsiyyah (sun letter)
 * - Silent letters
 */
object TajweedRuleEngine {

    // Arabic letter constants
    private const val NOON = 'ن'
    private const val MEEM = 'م'
    private const val LAM = 'ل'
    private const val ALIF = 'ا'
    private const val ALIF_KHANJARIYAH = 'ٰ'
    private const val ALIF_MADDA = 'آ'
    private const val ALIF_HAMZA_ABOVE = 'أ'
    private const val ALIF_HAMZA_BELOW = 'إ'
    private const val WAW = 'و'
    private const val YA = 'ي'
    private const val HAMZA = 'ء'
    private const val TA_MARBUTA = 'ة'

    // Special markers
    private const val HAMZAT_WASL = 'ٱ'
    private const val ALIF_WASLA = 'ٱ'

    // Diacritics
    private const val FATHA = 'َ'
    private const val DAMMA = 'ُ'
    private const val KASRA = 'ِ'
    private const val SUKUN = 'ۡ'
    private const val SHADDA = 'ّ'
    private const val TANWEEN_FATH = 'ً'
    private const val TANWEEN_DAMM = 'ٌ'
    private const val TANWEEN_KASR = 'ٍ'
    private const val MADD = 'ٓ'
    private const val SMALL_ALIF = 'ٰ'

    // All Alef variations that can form ligatures with Lam
    private val ALEF_VARIANTS = setOf(ALIF, ALIF_MADDA, ALIF_HAMZA_ABOVE, ALIF_HAMZA_BELOW, HAMZAT_WASL)

    // Additional ligature pairs in KFGQPC Hafs font
    // These character sequences should not have colors applied between them
    private val LIGATURE_PAIRS = listOf(
        Pair(LAM, ALIF),
        Pair(LAM, ALIF_MADDA),
        Pair(LAM, ALIF_HAMZA_ABOVE),
        Pair(LAM, ALIF_HAMZA_BELOW),
        Pair(LAM, HAMZAT_WASL)
    )

    // Qalqalah letters (ق ط ب ج د)
    private val QALQALAH_LETTERS = setOf('ق', 'ط', 'ب', 'ج', 'د')

    // Idgham letters - with Ghunnah (ي ن م و)
    private val IDGHAM_WITH_GHUNNAH = setOf('ي', 'ن', 'م', 'و')

    // Idgham letters - without Ghunnah (ل ر)
    private val IDGHAM_WITHOUT_GHUNNAH = setOf('ل', 'ر')

    // Ikhfa letters (15 letters)
    private val IKHFA_LETTERS = setOf(
        'ت', 'ث', 'ج', 'د', 'ذ', 'ز', 'س', 'ش',
        'ص', 'ض', 'ط', 'ظ', 'ف', 'ق', 'ك'
    )

    // Sun letters (for Lam Shamsiyyah)
    private val SUN_LETTERS = setOf(
        'ت', 'ث', 'د', 'ذ', 'ر', 'ز', 'س', 'ش',
        'ص', 'ض', 'ط', 'ظ', 'ل', 'ن'
    )

    /**
     * Analyze text and generate Tajweed annotations
     */
    fun analyzeText(text: String): List<TajweedAnnotation> {
        val annotations = mutableListOf<TajweedAnnotation>()

        // Strip Kashida for analysis
        val cleanText = text.replace("\u0640", "")

        // Build a set of positions that are part of Lam-Alef ligatures
        // This includes: لا، لأ، لإ، لآ، لٱ
        // These positions should NOT have individual colors applied to preserve ligature formation
        val lamAlifPositions = mutableSetOf<Int>()
        for (i in 0 until cleanText.length) {
            if (cleanText[i] == LAM) {
                // Check if next non-diacritic character is any Alef variant
                var nextIndex = i + 1
                while (nextIndex < cleanText.length && isDiacritic(cleanText[nextIndex])) {
                    nextIndex++
                }
                if (nextIndex < cleanText.length && cleanText[nextIndex] in ALEF_VARIANTS) {
                    // This is a Lam-Alef ligature (لا، لأ، لإ، لآ، لٱ)
                    lamAlifPositions.add(i)           // Lam position
                    lamAlifPositions.add(nextIndex)   // Alef variant position
                    // Also add all diacritic positions between them
                    for (j in i + 1 until nextIndex) {
                        lamAlifPositions.add(j)
                    }
                }
            }
        }

        var i = 0
        while (i < cleanText.length) {
            val char = cleanText[i]

            // Skip if this character is part of لا ligature
            if (i in lamAlifPositions) {
                i++
                continue
            }

            // Check for Hamzat Wasl
            if (char == HAMZAT_WASL) {
                // Include ALL diacritics after Hamzat Wasl to prevent displacement
                val endIndex = findEndOfDiacritics(cleanText, i + 1)
                annotations.add(TajweedAnnotation("hamzat_wasl", i, endIndex))
            }

            // Check for Noon Sakinah/Tanween rules
            if (char == NOON && i + 1 < cleanText.length) {
                val nextChar = cleanText[i + 1]
                if (nextChar == SUKUN || nextChar in setOf(TANWEEN_FATH, TANWEEN_DAMM, TANWEEN_KASR)) {
                    checkNoonRules(cleanText, i, annotations)
                }
            }

            // Check for Tanween at end
            if (char in setOf(TANWEEN_FATH, TANWEEN_DAMM, TANWEEN_KASR)) {
                checkTanweenRules(cleanText, i, annotations)
            }

            // Check for Meem Sakinah
            if (char == MEEM && i + 1 < cleanText.length && cleanText[i + 1] == SUKUN) {
                checkMeemRules(cleanText, i, annotations)
            }

            // Check for Qalqalah
            if (char in QALQALAH_LETTERS && i + 1 < cleanText.length && cleanText[i + 1] == SUKUN) {
                // Include ALL diacritics after the Sukun
                val endIndex = findEndOfDiacritics(cleanText, i + 1)
                annotations.add(TajweedAnnotation("qalqalah", i, endIndex))
            }

            // Check for Madd
            checkMaddRules(cleanText, i, annotations, lamAlifPositions)

            // Check for Lam Shamsiyyah
            if (char == LAM && i > 0 && cleanText[i - 1] == ALIF_WASLA) {
                checkLamShamsiyyah(cleanText, i, annotations)
            }

            // Check for Ghunnah (Noon/Meem with Shadda)
            if ((char == NOON || char == MEEM) && i + 1 < cleanText.length && cleanText[i + 1] == SHADDA) {
                // Include ALL diacritics after the Shadda (e.g., Fatha, Kasra, etc.)
                val endIndex = findEndOfDiacritics(cleanText, i + 1)
                annotations.add(TajweedAnnotation("ghunnah", i, endIndex))
            }

            i++
        }

        return annotations.sortedBy { it.start }
    }

    /**
     * Check Noon Sakinah/Tanween rules (Ghunnah, Idgham, Ikhfa, Iqlab)
     */
    private fun checkNoonRules(text: String, index: Int, annotations: MutableList<TajweedAnnotation>) {
        // Find next Arabic letter (skip diacritics)
        var nextLetterIndex = index + 1
        while (nextLetterIndex < text.length && isDiacritic(text[nextLetterIndex])) {
            nextLetterIndex++
        }

        if (nextLetterIndex >= text.length) return

        val nextLetter = text[nextLetterIndex]

        // Include ALL diacritics after the next letter
        val endIndex = findEndOfDiacritics(text, nextLetterIndex + 1)

        when {
            // Iqlab: Noon + Ba = conversion to Meem sound
            nextLetter == 'ب' -> {
                annotations.add(TajweedAnnotation("iqlab", index, endIndex))
            }

            // Idgham with Ghunnah
            nextLetter in IDGHAM_WITH_GHUNNAH -> {
                annotations.add(TajweedAnnotation("idghaam_ghunnah", index, endIndex))
            }

            // Idgham without Ghunnah
            nextLetter in IDGHAM_WITHOUT_GHUNNAH -> {
                annotations.add(TajweedAnnotation("idghaam_no_ghunnah", index, endIndex))
            }

            // Ikhfa: hiding
            nextLetter in IKHFA_LETTERS -> {
                annotations.add(TajweedAnnotation("ikhfa", index, endIndex))
            }
        }
    }

    /**
     * Check Tanween rules
     */
    private fun checkTanweenRules(text: String, index: Int, annotations: MutableList<TajweedAnnotation>) {
        // Find next Arabic letter
        var nextLetterIndex = index + 1
        while (nextLetterIndex < text.length && isDiacritic(text[nextLetterIndex])) {
            nextLetterIndex++
        }

        if (nextLetterIndex >= text.length) return

        val nextLetter = text[nextLetterIndex]

        // Include ALL diacritics after the next letter
        val endIndex = findEndOfDiacritics(text, nextLetterIndex + 1)

        when {
            nextLetter == 'ب' -> {
                annotations.add(TajweedAnnotation("iqlab", index, endIndex))
            }
            nextLetter in IDGHAM_WITH_GHUNNAH -> {
                annotations.add(TajweedAnnotation("idghaam_ghunnah", index, endIndex))
            }
            nextLetter in IDGHAM_WITHOUT_GHUNNAH -> {
                annotations.add(TajweedAnnotation("idghaam_no_ghunnah", index, endIndex))
            }
            nextLetter in IKHFA_LETTERS -> {
                annotations.add(TajweedAnnotation("ikhfa", index, endIndex))
            }
        }
    }

    /**
     * Check Meem Sakinah rules
     */
    private fun checkMeemRules(text: String, index: Int, annotations: MutableList<TajweedAnnotation>) {
        var nextLetterIndex = index + 1
        while (nextLetterIndex < text.length && isDiacritic(text[nextLetterIndex])) {
            nextLetterIndex++
        }

        if (nextLetterIndex >= text.length) return

        val nextLetter = text[nextLetterIndex]

        // Include ALL diacritics after the next letter
        val endIndex = findEndOfDiacritics(text, nextLetterIndex + 1)

        when {
            // Idgham Shafawi: Meem + Meem
            nextLetter == MEEM -> {
                annotations.add(TajweedAnnotation("idghaam_shafawi", index, endIndex))
            }

            // Ikhfa Shafawi: Meem + Ba
            nextLetter == 'ب' -> {
                annotations.add(TajweedAnnotation("ikhfa_shafawi", index, endIndex))
            }
        }
    }

    /**
     * Check for Madd (prolongation) rules
     */
    private fun checkMaddRules(
        text: String,
        index: Int,
        annotations: MutableList<TajweedAnnotation>,
        lamAlifPositions: Set<Int>
    ) {
        val char = text[index]

        // Check for Madd letters with specific conditions
        when {
            // Alif with Fatha before
            char == ALIF && index > 0 && text[index - 1] == FATHA -> {
                // Include all diacritics after the Alif
                val endIndex = findEndOfDiacritics(text, index + 1)
                // Check if followed by Hamza (Madd Munfasil/Muttasil)
                if (index + 1 < text.length && text[index + 1] == HAMZA) {
                    annotations.add(TajweedAnnotation("madd_munfasil", index, endIndex))
                } else {
                    annotations.add(TajweedAnnotation("madd_2", index, endIndex))
                }
            }

            // Waw with Damma before
            char == WAW && index > 0 && text[index - 1] == DAMMA -> {
                val endIndex = findEndOfDiacritics(text, index + 1)
                annotations.add(TajweedAnnotation("madd_2", index, endIndex))
            }

            // Ya with Kasra before
            char == YA && index > 0 && text[index - 1] == KASRA -> {
                val endIndex = findEndOfDiacritics(text, index + 1)
                annotations.add(TajweedAnnotation("madd_2", index, endIndex))
            }

            // Madd sign (ٓ) - combining diacritic
            // MUST include previous base letter to preserve diacritic positioning
            char == MADD -> {
                // Find the previous base letter (skip diacritics)
                var baseLetterIndex = index - 1
                while (baseLetterIndex >= 0 && isDiacritic(text[baseLetterIndex])) {
                    baseLetterIndex--
                }

                // Check if base letter is part of لا ligature - if so, skip this annotation
                if (baseLetterIndex >= 0 && baseLetterIndex in lamAlifPositions) {
                    // Skip - don't color parts of لا ligature
                } else {
                    // If we found a base letter, include it in the annotation
                    val startIndex = if (baseLetterIndex >= 0) baseLetterIndex else index
                    annotations.add(TajweedAnnotation("madd_munfasil", startIndex, index + 1))
                }
            }

            // Small Alif (Alif Khanjariyah) - combining diacritic
            // MUST include previous base letter to preserve diacritic positioning
            char == SMALL_ALIF -> {
                // Find the previous base letter (skip diacritics)
                var baseLetterIndex = index - 1
                while (baseLetterIndex >= 0 && isDiacritic(text[baseLetterIndex])) {
                    baseLetterIndex--
                }

                // Check if base letter is part of لا ligature - if so, skip this annotation
                if (baseLetterIndex >= 0 && baseLetterIndex in lamAlifPositions) {
                    // Skip - don't color parts of لا ligature
                } else {
                    // If we found a base letter, include it in the annotation
                    // This prevents the Alif Madd from dropping down during rendering
                    val startIndex = if (baseLetterIndex >= 0) baseLetterIndex else index
                    annotations.add(TajweedAnnotation("madd_2", startIndex, index + 1))
                }
            }
        }
    }

    /**
     * Check for Lam Shamsiyyah (sun letters)
     */
    private fun checkLamShamsiyyah(text: String, index: Int, annotations: MutableList<TajweedAnnotation>) {
        var nextLetterIndex = index + 1
        while (nextLetterIndex < text.length && isDiacritic(text[nextLetterIndex])) {
            nextLetterIndex++
        }

        if (nextLetterIndex < text.length && text[nextLetterIndex] in SUN_LETTERS) {
            // Include ALL diacritics on the Lam to prevent displacement
            val endIndex = findEndOfDiacritics(text, index + 1)
            annotations.add(TajweedAnnotation("lam_shamsiyyah", index, endIndex))
        }
    }

    /**
     * Check if character is a diacritic
     */
    private fun isDiacritic(char: Char): Boolean {
        return char in setOf(
            FATHA, DAMMA, KASRA, SUKUN, SHADDA,
            TANWEEN_FATH, TANWEEN_DAMM, TANWEEN_KASR,
            MADD, SMALL_ALIF, '\u064B', '\u064C', '\u064D', '\u064E',
            '\u064F', '\u0650', '\u0651', '\u0652'
        )
    }

    /**
     * Find the end position after all consecutive diacritics
     * This ensures we include ALL diacritics with the base letter to prevent displacement
     */
    private fun findEndOfDiacritics(text: String, startIndex: Int): Int {
        var endIndex = startIndex
        while (endIndex < text.length && isDiacritic(text[endIndex])) {
            endIndex++
        }
        return endIndex
    }
}
