package com.quranmedia.player.wear.data.model

import com.google.gson.annotations.SerializedName

/**
 * Root model for bundled athkar_data.json
 */
data class BundledAthkarData(
    @SerializedName("categories")
    val categories: List<BundledCategory>
)

/**
 * Category model from bundled JSON
 */
data class BundledCategory(
    @SerializedName("id")
    val id: String,
    @SerializedName("nameArabic")
    val nameArabic: String,
    @SerializedName("nameEnglish")
    val nameEnglish: String,
    @SerializedName("iconName")
    val iconName: String,
    @SerializedName("order")
    val order: Int,
    @SerializedName("athkar")
    val athkar: List<BundledThikr>
)

/**
 * Thikr model from bundled JSON
 */
data class BundledThikr(
    @SerializedName("id")
    val id: String,
    @SerializedName("textArabic")
    val textArabic: String,
    @SerializedName("repeatCount")
    val repeatCount: Int,
    @SerializedName("reference")
    val reference: String?
)
