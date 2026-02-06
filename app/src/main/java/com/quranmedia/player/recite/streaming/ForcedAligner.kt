package com.quranmedia.player.recite.streaming

import timber.log.Timber

/**
 * Forced Aligner for matching streaming transcription to expected Quran text.
 *
 * Uses ROMANIZED matching approach:
 * 1. Transcription comes in as romanized text (English ASR of Arabic speech)
 * 2. Match against pre-computed transliterations from Quran data
 * 3. Use fuzzy matching with normalized strings
 *
 * This approach is more reliable than Arabic ASR because:
 * - English ASR models are more robust
 * - Romanized matching is simpler than Arabic diacritics
 * - Word boundaries are cleaner
 */
class ForcedAligner {

    companion object {
        // Minimum similarity for accepting a match (lenient - follow along mode)
        private const val MIN_SIMILARITY = 0.50

        // Threshold for exact match display
        private const val EXACT_MATCH_THRESHOLD = 0.70

        // Minimum word length for fuzzy matching
        private const val MIN_FUZZY_LENGTH = 2

        /**
         * Normalize transliteration for fuzzy matching.
         */
        fun normalizeTransliteration(text: String): String {
            var result = text.lowercase()

            // Replace diacritical variants
            val replacements = mapOf(
                'ā' to 'a', 'ī' to 'i', 'ū' to 'u',
                'ṣ' to 's', 'ḍ' to 'd', 'ṭ' to 't', 'ẓ' to 'z',
                'ḥ' to 'h', 'ġ' to 'g', 'ḫ' to 'k',
                'ʿ' to ' ', 'ʾ' to ' ', '\'' to ' ',
                '-' to ' ', '_' to ' ', '.' to ' ', ',' to ' '
            )

            for ((old, new) in replacements) {
                result = result.replace(old, new)
            }

            // Remove extra spaces and trim
            return result.replace(Regex("\\s+"), "").trim()
        }
    }

    // Expected words organized by ayah: List<AyahWords>
    // Each AyahWords contains: ayahNumber, list of words with transliterations
    private var expectedAyahs: List<AyahWords> = emptyList()

    // Current position in the expected text (shared UI position)
    private var currentAyahIndex: Int = 0
    private var currentWordIndex: Int = 0

    // Track errors in current ayah
    private val currentAyahErrors = mutableSetOf<Int>()

    /**
     * Initialize the aligner with expected Quran text and transliterations.
     *
     * @param ayahs List of pairs (ayahNumber, rawArabicText)
     * @param transliterations Optional map of ayahNumber -> list of (arabic, transliteration) pairs
     */
    fun initialize(
        ayahs: List<Pair<Int, String>>,
        transliterations: Map<Int, List<Pair<String, String>>>? = null
    ) {
        expectedAyahs = ayahs.map { (ayahNumber, text) ->
            val arabicWords = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
            val translitWords = transliterations?.get(ayahNumber) ?: emptyList()

            // Build word list with transliterations
            val words = arabicWords.mapIndexed { index, arabic ->
                val (_, translit) = translitWords.getOrElse(index) { arabic to "" }
                WordInfo(
                    arabic = arabic,
                    transliteration = translit,
                    normalized = normalizeTransliteration(translit.ifBlank { arabic })
                )
            }

            AyahWords(
                ayahNumber = ayahNumber,
                words = words,
                originalText = text
            )
        }

        currentAyahIndex = 0
        currentWordIndex = 0
        currentAyahErrors.clear()

        val totalWords = expectedAyahs.sumOf { it.words.size }
        Timber.d("═══════════════════════════════════════════")
        Timber.d("ForcedAligner initialized (ROMANIZED): ${expectedAyahs.size} ayahs, $totalWords words")
        expectedAyahs.forEach { ayah ->
            val translits = ayah.words.joinToString(" ") { it.transliteration.ifBlank { it.arabic } }
            Timber.d("  Ayah ${ayah.ayahNumber}: $translits")
        }
        Timber.d("═══════════════════════════════════════════")
    }

