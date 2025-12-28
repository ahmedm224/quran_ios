package com.quranmedia.player.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.quranmedia.player.data.database.entity.DownloadTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadTaskDao {

    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun getAllDownloadTasks(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE id = :taskId")
    suspend fun getDownloadTaskById(taskId: String): DownloadTaskEntity?

    @Query("SELECT * FROM download_tasks WHERE status = :status")
    fun getDownloadTasksByStatus(status: String): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE reciterId = :reciterId AND surahNumber = :surahNumber")
    suspend fun getDownloadTaskForSurah(reciterId: String, surahNumber: Int): DownloadTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadTask(task: DownloadTaskEntity): Long

    @Update
    suspend fun updateDownloadTask(task: DownloadTaskEntity)

    @Query("DELETE FROM download_tasks WHERE id = :taskId")
    suspend fun deleteDownloadTask(taskId: String)

    @Query("DELETE FROM download_tasks WHERE status = 'COMPLETED'")
    suspend fun deleteCompletedTasks()
}
