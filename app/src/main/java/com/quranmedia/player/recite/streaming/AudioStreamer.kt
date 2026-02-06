package com.quranmedia.player.recite.streaming

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber

/**
 * Audio streamer for real-time recitation using AudioRecord API.
 * Captures raw PCM audio at 24kHz mono (required by OpenAI Realtime API).
 *
 * Audio Format:
 * - Sample Rate: 24000 Hz
 * - Channels: Mono
 * - Encoding: PCM 16-bit signed little-endian
 * - Chunk Size: 100ms = 4800 bytes (2400 samples * 2 bytes)
 */
class AudioStreamer {

    companion object {
        // OpenAI Realtime API requires 24kHz sample rate
        const val SAMPLE_RATE = 24000

        // Mono channel
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO

        // 16-bit PCM
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        // Chunk duration in milliseconds (100ms provides good real-time balance)
        const val CHUNK_DURATION_MS = 100

        // Calculate buffer size for chunk duration
        // 24000 samples/sec * 0.1 sec * 2 bytes/sample = 4800 bytes
        private val CHUNK_SIZE_BYTES = (SAMPLE_RATE * CHUNK_DURATION_MS / 1000) * 2
    }

    private var audioRecord: AudioRecord? = null
    private var streamingJob: Job? = null
    private var isStreaming = false

    // Flow for emitting audio chunks (base64 encoded for OpenAI)
    private val _audioChunks = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val audioChunks: SharedFlow<String> = _audioChunks

    // Flow for emitting raw audio bytes (for Deepgram)
    private val _rawAudioChunks = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val rawAudioChunks: SharedFlow<ByteArray> = _rawAudioChunks

    // Flow for streaming state changes
    private val _streamingState = MutableSharedFlow<StreamingState>(extraBufferCapacity = 8)
    val streamingState: SharedFlow<StreamingState> = _streamingState

    /**
     * Start streaming audio from the microphone.
     * Audio chunks are emitted as base64-encoded PCM data via [audioChunks] flow.
     *
     * @return true if streaming started successfully, false otherwise
     */
    @SuppressLint("MissingPermission")
    fun startStreaming(): Boolean {
        if (isStreaming) {
            Timber.w("Already streaming audio")
            return true
        }

        try {
            // Calculate minimum buffer size
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )

            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Timber.e("Invalid buffer size: $minBufferSize")
                emitState(StreamingState.Error("Audio configuration not supported on this device"))
                return false
            }

            // Use larger buffer to prevent underruns (at least 2x chunk size)
            val bufferSize = maxOf(minBufferSize, CHUNK_SIZE_BYTES * 2)

            // Create AudioRecord
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Timber.e("AudioRecord failed to initialize")
                cleanup()
                emitState(StreamingState.Error("Failed to initialize audio recorder"))
                return false
            }

            // Start recording
            audioRecord?.startRecording()
            isStreaming = true

            Timber.d("Audio streaming started: ${SAMPLE_RATE}Hz, mono, 16-bit PCM")
            emitState(StreamingState.Started)

            // Start streaming coroutine
            streamingJob = CoroutineScope(Dispatchers.IO).launch {
                streamAudioLoop()
            }

            return true
        } catch (e: SecurityException) {
            Timber.e(e, "Microphone permission not granted")
            emitState(StreamingState.Error("Microphone permission required"))
            return false
        } catch (e: Exception) {
            Timber.e(e, "Failed to start audio streaming")
            cleanup()
            emitState(StreamingState.Error("Failed to start audio: ${e.message}"))
            return false
        }
    }

    /**
     * Stop streaming audio.
     */
    fun stopStreaming() {
        if (!isStreaming) {
            Timber.w("Not currently streaming")
            return
        }

        isStreaming = false
        streamingJob?.cancel()
        streamingJob = null

        try {
            audioRecord?.stop()
        } catch (e: IllegalStateException) {
            Timber.w(e, "AudioRecord already stopped")
        }

        cleanup()
        Timber.d("Audio streaming stopped")
        emitState(StreamingState.Stopped)
    }

    /**
     * Check if currently streaming.
     */
    fun isCurrentlyStreaming(): Boolean = isStreaming

    /**
     * Main streaming loop - reads audio and emits base64 chunks.
     */
    private suspend fun streamAudioLoop() {
        val buffer = ByteArray(CHUNK_SIZE_BYTES)

        while (isStreaming && audioRecord != null) {
            try {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1

                when {
                    bytesRead > 0 -> {
                        // Get the actual audio bytes
                        val audioBytes = if (bytesRead == buffer.size) {
                            buffer.copyOf()
                        } else {
                            buffer.copyOf(bytesRead)
                        }

                        // Emit raw bytes for Deepgram
                        _rawAudioChunks.emit(audioBytes)

                        // Convert to base64 for OpenAI JSON transport
                        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

                        // Emit the base64 chunk
                        _audioChunks.emit(base64Audio)
                    }
                    bytesRead == AudioRecord.ERROR_INVALID_OPERATION -> {
                        Timber.e("AudioRecord: Invalid operation")
                        withContext(Dispatchers.Main) {
                            emitState(StreamingState.Error("Audio recording error"))
                        }
                        break
                    }
                    bytesRead == AudioRecord.ERROR_BAD_VALUE -> {
                        Timber.e("AudioRecord: Bad value")
                        withContext(Dispatchers.Main) {
                            emitState(StreamingState.Error("Audio configuration error"))
                        }
                        break
                    }
                    bytesRead == AudioRecord.ERROR -> {
                        Timber.e("AudioRecord: Generic error")
                        withContext(Dispatchers.Main) {
                            emitState(StreamingState.Error("Audio recording failed"))
                        }
                        break
                    }
                }

                // Small delay to prevent tight loop if reads are too fast
                delay(1)
            } catch (e: CancellationException) {
                // Normal cancellation, just exit
                break
            } catch (e: Exception) {
                Timber.e(e, "Error in audio streaming loop")
                withContext(Dispatchers.Main) {
                    emitState(StreamingState.Error("Streaming error: ${e.message}"))
                }
                break
            }
        }

        Timber.d("Audio streaming loop ended")
    }

    /**
     * Clean up resources.
     */
    private fun cleanup() {
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Timber.e(e, "Error releasing AudioRecord")
        }
        audioRecord = null
    }

    private fun emitState(state: StreamingState) {
        _streamingState.tryEmit(state)
    }

    /**
     * Streaming state events.
     */
    sealed class StreamingState {
        object Started : StreamingState()
        object Stopped : StreamingState()
        data class Error(val message: String) : StreamingState()
    }
}