    /**
     * Process transcription in "follow along" mode.
     *
     * Expects ROMANIZED transcription from English ASR model.
     * Matches against pre-computed transliterations using fuzzy matching.
     *
     * @param transcription The romanized transcription text
     * @param isComplete Whether this is the final transcription
     * @return List of alignment results for matched/mismatched words
     */
    fun processTranscription(transcription: String, isComplete: Boolean = false): List<AlignmentResult> {
        if (expectedAyahs.isEmpty()) {
            Timber.w("ForcedAligner not initialized")
            return emptyList()
        }

        if (transcription.isBlank()) {
            return emptyList()
        }

        // Check for source tag (e.g., "openai:text" or "moonshine:text")
        val (source, actualText) = parseSourceTag(transcription)

        // Split romanized transcription into words (already in Latin script)
        val transcribedWords = actualText.trim().split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        if (transcribedWords.isEmpty()) {
            return emptyList()
        }

        val results = mutableListOf<AlignmentResult>()

        // Process each transcribed word
        for (transcribedWord in transcribedWords) {
            if (isComplete()) break

            // Normalize the transcribed word for matching
            val normalizedTranscribed = normalizeTransliteration(transcribedWord)

            val result = matchWordRomanized(normalizedTranscribed, transcribedWord)

            if (result != null) {
                results.add(result)
            }
        }

        return results
    }

    /**
     * Parse source tag from transcription (e.g., "openai:text" -> Pair("openai", "text"))
     */
    private fun parseSourceTag(transcription: String): Pair<String?, String> {
        val colonIndex = transcription.indexOf(':')
        if (colonIndex > 0 && colonIndex < 15) { // Source tag should be short
            val possibleSource = transcription.substring(0, colonIndex)
            if (possibleSource in listOf("openai", "moonshine")) {
                return Pair(possibleSource, transcription.substring(colonIndex + 1))
            }
        }
        return Pair(null, transcription)
    }

    /**
     * Check if text contains Arabic characters.
     */
    private fun isArabicText(text: String): Boolean {
        return text.any { char ->
            val block = Character.UnicodeBlock.of(char)
            block == Character.UnicodeBlock.ARABIC ||
            block == Character.UnicodeBlock.ARABIC_SUPPLEMENT ||
            block == Character.UnicodeBlock.ARABIC_EXTENDED_A
        }
    }

    /**
     * Match a transcribed word against expected words.
     * Handles BOTH Arabic and romanized transcriptions.
     *
     * @param normalizedTranscribed The normalized transcribed word
     * @param originalTranscribed The original transcribed word (for display)
     * @return AlignmentResult or null if no match attempted
     */
    private fun matchWordRomanized(normalizedTranscribed: String, originalTranscribed: String): AlignmentResult? {
        if (isComplete()) return null

        val currentAyah = expectedAyahs.getOrNull(currentAyahIndex) ?: return null
        val expectedWordInfo = currentAyah.words.getOrNull(currentWordIndex)

        if (expectedWordInfo == null) {
            moveToNextAyah()
            return matchWordRomanized(normalizedTranscribed, originalTranscribed)
        }

        // Detect if transcription is Arabic or romanized
        val isArabic = isArabicText(originalTranscribed)

        // Calculate similarity based on script type
        val similarity = if (isArabic) {
            // Compare Arabic transcription against Arabic expected word
            calculateSimilarity(originalTranscribed, expectedWordInfo.arabic)
        } else {
            // Compare romanized transcription against normalized transliteration
            calculateSimilarity(normalizedTranscribed, expectedWordInfo.normalized)
        }

        val expectedDisplay = if (isArabic) expectedWordInfo.arabic else expectedWordInfo.transliteration
        Timber.d("📍 Ayah ${currentAyah.ayahNumber}, Word ${currentWordIndex + 1}: '$expectedDisplay' vs '$originalTranscribed' (${(similarity * 100).toInt()}%) [${if (isArabic) "AR" else "ROM"}]")

        // Exact or good match
        if (similarity >= MIN_SIMILARITY) {
            Timber.d("✅ MATCH: '$originalTranscribed' ~ '${expectedWordInfo.transliteration}' (${(similarity * 100).toInt()}%)")

            val result = if (similarity >= EXACT_MATCH_THRESHOLD) {
                AlignmentResult.Match(
                    ayahNumber = currentAyah.ayahNumber,
                    wordIndex = currentWordIndex,
                    expectedWord = expectedWordInfo.arabic,
                    transcribedWord = originalTranscribed
                )
            } else {
                AlignmentResult.FuzzyMatch(
                    ayahNumber = currentAyah.ayahNumber,
                    wordIndex = currentWordIndex,
                    expectedWord = expectedWordInfo.arabic,
                    transcribedWord = originalTranscribed,
                    similarity = similarity
                )
            }
            advancePosition()
            return result
        }

        // Check lookahead for matches (user might be ahead)
        val lookAheadResult = tryRomanizedLookahead(normalizedTranscribed, originalTranscribed, currentAyah)
        if (lookAheadResult != null) {
            return lookAheadResult
        }

        // Low similarity - either wrong word or noise
        // For now, don't advance and don't flag as error (wait for clearer input)
        Timber.d("⏸️ IGNORING '$originalTranscribed' (${(similarity * 100).toInt()}%) - waiting for clearer match")
        return null
    }

