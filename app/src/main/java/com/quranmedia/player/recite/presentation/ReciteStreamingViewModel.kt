package com.quranmedia.player.recite.presentation

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.BuildConfig
import com.quranmedia.player.data.database.dao.AyahDao
import com.quranmedia.player.data.database.dao.SurahDao
import com.quranmedia.player.data.database.entity.SurahEntity
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.domain.model.Ayah
import com.quranmedia.player.recite.domain.model.*
import com.quranmedia.player.recite.service.ReciteService
import com.quranmedia.player.recite.streaming.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the new streaming-based Recite feature.
 *
 * Supports multiple transcription providers:
 * - Deepgram Whisper API (chunked HTTP)
 * - OpenAI GPT-4o-Transcribe (WebSocket streaming)
 */
@HiltViewModel
class ReciteStreamingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val surahDao: SurahDao,
    private val ayahDao: AyahDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // Components
    private val audioStreamer = AudioStreamer()
    private val forcedAligner = ForcedAligner()
    private val transliterationProvider = TransliterationDataProvider(context)

    // Transcription providers
    private val openaiClient = OpenAITranscribeClient(BuildConfig.OPENAI_API_KEY)
    private val quranServerClient = QuranReciteServerClient(BuildConfig.QURAN_RECITE_SERVER_URL)

    // Current active provider (default to Quran Server - wav2vec2 + segmenter)
    private var _currentProviderType = MutableStateFlow(TranscriptionProviderType.QURAN_SERVER)
    val currentProviderType: StateFlow<TranscriptionProviderType> = _currentProviderType

    // Get current provider based on selection
    private val transcriptionProvider: TranscriptionProvider
        get() = when (_currentProviderType.value) {
            TranscriptionProviderType.QURAN_SERVER -> quranServerClient
            TranscriptionProviderType.OPENAI_GPT4O_TRANSCRIBE -> openaiClient
        }

    // UI State
    private val _streamingState = MutableStateFlow<ReciteStreamingState>(ReciteStreamingState.Idle)
    val streamingState: StateFlow<ReciteStreamingState> = _streamingState

    // Ayahs for the selected range
    private val _selectedAyahs = MutableStateFlow<List<Ayah>>(emptyList())
    val selectedAyahs: StateFlow<List<Ayah>> = _selectedAyahs

    // Word highlights for rendering
    private val _wordHighlights = MutableStateFlow<List<WordHighlightState>>(emptyList())
    val wordHighlights: StateFlow<List<WordHighlightState>> = _wordHighlights

    // Error states for display
    private val _errorHighlights = MutableStateFlow<List<WordHighlightState>>(emptyList())
    val errorHighlights: StateFlow<List<WordHighlightState>> = _errorHighlights

    // Current selection
    private val _selection = MutableStateFlow<ReciteSelection?>(null)
    val selection: StateFlow<ReciteSelection?> = _selection

    // Surahs list
    val allSurahs = surahDao.getAllSurahs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Load ayahs for a specific page.
     * Used by ReciteMushafScreen to display page content.
     */
    fun loadAyahsForPage(pageNumber: Int): kotlinx.coroutines.flow.Flow<List<Ayah>> {
        return kotlinx.coroutines.flow.flow {
            val ayahEntities = ayahDao.getAyahsByPageSync(pageNumber)
            val ayahs = ayahEntities.map { entity ->
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
            emit(ayahs)
        }
    }

    /**
     * Update selection with ayahs from a page.
     * Handles pages with multiple surahs.
     */
    fun updateSelectionFromPage(pageNumber: Int, ayahs: List<Ayah>, allSurahs: List<SurahEntity>) {
        if (ayahs.isEmpty()) return

        viewModelScope.launch {
            _selectedAyahs.value = ayahs

            // For display purposes, use the first surah on the page
            val firstAyah = ayahs.first()
            val lastAyah = ayahs.last()
            val surah = allSurahs.find { it.number == firstAyah.surahNumber }

            val selection = ReciteSelection(
                surahNumber = firstAyah.surahNumber,
                surahName = surah?.nameEnglish ?: "",
                surahNameArabic = surah?.nameArabic ?: "",
                startAyah = firstAyah.ayahNumber,
                endAyah = lastAyah.ayahNumber,
                totalAyahs = surah?.ayahCount ?: ayahs.size,
                pages = listOf(pageNumber)
            )

            _selection.value = selection
            _streamingState.value = ReciteStreamingState.Ready(selection)
        }
    }

    /**
     * Update selection starting from a specific ayah (long-press).
     * User can recite from any ayah and scoring is based on what they recite.
     */
    fun updateSelectionFromAyah(startAyah: Ayah, ayahs: List<Ayah>, allSurahs: List<SurahEntity>) {
        if (ayahs.isEmpty()) return

        viewModelScope.launch {
            _selectedAyahs.value = ayahs

            val lastAyah = ayahs.last()
            val surah = allSurahs.find { it.number == startAyah.surahNumber }

            val selection = ReciteSelection(
                surahNumber = startAyah.surahNumber,
                surahName = surah?.nameEnglish ?: "",
                surahNameArabic = surah?.nameArabic ?: "",
                startAyah = startAyah.ayahNumber,
                endAyah = lastAyah.ayahNumber,
                totalAyahs = surah?.ayahCount ?: ayahs.size,
                pages = ayahs.map { it.page }.distinct().sorted()
            )

            _selection.value = selection
            _streamingState.value = ReciteStreamingState.Ready(selection)

            Timber.d("Selection updated: Surah ${startAyah.surahNumber}, Ayah ${startAyah.ayahNumber} to ${lastAyah.ayahNumber}")
        }
    }

    // App language
    val language = settingsRepository.settings
        .map { it.appLanguage }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppLanguage.ENGLISH
        )

    // Event collection jobs
    private var audioStreamingJob: Job? = null
    private var transcriptionEventsJob: Job? = null
    private var serverResultJob: Job? = null

    /**
     * Switch transcription provider.
     * Can only be changed when not streaming.
     */
    fun setTranscriptionProvider(providerType: TranscriptionProviderType) {
        val currentState = _streamingState.value
        if (currentState is ReciteStreamingState.Streaming) {
            Timber.w("Cannot switch provider while streaming")
            return
        }
        _currentProviderType.value = providerType
        Timber.d("Switched to provider: $providerType")
    }

    // Haptic feedback
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Start selection process.
     */
    fun startSelection() {
        _streamingState.value = ReciteStreamingState.Selecting
    }

    /**
     * Update selection (surah, ayah range).
     */
    fun updateSelection(
        surahNumber: Int,
        startAyah: Int,
        endAyah: Int,
        allSurahs: List<SurahEntity>
    ) {
        viewModelScope.launch {
            val surah = allSurahs.find { it.number == surahNumber } ?: return@launch

            // Load ayahs
            val ayahEntities = ayahDao.getAyahRange(surahNumber, startAyah, endAyah)
            val ayahs = ayahEntities.map { entity ->
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

            _selectedAyahs.value = ayahs

            val selection = ReciteSelection(
                surahNumber = surah.number,
                surahName = surah.nameEnglish,
                surahNameArabic = surah.nameArabic,
                startAyah = startAyah,
                endAyah = endAyah,
                totalAyahs = surah.ayahCount,
                pages = ayahs.map { it.page }.distinct().sorted()
            )

            _selection.value = selection
            _streamingState.value = ReciteStreamingState.Ready(selection)
        }
    }

    /**
     * Start streaming recitation with real-time word highlighting.
     */
    fun startStreaming() {
        val currentSelection = _selection.value ?: return
        val ayahs = _selectedAyahs.value

        if (ayahs.isEmpty()) {
            _streamingState.value = ReciteStreamingState.Error("No ayahs selected")
            return
        }

        viewModelScope.launch {
            try {
                val surahNumber = currentSelection.surahNumber

                // Clear previous highlights
                _wordHighlights.value = emptyList()
                _errorHighlights.value = emptyList()

                // Start foreground service for screen-off support
                ReciteService.start(context)

                // Update state to connecting
                _streamingState.value = ReciteStreamingState.Connecting

                // Provider-specific setup
                val expectedText: String

                if (_currentProviderType.value == TranscriptionProviderType.QURAN_SERVER) {
                    // Quran Server: Configure session with surah/verse info
                    // Server handles Arabic ASR and alignment internally
                    quranServerClient.configureSession(
                        surahId = surahNumber,
                        verseStart = currentSelection.startAyah,
                        verseEnd = currentSelection.endAyah
                    )
                    expectedText = "" // Server has its own Quran data

                    Timber.d("Starting QURAN_SERVER: Surah $surahNumber, verses ${currentSelection.startAyah}-${currentSelection.endAyah}")

                    // Cancel any previous result collection job
                    serverResultJob?.cancel()

                    // Collect recitation results for complete processing
                    serverResultJob = viewModelScope.launch {
                        quranServerClient.recitationResult.collect { result ->
                            result?.let { processServerResult(it, ayahs) }
                        }
                    }
                } else {
                    // Other providers: Use romanized matching
                    // Load transliteration data
                    transliterationProvider.loadData()

                    // Get transliterations for the selected range
                    val transliterations = mutableMapOf<Int, List<Pair<String, String>>>()

                    for (ayah in ayahs) {
                        val ayahTranslit = transliterationProvider.getAyahTransliteration(surahNumber, ayah.ayahNumber)
                        if (ayahTranslit != null) {
                            transliterations[ayah.ayahNumber] = ayahTranslit.words.map { it.arabic to it.transliteration }
                        }
                    }

                    // Initialize forced aligner with expected ayahs and transliterations
                    val ayahData = ayahs.map { it.ayahNumber to it.textArabic }
                    forcedAligner.initialize(ayahData, transliterations)

                    // Set first word as current
                    val firstAyah = ayahs.first()
                    setCurrentWord(firstAyah.surahNumber, firstAyah.ayahNumber, 0)

                    // Build ROMANIZED text for prompt conditioning (not Arabic)
                    // OpenAI has 1024 char limit for prompt
                    val fullRomanizedText = transliterationProvider.getRomanizedText(
                        surahNumber,
                        currentSelection.startAyah,
                        currentSelection.endAyah
                    )
                    expectedText = if (fullRomanizedText.length > 1000) {
                        fullRomanizedText.take(1000)
                    } else {
                        fullRomanizedText
                    }

                    Timber.d("Romanized prompt (${expectedText.length} chars): ${expectedText.take(100)}...")
                }

                Timber.d("Starting with provider: ${_currentProviderType.value}")

                // Collect transcription events from current provider
                transcriptionEventsJob = viewModelScope.launch {
                    transcriptionProvider.events.collect { event ->
                        handleTranscriptionEvent(event)
                    }
                }

                // Start transcription provider with expected text
                transcriptionProvider.start(expectedText)

                // Start audio streaming
                startAudioStreaming()

            } catch (e: Exception) {
                Timber.e(e, "Failed to start streaming")
                _streamingState.value = ReciteStreamingState.Error("Failed to connect: ${e.message}")
            }
        }
    }

    /**
     * Process complete results from Quran server.
     * Converts server word matches to UI highlights.
     */
    private fun processServerResult(result: RecitationResult, ayahs: List<Ayah>) {
        Timber.d("processServerResult called with ${result.wordMatches.size} word matches")

        // Only process if we're still in streaming/connecting state
        val currentState = _streamingState.value
        Timber.d("Current state: $currentState")

        if (currentState !is ReciteStreamingState.Streaming &&
            currentState !is ReciteStreamingState.Connecting) {
            Timber.w("Ignoring server result - not in streaming state (state=$currentState)")
            return
        }

        val surahNumber = _selection.value?.surahNumber ?: run {
            Timber.e("No selection available, cannot process result")
            return
        }

        Timber.d("Processing server result: ${result.accuracy}% accuracy")
        Timber.d("Transcribed: ${result.transcribedText}")
        Timber.d("Word matches: ${result.wordMatches.size}")

        // Build word highlights from server result
        val highlights = mutableListOf<WordHighlightState>()

        // Build a mapping from expected word index to ayah/word position
        var globalWordIndex = 0
        val wordPositions = mutableMapOf<Int, Pair<Int, Int>>() // globalIndex -> (ayahNumber, localWordIndex)

        for (ayah in ayahs) {
            val words = ayah.textArabic.split("\\s+".toRegex()).filter { it.isNotBlank() }
            for (wordIdx in words.indices) {
                wordPositions[globalWordIndex] = ayah.ayahNumber to wordIdx
                globalWordIndex++
            }
        }

        // Process word matches from server
        for (match in result.wordMatches) {
            // Skip if no valid expected index (extra words)
            if (match.expectedIndex < 0) continue

            val position = wordPositions[match.expectedIndex] ?: continue
            val (ayahNumber, wordIndex) = position

            val highlightType = when (match.type) {
                "correct" -> HighlightType.CORRECT
                "wrong" -> HighlightType.ERROR
                "skipped" -> HighlightType.ERROR  // User skipped this word
                "missing" -> HighlightType.ERROR
                "extra" -> continue // Skip extra words (not in expected)
                else -> continue
            }

            val highlight = WordHighlightState(
                surahNumber = surahNumber,
                ayahNumber = ayahNumber,
                wordIndex = wordIndex,
                type = highlightType,
                expectedWord = match.expected,
                transcribedWord = match.transcribed
            )

            highlights.add(highlight)

            // Track errors
            if (highlightType == HighlightType.ERROR) {
                _errorHighlights.value = _errorHighlights.value + highlight
            }
        }

        _wordHighlights.value = highlights

        // Handle completion - show words recited info
        val errorCount = highlights.count { it.type == HighlightType.ERROR }
        val correctCount = highlights.count { it.type == HighlightType.CORRECT }

        Timber.d("Recitation complete: ${result.wordsRecited} words recited, $correctCount correct, $errorCount errors")

        _streamingState.value = ReciteStreamingState.Completed(
            selection = _selection.value!!,
            accuracy = result.accuracy,
            errorCount = errorCount
        )

        // Haptic feedback based on result
        if (errorCount > 0) {
            // Vibrate pattern: error pulse for mistakes
            vibrateForErrors(errorCount)
        } else {
            // Success vibration
            vibrateOnCompletion()
        }
    }

    /**
     * Handle events from the transcription provider (Deepgram or OpenAI).
     */
    private fun handleTranscriptionEvent(event: TranscriptionProvider.TranscriptionEvent) {
        val providerName = _currentProviderType.value.name
        when (event) {
            is TranscriptionProvider.TranscriptionEvent.Ready -> {
                Timber.d("$providerName ready")
                _streamingState.value = ReciteStreamingState.Streaming(
                    selection = _selection.value!!,
                    isActive = true
                )
            }

            is TranscriptionProvider.TranscriptionEvent.Transcription -> {
                Timber.d("$providerName transcription: ${event.text}")
                processTranscriptionDelta(event.text)
            }

            is TranscriptionProvider.TranscriptionEvent.Completed -> {
                Timber.d("$providerName completed: ${event.finalText}")
                // Process any remaining partial words with isComplete=true
                processTranscriptionDelta("", isComplete = true)
            }

            is TranscriptionProvider.TranscriptionEvent.Error -> {
                Timber.e("$providerName error: ${event.message}")
                _streamingState.value = ReciteStreamingState.Error(event.message)
            }
        }
    }

    /**
     * Start audio streaming from microphone.
     */
    private fun startAudioStreaming() {
        if (!audioStreamer.startStreaming()) {
            _streamingState.value = ReciteStreamingState.Error("Failed to start audio capture")
            return
        }

        // Collect raw audio chunks and send to transcription provider
        audioStreamingJob = viewModelScope.launch {
            audioStreamer.rawAudioChunks.collect { audioBytes ->
                transcriptionProvider.addAudio(audioBytes)
            }
        }
    }

    /**
     * Stop audio streaming.
     */
    private fun stopAudioStreaming() {
        audioStreamingJob?.cancel()
        audioStreamingJob = null
        audioStreamer.stopStreaming()
    }

    /**
     * Process transcription from provider.
     * Both providers return complete transcriptions per event.
     */
    private fun processTranscriptionDelta(transcriptionText: String, isComplete: Boolean = false) {
        if (transcriptionText.isBlank()) {
            if (isComplete) {
                // Flush any remaining in aligner
                forcedAligner.processTranscription("", true)
            }
            return
        }

        // Deepgram returns complete transcriptions, process directly
        // Add trailing space to mark words as complete
        val textWithSpace = transcriptionText.trim() + " "
        val results = forcedAligner.processTranscription(textWithSpace, isComplete)

        for (result in results) {
            when (result) {
                is ForcedAligner.AlignmentResult.Match -> {
                    handleWordMatch(result)
                }
                is ForcedAligner.AlignmentResult.FuzzyMatch -> {
                    handleWordMatch(result)
                }
                is ForcedAligner.AlignmentResult.Mismatch -> {
                    handleWordMismatch(result)
                }
            }
        }

        // Update current word position
        val position = forcedAligner.getCurrentPosition()
        if (!position.isComplete) {
            setCurrentWord(
                _selection.value?.surahNumber ?: return,
                position.ayahNumber,
                position.wordIndex
            )

            // Update the transcription provider prompt with remaining expected text
            // This keeps the model "primed" for what's coming next
            // Skip for Moonshine and Dual - they handle prompts internally
            if (results.isNotEmpty() && _currentProviderType.value == TranscriptionProviderType.OPENAI_GPT4O_TRANSCRIBE) {
                val remainingText = forcedAligner.getRemainingExpectedText(50)
                if (remainingText.isNotBlank()) {
                    transcriptionProvider.updateExpectedText(remainingText)
                }
            }
        } else {
            // Recitation complete
            handleRecitationComplete()
        }
    }

    /**
     * Handle a word match (correct recitation).
     */
    private fun handleWordMatch(result: ForcedAligner.AlignmentResult) {
        val surahNumber = _selection.value?.surahNumber ?: return

        // Check if this word was already highlighted (avoid duplicates from resync)
        val existingHighlight = _wordHighlights.value.find {
            it.surahNumber == surahNumber &&
            it.ayahNumber == result.ayahNumber &&
            it.wordIndex == result.wordIndex &&
            it.type == HighlightType.CORRECT
        }

        if (existingHighlight != null) {
            // Already highlighted as correct, skip
            return
        }

        // Remove any ERROR highlight for this word (user corrected it)
        val updatedHighlights = _wordHighlights.value.filter {
            !(it.surahNumber == surahNumber &&
              it.ayahNumber == result.ayahNumber &&
              it.wordIndex == result.wordIndex &&
              it.type == HighlightType.ERROR)
        }

        // Also remove from error list
        _errorHighlights.value = _errorHighlights.value.filter {
            !(it.surahNumber == surahNumber &&
              it.ayahNumber == result.ayahNumber &&
              it.wordIndex == result.wordIndex)
        }

        // Add correct highlight
        val newHighlight = WordHighlightState(
            surahNumber = surahNumber,
            ayahNumber = result.ayahNumber,
            wordIndex = result.wordIndex,
            type = HighlightType.CORRECT,
            expectedWord = result.expectedWord,
            transcribedWord = result.transcribedWord
        )

        _wordHighlights.value = updatedHighlights + newHighlight
    }

    /**
     * Handle a word mismatch (error).
     * Note: Aligner no longer advances on mismatch, so user can retry.
     * We only show error once per word position to avoid spam.
     */
    private fun handleWordMismatch(result: ForcedAligner.AlignmentResult.Mismatch) {
        val surahNumber = _selection.value?.surahNumber ?: return

        // Check if we already have an error for this word position (avoid duplicates)
        val existingError = _errorHighlights.value.find {
            it.surahNumber == surahNumber &&
            it.ayahNumber == result.ayahNumber &&
            it.wordIndex == result.wordIndex
        }

        if (existingError != null) {
            // Already have an error for this position, don't add another
            // Just log it for debugging
            Timber.d("Mismatch (duplicate, not adding): expected '${result.expectedWord}', got '${result.transcribedWord}'")
            return
        }

        // Add error highlight
        val errorHighlight = WordHighlightState(
            surahNumber = surahNumber,
            ayahNumber = result.ayahNumber,
            wordIndex = result.wordIndex,
            type = HighlightType.ERROR,
            expectedWord = result.expectedWord,
            transcribedWord = result.transcribedWord
        )

        _wordHighlights.value = _wordHighlights.value + errorHighlight
        _errorHighlights.value = _errorHighlights.value + errorHighlight

        // Vibrate on mistake
        vibrateOnMistake()

        Timber.d("Mistake: expected '${result.expectedWord}', got '${result.transcribedWord}' (${result.type})")
    }

    /**
     * Set the current word being waited for.
     */
    private fun setCurrentWord(surahNumber: Int, ayahNumber: Int, wordIndex: Int) {
        // Remove previous CURRENT highlight
        val updatedHighlights = _wordHighlights.value.filter { it.type != HighlightType.CURRENT }

        // Add new CURRENT highlight
        val currentHighlight = WordHighlightState(
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            wordIndex = wordIndex,
            type = HighlightType.CURRENT
        )

        _wordHighlights.value = updatedHighlights + currentHighlight
    }

    /**
     * Handle recitation completion.
     */
    private fun handleRecitationComplete() {
        stopStreaming()

        val errorCount = _errorHighlights.value.size
        val totalWords = _wordHighlights.value.count { it.type != HighlightType.CURRENT }
        val accuracy = if (totalWords > 0) {
            ((totalWords - errorCount).toFloat() / totalWords) * 100f
        } else {
            100f
        }

        _streamingState.value = ReciteStreamingState.Completed(
            selection = _selection.value!!,
            accuracy = accuracy,
            errorCount = errorCount
        )

        // Haptic feedback for completion
        vibrateOnCompletion()
    }

    /**
     * Stop streaming and clean up.
     * Note: We don't cancel serverResultJob here - we need to receive the final result from server.
     */
    fun stopStreaming() {
        stopAudioStreaming()
        transcriptionEventsJob?.cancel()
        transcriptionEventsJob = null
        // DON'T cancel serverResultJob - we need to receive the result!
        transcriptionProvider.stop()

        // Stop foreground service
        ReciteService.stop(context)

        // Set timeout fallback - if no result after 15 seconds, go to Ready state
        viewModelScope.launch {
            kotlinx.coroutines.delay(15000)
            val current = _streamingState.value
            if (current is ReciteStreamingState.Streaming || current is ReciteStreamingState.Connecting) {
                Timber.w("Timeout waiting for server result, resetting to Ready")
                _streamingState.value = ReciteStreamingState.Ready(current.let {
                    when (it) {
                        is ReciteStreamingState.Streaming -> it.selection
                        is ReciteStreamingState.Ready -> it.selection
                        else -> _selection.value!!
                    }
                })
            }
        }
    }

    /**
     * Reset to idle state.
     */
    fun reset() {
        // Cancel all jobs first
        serverResultJob?.cancel()
        serverResultJob = null

        stopStreaming()
        _streamingState.value = ReciteStreamingState.Idle
        _selection.value = null
        _selectedAyahs.value = emptyList()
        _wordHighlights.value = emptyList()
        _errorHighlights.value = emptyList()
        forcedAligner.reset()
    }

    /**
     * Retry from a specific error position.
     * Resets aligner to that position, clears errors from that point forward,
     * and continues streaming.
     *
     * @param ayahNumber The ayah number to retry from
     * @param wordIndex The word index within the ayah
     */
    fun retryFromPosition(ayahNumber: Int, wordIndex: Int) {
        val surahNumber = _selection.value?.surahNumber ?: return

        Timber.d("Retrying from ayah $ayahNumber, word $wordIndex")

        // Reset aligner to this position
        forcedAligner.resumeFromPosition(ayahNumber, wordIndex)

        // Clear highlights from this position forward (keep earlier ones)
        _wordHighlights.value = _wordHighlights.value.filter { highlight ->
            // Keep highlights that are before the retry position
            if (highlight.ayahNumber < ayahNumber) {
                true
            } else if (highlight.ayahNumber == ayahNumber) {
                highlight.wordIndex < wordIndex
            } else {
                false // Remove highlights from later ayahs
            }
        }

        // Clear error highlights from this position forward
        _errorHighlights.value = _errorHighlights.value.filter { highlight ->
            if (highlight.ayahNumber < ayahNumber) {
                true
            } else if (highlight.ayahNumber == ayahNumber) {
                highlight.wordIndex < wordIndex
            } else {
                false
            }
        }

        // Set current word to the retry position
        setCurrentWord(surahNumber, ayahNumber, wordIndex)

        // Update the prompt with remaining expected text (only for OpenAI direct)
        if (_currentProviderType.value == TranscriptionProviderType.OPENAI_GPT4O_TRANSCRIBE) {
            val remainingText = forcedAligner.getRemainingExpectedText(50)
            if (remainingText.isNotBlank()) {
                transcriptionProvider.updateExpectedText(remainingText)
            }
        }

        Timber.d("Retry: cleared highlights from ayah $ayahNumber word $wordIndex forward")
    }

    /**
     * Vibrate on mistake.
     */
    private fun vibrateOnMistake() {
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(100)
            }
        }
    }

    /**
     * Vibrate on completion (success).
     */
    private fun vibrateOnCompletion() {
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(200)
            }
        }
    }

    /**
     * Vibrate pattern for errors - distinct from success.
     * Uses a double-pulse pattern to indicate mistakes were made.
     */
    private fun vibrateForErrors(errorCount: Int) {
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Double-pulse pattern: vibrate-pause-vibrate
                // timings: [delay, vibrate, pause, vibrate]
                val pattern = longArrayOf(0, 150, 100, 150)
                v.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                // Fallback: double vibration
                v.vibrate(longArrayOf(0, 150, 100, 150), -1)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopStreaming()
        quranServerClient.close()
    }
}

/**
 * Streaming state for the Recite feature.
 */
sealed class ReciteStreamingState {
    /** Initial idle state */
    object Idle : ReciteStreamingState()

    /** User is selecting surah/ayah */
    object Selecting : ReciteStreamingState()

    /** Selection complete, ready to start */
    data class Ready(val selection: ReciteSelection) : ReciteStreamingState()

    /** Connecting to transcription provider */
    object Connecting : ReciteStreamingState()

    /** Actively streaming and transcribing */
    data class Streaming(
        val selection: ReciteSelection,
        val isActive: Boolean
    ) : ReciteStreamingState()

    /** Recitation completed */
    data class Completed(
        val selection: ReciteSelection,
        val accuracy: Float,
        val errorCount: Int
    ) : ReciteStreamingState()

    /** Error occurred */
    data class Error(val message: String) : ReciteStreamingState()
}
