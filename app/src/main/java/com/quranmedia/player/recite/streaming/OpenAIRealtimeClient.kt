package com.quranmedia.player.recite.streaming

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * WebSocket client for OpenAI Realtime API transcription.
 *
 * Connects to OpenAI's Realtime API and streams audio for real-time transcription.
 * Uses gpt-4o-transcribe model for Arabic Quran recitation.
 *
 * API Reference: https://platform.openai.com/docs/guides/realtime-transcription
 */
class OpenAIRealtimeClient(private val apiKey: String) {

    companion object {
        // OpenAI Realtime API WebSocket endpoint for transcription
        private const val WS_URL = "wss://api.openai.com/v1/realtime?intent=transcription"

        // Transcription model
        private const val MODEL = "gpt-4o-transcribe"

        // Language for Arabic Quran
        private const val LANGUAGE = "ar"

        // Connection timeouts
        private const val CONNECT_TIMEOUT_SECONDS = 30L
        private const val READ_TIMEOUT_SECONDS = 60L
        private const val WRITE_TIMEOUT_SECONDS = 30L

        // Ping interval to keep connection alive
        private const val PING_INTERVAL_SECONDS = 20L
    }

    private var webSocket: WebSocket? = null
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
        .build()

    private var isConnected = false
    private var expectedText: String = ""
    private var isSessionConfigured = false
    private val scope = CoroutineScope(Dispatchers.IO)

    // Event flows
    private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeEvent> = _events

