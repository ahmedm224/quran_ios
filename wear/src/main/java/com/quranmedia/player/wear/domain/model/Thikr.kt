package com.quranmedia.player.wear.domain.model

/**
 * Represents a single Thikr (remembrance/supplication).
 */
data class Thikr(
    val id: String,
    val categoryId: String,
    val textArabic: String,
    val repeatCount: Int,
    val reference: String?,
    val order: Int
)
