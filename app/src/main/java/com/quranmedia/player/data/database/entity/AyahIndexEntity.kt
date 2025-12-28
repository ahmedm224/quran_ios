package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import com.quranmedia.player.domain.model.AyahIndex

@Entity(
    tableName = "ayah_index",
    primaryKeys = ["reciterId", "surahNumber", "ayahNumber"],
    indices = [
        Index(value = ["reciterId", "surahNumber"]),
        Index(value = ["startMs", "endMs"])
    ]
)
data class AyahIndexEntity(
    val reciterId: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val startMs: Long,
    val endMs: Long
)

fun AyahIndexEntity.toDomainModel() = AyahIndex(
    reciterId = reciterId,
    surahNumber = surahNumber,
    ayahNumber = ayahNumber,
    startMs = startMs,
    endMs = endMs
)

fun AyahIndex.toEntity() = AyahIndexEntity(
    reciterId = reciterId,
    surahNumber = surahNumber,
    ayahNumber = ayahNumber,
    startMs = startMs,
    endMs = endMs
)
