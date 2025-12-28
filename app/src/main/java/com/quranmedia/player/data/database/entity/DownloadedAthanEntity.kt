package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a downloaded Athan audio file.
 * Stores metadata about the athan and its local file path.
 */
@Entity(tableName = "downloaded_athans")
data class DownloadedAthanEntity(
    @PrimaryKey
    val id: String,  // Athan ID from API
    val name: String,
    val muezzin: String,
    val location: String,
    val localPath: String,  // Local file path where athan is stored
    val fileSize: Long = 0,  // File size in bytes
    val downloadedAt: Long = System.currentTimeMillis()
)
