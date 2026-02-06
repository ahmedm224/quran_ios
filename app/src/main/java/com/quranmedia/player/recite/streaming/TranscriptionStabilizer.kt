package com.quranmedia.player.recite.streaming

import timber.log.Timber

/**
 * Stabilization layer for streaming transcription.
 *
 * Only commits words when they are "sealed" by subsequent words,
 * preventing premature processing of partial/changing words.
 *
 * Strategy:
 * 1. Accumulate all transcription text
 * 2. Split into words
 * 3. Only emit words that have at least N subsequent words after them (sealed)
 * 4. On flush (transcription complete), emit all remaining words
 */
class TranscriptionStabilizer {

    companion object {
        // Number of subsequent words required to consider a word "stable"
        private const val SEAL_THRESHOLD = 1
    }

    // All accumulated transcription text
    private val buffer = StringBuilder()

    // Number of words already emitted/committed
    private var committedWordCount = 0

    /**
     * Add new transcription delta.
     *
     * @param delta New transcription text
     * @return List of newly stabilized words (ready for alignment)
     */
    fun addDelta(delta: String): List<String> {
        if (delta.isEmpty()) return emptyList()

        buffer.append(delta)

        // Clean up the buffer: normalize whitespace and remove punctuation
        val cleanedText = buffer.toString()
            .replace(Regex("[.،,!?؟]"), "") // Remove punctuation
            .replace(Regex("[\\s\\u00A0\\u200B-\\u200D\\uFEFF]+"), " ") // Normalize all whitespace
            .trim()

        // Split buffer into words
        val allWords = cleanedText
            .split(" ")
            .filter { it.isNotBlank() }

        // Only emit words that are "sealed" (have SEAL_THRESHOLD words after them)
        val stableWordCount = (allWords.size - SEAL_THRESHOLD).coerceAtLeast(0)

        if (stableWordCount <= committedWordCount) {
            // No new stable words
            return emptyList()
        }

        // Get newly stable words
        val newWords = allWords.subList(committedWordCount, stableWordCount)
        committedWordCount = stableWordCount

        Timber.v("Stabilizer: committed ${newWords.size} words: ${newWords.joinToString(" ")}")

        return newWords
    }

    /**
     * Flush remaining words (call on transcription complete).
     *
     * @return List of remaining uncommitted words
     */
    fun flush(): List<String> {
        // Clean up the buffer same as addDelta
        val cleanedText = buffer.toString()
            .replace(Regex("[.،,!?؟]"), "") // Remove punctuation
            .replace(Regex("[\\s\\u00A0\\u200B-\\u200D\\uFEFF]+"), " ") // Normalize all whitespace
            .trim()

        val allWords = cleanedText
            .split(" ")
            .filter { it.isNotBlank() }

        if (allWords.size <= committedWordCount) {
            return emptyList()
        }

        // Emit all remaining words
        val remainingWords = allWords.subList(committedWordCount, allWords.size)
        committedWordCount = allWords.size

        Timber.v("Stabilizer flush: committed ${remainingWords.size} words: ${remainingWords.joinToString(" ")}")

        return remainingWords
    }

    /**
     * Reset the stabilizer for a new session.
     */
    fun reset() {
        buffer.clear()
        committedWordCount = 0
        Timber.d("Stabilizer reset")
    }

    /**
     * Get current buffer contents (for debugging).
     */
    fun getBuffer(): String = buffer.toString()

    /**
     * Get count of committed words.
     */
    fun getCommittedCount(): Int = committedWordCount
}
