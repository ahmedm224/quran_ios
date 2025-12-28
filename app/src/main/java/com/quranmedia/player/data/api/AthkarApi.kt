package com.quranmedia.player.data.api

import com.quranmedia.player.data.api.model.AthkarApiResponse
import retrofit2.http.GET

/**
 * HisnMuslim API for Athkar
 * API provides Islamic remembrances and supplications
 */
interface AthkarApi {

    /**
     * Get all athkar in Arabic
     */
    @GET("ar.json")
    suspend fun getAllAthkar(): AthkarApiResponse

    companion object {
        const val BASE_URL = "https://hisnmuslim.com/api/"
    }
}