    /**
     * Try lookahead to find a match in upcoming words.
     * Handles both Arabic and romanized transcriptions.
     */
    private fun tryRomanizedLookahead(normalizedTranscribed: String, originalTranscribed: String, currentAyah: AyahWords): AlignmentResult? {
        val isArabic = isArabicText(originalTranscribed)

        // Look 1-2 words ahead
        for (offset in 1..2) {
            val checkIdx = currentWordIndex + offset
            if (checkIdx >= currentAyah.words.size) break

            val futureWord = currentAyah.words[checkIdx]

            // Calculate similarity based on script type
            val similarity = if (isArabic) {
                calculateSimilarity(originalTranscribed, futureWord.arabic)
            } else {
                calculateSimilarity(normalizedTranscribed, futureWord.normalized)
            }

            if (similarity >= MIN_SIMILARITY) {
                Timber.d("⏭️ MATCH AHEAD at word ${checkIdx + 1}: '$originalTranscribed' ~ '${futureWord.transliteration}' (${(similarity * 100).toInt()}%) [${if (isArabic) "AR" else "ROM"}]")

                // Move to the matched position
                currentWordIndex = checkIdx

                val result = if (similarity >= EXACT_MATCH_THRESHOLD) {
                    AlignmentResult.Match(
                        ayahNumber = currentAyah.ayahNumber,
                        wordIndex = checkIdx,
                        expectedWord = futureWord.arabic,
                        transcribedWord = originalTranscribed
                    )
                } else {
                    AlignmentResult.FuzzyMatch(
                        ayahNumber = currentAyah.ayahNumber,
                        wordIndex = checkIdx,
                        expectedWord = futureWord.arabic,
                        transcribedWord = originalTranscribed,
                        similarity = similarity
                    )
                }

                advancePosition()
                return result
            }
        }

        return null
    }

    // NOTE: Old dual-model consensus methods removed.
    // The romanized approach uses matchWordRomanized() instead.

    /**
     * Advance position to next word.
     */
    private fun advancePosition() {
        val currentAyah = expectedAyahs.getOrNull(currentAyahIndex) ?: return

        currentWordIndex++

        if (currentWordIndex >= currentAyah.words.size) {
            moveToNextAyah()
        }
    }

    /**
     * Move to the next ayah.
     * Always advances - errors are tracked but don't block progress.
     * This is important for streaming where transcription errors happen.
     */
    private fun moveToNextAyah(): AyahWords? {
        // Log if there were errors (but don't block advancement)
        if (currentAyahErrors.isNotEmpty()) {
            val currentAyah = expectedAyahs.getOrNull(currentAyahIndex)
            Timber.d("⚠️ Ayah ${currentAyah?.ayahNumber} had ${currentAyahErrors.size} errors, continuing to next")
        }

        // Always advance to next ayah
        currentAyahIndex++
        currentWordIndex = 0
        currentAyahErrors.clear()  // Clear error tracking for new ayah

        return if (currentAyahIndex < expectedAyahs.size) {
            val nextAyah = expectedAyahs[currentAyahIndex]
            Timber.d("✅ Moving to ayah ${nextAyah.ayahNumber}")
            nextAyah
        } else {
            Timber.d("Reached end of all ayahs")
            null
        }
    }

    /**
     * Calculate similarity between two strings using Levenshtein distance.
     */
    private fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val distance = levenshteinDistance(s1, s2)
        val maxLen = maxOf(s1.length, s2.length)

