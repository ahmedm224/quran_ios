package com.quranmedia.player.domain.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * Helper class for parsing and rendering Tajweed-annotated Quranic text
 *
 * This class converts position-based Tajweed annotations into word-based segments
 * and renders them as colored AnnotatedString for display in Compose.
 *
 * SCOPE: Only used when Tajweed theme is active. Does not affect other themes.
 */
object TajweedHelper {

    /**
     * Converts plain Arabic text with position-based annotations into TajweedWord segments
     *
     * @param text The plain Arabic text (without markers)
     * @param annotations List of Tajweed annotations with start/end positions and rules
     * @return List of TajweedWord objects representing colored and uncolored segments
     */
    fun textToWords(text: String, annotations: List<TajweedAnnotation>): List<TajweedWord> {
        if (annotations.isEmpty()) {
            return listOf(TajweedWord(text, null))
        }

        val words = mutableListOf<TajweedWord>()
        var currentIndex = 0

        // Sort annotations by start position
        val sortedAnnotations = annotations.sortedBy { it.start }

        timber.log.Timber.d("TajweedHelper: Processing text length=${text.length}, annotations=${annotations.size}")

        for (annotation in sortedAnnotations) {
            // Skip annotation if it's completely out of bounds
            if (annotation.start >= text.length) {
                timber.log.Timber.w("TajweedHelper: Skipping out-of-bounds annotation: start=${annotation.start}, textLength=${text.length}")
                continue
            }

            // Ensure start and end are within bounds
            val safeStart = annotation.start.coerceIn(0, text.length)
            val safeEnd = annotation.end.coerceIn(0, text.length)

            // Add plain text before this annotation (if any)
            if (currentIndex < safeStart) {
                val plainText = text.substring(currentIndex, safeStart)
                if (plainText.isNotEmpty()) {
                    words.add(TajweedWord(plainText, null))
                    timber.log.Timber.d("TajweedHelper: Plain text[$currentIndex-$safeStart]: '${plainText}' (${plainText.length} chars)")
                }
            }

            // Add colored segment (only if valid range)
            if (safeStart < safeEnd) {
                val coloredText = text.substring(safeStart, safeEnd)
                if (coloredText.isNotEmpty()) {
                    words.add(TajweedWord(coloredText, annotation.rule))
                    timber.log.Timber.d("TajweedHelper: Colored text[$safeStart-$safeEnd] rule=${annotation.rule}: '${coloredText}' (${coloredText.length} chars)")
                }
            }

            currentIndex = safeEnd
        }

        // Add remaining plain text after last annotation (if any)
        if (currentIndex < text.length) {
            val remainingText = text.substring(currentIndex)
            if (remainingText.isNotEmpty()) {
                words.add(TajweedWord(remainingText, null))
                timber.log.Timber.d("TajweedHelper: Remaining text[$currentIndex-${text.length}]: '${remainingText}' (${remainingText.length} chars)")
            }
        }

        timber.log.Timber.d("TajweedHelper: Created ${words.size} words")
        return words
    }

    /**
     * Renders a list of TajweedWord objects as a colored AnnotatedString
     *
     * This is the final step after any Kashida justification has been applied.
     * Each TajweedWord's text is rendered with its associated color.
     *
     * @param words List of TajweedWord objects (text may include Kashida from justification)
     * @param baseColor Default color for uncolored text
     * @return AnnotatedString with Tajweed colors applied
     */
    fun wordsToAnnotatedString(words: List<TajweedWord>, baseColor: Color = Color.Black): AnnotatedString {
        return buildAnnotatedString {
            for (word in words) {
                val color = if (word.rule != null) {
                    TajweedColors.getColor(word.rule)
                } else {
                    baseColor
                }

                withStyle(SpanStyle(color = color)) {
                    append(word.text)
                }
            }
        }
    }

