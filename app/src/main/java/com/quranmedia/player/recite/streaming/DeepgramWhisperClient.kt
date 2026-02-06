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
 * Client for Deepgram's hosted Whisper API for Arabic transcription.
 *
 * Uses chunked audio approach - collects audio for a duration then sends for transcription.
 * Whisper model supports Arabic language well.
 *
 * API Reference: https://developers.deepgram.com/docs/deepgram-whisper-cloud
 */
class DeepgramWhisperClient(private val apiKey: String) : TranscriptionProvider {

    companion object {
        // Deepgram API endpoint
        private const val API_URL = "https://api.deepgram.com/v1/listen"

        // Whisper model - large for best accuracy
        private const val MODEL = "whisper-large"

        // Language for Arabic
        private const val LANGUAGE = "ar"

        // Audio chunk duration in milliseconds (how much audio to collect before sending)
        private const val CHUNK_DURATION_MS = 3000L

        // Sample rate (must match AudioStreamer)
        private const val SAMPLE_RATE = 24000

        // Connection timeouts
        private const val CONNECT_TIMEOUT_SECONDS = 30L
        private const val READ_TIMEOUT_SECONDS = 60L
        private const val WRITE_TIMEOUT_SECONDS = 30L

        // Silence detection threshold (RMS value below which audio is considered silent)
        // Based on testing: RMS 50-100 is silence, 200+ is speech
        private const val SILENCE_THRESHOLD = 100

        // Common Whisper hallucination phrases to filter out
        private val HALLUCINATION_PATTERNS = listOf(
            "ترجمة",           // "translation"
            "شكرا",            // "thanks"
            "المشاهدة",        // "watching"
            "الاشتراك",        // "subscribe"
            "نانسي",           // random names
            "قنقر",
            "السلام عليكم",    // greetings (when not expected)
            "سلام عليكم",      // greeting variant
            "عليكم السلام",    // greeting response
            "ورحمة الله",      // "and God's mercy"
            "وبركاته",         // "and his blessings"
            "مرحبا",
            "أهلا",
            "تابعونا",         // "follow us"
            "لا تنسى",         // "don't forget"
            "اللايك",          // "like"
            "كومنت",           // "comment"
            "سبسكرايب"         // "subscribe" (transliteration)
        )
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

    // Expected text for potential prompt conditioning
    private var expectedText: String = ""

    /**
     * Start the transcription session.
     *
     * @param expectedText The expected Quran text (for prompt conditioning)
     */
    override fun start(expectedText: String) {
        if (isActive) {
            Timber.w("DeepgramWhisperClient already active")
            return
        }

        this.expectedText = expectedText
        isActive = true
        audioBuffer.reset()
        bufferStartTime = System.currentTimeMillis()

        Timber.d("DeepgramWhisperClient started")
        Timber.d("Expected text: $expectedText")

        emitEvent(TranscriptionProvider.TranscriptionEvent.Ready)
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
        Timber.d("DeepgramWhisperClient stopped")
    }

    /**
     * Update the expected text.
     *
     * NOTE: We no longer use the expected text as a prompt to the model.
     * This was causing hallucination/auto-completion of Quran verses.
     * The expected text is still tracked internally for other purposes.
     *
     * @param newExpectedText The remaining expected text from current position
     */
    override fun updateExpectedText(newExpectedText: String) {
        this.expectedText = newExpectedText
        Timber.d("Updated expected text (not sent to model): ${newExpectedText.take(50)}...")
    }

    /**
     * Add audio data (PCM16 bytes).
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

        Timber.d("Processing audio chunk (RMS: $rms)")

        // Convert PCM to WAV format for Deepgram
        val wavData = pcmToWav(audioData, SAMPLE_RATE, 1, 16)

        scope.launch {
            transcribeAudio(wavData, isComplete)
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
     * Send audio to Deepgram Whisper API for transcription.
     */
    private suspend fun transcribeAudio(wavData: ByteArray, isComplete: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                // Build URL with parameters
                val urlBuilder = HttpUrl.Builder()
                    .scheme("https")
                    .host("api.deepgram.com")
                    .addPathSegment("v1")
                    .addPathSegment("listen")
                    .addQueryParameter("model", MODEL)
                    .addQueryParameter("language", LANGUAGE)
                    .addQueryParameter("punctuate", "false")
                    .addQueryParameter("smart_format", "false")

                // No prompt - avoids hallucination and prompt echo issues
                // Using expected Quran text causes auto-completion of verses
                // Using Arabic prompts can cause the prompt to be echoed back
                Timber.d("Configured without prompt to avoid hallucination/echo")

                val url = urlBuilder.build()

                val requestBody = wavData.toRequestBody("audio/wav".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Token $apiKey")
                    .post(requestBody)
                    .build()

                Timber.d("Sending ${wavData.size} bytes to Deepgram Whisper API")

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: "Unknown error"
                        Timber.e("Deepgram API error: ${response.code} - $errorBody")
                        emitEvent(TranscriptionProvider.TranscriptionEvent.Error("API error: ${response.code}"))
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
     * Parse the Deepgram API response.
     */
    private fun parseTranscriptionResponse(responseBody: String, isComplete: Boolean) {
        try {
            val json = JSONObject(responseBody)
            val results = json.optJSONObject("results") ?: return
            val channels = results.optJSONArray("channels") ?: return

            if (channels.length() == 0) return

            val channel = channels.getJSONObject(0)
            val alternatives = channel.optJSONArray("alternatives") ?: return

            if (alternatives.length() == 0) return

            val alternative = alternatives.getJSONObject(0)
            val transcript = alternative.optString("transcript", "")

            if (transcript.isNotBlank()) {
                // Check for hallucination patterns
                if (isHallucination(transcript)) {
                    Timber.d("Filtered hallucination: $transcript")
                    if (isComplete) {
                        emitEvent(TranscriptionProvider.TranscriptionEvent.Completed(""))
                    }
                    return
                }

                Timber.d("Deepgram transcription: $transcript")
                emitEvent(TranscriptionProvider.TranscriptionEvent.Transcription(transcript))
            }

            if (isComplete) {
                emitEvent(TranscriptionProvider.TranscriptionEvent.Completed(transcript))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse Deepgram response: $responseBody")
        }
    }

    /**
     * Check if transcription is a common Whisper hallucination.
     */
    private fun isHallucination(transcript: String): Boolean {
        val normalized = transcript.trim()

        // Check if the entire transcript matches a hallucination pattern
        for (pattern in HALLUCINATION_PATTERNS) {
            if (normalized.contains(pattern)) {
                // If transcript is mostly hallucination pattern, filter it
                // Allow if there's substantial other content
                val patternRatio = pattern.length.toFloat() / normalized.length
                if (patternRatio > 0.3f) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Convert PCM audio to WAV format.
     */
    private fun pcmToWav(pcmData: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcmData.size
        val fileSize = 36 + dataSize

        val output = ByteArrayOutputStream()

        // RIFF header
        output.write("RIFF".toByteArray())
        output.write(intToLittleEndian(fileSize))
        output.write("WAVE".toByteArray())

        // fmt subchunk
        output.write("fmt ".toByteArray())
        output.write(intToLittleEndian(16)) // Subchunk1Size for PCM
        output.write(shortToLittleEndian(1)) // AudioFormat (1 = PCM)
        output.write(shortToLittleEndian(channels.toShort()))
        output.write(intToLittleEndian(sampleRate))
        output.write(intToLittleEndian(byteRate))
        output.write(shortToLittleEndian(blockAlign.toShort()))
        output.write(shortToLittleEndian(bitsPerSample.toShort()))

        // data subchunk
        output.write("data".toByteArray())
        output.write(intToLittleEndian(dataSize))
        output.write(pcmData)

        return output.toByteArray()
    }

    private fun intToLittleEndian(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun shortToLittleEndian(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }

    private fun emitEvent(event: TranscriptionProvider.TranscriptionEvent) {
        scope.launch {
            _events.emit(event)
        }
    }
}
