package com.quranmedia.player.recite.domain.repository

import com.quranmedia.player.domain.util.Resource
import com.quranmedia.player.recite.domain.model.ReciteResult
import com.quranmedia.player.recite.domain.model.ReciteSelection
import java.io.File

/**
 * Repository interface for Recite feature
 */
interface ReciteRepository {
    /**
     * Get Quranic text for the selected ayah range
     */
    suspend fun getAyahsText(selection: ReciteSelection): Resource<String>

    /**
     * Transcribe audio file using Whisper API
     */
    suspend fun transcribeAudio(audioFile: File): Resource<String>

    /**
     * Match transcription against expected Quran text
     * @param expectedText The normalized Quran text
     * @param transcribedText The transcribed audio text
     * @param selection The recitation selection for ayah numbering
     * @param durationSeconds Total recording duration
     */
    suspend fun matchTranscription(
        expectedText: String,
        transcribedText: String,
        selection: ReciteSelection,
        durationSeconds: Long
    ): Resource<ReciteResult>
}
