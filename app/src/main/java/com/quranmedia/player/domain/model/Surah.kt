package com.quranmedia.player.domain.model

data class Surah(
    val id: Int,
    val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val nameTransliteration: String,
    val ayahCount: Int,
    val revelationType: RevelationType
)

enum class RevelationType {
    MECCAN,
    MEDINAN
}
