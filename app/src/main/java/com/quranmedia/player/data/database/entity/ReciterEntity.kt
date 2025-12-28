package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quranmedia.player.domain.model.Reciter

@Entity(tableName = "reciters")
data class ReciterEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val nameArabic: String?,
    val style: String?,
    val version: String,
    val imageUrl: String?
)

fun ReciterEntity.toDomainModel() = Reciter(
    id = id,
    name = name,
    nameArabic = nameArabic,
    style = style,
    version = version,
    imageUrl = imageUrl
)

fun Reciter.toEntity() = ReciterEntity(
    id = id,
    name = name,
    nameArabic = nameArabic,
    style = style,
    version = version,
    imageUrl = imageUrl
)