        return 1.0 - (distance.toDouble() / maxLen)
    }

    /**
     * Calculate Levenshtein distance between two strings.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        // Create DP matrix
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        // Initialize first row and column
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        // Fill the matrix
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1

                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * Check if alignment is complete (all words matched).
     */
    fun isComplete(): Boolean {
        return currentAyahIndex >= expectedAyahs.size
    }

    /**
     * Get current position for progress display.
     */
    fun getCurrentPosition(): AlignmentPosition {
        val currentAyah = expectedAyahs.getOrNull(currentAyahIndex)

        return AlignmentPosition(
            ayahIndex = currentAyahIndex,
            ayahNumber = currentAyah?.ayahNumber ?: 0,
            wordIndex = currentWordIndex,
            totalAyahs = expectedAyahs.size,
            totalWordsInCurrentAyah = currentAyah?.words?.size ?: 0,
            isComplete = isComplete()
        )
    }

    /**
     * Get the remaining expected text from the current position.
     * Used for Whisper prompt conditioning - gives the model context about what to expect.
     *
     * @param maxWords Maximum words to include (Whisper prompt has limited tokens)
     * @return The remaining expected text, starting from current word
     */
    fun getRemainingExpectedText(maxWords: Int = 50): String {
        if (expectedAyahs.isEmpty() || isComplete()) return ""

        val words = mutableListOf<String>()
        var ayahIdx = currentAyahIndex
        var wordIdx = currentWordIndex

        // Collect transliterations from current position
        while (words.size < maxWords && ayahIdx < expectedAyahs.size) {
            val ayah = expectedAyahs[ayahIdx]
            while (wordIdx < ayah.words.size && words.size < maxWords) {
                // Use transliteration for romanized prompt
                words.add(ayah.words[wordIdx].transliteration)
                wordIdx++
            }
            ayahIdx++
            wordIdx = 0
        }

        return words.joinToString(" ")
    }

    /**
     * Reset the aligner to start from beginning.
     */
    fun reset() {
        currentAyahIndex = 0
        currentWordIndex = 0
        currentAyahErrors.clear()
        Timber.d("ForcedAligner reset")
    }

    /**
     * Resume from a specific ayah (after user clicks "continue from mistake").
     */
    fun resumeFromAyah(ayahNumber: Int) {
        val index = expectedAyahs.indexOfFirst { it.ayahNumber == ayahNumber }
        if (index >= 0) {
            currentAyahIndex = index
            currentWordIndex = 0
            currentAyahErrors.clear()
            Timber.d("Resuming from ayah $ayahNumber (index $index)")
        } else {
            Timber.w("Ayah $ayahNumber not found, resetting to beginning")
            reset()
        }
    }

    /**
     * Resume from a specific word position (after user clicks "retry from here").
     *
     * @param ayahNumber The ayah number to resume from
     * @param wordIndex The word index within the ayah
     */
    fun resumeFromPosition(ayahNumber: Int, wordIndex: Int) {
        val ayahIdx = expectedAyahs.indexOfFirst { it.ayahNumber == ayahNumber }
        if (ayahIdx >= 0) {
            val ayah = expectedAyahs[ayahIdx]
            // Ensure word index is valid
            val safeWordIndex = wordIndex.coerceIn(0, ayah.words.size - 1)
            currentAyahIndex = ayahIdx
            currentWordIndex = safeWordIndex
            currentAyahErrors.clear()
            Timber.d("Resuming from ayah $ayahNumber, word $safeWordIndex")
        } else {
            Timber.w("Ayah $ayahNumber not found, resetting to beginning")
            reset()
        }
    }

    /**
     * Check if current ayah has unresolved errors.
     */
    fun hasErrorsInCurrentAyah(): Boolean {
        return currentAyahErrors.isNotEmpty()
    }

    // Data classes

    /**
     * Word information with Arabic and transliteration.
     */
    data class WordInfo(
        val arabic: String,
        val transliteration: String,
        val normalized: String  // Pre-normalized transliteration for matching
    )

    data class AyahWords(
        val ayahNumber: Int,
        val words: List<WordInfo>,
        val originalText: String
    )

    data class AlignmentPosition(
        val ayahIndex: Int,
        val ayahNumber: Int,
        val wordIndex: Int,
        val totalAyahs: Int,
        val totalWordsInCurrentAyah: Int,
        val isComplete: Boolean
    )

    sealed class AlignmentResult {
        abstract val ayahNumber: Int
        abstract val wordIndex: Int
        abstract val expectedWord: String
        abstract val transcribedWord: String

        data class Match(
            override val ayahNumber: Int,
            override val wordIndex: Int,
            override val expectedWord: String,
            override val transcribedWord: String
        ) : AlignmentResult()

        data class FuzzyMatch(
            override val ayahNumber: Int,
            override val wordIndex: Int,
            override val expectedWord: String,
            override val transcribedWord: String,
            val similarity: Double
        ) : AlignmentResult()

        data class Mismatch(
            override val ayahNumber: Int,
            override val wordIndex: Int,
            override val expectedWord: String,
            override val transcribedWord: String,
            val type: MismatchType
        ) : AlignmentResult()
    }

    enum class MismatchType {
        INCORRECT,  // Word was said but wrong
        SKIPPED,    // Word was skipped (detected via lookahead)
        EXTRA       // Extra word that doesn't match (future use)
    }
}
