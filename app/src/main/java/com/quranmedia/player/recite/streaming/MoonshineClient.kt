package com.quranmedia.player.recite.streaming

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Client for local Moonshine Tiny AR model server for Arabic transcription.
 *
 * Uses chunked audio approach - collects audio for a duration then sends to
 * local Python server for transcription.
 *
 * Moonshine Tiny AR (27M params) provides high accuracy Arabic transcription
 * without hallucination issues common in prompted models.
 *
 * Server: moonshine_server.py running on local network
 */
class MoonshineClient(private val serverUrl: String) : TranscriptionProvider {

    companion object {
        // Default server URL (local development)
        const val DEFAULT_SERVER_URL = "http://192.168.1.100:5000"

        // Audio chunk duration in milliseconds (how much audio to collect before sending)
        // Longer chunks give Moonshine more context for better word boundaries
        private const val CHUNK_DURATION_MS = 7000L

        // Sample rate (must match AudioStreamer - 24kHz)
        private const val SAMPLE_RATE = 24000

        // Connection timeouts
        private const val CONNECT_TIMEOUT_SECONDS = 10L
        private const val READ_TIMEOUT_SECONDS = 30L
        private const val WRITE_TIMEOUT_SECONDS = 30L

        // Silence detection threshold (RMS value below which audio is considered silent)
        private const val SILENCE_THRESHOLD = 100
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)

    // Audio buffer for collecting chunks
    private val audioBuffer = ByteArrayOutputStream()
    private var bufferStartTime = 0L
    private var isActive = false

    // Event flows
    private val _events = MutableSharedFlow<TranscriptionProvider.TranscriptionEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<TranscriptionProvider.TranscriptionEvent> = _events

    // Expected text (not used for Moonshine - no prompt needed)
    private var expectedText: String = ""

    /**
     * Start the transcription session.
     *
     * @param expectedText The expected Quran text (not used - Moonshine doesn't need prompting)
     */
    override fun start(expectedText: String) {
        if (isActive) {
            Timber.w("MoonshineClient already active")
            return
        }

        this.expectedText = expectedText
        isActive = true
        audioBuffer.reset()
        bufferStartTime = System.currentTimeMillis()

        Timber.d("MoonshineClient started (server: $serverUrl)")

        // Check server health before marking ready
        scope.launch {
            if (checkServerHealth()) {
                emitEvent(TranscriptionProvider.TranscriptionEvent.Ready)
            } else {
                emitEvent(TranscriptionProvider.TranscriptionEvent.Error("Moonshine server not available"))
            }
        }
    }

    /**
     * Stop the transcription session.
     */
    override fun stop() {
        if (!isActive) return

        isActive = false

        // Process any remaining audio
        if (audioBuffer.size() > 0) {
            processAudioBuffer(isComplete = true)
        }

        audioBuffer.reset()
        Timber.d("MoonshineClient stopped")
    }

    /**
     * Update the expected text.
     *
     * NOTE: Moonshine doesn't use prompts, so this is a no-op.
     * The text is stored for debugging purposes only.
     *
     * @param newExpectedText The remaining expected text from current position
     */
    override fun updateExpectedText(newExpectedText: String) {
        this.expectedText = newExpectedText
        Timber.d("Expected text updated (not used by Moonshine): ${newExpectedText.take(50)}...")
    }

    /**
     * Add audio data (PCM16 bytes at 24kHz mono).
     *
     * @param audioData Raw PCM16 audio bytes
     */
    override fun addAudio(audioData: ByteArray) {
        if (!isActive) return

        synchronized(audioBuffer) {
            audioBuffer.write(audioData)
        }

        // Check if we've collected enough audio
        val elapsed = System.currentTimeMillis() - bufferStartTime
        if (elapsed >= CHUNK_DURATION_MS) {
            processAudioBuffer(isComplete = false)
        }
    }

