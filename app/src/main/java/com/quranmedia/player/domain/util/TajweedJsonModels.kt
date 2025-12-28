package com.quranmedia.player.domain.util

import com.google.gson.annotations.SerializedName

/**
 * Data models for parsing Tajweed JSON from semarketir/quranjson
 *
 * JSON structure:
 * {
 *   "index": "001",
 *   "verse": {
 *     "verse_1": [...annotations...],
 *     "verse_2": [...annotations...],
 *     ...
 *   },
 *   "count": 7
 * }
 */

/**
 * Root object for a surah's Tajweed data
 */
data class TajweedSurahJson(
    @SerializedName("index") val index: String,
    @SerializedName("verse") val verses: Map<String, List<TajweedAnnotationJson>>,
    @SerializedName("count") val count: Int
)

/**
 * Single Tajweed annotation from JSON
 */
data class TajweedAnnotationJson(
    @SerializedName("start") val start: Int,
    @SerializedName("end") val end: Int,
    @SerializedName("rule") val rule: String
)
