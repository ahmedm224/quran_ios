package com.quranmedia.player.media.model

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val currentReciter: String? = null,
    val currentReciterName: String? = null,
    val currentSurah: Int? = null,
    val currentSurahNameArabic: String? = null,
    val currentSurahNameEnglish: String? = null,
    val currentAyah: Int? = null,
    val currentAyahText: String? = null,
    val totalAyahs: Int? = null,
    val isBuffering: Boolean = false,
    val error: String? = null
)

data class LoopState(
    val isEnabled: Boolean = false,
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val currentLoop: Int = 0,
    val totalLoops: Int = 1 // -1 for infinite
)
