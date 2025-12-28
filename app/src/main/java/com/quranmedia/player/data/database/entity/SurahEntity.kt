package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quranmedia.player.domain.model.RevelationType
import com.quranmedia.player.domain.model.Surah

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey
    val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val nameTransliteration: String,
    val ayahCount: Int,
    val revelationType: String
)

fun SurahEntity.toDomainModel() = Surah(
    id = number,
    number = number,
    nameArabic = nameArabic,
    nameEnglish = nameEnglish,
    nameTransliteration = nameTransliteration,
    ayahCount = ayahCount,
    revelationType = when (revelationType) {
        "MECCAN" -> RevelationType.MECCAN
        "MEDINAN" -> RevelationType.MEDINAN
        else -> RevelationType.MECCAN
    }
)

fun Surah.toEntity() = SurahEntity(
    number = number,
    nameArabic = nameArabic,
    nameEnglish = nameEnglish,
    nameTransliteration = nameTransliteration,
    ayahCount = ayahCount,
    revelationType = revelationType.name
)
