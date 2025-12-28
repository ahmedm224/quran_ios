package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.quranmedia.player.domain.model.Ayah

@Entity(
    tableName = "ayahs",
    primaryKeys = ["surahNumber", "ayahNumber"],
    foreignKeys = [
        ForeignKey(
            entity = SurahEntity::class,
            parentColumns = ["number"],
            childColumns = ["surahNumber"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["surahNumber"]),
        Index(value = ["globalAyahNumber"])
    ]
)
data class AyahEntity(
    val surahNumber: Int,
    val ayahNumber: Int,
    val globalAyahNumber: Int,  // Global ayah number (1-6236)
    val textArabic: String,
    val textTajweed: String? = null,  // Tajweed-encoded text with color markers
    val juz: Int,
    val manzil: Int,
    val page: Int,
    val ruku: Int,
    val hizbQuarter: Int,
    val sajda: Boolean
)

fun AyahEntity.toDomainModel() = Ayah(
    surahNumber = surahNumber,
    ayahNumber = ayahNumber,
    globalAyahNumber = globalAyahNumber,
    textArabic = textArabic,
    juz = juz,
    manzil = manzil,
    page = page,
    ruku = ruku,
    hizbQuarter = hizbQuarter,
    sajda = sajda
)

fun Ayah.toEntity() = AyahEntity(
    surahNumber = surahNumber,
    ayahNumber = ayahNumber,
    globalAyahNumber = globalAyahNumber,
    textArabic = textArabic,
    textTajweed = null,  // Not used - Tajweed colors applied from bundled JSON
    juz = juz,
    manzil = manzil,
    page = page,
    ruku = ruku,
    hizbQuarter = hizbQuarter,
    sajda = sajda
)
