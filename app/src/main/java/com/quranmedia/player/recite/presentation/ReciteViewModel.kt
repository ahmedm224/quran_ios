package com.quranmedia.player.recite.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.data.database.dao.AyahDao
import com.quranmedia.player.data.database.dao.SurahDao
import com.quranmedia.player.data.database.entity.SurahEntity
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.domain.model.Ayah
import com.quranmedia.player.domain.util.Resource
import com.quranmedia.player.recite.audio.AudioRecorder
import com.quranmedia.player.recite.domain.model.ReciteSelection
import com.quranmedia.player.recite.domain.model.ReciteState
import com.quranmedia.player.recite.domain.repository.ReciteRepository
import com.quranmedia.player.recite.util.SimpleQuranText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ReciteViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reciteRepository: ReciteRepository,
    private val surahDao: SurahDao,
    private val ayahDao: AyahDao,
    private val audioRecorder: AudioRecorder,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // UI state
    private val _state = MutableStateFlow<ReciteState>(ReciteState.Idle)
    val state: StateFlow<ReciteState> = _state

    // Ayahs for the selected range
    private val _selectedAyahs = MutableStateFlow<List<Ayah>>(emptyList())
    val selectedAyahs: StateFlow<List<Ayah>> = _selectedAyahs

    // List of all surahs for selection
    val allSurahs = surahDao.getAllSurahs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // App language from settings
    val language = settingsRepository.settings
        .map { it.appLanguage }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.quranmedia.player.data.repository.AppLanguage.ENGLISH
        )

    // Full settings (for real-time assessment toggle)
    val settings = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.quranmedia.player.data.repository.UserSettings()
        )

    // Recording timer
    private var recordingStartTime = 0L
    private var timerJob: Job? = null
    private var recordingFile: File? = null

    // Real-time assessment constants
    companion object {
        private const val CHUNK_INTERVAL_MS = 5000L // 5 seconds per chunk
    }

    // Real-time assessment jobs
    private var chunkAssessmentJob: Job? = null
    private var currentChunkStartTime = 0L
    private var assessedText = StringBuilder() // Accumulated transcription text
    private var expectedAyahTexts: List<Pair<Int, String>> = emptyList() // (ayahNumber, text) pairs
    private var currentAyahIndex = 0 // Index into expectedAyahTexts
    private var hasSynced = false // Whether we've synced transcription with expected text
    private var syncAttempts = 0 // Counter for sync attempts before flagging errors
    private val maxSyncAttempts = 3 // Wait up to 3 chunks (15 seconds) before requiring sync

    init {
        // Load Tanzil Simple text for AI comparison
        if (!SimpleQuranText.isLoaded()) {
            SimpleQuranText.load(context)
        }
    }

    /**
     * Start the selection process
     */
    fun startSelection() {
        _state.value = ReciteState.Selecting(selection = null)
    }

    /**
     * Update the current selection
     */
    fun updateSelection(
        surahNumber: Int,
        startAyah: Int,
        endAyah: Int,
        allSurahs: List<SurahEntity>
    ) {
        viewModelScope.launch {
            val surah = allSurahs.find { it.number == surahNumber }
            if (surah != null) {
                // Load ayahs for the selected range
                val ayahs = ayahDao.getAyahRange(
                    surahNumber = surahNumber,
                    startAyah = startAyah,
                    endAyah = endAyah
                ).map { entity ->
                    Ayah(
                        surahNumber = entity.surahNumber,
                        ayahNumber = entity.ayahNumber,
                        globalAyahNumber = entity.globalAyahNumber,
                        textArabic = entity.textArabic,
                        juz = entity.juz,
                        manzil = entity.manzil,
                        page = entity.page,
                        ruku = entity.ruku,
                        hizbQuarter = entity.hizbQuarter,
                        sajda = entity.sajda
                    )
                }

                // Get unique page numbers
                val pages = ayahs.map { it.page }.distinct().sorted()

                _selectedAyahs.value = ayahs

                val selection = ReciteSelection(
                    surahNumber = surah.number,
                    surahName = surah.nameEnglish,
                    surahNameArabic = surah.nameArabic,
                    startAyah = startAyah,
                    endAyah = endAyah,
                    totalAyahs = surah.ayahCount,
                    pages = pages
                )
                _state.value = ReciteState.Selecting(selection = selection)
            }
        }
    }

    /**
     * Start recording recitation
     */
    fun startRecording(selection: ReciteSelection) {
        viewModelScope.launch {
            try {
                // Start audio recording
                val file = audioRecorder.startRecording()
                if (file == null) {
                    _state.value = ReciteState.Error("Failed to start recording. Please check microphone permission.")
                    return@launch
                }

                recordingFile = file
                recordingStartTime = System.currentTimeMillis()

                // Update state to Recording
                _state.value = ReciteState.Recording(
                    selection = selection,
                    elapsedSeconds = 0
                )

                // Start timer
                startTimer(selection)

                Timber.d("Recording started for ${selection.surahNameArabic} ${selection.startAyah}-${selection.endAyah}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to start recording")
                _state.value = ReciteState.Error("Failed to start recording: ${e.message}")
            }
        }
    }

    /**
     * Stop recording and process the recitation
     */
    fun stopRecording() {
        viewModelScope.launch {
            try {
                // Stop timer
                timerJob?.cancel()
                timerJob = null

                // Get current selection
                val currentState = _state.value
                if (currentState !is ReciteState.Recording) {
                    Timber.w("Attempted to stop recording but not in recording state")
                    return@launch
                }

                val selection = currentState.selection
                val durationSeconds = (System.currentTimeMillis() - recordingStartTime) / 1000

                // Stop audio recording
                val file = audioRecorder.stopRecording()
                if (file == null) {
                    _state.value = ReciteState.Error("Failed to stop recording")
                    return@launch
                }

                Timber.d("Recording stopped, duration: ${durationSeconds}s")

                // Update state to Processing
                _state.value = ReciteState.Processing(selection = selection)

                // Get expected text from database
                val expectedTextResult = reciteRepository.getAyahsText(selection)
                if (expectedTextResult is Resource.Error) {
                    _state.value = ReciteState.Error(expectedTextResult.message ?: "Failed to get Quran text")
                    file.delete()
                    return@launch
                }

                val expectedText = (expectedTextResult as Resource.Success).data!!

                // Transcribe audio using Whisper API
                val transcriptionResult = reciteRepository.transcribeAudio(file)
                if (transcriptionResult is Resource.Error) {
                    _state.value = ReciteState.Error(transcriptionResult.message ?: "Failed to transcribe audio")
                    file.delete()
                    return@launch
                }

                val transcribedText = (transcriptionResult as Resource.Success).data!!

                // Match transcription against expected text
                val matchResult = reciteRepository.matchTranscription(
                    expectedText = expectedText,
                    transcribedText = transcribedText,
                    selection = selection,
                    durationSeconds = durationSeconds
                )

                // Delete the audio file
                file.delete()

                if (matchResult is Resource.Error) {
                    _state.value = ReciteState.Error(matchResult.message ?: "Failed to match transcription")
                    return@launch
                }

                val result = (matchResult as Resource.Success).data!!

                // Update state to ShowingResults
                _state.value = ReciteState.ShowingResults(result = result)

                Timber.d("Recitation processed: ${result.accuracyPercentage}% accuracy, ${result.mismatches.size} mismatches")
            } catch (e: Exception) {
                Timber.e(e, "Failed to process recording")
                _state.value = ReciteState.Error("Failed to process recording: ${e.message}")
                recordingFile?.delete()
            }
        }
    }

    /**
     * Cancel current recording
     */
    fun cancelRecording() {
        timerJob?.cancel()
        timerJob = null
        audioRecorder.cancelRecording()
        recordingFile = null
        _state.value = ReciteState.Idle
        Timber.d("Recording cancelled")
    }

    /**
     * Reset to idle state
     */
    fun reset() {
        timerJob?.cancel()
        timerJob = null
        recordingFile?.let { audioRecorder.deleteRecording(it) }
        recordingFile = null
        _state.value = ReciteState.Idle
    }

    /**
     * Start real-time continuous recording with chunk-based assessment
     * User recites continuously - system assesses audio chunks in background
     */
    fun startRealTimeRecording(selection: ReciteSelection) {
        viewModelScope.launch {
            try {
                // Load expected ayah texts
                val ayahEntities = ayahDao.getAyahRange(
                    surahNumber = selection.surahNumber,
                    startAyah = selection.startAyah,
                    endAyah = selection.endAyah
                )

                if (ayahEntities.isEmpty()) {
                    _state.value = ReciteState.Error("Failed to load ayahs")
                    return@launch
                }

                // Build list of (ayahNumber, normalizedText) pairs
                expectedAyahTexts = ayahEntities.map { entity ->
                    entity.ayahNumber to com.quranmedia.player.recite.util.ArabicNormalizer.normalize(entity.textArabic)
                }

                // Reset assessment state
                currentAyahIndex = 0
                assessedText.clear()
                hasSynced = false
                syncAttempts = 0

                // Start audio recording
                val file = audioRecorder.startRecording()
                if (file == null) {
                    _state.value = ReciteState.Error("Failed to start recording. Please check microphone permission.")
                    return@launch
                }

                recordingFile = file
                recordingStartTime = System.currentTimeMillis()
                currentChunkStartTime = System.currentTimeMillis()

                // Update state to RealTimeRecording
                _state.value = ReciteState.RealTimeRecording(
                    selection = selection,
                    elapsedSeconds = 0,
                    isAssessing = false,
                    currentAyahNumber = selection.startAyah,
                    mistakeDetected = false,
                    currentMistake = null
                )

                // Start timer and chunk assessment
                startRealTimeTimer()
                startChunkAssessment()

                Timber.d("Real-time continuous recording started for ${selection.surahNameArabic}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to start real-time recording")
                _state.value = ReciteState.Error("Failed to start recording: ${e.message}")
            }
        }
    }

    /**
     * Start periodic chunk assessment
     * Every CHUNK_INTERVAL_MS, capture and assess an audio chunk
     */
    private fun startChunkAssessment() {
        chunkAssessmentJob = viewModelScope.launch {
            while (true) {
                delay(CHUNK_INTERVAL_MS)

                val currentState = _state.value
                if (currentState !is ReciteState.RealTimeRecording) {
                    break
                }

                // Don't assess if already assessing or mistake detected
                if (currentState.isAssessing || currentState.mistakeDetected) {
                    continue
                }

                // Capture and assess chunk
                assessCurrentChunk()
            }
        }
    }

    /**
     * Capture current audio chunk and send to Whisper API for assessment
     */
    private suspend fun assessCurrentChunk() {
        val currentState = _state.value as? ReciteState.RealTimeRecording ?: return

        try {
            // Mark as assessing
            _state.value = currentState.copy(isAssessing = true)

            // Stop current recording to capture chunk
            val chunkFile = audioRecorder.stopRecording()

            if (chunkFile == null || !chunkFile.exists() || chunkFile.length() == 0L) {
                Timber.w("Empty or invalid audio chunk")
                // Restart recording and continue
                val newFile = audioRecorder.startRecording()
                if (newFile != null) {
                    recordingFile = newFile
                    currentChunkStartTime = System.currentTimeMillis()
                    _state.value = (_state.value as? ReciteState.RealTimeRecording)?.copy(isAssessing = false) ?: return
                }
                return
            }

            // Immediately start new recording for next chunk (don't wait for API)
            val newFile = audioRecorder.startRecording()
            if (newFile != null) {
                recordingFile = newFile
                currentChunkStartTime = System.currentTimeMillis()
            }

            Timber.d("Assessing audio chunk: ${chunkFile.length()} bytes")

            // Transcribe the chunk using Whisper API
            val transcriptionResult = reciteRepository.transcribeAudio(chunkFile)
            chunkFile.delete() // Clean up chunk file

            if (transcriptionResult is Resource.Error) {
                Timber.e("Transcription error: ${transcriptionResult.message}")
                _state.value = (_state.value as? ReciteState.RealTimeRecording)?.copy(isAssessing = false) ?: return
                return
            }

            val transcribedText = (transcriptionResult as Resource.Success).data ?: ""
            val normalizedTranscribed = com.quranmedia.player.recite.util.ArabicNormalizer.normalize(transcribedText)

            Timber.d("Transcribed chunk: $normalizedTranscribed")

            // Append to accumulated transcription
            if (normalizedTranscribed.isNotBlank()) {
                if (assessedText.isNotEmpty()) {
                    assessedText.append(" ")
                }
                assessedText.append(normalizedTranscribed)
            }

            // Compare against expected ayahs
            val mistake = findMistakeInTranscription()

            if (mistake != null) {
                // Mistake found! Vibrate and pause recording
                Timber.d("Mistake detected at ayah ${mistake.ayahNumber}")

                // Vibrate if haptic feedback is enabled
                if (settingsRepository.getCurrentSettings().reciteHapticOnMistake) {
                    com.quranmedia.player.recite.util.HapticFeedback.vibrateOnMistake(context)
                }

                // Stop recording
                audioRecorder.cancelRecording()
                timerJob?.cancel()
                chunkAssessmentJob?.cancel()

                _state.value = currentState.copy(
                    isAssessing = false,
                    mistakeDetected = true,
                    currentMistake = mistake
                )
            } else {
                // No mistake - update progress and continue
                val latestState = _state.value as? ReciteState.RealTimeRecording ?: return

                // Update current ayah position based on transcription progress
                val newAyahNumber = estimateCurrentAyah()

                // Check if completed
                if (newAyahNumber > currentState.selection.endAyah) {
                    // All ayahs completed successfully!
                    com.quranmedia.player.recite.util.HapticFeedback.vibrateOnCompletion(context)
                    Timber.d("Real-time assessment completed successfully!")

                    audioRecorder.cancelRecording()
                    timerJob?.cancel()
                    chunkAssessmentJob?.cancel()

                    _state.value = ReciteState.Idle
                } else {
                    _state.value = latestState.copy(
                        isAssessing = false,
                        currentAyahNumber = newAyahNumber
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error assessing chunk")
            _state.value = (_state.value as? ReciteState.RealTimeRecording)?.copy(isAssessing = false) ?: return
        }
    }

    /**
     * Compare accumulated transcription against expected ayahs
     * Returns a RealTimeMistake if mismatch found, null otherwise
     *
     * Uses flexible matching to handle:
     * - First chunk may miss beginning (sync mechanism)
     * - Extra words inserted by Whisper
     * - Slight word order variations
     */
    private fun findMistakeInTranscription(): com.quranmedia.player.recite.domain.model.RealTimeMistake? {
        if (expectedAyahTexts.isEmpty() || currentAyahIndex >= expectedAyahTexts.size) {
            return null
        }

        val transcribedWords = assessedText.toString().split(" ").filter { it.isNotBlank() }
        if (transcribedWords.isEmpty()) {
            return null
        }

        // Build flat list of all expected words from beginning (for sync) or current position
        val allExpectedWords = mutableListOf<Triple<Int, Int, String>>() // (ayahNumber, wordIdx, word)
        val startFromIndex = if (hasSynced) currentAyahIndex else 0

        for ((ayahIdx, ayahData) in expectedAyahTexts.withIndex().drop(startFromIndex)) {
            val (ayahNumber, expectedText) = ayahData
            val words = expectedText.split(" ").filter { it.isNotBlank() }
            words.forEachIndexed { idx, word ->
                allExpectedWords.add(Triple(ayahNumber, idx, word))
            }
        }

        if (allExpectedWords.isEmpty()) {
            return null
        }

        // If not synced yet, find where the transcription starts in the expected text
        if (!hasSynced) {
            syncAttempts++
            val syncResult = findSyncPoint(transcribedWords, allExpectedWords)
            if (syncResult != null) {
                val (syncedAyahNumber, syncedExpectedIdx) = syncResult
                hasSynced = true
                currentAyahIndex = expectedAyahTexts.indexOfFirst { it.first == syncedAyahNumber }
                    .coerceAtLeast(0)
                Timber.d("Synced at ayah $syncedAyahNumber, expectedIdx $syncedExpectedIdx after $syncAttempts attempts")

                // Continue matching from sync point
                return matchFromSyncPoint(transcribedWords, allExpectedWords, syncedExpectedIdx)
            } else if (syncAttempts >= maxSyncAttempts) {
                // Too many attempts without sync - user may be reciting wrong content
                Timber.w("Failed to sync after $syncAttempts attempts")
                val firstExpected = allExpectedWords.firstOrNull()
                if (firstExpected != null) {
                    return com.quranmedia.player.recite.domain.model.RealTimeMistake(
                        ayahNumber = firstExpected.first,
                        expectedText = expectedAyahTexts.find { it.first == firstExpected.first }?.second ?: "",
                        transcribedText = assessedText.toString(),
                        mismatchType = "SYNC_FAILED",
                        expectedWord = firstExpected.third,
                        detectedWord = transcribedWords.firstOrNull() ?: ""
                    )
                }
                return null
            } else {
                // Could not sync yet - wait for more transcription
                Timber.d("Could not sync yet (attempt $syncAttempts/$maxSyncAttempts), waiting for more transcription")
                return null
            }
        }

        // Already synced - do normal matching
        return matchFromSyncPoint(transcribedWords, allExpectedWords, 0)
    }

    /**
     * Find where the transcription syncs with expected text
     * Returns (ayahNumber, expectedWordIndex) or null if no sync found
     */
    private fun findSyncPoint(
        transcribedWords: List<String>,
        allExpectedWords: List<Triple<Int, Int, String>>
    ): Pair<Int, Int>? {
        if (transcribedWords.isEmpty() || allExpectedWords.isEmpty()) {
            return null
        }

        val firstTranscribed = transcribedWords[0]

        // Look for the first transcribed word in expected words
        for ((idx, triple) in allExpectedWords.withIndex()) {
            val (ayahNumber, _, expectedWord) = triple
            if (wordsMatch(expectedWord, firstTranscribed)) {
                // Verify next few words also match to confirm sync
                var matchCount = 1
                for (i in 1 until minOf(3, transcribedWords.size)) {
                    if (idx + i < allExpectedWords.size) {
                        if (wordsMatch(allExpectedWords[idx + i].third, transcribedWords[i])) {
                            matchCount++
                        }
                    }
                }

                // Require at least 2 consecutive matches to confirm sync
                if (matchCount >= 2 || transcribedWords.size == 1) {
                    return Pair(ayahNumber, idx)
                }
            }
        }

        return null
    }

    /**
     * Match transcribed words against expected from a sync point
     */
    private fun matchFromSyncPoint(
        transcribedWords: List<String>,
        allExpectedWords: List<Triple<Int, Int, String>>,
        startExpectedIdx: Int
    ): com.quranmedia.player.recite.domain.model.RealTimeMistake? {
        var expectedIdx = startExpectedIdx
        var transcribedIdx = 0
        var consecutiveMismatches = 0
        val maxConsecutiveMismatches = 3

        while (expectedIdx < allExpectedWords.size && transcribedIdx < transcribedWords.size) {
            val (ayahNumber, wordIdx, expectedWord) = allExpectedWords[expectedIdx]
            val transcribedWord = transcribedWords[transcribedIdx]

            if (wordsMatch(expectedWord, transcribedWord)) {
                expectedIdx++
                transcribedIdx++
                consecutiveMismatches = 0

                // Update currentAyahIndex
                val newAyahIdx = expectedAyahTexts.indexOfFirst { it.first == ayahNumber }
                if (newAyahIdx > currentAyahIndex) {
                    currentAyahIndex = newAyahIdx
                }
            } else {
                // Look ahead in transcribed words
                var foundAhead = false
                for (lookAhead in 1..2) {
                    if (transcribedIdx + lookAhead < transcribedWords.size) {
                        if (wordsMatch(expectedWord, transcribedWords[transcribedIdx + lookAhead])) {
                            transcribedIdx += lookAhead
                            foundAhead = true
                            break
                        }
                    }
                }

                if (foundAhead) {
                    consecutiveMismatches = 0
                    continue
                }

                // Check if user skipped a word
                if (expectedIdx + 1 < allExpectedWords.size) {
                    val nextExpected = allExpectedWords[expectedIdx + 1]
                    if (wordsMatch(nextExpected.third, transcribedWord)) {
                        return com.quranmedia.player.recite.domain.model.RealTimeMistake(
                            ayahNumber = ayahNumber,
                            expectedText = expectedAyahTexts.find { it.first == ayahNumber }?.second ?: "",
                            transcribedText = assessedText.toString(),
                            mismatchType = "MISSING_WORD",
                            expectedWord = expectedWord,
                            detectedWord = "(skipped)"
                        )
                    }
                }

                consecutiveMismatches++
                transcribedIdx++

                if (consecutiveMismatches >= maxConsecutiveMismatches) {
                    return com.quranmedia.player.recite.domain.model.RealTimeMistake(
                        ayahNumber = ayahNumber,
                        expectedText = expectedAyahTexts.find { it.first == ayahNumber }?.second ?: "",
                        transcribedText = assessedText.toString(),
                        mismatchType = "WORD_MISMATCH",
                        expectedWord = expectedWord,
                        detectedWord = transcribedWords.getOrNull(transcribedIdx - consecutiveMismatches) ?: ""
                    )
                }
            }
        }

        // Update ayah index based on progress
        if (expectedIdx > startExpectedIdx) {
            val lastMatched = allExpectedWords.getOrNull(expectedIdx - 1)
            if (lastMatched != null) {
                val newIndex = expectedAyahTexts.indexOfFirst { it.first == lastMatched.first }
                if (newIndex >= 0) {
                    val ayahWords = expectedAyahTexts[newIndex].second.split(" ").filter { it.isNotBlank() }
                    if (lastMatched.second == ayahWords.size - 1) {
                        currentAyahIndex = (newIndex + 1).coerceAtMost(expectedAyahTexts.size - 1)
                    }
                }
            }
        }

        return null
    }

    /**
     * Check if two words match (with fuzzy tolerance)
     */
    private fun wordsMatch(expected: String, transcribed: String): Boolean {
        if (expected == transcribed) return true

        // Calculate Levenshtein distance for fuzzy matching
        val maxLen = maxOf(expected.length, transcribed.length)
        if (maxLen == 0) return true

        val distance = levenshteinDistance(expected, transcribed)
        val similarity = 1.0 - (distance.toDouble() / maxLen)

        return similarity >= 0.7 // 70% similarity threshold
    }

    /**
     * Levenshtein distance calculation
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1]) + 1
                }
            }
        }

        return dp[m][n]
    }

    /**
     * Estimate current ayah based on transcription progress
     */
    private fun estimateCurrentAyah(): Int {
        if (expectedAyahTexts.isEmpty()) return 0

        val transcribedWords = assessedText.toString().split(" ").filter { it.isNotBlank() }.size
        var wordCount = 0

        for ((ayahNumber, ayahText) in expectedAyahTexts) {
            val ayahWords = ayahText.split(" ").filter { it.isNotBlank() }.size
            wordCount += ayahWords

            if (transcribedWords <= wordCount) {
                return ayahNumber
            }
        }

        return expectedAyahTexts.lastOrNull()?.first ?: 0
    }

    /**
     * Continue recording after a mistake - resume from the ayah where mistake occurred
     */
    fun continueFromMistake() {
        val currentState = _state.value as? ReciteState.RealTimeRecording ?: return

        if (!currentState.mistakeDetected) {
            Timber.w("Attempted to continue but no mistake detected")
            return
        }

        viewModelScope.launch {
            try {
                val mistakeAyahNumber = currentState.currentMistake?.ayahNumber
                    ?: currentState.currentAyahNumber

                // Reset transcription and sync state from the mistake ayah
                assessedText.clear()
                hasSynced = false
                syncAttempts = 0

                // Reset ayah index to the mistake ayah
                currentAyahIndex = expectedAyahTexts.indexOfFirst { it.first == mistakeAyahNumber }
                    .coerceAtLeast(0)

                // Start new recording
                val file = audioRecorder.startRecording()
                if (file == null) {
                    _state.value = ReciteState.Error("Failed to resume recording")
                    return@launch
                }

                recordingFile = file
                recordingStartTime = System.currentTimeMillis()
                currentChunkStartTime = System.currentTimeMillis()

                // Update state - clear mistake, resume recording
                _state.value = ReciteState.RealTimeRecording(
                    selection = currentState.selection,
                    elapsedSeconds = currentState.elapsedSeconds, // Keep total time
                    isAssessing = false,
                    currentAyahNumber = mistakeAyahNumber,
                    mistakeDetected = false,
                    currentMistake = null
                )

                // Restart timer and chunk assessment
                startRealTimeTimer()
                startChunkAssessment()

                Timber.d("Resumed real-time recording from ayah $mistakeAyahNumber")
            } catch (e: Exception) {
                Timber.e(e, "Failed to continue from mistake")
                _state.value = ReciteState.Error("Failed to resume: ${e.message}")
            }
        }
    }

    /**
     * Cancel real-time recording
     */
    fun cancelRealTimeRecording() {
        timerJob?.cancel()
        timerJob = null
        chunkAssessmentJob?.cancel()
        chunkAssessmentJob = null
        audioRecorder.cancelRecording()
        recordingFile = null
        assessedText.clear()
        expectedAyahTexts = emptyList()
        currentAyahIndex = 0
        _state.value = ReciteState.Idle
        Timber.d("Real-time recording cancelled")
    }

    /**
     * Start the recording timer
     */
    private fun startTimer(selection: ReciteSelection) {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000) // Update every second
                val elapsed = (System.currentTimeMillis() - recordingStartTime) / 1000
                _state.value = ReciteState.Recording(
                    selection = selection,
                    elapsedSeconds = elapsed
                )
            }
        }
    }

    /**
     * Start the real-time recording timer (updates elapsed seconds)
     */
    private fun startRealTimeTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000) // Update every second
                val currentState = _state.value
                if (currentState is ReciteState.RealTimeRecording && !currentState.mistakeDetected) {
                    _state.value = currentState.copy(elapsedSeconds = currentState.elapsedSeconds + 1)
                } else if (currentState !is ReciteState.RealTimeRecording) {
                    break
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        chunkAssessmentJob?.cancel()
        recordingFile?.let { audioRecorder.deleteRecording(it) }
        assessedText.clear()
        expectedAyahTexts = emptyList()
    }
}
