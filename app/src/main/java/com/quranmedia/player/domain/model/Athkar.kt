package com.quranmedia.player.domain.model

/**
 * Domain model for Athkar category
 */
data class AthkarCategory(
    val id: String,
    val nameArabic: String,
    val nameEnglish: String,
    val iconName: String,
    val order: Int
)

/**
 * Domain model for individual Thikr (remembrance)
 */
data class Thikr(
    val id: String,
    val categoryId: String,
    val textArabic: String,
    val transliteration: String? = null,
    val translation: String? = null,
    val repeatCount: Int = 1,
    val reference: String? = null,
    val audioUrl: String? = null,
    val order: Int
)
