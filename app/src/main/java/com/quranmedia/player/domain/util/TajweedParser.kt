package com.quranmedia.player.domain.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import timber.log.Timber

/**
 * Tajweed parser using word-based approach with TajweedHelper
 *
 * This class retrieves Tajweed annotations for a specific ayah and converts
 * them into colored AnnotatedString for display.
 *
 * SCOPE: Only used when Tajweed theme is active. Does not affect other themes.
 */
object TajweedParser {

    /**
     * Apply Tajweed colors to Arabic text
     *
     * @param text Plain Arabic text
     * @param surahNumber Surah number (1-114)
     * @param ayahNumber Ayah number within the surah
     * @param baseColor Default text color
     * @param stripKashida Whether to remove kashida (U+0640)
     * @return AnnotatedString with Tajweed colors applied
     */
    fun applyTajweedColors(
        text: String,
        surahNumber: Int,
        ayahNumber: Int,
        baseColor: Color = Color.Black,
        stripKashida: Boolean = true
    ): AnnotatedString {
        // Use Tajweed Rule Engine to analyze text and generate annotations
        val processedText = if (stripKashida) text.stripKashida() else text

        // Generate annotations dynamically using Tajweed rules
        val annotations = TajweedRuleEngine.analyzeText(processedText)

        if (surahNumber == 1 && ayahNumber == 1) {
            Timber.d("=== TAJWEED RULE ENGINE ===")
            Timber.d("Text: '$processedText'")
            Timber.d("Generated ${annotations.size} annotations")
            annotations.take(5).forEach {
                Timber.d("  ${it.rule}: [${it.start}-${it.end}] = '${processedText.substring(it.start, it.end.coerceAtMost(processedText.length))}'")
            }
        }

        if (annotations.isEmpty()) {
            // No Tajweed rules applied - return plain text
            return buildAnnotatedString {
                withStyle(SpanStyle(color = baseColor)) {
                    append(processedText)
                }
            }
        }

        // Apply colors using TajweedHelper
        return TajweedHelper.applyTajweedColors(processedText, annotations, baseColor)
    }

    /**
     * Parse embedded Tajweed markers like [n]ن[/n] and apply colors
     */
    private fun parseEmbeddedMarkers(text: String, baseColor: Color): AnnotatedString {
        return buildAnnotatedString {
            var currentIndex = 0

            // Pattern: [marker]text[/marker] where marker is a single lowercase letter
            val pattern = Regex("\\[([a-z])\\](.*?)\\[/\\1\\]")

            pattern.findAll(text).forEach { match ->
                // Add plain text before this marker
                if (currentIndex < match.range.first) {
                    withStyle(SpanStyle(color = baseColor)) {
                        append(text.substring(currentIndex, match.range.first))
                    }
                }

                val marker = match.groupValues[1][0]
                val markedText = match.groupValues[2]
                val color = TajweedColors.getColorFromMarker(marker)

                // Add colored text
                withStyle(SpanStyle(color = color)) {
                    append(markedText)
                }

                currentIndex = match.range.last + 1
            }

            // Add remaining plain text
            if (currentIndex < text.length) {
                withStyle(SpanStyle(color = baseColor)) {
                    append(text.substring(currentIndex))
                }
            }
        }
    }

    private fun String.stripKashida(): String = this.replace("\u0640", "")
}
