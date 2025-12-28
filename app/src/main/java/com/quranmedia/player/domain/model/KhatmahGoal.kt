package com.quranmedia.player.domain.model

import java.time.LocalDate
import java.util.Date

/**
 * Domain model for Khatmah (ختم) completion goal
 */
data class KhatmahGoal(
    val id: String,
    val name: String,                // "Ramadan 1446", "Monthly Goal", etc.
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startPage: Int,              // Starting page (default 1)
    val targetPages: Int,            // Total pages to complete (default 604)
    val isActive: Boolean,           // Only one active goal at a time
    val goalType: GoalType,
    val createdAt: Date,
    val completedAt: Date?           // Null if not yet completed
)

/**
 * Types of Khatmah goals
 * MONTHLY: Finish by end of current Hijri month
 * CUSTOM: User-specified end date
 */
enum class GoalType(
    val nameArabic: String,
    val nameEnglish: String
) {
    MONTHLY("شهري", "Monthly"),
    CUSTOM("مخصص", "Custom")
}

/**
 * Calculated progress for a Khatmah goal with Hijri calendar integration
 */
data class KhatmahProgress(
    val goal: KhatmahGoal,
    val currentPage: Int,            // Last page read
    val pagesCompleted: Int,         // Pages read since goal started
    val pagesRemaining: Int,         // Pages left to complete goal
    val daysElapsed: Int,            // Days since goal started
    val daysRemaining: Int,          // Days until goal end date
    val progressPercentage: Float,   // 0.0 - 100.0
    val dailyTargetPages: Float,     // Pages needed per day to finish on time
    val isOnTrack: Boolean,          // Whether user is meeting daily target
    val hijriMonthDay: Int,          // Current Hijri day (1-30)
    val hijriMonthLength: Int        // Expected Hijri month length (29 or 30)
)
