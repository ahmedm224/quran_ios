package com.quranmedia.player.recite.streaming

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Dual Transcription Provider - uses both OpenAI and Moonshine simultaneously.
 *
 * Only flags errors when BOTH models agree something is wrong.
 * This reduces false positives from either model's hallucinations.
 *
 * Strategy:
 * - OpenAI (GPT-4o-transcribe): Fast streaming, good for real-time following
 * - Moonshine (local): Slower chunks, good for verification
 *
 * Error consensus:
 * - Both agree correct → Mark as correct (green)
 * - Both agree wrong → Mark as error (red)
 * - Disagree → Trust the positive, mark as correct (green)
 */
class DualTranscriptionProvider(
    private val openaiProvider: OpenAITranscribeClient,
    private val moonshineProvider: MoonshineClient
) : TranscriptionProvider {

    private val scope = CoroutineScope(Dispatchers.IO)

    // Event flow for consensus results
    private val _events = MutableSharedFlow<TranscriptionProvider.TranscriptionEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<TranscriptionProvider.TranscriptionEvent> = _events

    // Track transcriptions from both providers for comparison
    private val openaiTranscriptions = mutableListOf<String>()
    private val moonshineTranscriptions = mutableListOf<String>()

    // Flag to track if both are ready
    private var openaiReady = false
    private var moonshineReady = false

    private var isActive = false

    init {
        // Listen to OpenAI events
        scope.launch {
            openaiProvider.events.collect { event ->
                handleOpenAIEvent(event)
            }
        }

        // Listen to Moonshine events
        scope.launch {
            moonshineProvider.events.collect { event ->
                handleMoonshineEvent(event)
            }
        }
    }

    override fun start(expectedText: String) {
        if (isActive) return

        isActive = true
        openaiReady = false
        moonshineReady = false
        openaiTranscriptions.clear()
        moonshineTranscriptions.clear()

        Timber.d("DualTranscriptionProvider starting both providers")

        // Start both providers
        openaiProvider.start(expectedText)
        moonshineProvider.start(expectedText)
    }

    override fun stop() {
        if (!isActive) return

        isActive = false

        Timber.d("DualTranscriptionProvider stopping both providers")

        openaiProvider.stop()
        moonshineProvider.stop()
    }

    override fun updateExpectedText(newExpectedText: String) {
        // Only update OpenAI - Moonshine doesn't use prompts
        openaiProvider.updateExpectedText(newExpectedText)
    }

    override fun addAudio(audioData: ByteArray) {
        if (!isActive) return

        // Send audio to BOTH providers
        openaiProvider.addAudio(audioData)
        moonshineProvider.addAudio(audioData)
    }

    /**
     * Handle events from OpenAI provider.
     */
    private suspend fun handleOpenAIEvent(event: TranscriptionProvider.TranscriptionEvent) {
        when (event) {
            is TranscriptionProvider.TranscriptionEvent.Ready -> {
                openaiReady = true
                checkBothReady()
            }
            is TranscriptionProvider.TranscriptionEvent.Transcription -> {
                Timber.d("OpenAI transcription: ${event.text}")
                openaiTranscriptions.add(event.text)
                // Emit OpenAI transcription immediately for responsive UI
                emitConsensusTranscription(event.text, "openai")
            }
            is TranscriptionProvider.TranscriptionEvent.Error -> {
                Timber.e("OpenAI error: ${event.message}")
                // Don't fail completely - Moonshine might still work
            }
            is TranscriptionProvider.TranscriptionEvent.Completed -> {
                Timber.d("OpenAI completed: ${event.finalText}")
            }
        }
    }

    /**
     * Handle events from Moonshine provider.
     */
    private suspend fun handleMoonshineEvent(event: TranscriptionProvider.TranscriptionEvent) {
        when (event) {
            is TranscriptionProvider.TranscriptionEvent.Ready -> {
                moonshineReady = true
                checkBothReady()
            }
            is TranscriptionProvider.TranscriptionEvent.Transcription -> {
                Timber.d("Moonshine transcription: ${event.text}")
                moonshineTranscriptions.add(event.text)
                // Emit Moonshine transcription for consensus checking
                emitConsensusTranscription(event.text, "moonshine")
            }
            is TranscriptionProvider.TranscriptionEvent.Error -> {
                Timber.e("Moonshine error: ${event.message}")
                // Don't fail completely - OpenAI might still work
            }
            is TranscriptionProvider.TranscriptionEvent.Completed -> {
                Timber.d("Moonshine completed: ${event.finalText}")
            }
        }
    }

    /**
     * Check if both providers are ready.
     */
    private suspend fun checkBothReady() {
        if (openaiReady && moonshineReady) {
            Timber.d("Both providers ready")
            _events.emit(TranscriptionProvider.TranscriptionEvent.Ready)
        } else if (openaiReady || moonshineReady) {
            // One is ready - emit ready anyway for responsiveness
            Timber.d("One provider ready (OpenAI=$openaiReady, Moonshine=$moonshineReady)")
            _events.emit(TranscriptionProvider.TranscriptionEvent.Ready)
        }
    }

    /**
     * Emit transcription with source tag for consensus processing.
     *
     * The ForcedAligner will receive transcriptions from both sources
     * and can use consensus logic to determine errors.
     */
    private suspend fun emitConsensusTranscription(text: String, source: String) {
        // Tag the transcription with source for the aligner to process
        // Format: "source:text" - the aligner will parse this
        _events.emit(TranscriptionProvider.TranscriptionEvent.Transcription("$source:$text"))
    }
}
