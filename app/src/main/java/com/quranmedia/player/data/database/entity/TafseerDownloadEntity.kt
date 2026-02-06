package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks which tafseers have been downloaded.
 * Each tafseer is downloaded as a complete set (all 114 surahs).
 */
@Entity(tableName = "tafseer_downloads")
data class TafseerDownloadEntity(
    @PrimaryKey
    val tafseerId: String,           // e.g., "ar-tafsir-ibn-kathir"
    val nameArabic: String,          // e.g., "تفسير ابن كثير"
    val nameEnglish: String,         // e.g., "Tafsir Ibn Kathir"
    val language: String,            // "ar" or "en"
    val type: String,                // "tafseer" or "word_meaning"
    val downloadedAt: Long,          // Timestamp when download completed
    val totalSizeBytes: Long,        // Total size in bytes
    val surahsDownloaded: Int = 114  // Number of surahs downloaded (for progress tracking)
)
