package com.quranmedia.player.domain.model

import java.time.LocalDate
import java.util.Date

/**
 * Domain model for daily spiritual activities (Azkar)
 */
data class DailyActivity(
    val date: LocalDate,
    val activityType: ActivityType,
    val completed: Boolean,
    val completedAt: Date?
)

/**
 * Types of daily spiritual activities
 */
enum class ActivityType(
    val nameArabic: String,
    val nameEnglish: String
) {
    MORNING_AZKAR("أذكار الصباح", "Morning Azkar"),
    EVENING_AZKAR("أذكار المساء", "Evening Azkar"),
    AFTER_PRAYER_AZKAR("أذكار بعد الصلاة", "After Prayer Azkar")
}
