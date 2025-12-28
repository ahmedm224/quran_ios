package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "reading_bookmarks")
data class ReadingBookmarkEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val pageNumber: Int,
    val surahNumber: Int?,
    val surahName: String?,
    val label: String?,
    val createdAt: Long = System.currentTimeMillis()
)
