package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.quranmedia.player.domain.model.AudioFormat
import com.quranmedia.player.domain.model.AudioVariant
import java.util.UUID

@Entity(
    tableName = "audio_variants",
    foreignKeys = [
        ForeignKey(
            entity = ReciterEntity::class,
            parentColumns = ["id"],
            childColumns = ["reciterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["reciterId", "surahNumber"])
    ]
)
data class AudioVariantEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val reciterId: String,
    val surahNumber: Int,
    val bitrate: Int,
    val format: String,
    val url: String,
    val localPath: String?,
    val durationMs: Long,
    val fileSizeBytes: Long?,
    val hash: String?
)

fun AudioVariantEntity.toDomainModel() = AudioVariant(
    id = id,
    reciterId = reciterId,
    surahNumber = surahNumber,
    bitrate = bitrate,
    format = when (format) {
        "MP3" -> AudioFormat.MP3
        "FLAC" -> AudioFormat.FLAC
        "M4A" -> AudioFormat.M4A
        else -> AudioFormat.MP3
    },
    url = url,
    localPath = localPath,
    durationMs = durationMs,
    fileSizeBytes = fileSizeBytes,
    hash = hash
)

fun AudioVariant.toEntity() = AudioVariantEntity(
    id = id,
    reciterId = reciterId,
    surahNumber = surahNumber,
    bitrate = bitrate,
    format = format.name,
    url = url,
    localPath = localPath,
    durationMs = durationMs,
    fileSizeBytes = fileSizeBytes,
    hash = hash
)
