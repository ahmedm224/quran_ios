package com.quranmedia.player.recite.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Represents the highlight state of a single word during recitation.
 */
data class WordHighlightState(
    val surahNumber: Int,
    val ayahNumber: Int,
    val wordIndex: Int,        // 0-based word index within the ayah
    val type: HighlightType,
    val expectedWord: String = "",   // The expected word (for error display)
    val transcribedWord: String = "" // What was actually said (for error display)
) {
    /**
     * Unique key for this word position.
     */
    val key: String
        get() = "$surahNumber:$ayahNumber:$wordIndex"
}

/**
 * Type of word highlight.
 */
enum class HighlightType {
    /** Word currently being waited for (next expected) */
    CURRENT,

    /** Word was recited correctly */
    CORRECT,

    /** Word was recited incorrectly or skipped */
    ERROR;

    /**
     * Get the background color for this highlight type.
     */
    fun getBackgroundColor(): Color = when (this) {
        CURRENT -> Color(0xFF2196F3).copy(alpha = 0.3f)  // Blue
        CORRECT -> Color(0xFF4CAF50).copy(alpha = 0.2f)  // Green
        ERROR -> Color(0xFFF44336).copy(alpha = 0.3f)    // Red
    }

    /**
     * Get the text color for this highlight type.
     */
    fun getTextColor(): Color = when (this) {
        CURRENT -> Color(0xFF1565C0)   // Dark blue
        CORRECT -> Color(0xFF2E7D32)   // Dark green
        ERROR -> Color(0xFFC62828)     // Dark red
    }
}

/**
 * State tracking for all word highlights during recitation.
 */
data class ReciteHighlightMap(
    private val highlights: MutableMap<String, WordHighlightState> = mutableMapOf()
) {
    /**
     * Add or update a word highlight.
     */
    fun setHighlight(state: WordHighlightState) {
        highlights[state.key] = state
    }

    /**
     * Get highlight for a specific word.
     */
    fun getHighlight(surahNumber: Int, ayahNumber: Int, wordIndex: Int): WordHighlightState? {
        return highlights["$surahNumber:$ayahNumber:$wordIndex"]
    }

    /**
     * Get all highlights for a specific ayah.
     */
    fun getAyahHighlights(surahNumber: Int, ayahNumber: Int): List<WordHighlightState> {
        val prefix = "$surahNumber:$ayahNumber:"
        return highlights.filter { it.key.startsWith(prefix) }.values.toList()
    }

    /**
     * Get all error highlights.
     */
    fun getErrors(): List<WordHighlightState> {
        return highlights.values.filter { it.type == HighlightType.ERROR }
    }

    /**
     * Get current word being waited for.
     */
    fun getCurrentWord(): WordHighlightState? {
        return highlights.values.find { it.type == HighlightType.CURRENT }
    }

    /**
     * Get all highlights as a list.
     */
    fun getAllHighlights(): List<WordHighlightState> {
        return highlights.values.toList()
    }

    /**
     * Clear all highlights.
     */
    fun clear() {
        highlights.clear()
    }

    /**
     * Clear highlights from a specific ayah onwards (for resume from mistake).
     */
    fun clearFromAyah(surahNumber: Int, ayahNumber: Int) {
        val keysToRemove = highlights.keys.filter { key ->
            val parts = key.split(":")
            if (parts.size == 3) {
                val s = parts[0].toIntOrNull() ?: return@filter false
                val a = parts[1].toIntOrNull() ?: return@filter false
                s > surahNumber || (s == surahNumber && a >= ayahNumber)
            } else {
                false
            }
        }
        keysToRemove.forEach { highlights.remove(it) }
    }

    /**
     * Get count of correct words.
     */
    fun getCorrectCount(): Int = highlights.values.count { it.type == HighlightType.CORRECT }

    /**
     * Get count of error words.
     */
    fun getErrorCount(): Int = highlights.values.count { it.type == HighlightType.ERROR }

    /**
     * Calculate accuracy percentage.
     */
    fun getAccuracyPercentage(): Float {
        val total = getCorrectCount() + getErrorCount()
        return if (total > 0) {
            (getCorrectCount().toFloat() / total) * 100f
        } else {
            0f
        }
    }

    /**
     * Create a copy of this map.
     */
    fun copy(): ReciteHighlightMap {
        return ReciteHighlightMap(highlights.toMutableMap())
    }
}
