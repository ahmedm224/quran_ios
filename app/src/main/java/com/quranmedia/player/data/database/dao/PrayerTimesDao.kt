package com.quranmedia.player.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranmedia.player.data.database.entity.PrayerTimesEntity
import com.quranmedia.player.data.database.entity.UserLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerTimesDao {

    // Prayer times cache operations

    @Query("SELECT * FROM prayer_times_cache WHERE date = :date ORDER BY cachedAt DESC LIMIT 1")
    fun getPrayerTimesForDate(date: String): Flow<PrayerTimesEntity?>

    @Query("SELECT * FROM prayer_times_cache WHERE date = :date AND latitude = :latitude AND longitude = :longitude")
    suspend fun getPrayerTimesForDateAndLocation(
        date: String,
        latitude: Double,
        longitude: Double
    ): PrayerTimesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cachePrayerTimes(prayerTimes: PrayerTimesEntity)

    @Query("DELETE FROM prayer_times_cache WHERE cachedAt < :threshold")
    suspend fun deleteOldCache(threshold: Long)

    @Query("DELETE FROM prayer_times_cache")
    suspend fun clearAllCache()

    // User location operations

    @Query("SELECT * FROM user_location WHERE id = 1")
    fun getSavedLocation(): Flow<UserLocationEntity?>

    @Query("SELECT * FROM user_location WHERE id = 1")
    suspend fun getSavedLocationSync(): UserLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocation(location: UserLocationEntity)

    @Query("DELETE FROM user_location")
    suspend fun clearLocation()
}
