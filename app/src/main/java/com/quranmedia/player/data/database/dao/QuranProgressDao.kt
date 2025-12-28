package com.quranmedia.player.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranmedia.player.data.database.entity.QuranProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranProgressDao {
    @Query("SELECT * FROM quran_progress WHERE date = :date")
    fun getProgressForDate(date: String): Flow<QuranProgressEntity?>

    @Query("SELECT * FROM quran_progress WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getProgressForDateRange(startDate: String, endDate: String): Flow<List<QuranProgressEntity>>

    @Query("SELECT SUM(pagesRead) FROM quran_progress WHERE date BETWEEN :startDate AND :endDate")
    fun getTotalPagesReadInRange(startDate: String, endDate: String): Flow<Int?>

    @Query("SELECT SUM(pagesListened) FROM quran_progress WHERE date BETWEEN :startDate AND :endDate")
    fun getTotalPagesListenedInRange(startDate: String, endDate: String): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: QuranProgressEntity)

    @Query("DELETE FROM quran_progress WHERE date < :threshold")
    suspend fun deleteOldProgress(threshold: String)
}
