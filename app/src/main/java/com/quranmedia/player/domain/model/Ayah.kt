package com.quranmedia.player.domain.model

data class Ayah(
    val surahNumber: Int,
    val ayahNumber: Int,
    val globalAyahNumber: Int,
    val textArabic: String,
    val juz: Int,
    val manzil: Int,
    val page: Int,
    val ruku: Int,
    val hizbQuarter: Int,
    val sajda: Boolean
)
