package com.quranmedia.player.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * QCF (Quran Complex Fonts) page data models.
 * Used for rendering Mushaf pages with per-page fonts (V2 plain, V4 Tajweed).
 */

/**
 * Serializer that handles both Int and String values for surah field.
 */
object FlexibleSurahSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleSurah", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String?) {
        if (value != null) {
            encoder.encodeString(value)
        }
    }

    override fun deserialize(decoder: Decoder): String? {
        return try {
            val jsonDecoder = decoder as? JsonDecoder
            val element = jsonDecoder?.decodeJsonElement()
            when (element) {
                is JsonPrimitive -> {
                    element.intOrNull?.toString() ?: element.content
                }
                else -> decoder.decodeString()
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Represents a single page of the Mushaf.
 * Maps to the mushaf-layout JSON structure.
 */
@Serializable
data class QCFPageData(
    val page: Int,
    val lines: List<QCFLineData>
)

/**
 * Represents a line on a Mushaf page.
 * Can be a surah header, basmala, or regular text line.
 */
@Serializable
data class QCFLineData(
    val line: Int? = null,
    val type: QCFLineType,
    val text: String? = null,
    @Serializable(with = FlexibleSurahSerializer::class)
    val surah: String? = null,
    val verseRange: String? = null,
    val words: List<QCFWordData>? = null,
    val qpcV2: String? = null,
    val qpcV1: String? = null
)

/**
 * Type of line on a Mushaf page.
 */
@Serializable
enum class QCFLineType {
    @SerialName("surah-header")
    SURAH_HEADER,

    @SerialName("basmala")
    BASMALA,

    @SerialName("text")
    TEXT
}

/**
 * Represents a single word in a Quran line.
 * Contains both the Arabic text and QCF glyph codes.
 */
@Serializable
data class QCFWordData(
    val location: String,  // Format: "surah:verse:wordIndex"
    val word: String,      // Arabic Unicode text
    val qpcV2: String,     // QCF V2 glyph code for rendering
    val qpcV1: String,     // QCF V1 glyph code (fallback)
    val isEnd: Boolean? = null  // True if this word marks the end of a verse (ayah number)
) {
    /**
     * Parse location to get surah number.
     */
    fun getSurahNumber(): Int? = location.split(":").getOrNull(0)?.toIntOrNull()

    /**
     * Parse location to get verse (ayah) number.
     */
    fun getVerseNumber(): Int? = location.split(":").getOrNull(1)?.toIntOrNull()

    /**
     * Parse location to get word index within the verse.
     */
    fun getWordIndex(): Int? = location.split(":").getOrNull(2)?.toIntOrNull()
}

/**
 * Font mode for QCF rendering.
 */
enum class QCFFontMode {
    /**
     * Plain mode - customizable text color without Tajweed coloring.
     * Uses QCF V2 fonts (accepts color override).
     * Fonts located in: assets/qcf-v2/
     */
    PLAIN,

    /**
     * Tajweed mode - colored fonts showing Tajweed rules.
     * Uses QCF V4 COLRv1 fonts with embedded color layers.
     * Fonts located in: assets/qcf-v4/
     */
    TAJWEED
}

/**
 * Get the font folder name for a given font mode.
 */
fun QCFFontMode.getFontFolder(): String = when (this) {
    QCFFontMode.PLAIN -> "qcf-v2"
    QCFFontMode.TAJWEED -> "qcf-v4"
}
