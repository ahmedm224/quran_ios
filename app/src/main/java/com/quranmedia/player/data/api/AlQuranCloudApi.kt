package com.quranmedia.player.data.api

import com.quranmedia.player.data.api.model.AllSurahsResponse
import com.quranmedia.player.data.api.model.EditionsResponse
import com.quranmedia.player.data.api.model.SurahResponse
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Al-Quran Cloud API Service
 * Base URL: http://api.alquran.cloud/v1/
 * Documentation: https://alquran.cloud/api
 */
interface AlQuranCloudApi {

    /**
     * Get metadata for all 114 Surahs
     */
    @GET("surah")
    suspend fun getAllSurahs(): AllSurahsResponse

    /**
     * Get a specific Surah with all its Ayahs
     * @param surahNumber Surah number (1-114)
     * @param edition Quran edition identifier (default: quran-simple-enhanced)
     */
    @GET("surah/{number}/{edition}")
    suspend fun getSurah(
        @Path("number") surahNumber: Int,
        @Path("edition") edition: String = "quran-simple-enhanced"
    ): SurahResponse

    /**
     * Get a specific Surah with default edition (quran-simple-enhanced)
     * @param surahNumber Surah number (1-114)
     */
    @GET("surah/{number}/quran-simple-enhanced")
    suspend fun getSurahDefault(
        @Path("number") surahNumber: Int
    ): SurahResponse

    /**
     * Get all available audio editions (reciters)
     */
    @GET("edition/format/audio")
    suspend fun getAudioEditions(): EditionsResponse

    /**
     * Search for ayahs containing specific text
     * @param query Search text (Arabic)
     * @param surah Optional surah number to limit search (1-114)
     * @param edition Edition identifier (default: quran-simple-enhanced)
     */
    @GET("search/{query}/{surah}/{edition}")
    suspend fun searchInSurah(
        @Path("query") query: String,
        @Path("surah") surah: Int,
        @Path("edition") edition: String = "quran-simple-enhanced"
    ): com.quranmedia.player.data.api.model.SearchResponse

    /**
     * Search for ayahs containing specific text across all Quran
     * @param query Search text (Arabic)
     * @param edition Edition identifier (default: quran-simple-enhanced)
     */
    @GET("search/{query}/all/{edition}")
    suspend fun searchAll(
        @Path("query") query: String,
        @Path("edition") edition: String = "quran-simple-enhanced"
    ): com.quranmedia.player.data.api.model.SearchResponse

    /**
     * Get the entire Quran with full metadata (page, juz, manzil, ruku, hizbQuarter, sajda)
     * @param edition Edition identifier (default: quran-uthmani)
     * @return Full Quran response with all surahs and ayahs
     */
    @GET("quran/{edition}")
    suspend fun getFullQuran(
        @Path("edition") edition: String = "quran-uthmani"
    ): com.quranmedia.player.data.api.model.FullQuranResponse

    companion object {
        const val BASE_URL = "https://api.alquran.cloud/v1/"

        // Available editions
        const val EDITION_SIMPLE_ENHANCED = "quran-simple-enhanced"
        const val EDITION_UTHMANI = "quran-uthmani"
        const val EDITION_UTHMANI_QURAN_ACADEMY = "quran-uthmani-quran-academy"
        const val EDITION_SIMPLE = "quran-simple"
        const val EDITION_SIMPLE_CLEAN = "quran-simple-clean"
    }
}
