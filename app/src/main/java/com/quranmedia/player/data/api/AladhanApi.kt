package com.quranmedia.player.data.api

import com.quranmedia.player.data.api.model.PrayerTimesApiResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Aladhan Prayer Times API
 * Documentation: https://aladhan.com/prayer-times-api
 */
interface AladhanApi {

    /**
     * Get prayer times for a specific date and location
     * @param date Date in DD-MM-YYYY format
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @param method Calculation method ID (1-16)
     * @param school Asr juristic method: 0 = Shafi (standard), 1 = Hanafi
     * @param adjustment Adjustment in days for Hijri date
     */
    @GET("v1/timings/{date}")
    suspend fun getPrayerTimes(
        @Path("date") date: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int,
        @Query("school") school: Int = 0,
        @Query("adjustment") adjustment: Int = 0
    ): PrayerTimesApiResponse

    /**
     * Get prayer times for today
     */
    @GET("v1/timings")
    suspend fun getPrayerTimesToday(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int,
        @Query("school") school: Int = 0
    ): PrayerTimesApiResponse

    companion object {
        const val BASE_URL = "https://api.aladhan.com/"
    }
}
