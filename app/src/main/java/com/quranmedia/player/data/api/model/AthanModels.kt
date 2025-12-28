package com.quranmedia.player.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Response from /api/v1/athan/muezzins endpoint
 */
data class MuezzinListResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("muezzins") val muezzins: List<MuezzinDto>
)

/**
 * Individual muezzin data
 */
data class MuezzinDto(
    @SerializedName("name") val name: String,
    @SerializedName("location") val location: String,
    @SerializedName("count") val count: Int  // Number of athan recordings by this muezzin
)

/**
 * Response from /api/v1/athan/list endpoint
 */
data class AthanListResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("athans") val athans: List<AthanDto>
)

/**
 * Individual athan data
 */
data class AthanDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("muezzin") val muezzin: String,
    @SerializedName("location") val location: String,
    @SerializedName("audioUrl") val audioUrl: String  // Relative URL: /api/v1/athan/{id}
)
