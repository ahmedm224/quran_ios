package com.quranmedia.player.recite.domain.model

/**
 * User's selection for recitation
 */
data class ReciteSelection(
    val surahNumber: Int,
    val surahName: String,
    val surahNameArabic: String,
    val startAyah: Int,
    val endAyah: Int,
    val totalAyahs: Int,
    val pages: List<Int> = emptyList() // Page numbers covered by this selection
)

/**
 * Details of a word mismatch during recitation
 */
data class WordMismatch(
    val surahNumber: Int,      // Surah number for this mismatch
    val ayahNumber: Int,       // Ayah number within the surah
    val wordIndex: Int,        // Word index within the ayah (0-based)
    val expectedWord: String,  // The correct word from Quran
    val detectedWord: String?, // What the user said (null if MISSING)
    val type: MismatchType     // Type of mismatch
)

/**
 * Type of mismatch detected
 */
enum class MismatchType {
    MISSING,    // Word was not recited
    INCORRECT,  // Word was recited but incorrect
    EXTRA       // Extra word was recited
}

/**
 * Highlighted word for display in Quran page view
 */
data class HighlightedWord(
    val surahNumber: Int,
    val ayahNumber: Int,
    val wordIndex: Int
)

/**
 * Result of a recitation session
 */
data class ReciteResult(
    val selection: ReciteSelection,
    val accuracyPercentage: Float,
    val mismatches: List<WordMismatch>,
    val durationSeconds: Long,
    val expectedText: String,
    val transcribedText: String
)

/**
 * Details of a mistake detected in real-time mode
 */
data class RealTimeMistake(
    val ayahNumber: Int,         // Ayah where mistake occurred
    val expectedText: String,    // Expected ayah text
    val transcribedText: String, // What was actually transcribed
    val mismatchType: String,    // "WORD_MISMATCH", "MISSING_WORDS", etc.
    val expectedWord: String = "",   // Specific word expected (if known)
    val detectedWord: String = ""    // Specific word detected (if known)
)

/**
 * UI state for the Recite feature
 */
sealed class ReciteState {
    object Idle : ReciteState()

    data class Selecting(
        val selection: ReciteSelection? = null
    ) : ReciteState()

    data class Recording(
        val selection: ReciteSelection,
        val elapsedSeconds: Long = 0
    ) : ReciteState()

    /**
     * Real-time continuous recording state with live assessment
     * User recites continuously - system assesses chunks in background
     * Vibrates immediately when mistake detected - user clicks Continue to resume
     *
     * @param selection The complete selection range
     * @param elapsedSeconds Total elapsed recording time
     * @param isAssessing True when currently sending chunk to Whisper API
     * @param currentAyahNumber The ayah user is currently reciting (tracking position)
     * @param mistakeDetected True when mistake found - pauses recording
     * @param currentMistake Details of detected mistake (null if no mistake)
     */
    data class RealTimeRecording(
        val selection: ReciteSelection,
        val elapsedSeconds: Long = 0,
        val isAssessing: Boolean = false,
        val currentAyahNumber: Int = 0,  // Current position in recitation
        val mistakeDetected: Boolean = false,
        val currentMistake: RealTimeMistake? = null
    ) : ReciteState() {
        // Get total ayahs in selection
        fun getTotalAyahs(): Int = selection.endAyah - selection.startAyah + 1

        // Get progress based on current ayah position
        fun getProgress(): Float {
            val total = getTotalAyahs()
            val progress = currentAyahNumber - selection.startAyah
            return if (total > 0) progress.toFloat() / total else 0f
        }

        // Check if completed all ayahs
        fun isComplete(): Boolean = currentAyahNumber > selection.endAyah
    }

    data class Processing(
        val selection: ReciteSelection
    ) : ReciteState()

    data class ShowingResults(
        val result: ReciteResult
    ) : ReciteState()

    data class Error(
        val message: String
    ) : ReciteState()
}
