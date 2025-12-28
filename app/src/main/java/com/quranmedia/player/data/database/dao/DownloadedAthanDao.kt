package com.quranmedia.player.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranmedia.player.data.database.entity.DownloadedAthanEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing downloaded Athan files
 */
@Dao
interface DownloadedAthanDao {

    /**
     * Get all downloaded athans
     */
    @Query("SELECT * FROM downloaded_athans ORDER BY muezzin ASC")
    fun getAllDownloadedAthans(): Flow<List<DownloadedAthanEntity>>

    /**
     * Get a specific downloaded athan by ID
     */
    @Query("SELECT * FROM downloaded_athans WHERE id = :athanId")
    suspend fun getDownloadedAthan(athanId: String): DownloadedAthanEntity?

    /**
     * Check if an athan is downloaded
     */
    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_athans WHERE id = :athanId)")
    suspend fun isAthanDownloaded(athanId: String): Boolean

    /**
     * Get the local file path for a downloaded athan
     */
    @Query("SELECT localPath FROM downloaded_athans WHERE id = :athanId")
    suspend fun getAthanLocalPath(athanId: String): String?

    /**
     * Insert or replace a downloaded athan
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedAthan(athan: DownloadedAthanEntity)

    /**
     * Delete a downloaded athan record
     */
    @Query("DELETE FROM downloaded_athans WHERE id = :athanId")
    suspend fun deleteDownloadedAthan(athanId: String)

    /**
     * Delete all downloaded athans
     */
    @Query("DELETE FROM downloaded_athans")
    suspend fun deleteAllDownloadedAthans()

    /**
     * Get count of downloaded athans
     */
    @Query("SELECT COUNT(*) FROM downloaded_athans")
    suspend fun getDownloadedAthanCount(): Int
}
