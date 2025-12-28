package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.quranmedia.player.domain.model.Thikr

@Entity(
    tableName = "athkar",
    foreignKeys = [
        ForeignKey(
            entity = AthkarCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class ThikrEntity(
    @PrimaryKey
    val id: String,
    val categoryId: String,
    val textArabic: String,
    val transliteration: String? = null,
    val translation: String? = null,
    val repeatCount: Int = 1,
    val reference: String? = null,
    val audioUrl: String? = null,
    val order: Int
) {
    fun toDomainModel() = Thikr(
        id = id,
        categoryId = categoryId,
        textArabic = textArabic,
        transliteration = transliteration,
        translation = translation,
        repeatCount = repeatCount,
        reference = reference,
        audioUrl = audioUrl,
        order = order
    )

    companion object {
        fun fromDomainModel(thikr: Thikr) = ThikrEntity(
            id = thikr.id,
            categoryId = thikr.categoryId,
            textArabic = thikr.textArabic,
            transliteration = thikr.transliteration,
            translation = thikr.translation,
            repeatCount = thikr.repeatCount,
            reference = thikr.reference,
            audioUrl = thikr.audioUrl,
            order = thikr.order
        )
    }
}
