package com.quranmedia.player.domain.model

import java.util.Date

data class Bookmark(
    val id: String,
    val reciterId: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val positionMs: Long,
    val label: String?,
    val loopEndMs: Long?,
    val createdAt: Date,
    val updatedAt: Date
)
