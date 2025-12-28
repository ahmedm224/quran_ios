package com.quranmedia.player.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranmedia.player.data.database.entity.DailyActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyActivityDao {
    @Query("SELECT * FROM daily_activities WHERE date = :date")
    fun getActivitiesForDate(date: String): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activities WHERE date BETWEEN :startDate AND :endDate")
    fun getActivitiesForDateRange(startDate: String, endDate: String): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activities WHERE date = :date AND activityType = :type")
    suspend fun getActivity(date: String, type: String): DailyActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: DailyActivityEntity)

    @Query("DELETE FROM daily_activities WHERE date < :threshold")
    suspend fun deleteOldActivities(threshold: String)
}
