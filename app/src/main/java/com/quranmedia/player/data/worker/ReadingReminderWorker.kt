package com.quranmedia.player.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.quranmedia.player.R
import com.quranmedia.player.data.database.dao.AyahDao
import com.quranmedia.player.data.database.dao.ReadingBookmarkDao
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.data.repository.ReminderInterval
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.presentation.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Worker that sends reading reminder notifications based on user settings.
 * Respects quiet hours (night time) and user language preferences.
 */
@HiltWorker
class ReadingReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val ayahDao: AyahDao,
    private val readingBookmarkDao: ReadingBookmarkDao
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "reading_reminder_work"
        const val CHANNEL_ID = "reading_reminder_channel"
        const val NOTIFICATION_ID = 1001

        // Reminder messages in Arabic
        private val arabicMessages = listOf(
            "حان وقت قراءة القرآن الكريم",
            "لا تنسَ وردك اليومي من القرآن",
            "هل قرأت القرآن اليوم؟",
            "تذكير: واصل قراءة القرآن الكريم",
            "القرآن ينتظرك، تابع القراءة"
        )

        // Reminder messages in English
        private val englishMessages = listOf(
            "Time to read the Holy Quran",
            "Don't forget your daily Quran reading",
            "Have you read the Quran today?",
            "Reminder: Continue reading the Holy Quran",
            "The Quran awaits you, continue reading"
        )

        /**
         * Schedule the reading reminder worker based on settings
         */
        fun scheduleReminder(context: Context, interval: ReminderInterval) {
            val workManager = WorkManager.getInstance(context)

            if (interval == ReminderInterval.OFF) {
                // Cancel any existing reminder work
                workManager.cancelUniqueWork(WORK_NAME)
                Timber.d("Reading reminder cancelled")
                return
            }

            val reminderRequest = PeriodicWorkRequestBuilder<ReadingReminderWorker>(
                interval.hours.toLong(), TimeUnit.HOURS
            ).build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                reminderRequest
            )

            Timber.d("Reading reminder scheduled for every ${interval.hours} hours")
        }

        /**
         * Cancel all reading reminders
         */
        fun cancelReminder(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Timber.d("Reading reminder cancelled")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val settings = settingsRepository.getCurrentSettings()

            // Check if reminders are enabled
            if (!settings.readingReminderEnabled) {
                Timber.d("Reading reminders are disabled, skipping")
                return Result.success()
            }

            // Check if we're in quiet hours
            if (isQuietHours(settings.quietHoursStart, settings.quietHoursEnd)) {
                Timber.d("Currently in quiet hours, skipping notification")
                return Result.success()
            }

            // Check if user has read recently (within the reminder interval)
            val lastReading = settings.lastReadingTimestamp
            val intervalMs = settings.readingReminderInterval.hours * 60 * 60 * 1000L
            val timeSinceLastReading = System.currentTimeMillis() - lastReading

            if (lastReading > 0 && timeSinceLastReading < intervalMs) {
                Timber.d("User read recently (${timeSinceLastReading / 1000 / 60} minutes ago), skipping notification")
                return Result.success()
            }

            // Get ayah text from last bookmark or reading position
            val ayahPreview = getLastReadingAyahPreview()

            // Send notification
            showReminderNotification(settings.appLanguage, ayahPreview)

            Timber.d("Reading reminder notification sent")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error in ReadingReminderWorker")
            Result.failure()
        }
    }

    /**
     * Get a preview of the last ayah the user was reading (from bookmark or last position)
     */
    private suspend fun getLastReadingAyahPreview(): String? {
        try {
            // First try to get from the most recent reading bookmark
            val latestBookmark = readingBookmarkDao.getMostRecentBookmark()
            if (latestBookmark != null) {
                // Get first ayah of the bookmarked page
                val ayahs = ayahDao.getAyahsByPageSync(latestBookmark.pageNumber)
                if (ayahs.isNotEmpty()) {
                    val ayahText = ayahs.first().textArabic
                    // Return first 50 characters with ellipsis
                    return if (ayahText.length > 50) {
                        ayahText.take(50) + "..."
                    } else {
                        ayahText
                    }
                }
            }

            // Fallback: get from last playback position if available
            val settings = settingsRepository.getCurrentSettings()
            if (settings.lastSurahNumber > 0) {
                val ayah = ayahDao.getAyah(settings.lastSurahNumber, 1)
                if (ayah != null) {
                    val ayahText = ayah.textArabic
                    return if (ayahText.length > 50) {
                        ayahText.take(50) + "..."
                    } else {
                        ayahText
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting last reading ayah preview")
        }
        return null
    }

    /**
     * Check if current time is within quiet hours
     */
    private fun isQuietHours(startHour: Int, endHour: Int): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        return if (startHour <= endHour) {
            // Simple case: quiet hours don't span midnight (e.g., 14:00 - 18:00)
            currentHour in startHour until endHour
        } else {
            // Quiet hours span midnight (e.g., 22:00 - 07:00)
            currentHour >= startHour || currentHour < endHour
        }
    }

    /**
     * Show the reminder notification with optional ayah preview
     */
    private fun showReminderNotification(language: AppLanguage, ayahPreview: String?) {
        createNotificationChannel()

        // Check notification permission (required for Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Timber.w("Notification permission not granted")
                return
            }
        }

        // Create intent to open the app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification content based on language
        val title = if (language == AppLanguage.ARABIC) "الفرقان" else "Alfurqan"

        // Primary message based on language
        val primaryMessage = if (language == AppLanguage.ARABIC) {
            "حان وقت مواصلة القراءة"
        } else {
            "Time to continue reading"
        }

        // Build the notification - HIGH priority to ensure visibility and persistence
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(primaryMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        // Add ayah preview as expanded text if available
        if (!ayahPreview.isNullOrBlank()) {
            val expandedText = if (language == AppLanguage.ARABIC) {
                "تابع من حيث توقفت:\n$ayahPreview"
            } else {
                "Continue from where you left off:\n$ayahPreview"
            }

            notificationBuilder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedText)
                    .setBigContentTitle(title)
            )
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    /**
     * Create notification channel for reminders
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Reading Reminders"
            val descriptionText = "Reminders to continue reading the Quran"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
