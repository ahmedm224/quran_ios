package com.quranmedia.player.recite.streaming

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okio.ByteString.Companion.toByteString
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * WebSocket client for the Quran Recite server.
 *
 * Uses a Python server with specialized Quran AI models:
 * - Segmenter: obadx/recitation-segmenter-v2 (detects waqf/pause points)
 * - ASR: rabah2026/wav2vec2-large-xlsr-53-arabic-quran (Arabic Quran transcription)
 *
 * Flow:
 * 1. Connect to WebSocket
 * 2. Send start message with surah/verse info
 * 3. Stream audio chunks as binary
 * 4. Send stop message
 * 5. Receive complete results with word-level alignment
 */
class QuranReciteServerClient(
    private val serverUrl: String
) : TranscriptionProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isConnected = false

    // Session info
    private var currentSurahId: Int = 1
    private var currentVerseStart: Int = 1
    private var currentVerseEnd: Int = 1

    // Event flow for UI updates
    private val _events = MutableSharedFlow<TranscriptionProvider.TranscriptionEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )
    override val events: SharedFlow<TranscriptionProvider.TranscriptionEvent> = _events

    // Results flow for complete recitation results
    private val _recitationResult = MutableStateFlow<RecitationResult?>(null)
    val recitationResult: StateFlow<RecitationResult?> = _recitationResult

    // Coroutine scope for internal operations
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Configure the session with surah and verse range.
     * Must be called before start().
     */
    fun configureSession(surahId: Int, verseStart: Int, verseEnd: Int) {
        currentSurahId = surahId
        currentVerseStart = verseStart
        currentVerseEnd = verseEnd
        Timber.d("Session configured: Surah $surahId, verses $verseStart-$verseEnd")
    }

    override fun start(expectedText: String) {
        // Close any existing connection first
        if (isConnected || webSocket != null) {
            Timber.d("Closing existing connection before starting new one")
            webSocket?.close(1000, "Starting new session")
            webSocket = null
            isConnected = false
        }

        // Reset result state
        _recitationResult.value = null

        val wsUrl = "$serverUrl/ws/transcribe"
        Timber.d("Connecting to Quran server: $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("WebSocket connected to Quran server")
                isConnected = true

                // Send start message with session info
                val startMessage = StartMessage(
                    action = "start",
                    surahId = currentSurahId,
                    verseStart = currentVerseStart,
                    verseEnd = currentVerseEnd
                )

                val messageJson = json.encodeToString(startMessage)
                webSocket.send(messageJson)
                Timber.d("Sent start message: $messageJson")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Timber.d("Received message: ${text.take(200)}...")
                handleServerMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "WebSocket failure")
                isConnected = false
                this@QuranReciteServerClient.webSocket = null
                scope.launch {
                    _events.emit(TranscriptionProvider.TranscriptionEvent.Error(
                        "Connection failed: ${t.message}"
                    ))
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket closed: $code - $reason")
                isConnected = false
                this@QuranReciteServerClient.webSocket = null
            }
        })
    }

    override fun stop() {
        if (!isConnected) {
            Timber.w("Not connected, ignoring stop()")
            return
        }

        // Send stop message to signal end of audio
        val stopMessage = """{"action":"stop"}"""
        webSocket?.send(stopMessage)
        Timber.d("Sent stop message")
    }

    override fun addAudio(audioData: ByteArray) {
        if (!isConnected) {
            return
        }

        // Send raw audio bytes as binary
        webSocket?.send(audioData.toByteString())
    }

    override fun updateExpectedText(newExpectedText: String) {
        // Not used - server already has the expected text
    }

    /**
     * Close the connection and cleanup resources.
     */
    fun close() {
        webSocket?.close(1000, "Client closing")
        webSocket = null
        isConnected = false
        scope.cancel()
    }

    private fun handleServerMessage(text: String) {
        scope.launch {
            try {
                // Parse as generic response first to check status
                val response = json.decodeFromString<ServerResponse>(text)

                when (response.status) {
                    "ready" -> {
                        Timber.d("Server ready, expected: ${response.expectedText?.take(50)}...")
                        _events.emit(TranscriptionProvider.TranscriptionEvent.Ready)
                    }

                    "processing" -> {
                        // Server is processing audio
                        Timber.d("Server processing audio...")
                    }

                    "complete" -> {
                        // Parse full result
                        val result = json.decodeFromString<RecitationResult>(text)
                        _recitationResult.value = result

                        Timber.d("Recitation complete: ${result.accuracy}% accuracy, passed=${result.passed}")
                        Timber.d("Transcribed: ${result.transcribedText}")

                        // Emit transcription event with Arabic text
                        _events.emit(TranscriptionProvider.TranscriptionEvent.Transcription(
                            result.transcribedText
                        ))

                        // Emit completed event
                        _events.emit(TranscriptionProvider.TranscriptionEvent.Completed(
                            result.transcribedText
                        ))
                    }

                    "error" -> {
                        val errorMsg = response.error ?: "Unknown server error"
                        Timber.e("Server error: $errorMsg")
                        _events.emit(TranscriptionProvider.TranscriptionEvent.Error(errorMsg))
                    }

                    else -> {
                        Timber.w("Unknown status: ${response.status}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse server message")
                _events.emit(TranscriptionProvider.TranscriptionEvent.Error(
                    "Failed to parse response: ${e.message}"
                ))
            }
        }
    }
}

// Request messages

@Serializable
data class StartMessage(
    val action: String,
    @SerialName("surah_id") val surahId: Int,
    @SerialName("verse_start") val verseStart: Int,
    @SerialName("verse_end") val verseEnd: Int
)

// Response models

@Serializable
data class ServerResponse(
    val status: String,
    @SerialName("expected_text") val expectedText: String? = null,
    val error: String? = null
)

@Serializable
data class RecitationResult(
    val status: String = "",
    @SerialName("expected_text") val expectedText: String = "",
    @SerialName("transcribed_text") val transcribedText: String = "",
    val accuracy: Float = 0f,
    val passed: Boolean = false,
    @SerialName("word_matches") val wordMatches: List<WordMatch> = emptyList(),
    val segments: List<RecitationSegment> = emptyList(),
    @SerialName("words_recited") val wordsRecited: Int = 0,
    @SerialName("words_expected") val wordsExpected: Int = 0
)

@Serializable
data class WordMatch(
    val expected: String = "",
    val transcribed: String = "",
    val type: String = "",  // "correct", "wrong", "skipped", "extra"
    @SerialName("expected_index") val expectedIndex: Int = -1,
    @SerialName("transcribed_index") val transcribedIndex: Int = -1,
    val similarity: Float = 0f
)

@Serializable
data class RecitationSegment(
    val index: Int = 0,
    @SerialName("start_time") val startTime: Float = 0f,
    @SerialName("end_time") val endTime: Float = 0f,
    val expected: String = "",
    val transcribed: String = "",
    val similarity: Float = 0f,
    @SerialName("is_correct") val isCorrect: Boolean = false
)
