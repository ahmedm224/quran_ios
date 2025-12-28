package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.quranmedia.player.domain.model.Bookmark
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "bookmarks",
    indices = [
        Index(value = ["reciterId", "surahNumber", "ayahNumber"])
    ]
)
data class BookmarkEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val reciterId: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val positionMs: Long,
    val label: String?,
    val loopEndMs: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

fun BookmarkEntity.toDomainModel() = Bookmark(
    id = id,
    reciterId = reciterId,
    surahNumber = surahNumber,
    ayahNumber = ayahNumber,
    positionMs = positionMs,
    label = label,
    loopEndMs = loopEndMs,
    createdAt = Date(createdAt),
    updatedAt = Date(updatedAt)
)

fun Bookmark.toEntity() = BookmarkEntity(
    id = id,
    reciterId = reciterId,
    surahNumber = surahNumber,
    ayahNumber = ayahNumber,
    positionMs = positionMs,
    label = label,
    loopEndMs = loopEndMs,
    createdAt = createdAt.time,
    updatedAt = updatedAt.time
)
