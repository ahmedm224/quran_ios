package com.quranmedia.player.recite.data.repository

import com.quranmedia.player.data.database.dao.AyahDao
import com.quranmedia.player.domain.util.Resource
import com.quranmedia.player.recite.data.api.WhisperApi
import com.quranmedia.player.recite.domain.model.ReciteResult
import com.quranmedia.player.recite.domain.model.ReciteSelection
import com.quranmedia.player.recite.domain.repository.ReciteRepository
import com.quranmedia.player.recite.util.ArabicNormalizer
import com.quranmedia.player.recite.util.SimpleQuranText
import com.quranmedia.player.recite.util.WordMatcher
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ReciteRepository
 */
@Singleton
class ReciteRepositoryImpl @Inject constructor(
    private val ayahDao: AyahDao,
    private val whisperApi: WhisperApi
) : ReciteRepository {

    companion object {
        /**
         * Set to true to use Tanzil Simple text for comparison (recommended for AI).
         * Set to false to use normalized database text (original approach).
         */
        var USE_TANZIL_SIMPLE = true
    }

    /**
     * Get Quranic text for the selected ayah range
     * Uses Tanzil Simple text if available, falls back to normalized database text
     */
    override suspend fun getAyahsText(selection: ReciteSelection): Resource<String> {
        return try {
            // Try Tanzil Simple text first (better for AI comparison)
            if (USE_TANZIL_SIMPLE && SimpleQuranText.isLoaded()) {
                val simpleText = SimpleQuranText.getAyahRangeNormalized(
                    surahNumber = selection.surahNumber,
                    startAyah = selection.startAyah,
                    endAyah = selection.endAyah
                )

                if (simpleText != null) {
                    Timber.d("Using Tanzil Simple text for comparison, length: ${simpleText.length}")
                    return Resource.Success(simpleText)
                }

                Timber.w("Tanzil Simple text not available, falling back to database")
            }

            // Fallback: Get ayahs from database
            val ayahs = ayahDao.getAyahRange(
                surahNumber = selection.surahNumber,
                startAyah = selection.startAyah,
                endAyah = selection.endAyah
            )

            if (ayahs.isEmpty()) {
                return Resource.Error("No ayahs found for the selected range")
            }

            // Combine all ayah texts
            val fullText = ayahs.joinToString(separator = " ") { it.textArabic }

            // Normalize the text
            val normalizedText = ArabicNormalizer.normalize(fullText)

            Timber.d("Using database text (normalized), length: ${normalizedText.length}")

            Resource.Success(normalizedText)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get ayahs text")
            Resource.Error("Failed to retrieve Quran text: ${e.message}")
        }
    }

    /**
     * Transcribe audio file using Whisper API
     */
    override suspend fun transcribeAudio(audioFile: File): Resource<String> {
        return try {
            if (!audioFile.exists() || audioFile.length() == 0L) {
                return Resource.Error("Audio file is empty or doesn't exist")
            }

            Timber.d("Transcribing audio file: ${audioFile.absolutePath}, size: ${audioFile.length()} bytes")

            // Prepare multipart request
            val requestFile = audioFile.asRequestBody("audio/mp4".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
            val modelPart = WhisperApi.MODEL.toRequestBody()
            val languagePart = WhisperApi.LANGUAGE.toRequestBody()
            val formatPart = WhisperApi.RESPONSE_FORMAT.toRequestBody()
            val promptPart = WhisperApi.QURAN_PROMPT.trimIndent().toRequestBody()

            // Call gpt-4o-transcribe API
            val response = whisperApi.transcribeAudio(
                file = filePart,
                model = modelPart,
                language = languagePart,
                responseFormat = formatPart,
                prompt = promptPart
            )

            // Normalize the transcribed text
            val normalizedText = ArabicNormalizer.normalize(response.text)

            Timber.d("Transcription successful, normalized text: $normalizedText")

            Resource.Success(normalizedText)
        } catch (e: Exception) {
            Timber.e(e, "Failed to transcribe audio")
            Resource.Error("Failed to transcribe audio: ${e.message}")
        }
    }

    /**
     * Match transcription against expected Quran text
     */
    override suspend fun matchTranscription(
        expectedText: String,
        transcribedText: String,
        selection: ReciteSelection,
        durationSeconds: Long
    ): Resource<ReciteResult> {
        return try {
            // Split texts into words
            val expectedWords = ArabicNormalizer.normalizeAndSplit(expectedText)
            val transcribedWords = ArabicNormalizer.normalizeAndSplit(transcribedText)

            Timber.d("Expected words: ${expectedWords.size}, Transcribed words: ${transcribedWords.size}")

            // Match words
            val mismatches = WordMatcher.matchWords(
                expectedWords = expectedWords,
                transcribedWords = transcribedWords,
                selection = selection
            )

            // Calculate accuracy
            val accuracy = WordMatcher.calculateAccuracy(
                totalWords = expectedWords.size,
                mismatches = mismatches.size
            )

            Timber.d("Matching complete: ${mismatches.size} mismatches, accuracy: $accuracy%")

            val result = ReciteResult(
                selection = selection,
                accuracyPercentage = accuracy,
                mismatches = mismatches,
                durationSeconds = durationSeconds,
                expectedText = expectedText,
                transcribedText = transcribedText
            )

            Resource.Success(result)
        } catch (e: Exception) {
            Timber.e(e, "Failed to match transcription")
            Resource.Error("Failed to match transcription: ${e.message}")
        }
    }
}
