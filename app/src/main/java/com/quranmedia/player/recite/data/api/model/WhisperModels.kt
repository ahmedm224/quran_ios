package com.quranmedia.player.recite.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Response from Whisper API transcription
 */
data class WhisperResponse(
    @SerializedName("text")
    val text: String
)

/**
 * Error response from Whisper API
 */
data class WhisperError(
    @SerializedName("error")
    val error: WhisperErrorDetails
)

data class WhisperErrorDetails(
    @SerializedName("message")
    val message: String,
    @SerializedName("type")
    val type: String?,
    @SerializedName("code")
    val code: String?
)
