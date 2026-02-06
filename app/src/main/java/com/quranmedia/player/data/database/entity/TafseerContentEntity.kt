package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Stores tafseer content for each ayah.
 * Primary key is composite of (tafseerId, surah, ayah).
 */
@Entity(
    tableName = "tafseer_content",
    primaryKeys = ["tafseerId", "surah", "ayah"],
    indices = [
        Index(value = ["tafseerId"]),
        Index(value = ["surah", "ayah"])
    ]
)
data class TafseerContentEntity(
    val tafseerId: String,    // e.g., "ar-tafsir-ibn-kathir"
    val surah: Int,           // 1-114
    val ayah: Int,            // Ayah number within surah
    val text: String          // The tafseer text
)
