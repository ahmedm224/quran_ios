package com.quranmedia.player.domain.repository

import com.quranmedia.player.domain.model.AsrJuristicMethod
import com.quranmedia.player.domain.model.CalculationMethod
import com.quranmedia.player.domain.model.PrayerTimes
import com.quranmedia.player.domain.util.Resource
import com.quranmedia.player.domain.model.UserLocation
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository interface for Prayer Times data
 */
interface PrayerTimesRepository {
    /**
     * Get prayer times for a specific location and date
     */
    suspend fun getPrayerTimes(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        method: CalculationMethod,
        asrMethod: AsrJuristicMethod = AsrJuristicMethod.SHAFI
    ): Resource<PrayerTimes>

    /**
     * Get cached prayer times for a date (if available)
     */
    fun getCachedPrayerTimes(date: LocalDate): Flow<PrayerTimes?>

    /**
     * Save user's preferred location
     */
    suspend fun saveLocation(location: UserLocation)

    /**
     * Get saved user location
     */
    fun getSavedLocation(): Flow<UserLocation?>

    /**
     * Get saved user location synchronously (for background receivers)
     */
    suspend fun getSavedLocationSync(): UserLocation?

    /**
     * Get saved calculation method
     */
    suspend fun getSavedCalculationMethod(): CalculationMethod

    /**
     * Save calculation method preference
     */
    suspend fun saveCalculationMethod(method: CalculationMethod)

    /**
     * Clear old cached prayer times
     */
    suspend fun clearOldCache(daysToKeep: Int = 7)
}
