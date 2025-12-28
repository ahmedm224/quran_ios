package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val audioVariantId: String,
    val reciterId: String,
    val surahNumber: Int,
    val status: String, // PENDING, IN_PROGRESS, COMPLETED, FAILED, PAUSED
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0L,
    val bytesTotal: Long = 0L,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class DownloadStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    PAUSED
}
