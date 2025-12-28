package com.quranmedia.player.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Data models for Tanzil Quran JSON format
 * Source: https://cdn.jsdelivr.net/npm/quran-json@3.1.2/dist/quran.json
 */

data class TanzilSurah(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val nameArabic: String,
    @SerializedName("transliteration")
    val transliteration: String,
    @SerializedName("translation")
    val translation: String?,
    @SerializedName("type")
    val type: String,  // "meccan" or "medinan"
    @SerializedName("total_verses")
    val totalVerses: Int,
    @SerializedName("verses")
    val verses: List<TanzilVerse>
)

data class TanzilVerse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("text")
    val text: String
)
