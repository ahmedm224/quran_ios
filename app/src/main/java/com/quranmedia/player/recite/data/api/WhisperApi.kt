package com.quranmedia.player.recite.data.api

import com.quranmedia.player.recite.data.api.model.WhisperResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Retrofit interface for OpenAI Audio Transcription API
 * Uses gpt-4o-transcribe for better Arabic/Quran accuracy
 */
interface WhisperApi {

    /**
     * Transcribe audio file to text
     * @param file Audio file (M4A format)
     * @param model Model name ("gpt-4o-transcribe")
     * @param language Language code ("ar" for Arabic)
     * @param responseFormat Response format ("json")
     * @param prompt System prompt to guide transcription
     */
    @Multipart
    @POST("v1/audio/transcriptions")
    suspend fun transcribeAudio(
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("language") language: RequestBody,
        @Part("response_format") responseFormat: RequestBody,
        @Part("prompt") prompt: RequestBody
    ): WhisperResponse

    companion object {
        const val BASE_URL = "https://api.openai.com/"
        const val MODEL = "whisper-1"
        const val LANGUAGE = "ar"
        const val RESPONSE_FORMAT = "json"

        // System prompt for Quran recitation with Tashkeel
        const val QURAN_PROMPT = """
            The user is reciting verses from the Holy Quran in Arabic.
            Expect classical Arabic pronunciation with full Tashkeel (diacritical marks):
            - Fatha (فتحة), Damma (ضمة), Kasra (كسرة)
            - Sukun (سكون), Shadda (شدة), Tanween
            - Madd (elongation), Ghunna (nasalization)
            Transcribe accurately preserving the Tajweed recitation style.
            Output Arabic text only.
        """
    }
}
