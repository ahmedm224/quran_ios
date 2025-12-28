package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quranmedia.player.domain.model.QuranProgress
import java.time.LocalDate

@Entity(
    tableName = "quran_progress"
)
data class QuranProgressEntity(
    @PrimaryKey
    val date: String,              // ISO format YYYY-MM-DD
    val pagesRead: Int = 0,        // Pages viewed in reader
    val pagesListened: Int = 0,    // Pages worth of audio listened
    val readingDurationMs: Long = 0,   // Time spent in reader
    val listeningDurationMs: Long = 0, // Time spent listening
    val lastPage: Int = 1,         // Last page viewed
    val lastSurah: Int = 1,        // Last surah listened to
    val lastAyah: Int = 1,         // Last ayah listened to
    val updatedAt: Long = System.currentTimeMillis()
)

fun QuranProgressEntity.toDomainModel() = QuranProgress(
    date = LocalDate.parse(date),
    pagesRead = pagesRead,
    pagesListened = pagesListened,
    readingDurationMs = readingDurationMs,
    listeningDurationMs = listeningDurationMs,
    lastPage = lastPage,
    lastSurah = lastSurah,
    lastAyah = lastAyah
)

fun QuranProgress.toEntity() = QuranProgressEntity(
    date = date.toString(),
    pagesRead = pagesRead,
    pagesListened = pagesListened,
    readingDurationMs = readingDurationMs,
    listeningDurationMs = listeningDurationMs,
    lastPage = lastPage,
    lastSurah = lastSurah,
    lastAyah = lastAyah,
    updatedAt = System.currentTimeMillis()
)
