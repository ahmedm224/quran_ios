package com.quranmedia.player.data.api

import com.quranmedia.player.data.api.model.AthanListResponse
import com.quranmedia.player.data.api.model.MuezzinListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import okhttp3.ResponseBody

/**
 * Al Furqan Athan API
 * Documentation: https://alfurqan.online/docs
 *
 * Provides access to 32 athan (call to prayer) recordings from 29 muezzins.
 */
interface AthanApi {

    /**
     * Get list of all muezzins
     */
    @GET("api/v1/athan/muezzins")
    suspend fun getMuezzins(): MuezzinListResponse

    /**
     * Get list of all athans
     * @param muezzin Optional filter by muezzin name
     * @param location Optional filter by location
     */
    @GET("api/v1/athan/list")
    suspend fun getAthans(
        @Query("muezzin") muezzin: String? = null,
        @Query("location") location: String? = null
    ): AthanListResponse

    /**
     * Stream athan audio by ID
     * Returns audio file directly
     */
    @Streaming
    @GET("api/v1/athan/{id}")
    suspend fun getAthanAudio(@Path("id") id: String): ResponseBody

    companion object {
        const val BASE_URL = "https://api.alfurqan.online/"

        /**
         * Get the full audio URL for an athan
         */
        fun getAthanAudioUrl(athanId: String): String {
            return "${BASE_URL}api/v1/athan/$athanId"
        }
    }
}
