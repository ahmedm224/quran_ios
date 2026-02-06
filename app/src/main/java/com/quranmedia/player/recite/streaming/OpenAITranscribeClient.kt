package com.quranmedia.player.recite.streaming

import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Client for OpenAI's GPT-4o-Transcribe WebSocket API for Arabic transcription.
 *
 * Uses real-time WebSocket streaming for live transcription.
 * Supports prompt conditioning for improved accuracy with Quranic Arabic.
 *
 * API Reference: OpenAI Realtime Transcription API
 */
class OpenAITranscribeClient(private val apiKey: String) : TranscriptionProvider {

    companion object {
        // WebSocket endpoint for transcription-only mode
        private const val WS_URL = "wss://api.openai.com/v1/realtime?intent=transcription"

        // Transcription model
        private const val MODEL = "gpt-4o-transcribe"

        // Language for romanized output (English ASR for Arabic speech)
        private const val LANGUAGE = "en"

        // Connection timeouts
        private const val CONNECT_TIMEOUT_SECONDS = 30L
        private const val READ_TIMEOUT_SECONDS = 60L
        private const val PING_INTERVAL_SECONDS = 30L

        // Periodic commit interval in milliseconds
        // This forces transcription even during continuous speech
        // Longer interval = more context per chunk = better word boundaries
        private const val COMMIT_INTERVAL_MS = 5000L
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)

    private var webSocket: WebSocket? = null
    private var isActive = false
    private var isSessionConfigured = false

    // Periodic commit job - forces transcription at regular intervals
    private var periodicCommitJob: Job? = null
    private var hasAudioSinceLastCommit = false

    // Event flows
    private val _events = MutableSharedFlow<TranscriptionProvider.TranscriptionEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<TranscriptionProvider.TranscriptionEvent> = _events

    // Expected text for prompt conditioning
    private var expectedText: String = ""

    // Accumulated transcription from delta events
    private var accumulatedTranscript = StringBuilder()

    /**
     * Start the transcription session.
     *
     * @param expectedText The expected Quran text (for prompt conditioning)
     */
    override fun start(expectedText: String) {
        if (isActive) {
            Timber.w("OpenAITranscribeClient already active")
            return
        }

        this.expectedText = expectedText
        isActive = true
        isSessionConfigured = false
        accumulatedTranscript.clear()
        hasAudioSinceLastCommit = false

        Timber.d("OpenAITranscribeClient starting...")
        Timber.d("Expected text: ${expectedText.take(50)}...")

        // Connect WebSocket
        connectWebSocket()
    }

