package com.quranmedia.player.recite.streaming

import kotlinx.coroutines.flow.SharedFlow

/**
 * Common interface for transcription providers.
 * Allows switching between Deepgram and OpenAI implementations.
 */
interface TranscriptionProvider {

    /**
     * Event flow for transcription events.
     */
    val events: SharedFlow<TranscriptionEvent>

    /**
     * Start the transcription session.
     *
     * @param expectedText The expected Quran text (for prompt conditioning)
     */
    fun start(expectedText: String = "")

    /**
     * Stop the transcription session.
     */
    fun stop()

    /**
     * Add audio data (PCM16 bytes at 24kHz mono).
     *
     * @param audioData Raw PCM16 audio bytes
     */
    fun addAudio(audioData: ByteArray)

    /**
     * Update the expected text prompt dynamically.
     *
     * @param newExpectedText The remaining expected text from current position
     */
    fun updateExpectedText(newExpectedText: String)

    /**
     * Common transcription events across all providers.
     */
    sealed class TranscriptionEvent {
        /** Provider is ready to receive audio */
        object Ready : TranscriptionEvent()

        /** Transcription result (complete or partial) */
        data class Transcription(val text: String) : TranscriptionEvent()

        /** Transcription session completed */
        data class Completed(val finalText: String) : TranscriptionEvent()

        /** Error occurred */
        data class Error(val message: String) : TranscriptionEvent()
    }
}

/**
 * Enum for selecting transcription provider.
 */
enum class TranscriptionProviderType {
    QURAN_SERVER,            // Primary: Dedicated Quran ASR server (wav2vec2 + segmenter)
    OPENAI_GPT4O_TRANSCRIBE  // Backup: OpenAI GPT-4o transcription
}
