package com.quranmedia.player.domain.model

data class AudioVariant(
    val id: String,
    val reciterId: String,
    val surahNumber: Int,
    val bitrate: Int,
    val format: AudioFormat,
    val url: String,
    val localPath: String?,
    val durationMs: Long,
    val fileSizeBytes: Long?,
    val hash: String?
)

enum class AudioFormat {
    MP3,
    FLAC,
    M4A
}
