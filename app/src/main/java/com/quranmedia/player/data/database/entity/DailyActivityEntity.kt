package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import com.quranmedia.player.domain.model.ActivityType
import com.quranmedia.player.domain.model.DailyActivity
import java.time.LocalDate

@Entity(
    tableName = "daily_activities",
    primaryKeys = ["date", "activityType"]
)
data class DailyActivityEntity(
    val date: String,              // ISO format YYYY-MM-DD
    val activityType: String,      // MORNING_AZKAR, EVENING_AZKAR, AFTER_PRAYER_AZKAR
    val completed: Boolean,
    val completedAt: Long?,        // Timestamp when marked complete (null if not completed)
    val createdAt: Long = System.currentTimeMillis()
)

fun DailyActivityEntity.toDomainModel() = DailyActivity(
    date = LocalDate.parse(date),
    activityType = ActivityType.valueOf(activityType),
    completed = completed,
    completedAt = completedAt?.let { java.util.Date(it) }
)

fun DailyActivity.toEntity() = DailyActivityEntity(
    date = date.toString(),
    activityType = activityType.name,
    completed = completed,
    completedAt = completedAt?.time,
    createdAt = System.currentTimeMillis()
)
