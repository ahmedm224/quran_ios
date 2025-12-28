package com.quranmedia.player.data.api

import com.quranmedia.player.data.api.model.QuranApiRecitersResponse
import com.quranmedia.player.data.api.model.QuranApiSurahResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Alfurqan Quran API Service
 * Base URL: https://api.alfurqan.online/api/v1/
 *
 * Features:
 * - Fetch all reciters with metadata and R2 paths
 * - Stream individual ayah audio files
 * - Surah metadata lookup
 */
interface QuranApi {

    /**
     * Get all available reciters
     * Returns reciter metadata including ID, name, Arabic name, and R2 path
     */
    @GET("reciters")
    suspend fun getReciters(): QuranApiRecitersResponse

    /**
     * Get metadata for a specific surah
     * @param surahNumber Surah number (1-114)
     */
    @GET("surahs/{number}")
    suspend fun getSurah(
        @Path("number") surahNumber: Int
    ): QuranApiSurahResponse

    /**
     * Search for reciters
     * @param query Search query (reciter name)
     * @param type Search type (e.g., "reciter")
     */
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String
    ): QuranApiRecitersResponse

    companion object {
        const val BASE_URL = "https://api.alfurqan.online/api/v1/"

        /**
         * Build audio URL for a specific ayah using global ayah numbering
         * Pattern: /audio/{reciter-id}/{globalAyahNumber}
         * Global ayah numbers range from 1-6236
         */
        fun buildAudioUrl(reciterId: String, globalAyahNumber: Int): String {
            return "${BASE_URL}audio/$reciterId/$globalAyahNumber"
        }

        /**
         * Convert surah/ayah to global ayah number (1-6236)
         * Uses cumulative ayah counts to calculate global position
         */
        fun getGlobalAyahNumber(surahNumber: Int, ayahInSurah: Int): Int {
            // Cumulative ayah count for each surah
            val cumulativeAyahs = intArrayOf(
                0, 7, 293, 493, 669, 789, 954, 1160, 1235, 1364, 1473, 1596, 1707, 1750,
                1802, 1901, 2029, 2140, 2250, 2348, 2483, 2595, 2673, 2791, 2855, 2932,
                3159, 3252, 3340, 3409, 3469, 3503, 3533, 3606, 3660, 3705, 3788, 3970,
                4058, 4133, 4218, 4272, 4325, 4414, 4473, 4510, 4545, 4583, 4612, 4630,
                4675, 4735, 4784, 4846, 4901, 4979, 5075, 5104, 5126, 5150, 5163, 5177,
                5188, 5199, 5217, 5229, 5241, 5271, 5323, 5375, 5419, 5447, 5475, 5495,
                5551, 5591, 5622, 5672, 5712, 5758, 5800, 5829, 5848, 5884, 5909, 5931,
                5948, 5967, 5993, 6023, 6043, 6058, 6079, 6090, 6098, 6106, 6125, 6130,
                6138, 6146, 6157, 6168, 6176, 6179, 6188, 6193, 6197, 6204, 6207, 6213,
                6216, 6221, 6225, 6230
            )

            return if (surahNumber in 1..114) {
                cumulativeAyahs[surahNumber - 1] + ayahInSurah
            } else {
                1 // Default to first ayah if invalid
            }
        }
    }
}
