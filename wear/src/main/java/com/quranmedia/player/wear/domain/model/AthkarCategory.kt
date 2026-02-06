package com.quranmedia.player.wear.domain.model

/**
 * Represents an Athkar (Islamic remembrances) category.
 */
data class AthkarCategory(
    val id: String,
    val nameArabic: String,
    val nameEnglish: String,
    val iconName: String,
    val order: Int
)
