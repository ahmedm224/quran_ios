package com.quranmedia.player.domain.model

import java.time.LocalDate

/**
 * Domain model for daily Quran reading and listening progress
 */
data class QuranProgress(
    val date: LocalDate,
    val pagesRead: Int,              // Number of pages read in reader mode
    val pagesListened: Int,          // Number of pages listened to (audio)
    val readingDurationMs: Long,     // Total time spent reading
    val listeningDurationMs: Long,   // Total time spent listening
    val lastPage: Int,               // Last page viewed/read
    val lastSurah: Int,              // Last surah listened to
    val lastAyah: Int                // Last ayah listened to
)
