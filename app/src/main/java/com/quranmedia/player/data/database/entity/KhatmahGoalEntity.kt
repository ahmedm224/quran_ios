package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quranmedia.player.domain.model.GoalType
import com.quranmedia.player.domain.model.KhatmahGoal
import java.time.LocalDate
import java.util.Date
import java.util.UUID

@Entity(tableName = "khatmah_goals")
data class KhatmahGoalEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,              // "Ramadan 1446", "Monthly Goal", etc.
    val startDate: String,         // ISO format YYYY-MM-DD
    val endDate: String,           // ISO format YYYY-MM-DD
    val startPage: Int = 1,        // Starting page (default 1)
    val targetPages: Int = 604,    // Total pages to complete
    val isActive: Boolean = true,  // Only one active at a time
    val goalType: String,          // MONTHLY, RAMADAN, CUSTOM
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null  // Null if not completed
)

fun KhatmahGoalEntity.toDomainModel() = KhatmahGoal(
    id = id,
    name = name,
    startDate = LocalDate.parse(startDate),
    endDate = LocalDate.parse(endDate),
    startPage = startPage,
    targetPages = targetPages,
    isActive = isActive,
    goalType = GoalType.valueOf(goalType),
    createdAt = Date(createdAt),
    completedAt = completedAt?.let { Date(it) }
)

fun KhatmahGoal.toEntity() = KhatmahGoalEntity(
    id = id,
    name = name,
    startDate = startDate.toString(),
    endDate = endDate.toString(),
    startPage = startPage,
    targetPages = targetPages,
    isActive = isActive,
    goalType = goalType.name,
    createdAt = createdAt.time,
    completedAt = completedAt?.time
)
