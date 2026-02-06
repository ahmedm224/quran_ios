package com.quranmedia.player.recite.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Audio recorder for recitation using MediaRecorder
 * Records in M4A format compatible with Whisper API
 */
class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false

    /**
     * Start recording audio
     * @return File where audio is being recorded, or null if failed
     */
    fun startRecording(): File? {
        return try {
            // Create output file in cache directory
            val timestamp = System.currentTimeMillis()
            outputFile = File(context.cacheDir, "recite_$timestamp.m4a")

            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                // Set audio source
                setAudioSource(MediaRecorder.AudioSource.MIC)

                // Set output format (M4A container)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

                // Set audio encoder (AAC)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                // Set quality settings
                setAudioEncodingBitRate(128000) // 128 kbps
                setAudioSamplingRate(44100)     // 44.1 kHz

                // Set output file
                setOutputFile(outputFile?.absolutePath)

                // Prepare and start
                prepare()
                start()

                isRecording = true
                Timber.d("Audio recording started: ${outputFile?.absolutePath}")
            }

            outputFile
        } catch (e: IOException) {
            Timber.e(e, "Failed to start audio recording")
            cleanup()
            null
        } catch (e: IllegalStateException) {
            Timber.e(e, "MediaRecorder in illegal state")
            cleanup()
            null
        }
    }

    /**
     * Stop recording and return the recorded file
     * @return Recorded audio file, or null if recording failed
     */
    fun stopRecording(): File? {
        return try {
            if (isRecording && mediaRecorder != null) {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false

                Timber.d("Audio recording stopped: ${outputFile?.absolutePath}")

                // Verify file exists and has content
                if (outputFile?.exists() == true && outputFile?.length() ?: 0 > 0) {
                    outputFile
                } else {
                    Timber.e("Recording file is empty or doesn't exist")
                    null
                }
            } else {
                Timber.w("Attempted to stop recording but not currently recording")
                null
            }
        } catch (e: RuntimeException) {
            Timber.e(e, "Failed to stop audio recording")
            cleanup()
            null
        }
    }

    /**
     * Cancel recording and delete the file
     */
    fun cancelRecording() {
        try {
            if (isRecording) {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
            }

            // Delete the recording file
            outputFile?.let { file ->
                if (file.exists()) {
                    file.delete()
                    Timber.d("Recording cancelled and file deleted")
                }
            }
            outputFile = null
        } catch (e: RuntimeException) {
            Timber.e(e, "Error cancelling recording")
            cleanup()
        }
    }

    /**
     * Check if currently recording
     */
    fun isCurrentlyRecording(): Boolean = isRecording

    /**
     * Get current recording file (if recording)
     */
    fun getCurrentFile(): File? = outputFile

    /**
     * Clean up resources
     */
    private fun cleanup() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Timber.e(e, "Error releasing MediaRecorder")
        }
        mediaRecorder = null
        isRecording = false
        outputFile = null
    }

    /**
     * Delete a recording file
     * @param file The file to delete
     */
    fun deleteRecording(file: File) {
        try {
            if (file.exists()) {
                file.delete()
                Timber.d("Recording file deleted: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete recording file")
        }
    }
}
