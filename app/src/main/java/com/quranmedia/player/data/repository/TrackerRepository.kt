package com.quranmedia.player.data.repository

import com.quranmedia.player.data.database.dao.DailyActivityDao
import com.quranmedia.player.data.database.dao.KhatmahGoalDao
import com.quranmedia.player.data.database.dao.QuranProgressDao
import com.quranmedia.player.data.database.entity.toEntity
import com.quranmedia.player.data.database.entity.toDomainModel
import com.quranmedia.player.domain.model.ActivityType
import com.quranmedia.player.domain.model.DailyActivity
import com.quranmedia.player.domain.model.GoalType
import com.quranmedia.player.domain.model.HijriDate
import com.quranmedia.player.domain.model.KhatmahGoal
import com.quranmedia.player.domain.model.KhatmahProgress
import com.quranmedia.player.domain.model.QuranProgress
import com.quranmedia.player.domain.repository.PrayerTimesRepository
import com.quranmedia.player.domain.util.HijriCalendarUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing daily tracking of spiritual activities and Quran progress
 */
@Singleton
class TrackerRepository @Inject constructor(
    private val dailyActivityDao: DailyActivityDao,
    private val quranProgressDao: QuranProgressDao,
    private val khatmahGoalDao: KhatmahGoalDao,
    private val prayerTimesRepository: PrayerTimesRepository
) {

    // ============ Daily Activities (Azkar) ============

    /**
     * Get today's activities as a reactive flow
     */
    fun getTodayActivities(): Flow<List<DailyActivity>> {
        val today = LocalDate.now().toString()
        return dailyActivityDao.getActivitiesForDate(today).map { entities ->
            // Ensure all three activity types exist for today
            val existingTypes = entities.map { it.activityType }.toSet()
            val allActivities = mutableListOf<DailyActivity>()

            // Add existing activities
            allActivities.addAll(entities.map { it.toDomainModel() })

            // Add missing activity types as uncompleted
            ActivityType.entries.forEach { type ->
                if (!existingTypes.contains(type.name)) {
                    allActivities.add(
                        DailyActivity(
                            date = LocalDate.now(),
                            activityType = type,
                            completed = false,
                            completedAt = null
                        )
                    )
                }
            }

            // Sort by enum ordinal
            allActivities.sortedBy { it.activityType.ordinal }
        }
    }

    /**
     * Toggle an activity's completion status
     */
    suspend fun toggleActivity(activityType: ActivityType, completed: Boolean) {
        val today = LocalDate.now()
        val activity = DailyActivity(
            date = today,
            activityType = activityType,
            completed = completed,
            completedAt = if (completed) Date() else null
        )
        dailyActivityDao.insertActivity(activity.toEntity())
    }

    // ============ Quran Progress Tracking ============

    /**
     * Get today's Quran progress
     */
    fun getTodayProgress(): Flow<QuranProgress?> {
        val today = LocalDate.now().toString()
        return quranProgressDao.getProgressForDate(today).map { it?.toDomainModel() }
    }

    /**
     * Increment pages read count for today
     */
    suspend fun incrementPagesRead(pagesRead: Int) {
        val today = LocalDate.now().toString()
        val existing = quranProgressDao.getProgressForDate(today).first()

        val updated = if (existing != null) {
            existing.copy(
                pagesRead = existing.pagesRead + pagesRead,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            com.quranmedia.player.data.database.entity.QuranProgressEntity(
                date = today,
                pagesRead = pagesRead
            )
        }

        quranProgressDao.insertProgress(updated)
    }

    /**
     * Increment pages listened count for today
     */
    suspend fun incrementPagesListened(pagesListened: Int, durationMs: Long = 0) {
        val today = LocalDate.now().toString()
        val existing = quranProgressDao.getProgressForDate(today).first()

        val updated = if (existing != null) {
            existing.copy(
                pagesListened = existing.pagesListened + pagesListened,
                listeningDurationMs = existing.listeningDurationMs + durationMs,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            com.quranmedia.player.data.database.entity.QuranProgressEntity(
                date = today,
                pagesListened = pagesListened,
                listeningDurationMs = durationMs
            )
        }

        quranProgressDao.insertProgress(updated)
    }

    /**
     * Set pages listened manually (for user manual entry)
     */
    suspend fun setPagesListened(pagesListened: Int) {
        val today = LocalDate.now().toString()
        val existing = quranProgressDao.getProgressForDate(today).first()

        val updated = if (existing != null) {
            existing.copy(
                pagesListened = pagesListened,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            com.quranmedia.player.data.database.entity.QuranProgressEntity(
                date = today,
                pagesListened = pagesListened
            )
        }

        quranProgressDao.insertProgress(updated)
    }

    /**
     * Mark that user listened to audio today (simple tracking)
     * Called when user starts playing Quran audio
     */
    suspend fun markAudioPlayedToday() {
        val today = LocalDate.now().toString()
        val existing = quranProgressDao.getProgressForDate(today).first()

        // If no listening recorded yet today, mark 1 page as listened
        // User can manually adjust this later if needed
        if (existing == null || existing.pagesListened == 0) {
            incrementPagesListened(1)
        }
    }

    /**
     * Update last reading position
     */
    suspend fun updateLastReadingPosition(page: Int) {
        val today = LocalDate.now().toString()
        val existing = quranProgressDao.getProgressForDate(today).first()

        val updated = if (existing != null) {
            existing.copy(
                lastPage = page,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            com.quranmedia.player.data.database.entity.QuranProgressEntity(
                date = today,
                lastPage = page
            )
        }

        quranProgressDao.insertProgress(updated)
    }

    /**
     * Update last listening position
     */
    suspend fun updateLastListeningPosition(surah: Int, ayah: Int) {
        val today = LocalDate.now().toString()
        val existing = quranProgressDao.getProgressForDate(today).first()

        val updated = if (existing != null) {
            existing.copy(
                lastSurah = surah,
                lastAyah = ayah,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            com.quranmedia.player.data.database.entity.QuranProgressEntity(
                date = today,
                lastSurah = surah,
                lastAyah = ayah
            )
        }

        quranProgressDao.insertProgress(updated)
    }

    /**
     * Increment reading duration
     */
    suspend fun incrementReadingDuration(durationMs: Long) {
        val today = LocalDate.now().toString()
        val existing = quranProgressDao.getProgressForDate(today).first()

        val updated = if (existing != null) {
            existing.copy(
                readingDurationMs = existing.readingDurationMs + durationMs,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            com.quranmedia.player.data.database.entity.QuranProgressEntity(
                date = today,
                readingDurationMs = durationMs
            )
        }

        quranProgressDao.insertProgress(updated)
    }

    // ============ Khatmah Goals ============

    /**
     * Get active Khatmah goal
     */
    fun getActiveGoal(): Flow<KhatmahGoal?> {
        return khatmahGoalDao.getActiveGoal().map { it?.toDomainModel() }
    }

    /**
     * Get all Khatmah goals (active, completed, archived)
     */
    fun getAllGoals(): Flow<List<KhatmahGoal>> {
        return khatmahGoalDao.getAllGoals().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Create a new Khatmah goal
     */
    suspend fun createGoal(
        name: String,
        startDate: LocalDate,
        endDate: LocalDate,
        startPage: Int = 1,
        targetPages: Int = 604,
        goalType: GoalType,
        setAsActive: Boolean = true
    ): String {
        val goalId = UUID.randomUUID().toString()

        // Deactivate all goals if this should be active
        if (setAsActive) {
            khatmahGoalDao.deactivateAllGoals()
        }

        val goal = KhatmahGoal(
            id = goalId,
            name = name,
            startDate = startDate,
            endDate = endDate,
            startPage = startPage,
            targetPages = targetPages,
            isActive = setAsActive,
            goalType = goalType,
            createdAt = Date(),
            completedAt = null
        )

        khatmahGoalDao.insertGoal(goal.toEntity())
        return goalId
    }

    /**
     * Set a goal as active (deactivates all others)
     */
    suspend fun setActiveGoal(goalId: String) {
        val goal = khatmahGoalDao.getGoalById(goalId) ?: return

        // Deactivate all goals first
        khatmahGoalDao.deactivateAllGoals()

        // Activate this goal
        khatmahGoalDao.insertGoal(goal.copy(isActive = true))
    }

    /**
     * Delete a goal
     */
    suspend fun deleteGoal(goalId: String) {
        khatmahGoalDao.deleteGoal(goalId)
    }

    /**
     * Mark a goal as completed
     */
    suspend fun completeGoal(goalId: String) {
        val goal = khatmahGoalDao.getGoalById(goalId) ?: return
        khatmahGoalDao.insertGoal(
            goal.copy(
                completedAt = System.currentTimeMillis(),
                isActive = false
            )
        )
    }

    // ============ Progress Calculation ============

    /**
     * Calculate detailed progress for a Khatmah goal
     */
    suspend fun calculateKhatmahProgress(goalId: String): KhatmahProgress? {
        val goalEntity = khatmahGoalDao.getGoalById(goalId) ?: return null
        val goal = goalEntity.toDomainModel()

        // Get all progress since goal started
        val progressList = quranProgressDao.getProgressForDateRange(
            startDate = goal.startDate.toString(),
            endDate = LocalDate.now().toString()
        ).first()

        // Sum up pages read
        val pagesCompleted = progressList.sumOf { it.pagesRead }

        // Calculate dates
        val today = LocalDate.now()
        val daysElapsed = ChronoUnit.DAYS.between(goal.startDate, today).toInt()
        val daysRemaining = ChronoUnit.DAYS.between(today, goal.endDate).toInt().coerceAtLeast(0)

        // Get Hijri date info
        val hijriDate = getHijriDateInfo()
        val hijriMonthLength = HijriCalendarUtils.getMonthLength(hijriDate)

        // Calculate targets
        val totalDays = daysElapsed + daysRemaining
        val progressPercentage = if (goal.targetPages > 0) {
            (pagesCompleted.toFloat() / goal.targetPages * 100f)
        } else {
            0f
        }
        val pagesRemaining = goal.targetPages - pagesCompleted
        val dailyTargetPages = if (daysRemaining > 0) {
            pagesRemaining.toFloat() / daysRemaining
        } else {
            0f
        }

        // Check if on track (based on goal type and Hijri calendar for monthly goals)
        val expectedPages = when (goal.goalType) {
            GoalType.MONTHLY -> {
                // For monthly goals, expected progress based on current Hijri day
                // If today is 15th of 30-day month, expected to be at ~50% (302 pages)
                HijriCalendarUtils.expectedPageForHijriDay(
                    currentDay = hijriDate.day,
                    monthLength = hijriMonthLength,
                    totalPages = goal.targetPages
                )
            }
            GoalType.CUSTOM -> {
                // For custom goals, expected progress based on elapsed days
                if (totalDays > 0) {
                    (daysElapsed.toFloat() / totalDays * goal.targetPages).toInt()
                } else {
                    0
                }
            }
        }
        val isOnTrack = pagesCompleted >= expectedPages

        return KhatmahProgress(
            goal = goal,
            currentPage = progressList.lastOrNull()?.lastPage ?: goal.startPage,
            pagesCompleted = pagesCompleted,
            pagesRemaining = pagesRemaining.coerceAtLeast(0),
            daysElapsed = daysElapsed,
            daysRemaining = daysRemaining,
            progressPercentage = progressPercentage.coerceIn(0f, 100f),
            dailyTargetPages = dailyTargetPages,
            isOnTrack = isOnTrack,
            hijriMonthDay = hijriDate.day,
            hijriMonthLength = hijriMonthLength
        )
    }

    /**
     * Get Hijri date info from prayer times or calculate it
     */
    private suspend fun getHijriDateInfo(): HijriDate {
        // Try to get Hijri date from prayer times first
        val prayerTimes = prayerTimesRepository.getCachedPrayerTimes(LocalDate.now()).first()
        return prayerTimes?.hijriDate ?: HijriCalendarUtils.gregorianToHijri(LocalDate.now())
    }

    // ============ Cleanup ============

    /**
     * Delete old activity records (keep last 90 days)
     */
    suspend fun cleanupOldRecords(daysToKeep: Int = 90) {
        val threshold = LocalDate.now().minusDays(daysToKeep.toLong()).toString()
        dailyActivityDao.deleteOldActivities(threshold)
        quranProgressDao.deleteOldProgress(threshold)
    }
}
