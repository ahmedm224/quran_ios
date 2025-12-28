package com.quranmedia.player.domain.model

data class AyahIndex(
    val reciterId: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val startMs: Long,
    val endMs: Long
) {
    val durationMs: Long
        get() = endMs - startMs
}