    /**
     * Connect to the WebSocket endpoint.
     */
    private fun connectWebSocket() {
        val request = Request.Builder()
            .url(WS_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("OpenAI-Beta", "realtime=v1")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("OpenAI WebSocket connected")
                // Wait for transcription_session.created event to configure
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "OpenAI WebSocket error: ${response?.message}")
                emitEvent(TranscriptionProvider.TranscriptionEvent.Error("WebSocket error: ${t.message}"))
                isActive = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("OpenAI WebSocket closed: $code - $reason")
                isActive = false
            }
        })
    }

    /**
     * Handle incoming WebSocket messages.
     */
    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type", "")

            // Log all messages for debugging
            Timber.d("OpenAI WS message: $type")

            when (type) {
                "transcription_session.created" -> {
                    Timber.d("Transcription session created, configuring...")
                    configureSession()
                }

                "transcription_session.updated" -> {
                    Timber.d("Transcription session configured")
                    isSessionConfigured = true
                    // Start periodic commit job to force transcription at intervals
                    startPeriodicCommitJob()
                    emitEvent(TranscriptionProvider.TranscriptionEvent.Ready)
                }

                "conversation.item.input_audio_transcription.delta" -> {
                    val delta = json.optString("delta", "")
                    if (delta.isNotEmpty()) {
                        accumulatedTranscript.append(delta)
                        Timber.d("Delta: $delta (accumulated: ${accumulatedTranscript})")
                    }
                }

                "conversation.item.input_audio_transcription.completed" -> {
                    val transcript = json.optString("transcript", "")
                    Timber.d("OpenAI transcription completed: $transcript")

                    if (transcript.isNotBlank()) {
                        emitEvent(TranscriptionProvider.TranscriptionEvent.Transcription(transcript))
                    }
                    accumulatedTranscript.clear()
                }

                // Handle conversation.item.created - may contain transcription
                // Note: We now primarily use .completed events, this is just for backup
                "conversation.item.created" -> {
                    val item = json.optJSONObject("item")
                    if (item != null) {
                        val content = item.optJSONArray("content")
                        if (content != null && content.length() > 0) {
                            for (i in 0 until content.length()) {
                                val contentItem = content.optJSONObject(i)
                                val contentType = contentItem?.optString("type", "")
                                if (contentType == "input_audio") {
                                    // Check if transcript is null in JSON (returns "null" string)
                                    val isNull = contentItem?.isNull("transcript") == true
                                    val transcript = contentItem?.optString("transcript", "") ?: ""
                                    // Filter out empty, "null" string, and actual null values
                                    if (!isNull && transcript.isNotBlank() && transcript != "null") {
                                        Timber.d("OpenAI transcription from item.created: $transcript")
                                        emitEvent(TranscriptionProvider.TranscriptionEvent.Transcription(transcript))
                                    }
                                }
                            }
                        }
                    }
                }

                "input_audio_buffer.speech_started" -> {
                    Timber.d("Speech started (VAD)")
                }

                "input_audio_buffer.speech_stopped" -> {
                    Timber.d("Speech stopped (VAD)")
                }

                "input_audio_buffer.committed" -> {
                    Timber.d("Audio buffer committed")
                }

                "error" -> {
                    val error = json.optJSONObject("error")
                    val message = error?.optString("message") ?: "Unknown error"
                    val code = error?.optString("code") ?: ""

                    // Ignore empty buffer errors - they just mean VAD already committed
                    if (code == "input_audio_buffer_commit_empty") {
                        Timber.d("Empty buffer commit (VAD already committed) - ignoring")
                        return
                    }

                    Timber.e("OpenAI error [$code]: $message")
                    Timber.e("Full error response: $text")
                    emitEvent(TranscriptionProvider.TranscriptionEvent.Error(message))
                }

                else -> {
                    Timber.d("Unhandled message: $text")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse WebSocket message: $text")
        }
    }

    /**
     * Configure the transcription session.
     *
     * Uses romanized Quran text as prompt for the English ASR model.
     * This guides the model to output romanized Arabic (e.g., "tabbat yadā abī")
     * instead of recognizing it as random sounds.
     */
    private fun configureSession() {
        val sessionConfig = JSONObject().apply {
            put("type", "transcription_session.update")
            put("session", JSONObject().apply {
                put("input_audio_format", "pcm16")
                put("input_audio_transcription", JSONObject().apply {
                    put("model", MODEL)
                    put("language", LANGUAGE)
                    // Use romanized text as prompt to guide the model
                    // The expectedText should contain romanized transliterations
                    if (expectedText.isNotBlank()) {
                        put("prompt", expectedText)
                        Timber.d("Configured with romanized prompt: ${expectedText.take(50)}...")
                    }
                })
                // Use server-side VAD for automatic turn detection
                // Quran recitation has natural pauses between words/ayahs
                put("turn_detection", JSONObject().apply {
                    put("type", "server_vad")
                    put("threshold", 0.4)  // Lower threshold - more sensitive to speech
                    put("silence_duration_ms", 2000)  // 2s silence required before cutting
                    put("prefix_padding_ms", 500)  // Padding to capture word beginnings
                })
            })
        }

        webSocket?.send(sessionConfig.toString())
        Timber.d("Session configuration sent")
    }

    /**
     * Stop the transcription session.
     */
    override fun stop() {
        if (!isActive) return

        isActive = false
        isSessionConfigured = false

        // Stop periodic commit job
        stopPeriodicCommitJob()

        // Commit any remaining audio
        commitAudioBuffer()

        // Close WebSocket
        webSocket?.close(1000, "Session ended")
        webSocket = null

        // Emit final transcription if any accumulated
        if (accumulatedTranscript.isNotBlank()) {
            emitEvent(TranscriptionProvider.TranscriptionEvent.Completed(accumulatedTranscript.toString()))
        } else {
            emitEvent(TranscriptionProvider.TranscriptionEvent.Completed(""))
        }

        accumulatedTranscript.clear()
        hasAudioSinceLastCommit = false
        Timber.d("OpenAITranscribeClient stopped")
    }

    /**
     * Update the expected text.
     *
     * NOTE: We no longer reconfigure the session with expected text as prompt.
     * This was causing the model to hallucinate/auto-complete Quran verses.
     * The expected text is still tracked internally but not sent to OpenAI.
     *
     * @param newExpectedText The remaining expected text from current position
     */
    override fun updateExpectedText(newExpectedText: String) {
        this.expectedText = newExpectedText
        Timber.d("Updated expected text (not sent to OpenAI): ${newExpectedText.take(50)}...")
        // Do NOT reconfigure session - this was causing hallucination
    }

    /**
     * Add audio data (PCM16 bytes at 24kHz mono).
     *
     * @param audioData Raw PCM16 audio bytes
     */
    override fun addAudio(audioData: ByteArray) {
        if (!isActive || !isSessionConfigured) return

        // Base64 encode the audio
        val base64Audio = Base64.encodeToString(audioData, Base64.NO_WRAP)

        // Send audio chunk
        val message = JSONObject().apply {
            put("type", "input_audio_buffer.append")
            put("audio", base64Audio)
        }

        webSocket?.send(message.toString())

        // Track that we've sent audio (for periodic commit)
        hasAudioSinceLastCommit = true
    }

    /**
     * Commit the audio buffer to signal end of input.
     */
    private fun commitAudioBuffer() {
        val message = JSONObject().apply {
            put("type", "input_audio_buffer.commit")
        }
        webSocket?.send(message.toString())
        hasAudioSinceLastCommit = false
        Timber.d("Audio buffer commit sent")
    }

    /**
     * Start periodic commit job.
     * This forces transcription at regular intervals even during continuous speech.
     * Without this, transcription only happens when VAD detects silence.
     */
    private fun startPeriodicCommitJob() {
        periodicCommitJob?.cancel()
        periodicCommitJob = scope.launch {
            while (isActive && isSessionConfigured) {
                delay(COMMIT_INTERVAL_MS)

                if (!isActive || !isSessionConfigured) break

                // Only commit if audio was sent since last commit
                if (hasAudioSinceLastCommit) {
                    Timber.d("Periodic commit: forcing transcription")
                    commitAudioBuffer()
                }
            }
        }
        Timber.d("Periodic commit job started (interval: ${COMMIT_INTERVAL_MS}ms)")
    }

    /**
     * Stop periodic commit job.
     */
    private fun stopPeriodicCommitJob() {
        periodicCommitJob?.cancel()
        periodicCommitJob = null
    }

    private fun emitEvent(event: TranscriptionProvider.TranscriptionEvent) {
        scope.launch {
            _events.emit(event)
        }
    }
}