    /**
     * One-shot method: Convert text + annotations directly to AnnotatedString
     * Use this if you're not doing manual Kashida justification
     *
     * @param text Plain Arabic text
     * @param annotations Tajweed annotations
     * @param baseColor Default color for uncolored text
     * @return Colored AnnotatedString
     */
    fun applyTajweedColors(
        text: String,
        annotations: List<TajweedAnnotation>,
        baseColor: Color = Color.Black
    ): AnnotatedString {
        val words = textToWords(text, annotations)
        val mergedWords = mergeLigatureSegments(words, text)
        return wordsToAnnotatedString(mergedWords, baseColor)
    }

    /**
     * Merge segments around ligatures to prevent Compose from breaking them
     *
     * Arabic ligatures (especially Lam-Alef variants) break when Compose applies
     * different color spans to adjacent characters. This merges colored segments
     * that are adjacent to ligatures into plain segments to preserve rendering.
     */
    private fun mergeLigatureSegments(words: List<TajweedWord>, originalText: String): List<TajweedWord> {
        if (words.isEmpty()) return words

        val ligaturePositions = findAllLigatures(originalText)
        if (ligaturePositions.isEmpty()) return words

        timber.log.Timber.d("Found ${ligaturePositions.size} ligatures in text: ${ligaturePositions.take(3)}")

        // Build position map: originalText position -> word segment index
        val positionToSegment = mutableMapOf<Int, Int>()
        var currentPosition = 0
        words.forEachIndexed { index, word ->
            for (i in 0 until word.text.length) {
                positionToSegment[currentPosition + i] = index
            }
            currentPosition += word.text.length
        }

        // Mark segments that should be merged (near ligatures)
        val shouldMerge = BooleanArray(words.size) { false }
        for ((ligStart, ligEnd) in ligaturePositions) {
            // Mark all segments that overlap with ligature range (with 1-char buffer)
            for (pos in (ligStart - 1)..(ligEnd + 1)) {
                positionToSegment[pos]?.let { segmentIndex ->
                    shouldMerge[segmentIndex] = true
                }
            }
        }

        // Merge consecutive segments that are marked
        val merged = mutableListOf<TajweedWord>()
        var currentMerged = StringBuilder()

        words.forEachIndexed { index, word ->
            if (shouldMerge[index]) {
                // Accumulate into merged segment
                currentMerged.append(word.text)
            } else {
                // Not merging
                if (currentMerged.isNotEmpty()) {
                    // Flush previous merged segment as plain
                    merged.add(TajweedWord(currentMerged.toString(), null))
                    currentMerged = StringBuilder()
                }
                // Add this segment as-is (keep its color)
                merged.add(word)
            }
        }

        // Flush any remaining merged text
        if (currentMerged.isNotEmpty()) {
            merged.add(TajweedWord(currentMerged.toString(), null))
        }

        timber.log.Timber.d("Merged ${words.size} segments into ${merged.size} segments")
        return merged
    }

    /**
     * Find all ligature positions in the text (Lam-Alef variants)
     */
    private fun findAllLigatures(text: String): List<Pair<Int, Int>> {
        val ligatures = mutableListOf<Pair<Int, Int>>()
        val lam = 'ل'
        val alefVariants = setOf('ا', 'آ', 'أ', 'إ', 'ٱ')
        val diacritics = setOf(
            '\u064B', '\u064C', '\u064D', '\u064E', '\u064F',
            '\u0650', '\u0651', '\u0652', '\u0670', 'ٓ', 'ٰ'
        )

        var i = 0
        while (i < text.length) {
            if (text[i] == lam) {
                // Find next non-diacritic character
                var nextIndex = i + 1
                while (nextIndex < text.length && text[nextIndex] in diacritics) {
                    nextIndex++
                }

                if (nextIndex < text.length && text[nextIndex] in alefVariants) {
                    // Found a Lam-Alef ligature from i to nextIndex (inclusive)
                    ligatures.add(Pair(i, nextIndex + 1))
                }
            }
            i++
        }

        return ligatures
    }

    /**
     * Strip Kashida from text
     * Should be called AFTER colors are applied (if using AnnotatedString)
     */
    fun stripKashida(text: String): String {
        return text.replace("\u0640", "")
    }
}