    /**
     * Check if Moonshine server is available.
     */
    private suspend fun checkServerHealth(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$serverUrl/health")
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val ready = json.optBoolean("ready", false)
                        Timber.d("Moonshine server health: $body")
                        ready
                    } else {
                        Timber.w("Moonshine server health check failed: ${response.code}")
                        false
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Moonshine server health check error")
                false
            }
        }
    }

    /**
     * Process the accumulated audio buffer.
     */
    private fun processAudioBuffer(isComplete: Boolean) {
        val audioData: ByteArray
        synchronized(audioBuffer) {
            audioData = audioBuffer.toByteArray()
            audioBuffer.reset()
            bufferStartTime = System.currentTimeMillis()
        }

        if (audioData.isEmpty()) {
            if (isComplete) {
                emitEvent(TranscriptionProvider.TranscriptionEvent.Completed(""))
            }
            return
        }

        // Check audio level - skip if too quiet (likely silence)
        val rms = calculateRMS(audioData)
        if (rms < SILENCE_THRESHOLD) {
            Timber.d("Skipping silent audio chunk (RMS: $rms)")
            if (isComplete) {
                emitEvent(TranscriptionProvider.TranscriptionEvent.Completed(""))
            }
            return
        }

        Timber.d("Processing audio chunk: ${audioData.size} bytes (RMS: $rms)")

        scope.launch {
            transcribeAudio(audioData, isComplete)
        }
    }

    /**
     * Calculate RMS (Root Mean Square) of PCM16 audio data.
     * Used to detect silence.
     */
    private fun calculateRMS(pcmData: ByteArray): Int {
        if (pcmData.size < 2) return 0

        var sumSquares = 0L
        val sampleCount = pcmData.size / 2

        for (i in 0 until pcmData.size - 1 step 2) {
            // Convert two bytes to 16-bit signed sample (little-endian)
            val sample = (pcmData[i].toInt() and 0xFF) or (pcmData[i + 1].toInt() shl 8)
            val signedSample = if (sample > 32767) sample - 65536 else sample
            sumSquares += signedSample.toLong() * signedSample.toLong()
        }

        return kotlin.math.sqrt(sumSquares.toDouble() / sampleCount).toInt()
    }

    /**
     * Send audio to Moonshine server for transcription.
     */
    private suspend fun transcribeAudio(audioData: ByteArray, isComplete: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                val requestBody = audioData.toRequestBody("application/octet-stream".toMediaType())

                val request = Request.Builder()
                    .url("$serverUrl/transcribe")
                    .addHeader("X-Sample-Rate", SAMPLE_RATE.toString())
                    .post(requestBody)
                    .build()

                Timber.d("Sending ${audioData.size} bytes to Moonshine server")

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: "Unknown error"
                        Timber.e("Moonshine API error: ${response.code} - $errorBody")
                        emitEvent(TranscriptionProvider.TranscriptionEvent.Error("Server error: ${response.code}"))
                        return@withContext
                    }

                    val responseBody = response.body?.string() ?: return@withContext
                    parseTranscriptionResponse(responseBody, isComplete)
                }
            } catch (e: IOException) {
                Timber.e(e, "Network error during transcription")
                emitEvent(TranscriptionProvider.TranscriptionEvent.Error("Network error: ${e.message}"))
            } catch (e: Exception) {
                Timber.e(e, "Error during transcription")
                emitEvent(TranscriptionProvider.TranscriptionEvent.Error("Error: ${e.message}"))
            }
        }
    }

    /**
     * Parse the Moonshine server response.
     */
    private fun parseTranscriptionResponse(responseBody: String, isComplete: Boolean) {
        try {
            val json = JSONObject(responseBody)
            val success = json.optBoolean("success", false)

            if (!success) {
                val error = json.optString("error", "Unknown error")
                Timber.e("Moonshine transcription failed: $error")
                emitEvent(TranscriptionProvider.TranscriptionEvent.Error(error))
                return
            }

            val transcript = json.optString("transcript", "")

            if (transcript.isNotBlank()) {
                Timber.d("Moonshine transcription: $transcript")
                emitEvent(TranscriptionProvider.TranscriptionEvent.Transcription(transcript))
            }

            if (isComplete) {
                emitEvent(TranscriptionProvider.TranscriptionEvent.Completed(transcript))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse Moonshine response: $responseBody")
        }
    }

    private fun emitEvent(event: TranscriptionProvider.TranscriptionEvent) {
        scope.launch {
            _events.emit(event)
        }
    }
}
