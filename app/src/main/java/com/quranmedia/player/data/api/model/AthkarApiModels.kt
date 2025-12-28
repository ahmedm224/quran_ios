package com.quranmedia.player.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * HisnMuslim API response models
 */
data class AthkarApiResponse(
    @SerializedName("chapters") val chapters: List<AthkarChapter>?
)

data class AthkarChapter(
    @SerializedName("id") val id: Int,
    @SerializedName("arabic") val arabic: String,
    @SerializedName("english") val english: String?,
    @SerializedName("transliteration") val transliteration: String?,
    @SerializedName("contents") val contents: List<AthkarContent>?
)

data class AthkarContent(
    @SerializedName("id") val id: Int,
    @SerializedName("text") val text: String,
    @SerializedName("count") val count: Int?,
    @SerializedName("reference") val reference: String?,
    @SerializedName("audio") val audio: String?
)

/**
 * Bundled Athkar JSON format (for offline data)
 */
data class BundledAthkarData(
    val categories: List<BundledCategory>
)

data class BundledCategory(
    val id: String,
    val nameArabic: String,
    val nameEnglish: String,
    val iconName: String,
    val order: Int,
    val athkar: List<BundledThikr>
)

data class BundledThikr(
    val id: String,
    val textArabic: String,
    val transliteration: String?,
    val translation: String?,
    val repeatCount: Int,
    val reference: String?
)