    /**
     * Connect to OpenAI Realtime API.
     *
     * @param expectedQuranText The expected Quran text for prompt conditioning
     */
    fun connect(expectedQuranText: String = "") {
        if (isConnected) {
            Timber.w("Already connected to OpenAI Realtime API")
            return
        }

        this.expectedText = expectedQuranText
        Timber.d("Connecting to OpenAI Realtime API...")
        Timber.d("Expected text: $expectedQuranText")

        val request = Request.Builder()
            .url(WS_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("OpenAI-Beta", "realtime=v1")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("WebSocket connected")
                isConnected = true
                emitEvent(RealtimeEvent.Connected)

                // Configure session immediately after connection
                configureSession()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "WebSocket failure: ${response?.message}")
                isConnected = false
                isSessionConfigured = false
                emitEvent(RealtimeEvent.Error("Connection failed: ${t.message}"))
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket closing: $code - $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket closed: $code - $reason")
                isConnected = false
                isSessionConfigured = false
                emitEvent(RealtimeEvent.Disconnected)
            }
        })
    }

    /**
     * Disconnect from OpenAI Realtime API.
     */
    fun disconnect() {
        Timber.d("Disconnecting from OpenAI Realtime API...")
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        isConnected = false
        isSessionConfigured = false
    }

    /**
     * Configure the transcription session.
     */
    private fun configureSession() {
        if (!isConnected) {
            Timber.w("Cannot configure session - not connected")
            return
        }

        // Build prompt with expected text for better accuracy
        val prompt = if (expectedText.isNotBlank()) {
            "The user is reciting the following Quran verses. " +
            "Transcribe EXACTLY what they say in Arabic script. " +
            "Expected text: $expectedText"
        } else {
            "The user is reciting verses from the Holy Quran in Arabic. " +
            "Transcribe accurately using Arabic script only."
        }

        Timber.d("Transcription prompt: $prompt")

        val sessionConfig = JSONObject().apply {
            put("type", "transcription_session.update")
            put("session", JSONObject().apply {
                put("input_audio_format", "pcm16")
                put("input_audio_transcription", JSONObject().apply {
                    put("model", MODEL)
                    put("language", LANGUAGE)
                    put("prompt", prompt)
                })
                // Enable server-side VAD
                put("turn_detection", JSONObject().apply {
                    put("type", "server_vad")
                    put("threshold", 0.5)
                    put("prefix_padding_ms", 300)
                    put("silence_duration_ms", 500)
                })
            })
        }

        val success = webSocket?.send(sessionConfig.toString()) == true
        if (success) {
            Timber.d("Session configuration sent")
        } else {
            Timber.e("Failed to send session configuration")
            emitEvent(RealtimeEvent.Error("Failed to configure session"))
        }
    }

    /**
     * Send audio chunk to the server.
     *
     * @param base64Audio Base64-encoded PCM16 audio data
     */
    fun sendAudioChunk(base64Audio: String) {
        if (!isConnected) {
            Timber.w("Cannot send audio - not connected")
            return
        }

        val audioEvent = JSONObject().apply {
            put("type", "input_audio_buffer.append")
            put("audio", base64Audio)
        }

        val success = webSocket?.send(audioEvent.toString()) == true
        if (!success) {
            Timber.w("Failed to send audio chunk")
        }
    }

    /**
     * Commit the audio buffer (signal end of speech).
     */
    fun commitAudioBuffer() {
        if (!isConnected) {
            Timber.w("Cannot commit audio - not connected")
            return
        }

        val commitEvent = JSONObject().apply {
            put("type", "input_audio_buffer.commit")
        }

        val success = webSocket?.send(commitEvent.toString()) == true
        if (success) {
            Timber.d("Audio buffer committed")
        } else {
            Timber.w("Failed to commit audio buffer")
        }
    }

    /**
     * Clear the audio buffer.
     */
    fun clearAudioBuffer() {
        if (!isConnected) return

        val clearEvent = JSONObject().apply {
            put("type", "input_audio_buffer.clear")
        }

        webSocket?.send(clearEvent.toString())
        Timber.d("Audio buffer cleared")
    }

    /**
     * Handle incoming WebSocket messages.
     */
    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type", "")

            when (type) {
                "transcription_session.created" -> {
                    Timber.d("Transcription session created")
                    isSessionConfigured = true
                    emitEvent(RealtimeEvent.SessionReady)
                }

                "transcription_session.updated" -> {
                    Timber.d("Transcription session updated")
                    isSessionConfigured = true
                    emitEvent(RealtimeEvent.SessionReady)
                }

                "conversation.item.input_audio_transcription.delta" -> {
                    val delta = json.optString("delta", "")
                    if (delta.isNotEmpty()) {
                        Timber.v("Transcription delta: $delta")
                        emitEvent(RealtimeEvent.TranscriptionDelta(delta))
                    }
                }

                "conversation.item.input_audio_transcription.completed" -> {
                    val transcript = json.optString("transcript", "")
                    Timber.d("Transcription completed: $transcript")
                    emitEvent(RealtimeEvent.TranscriptionCompleted(transcript))
                }

                "input_audio_buffer.speech_started" -> {
                    val audioStartMs = json.optLong("audio_start_ms", 0)
                    Timber.d("Speech started at ${audioStartMs}ms")
                    emitEvent(RealtimeEvent.SpeechStarted(audioStartMs))
                }

                "input_audio_buffer.speech_stopped" -> {
                    val audioEndMs = json.optLong("audio_end_ms", 0)
                    Timber.d("Speech stopped at ${audioEndMs}ms")
                    emitEvent(RealtimeEvent.SpeechStopped(audioEndMs))
                }

                "input_audio_buffer.committed" -> {
                    Timber.d("Audio buffer committed by server")
                }

                "input_audio_buffer.cleared" -> {
                    Timber.d("Audio buffer cleared")
                }

                "error" -> {
                    val errorJson = json.optJSONObject("error")
                    val errorType = errorJson?.optString("type", "unknown") ?: "unknown"
                    val errorMessage = errorJson?.optString("message", "Unknown error") ?: "Unknown error"
                    Timber.e("API Error: $errorType - $errorMessage")
                    emitEvent(RealtimeEvent.Error("$errorType: $errorMessage"))
                }

                else -> {
                    Timber.v("Unhandled message type: $type")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse WebSocket message: $text")
        }
    }

    private fun emitEvent(event: RealtimeEvent) {
        scope.launch {
            _events.emit(event)
        }
    }

    /**
     * Check if connected to the API.
     */
    fun isConnected(): Boolean = isConnected

    /**
     * Check if session is configured and ready.
     */
    fun isSessionReady(): Boolean = isSessionConfigured

    /**
     * Events emitted by the Realtime client.
     */
    sealed class RealtimeEvent {
        // Connection events
        object Connected : RealtimeEvent()
        object Disconnected : RealtimeEvent()
        object SessionReady : RealtimeEvent()

        // Transcription events
        data class TranscriptionDelta(val text: String) : RealtimeEvent()
        data class TranscriptionCompleted(val text: String) : RealtimeEvent()

        // VAD events
        data class SpeechStarted(val audioStartMs: Long) : RealtimeEvent()
        data class SpeechStopped(val audioEndMs: Long) : RealtimeEvent()

        // Error events
        data class Error(val message: String) : RealtimeEvent()
    }
}
