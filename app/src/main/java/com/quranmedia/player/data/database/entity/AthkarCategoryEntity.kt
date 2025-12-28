package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quranmedia.player.domain.model.AthkarCategory

@Entity(tableName = "athkar_categories")
data class AthkarCategoryEntity(
    @PrimaryKey
    val id: String,
    val nameArabic: String,
    val nameEnglish: String,
    val iconName: String,
    val order: Int,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel() = AthkarCategory(
        id = id,
        nameArabic = nameArabic,
        nameEnglish = nameEnglish,
        iconName = iconName,
        order = order
    )

    companion object {
        fun fromDomainModel(category: AthkarCategory) = AthkarCategoryEntity(
            id = category.id,
            nameArabic = category.nameArabic,
            nameEnglish = category.nameEnglish,
            iconName = category.iconName,
            order = category.order
        )
    }
}
