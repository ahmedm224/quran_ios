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
import androidx.work.WorkerParameters
import com.quranmedia.player.R
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.data.repository.PrayerNotificationMode
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.domain.model.PrayerType
import com.quranmedia.player.media.service.AthanService
import com.quranmedia.player.presentation.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Worker that shows prayer time notifications.
 * Receives the prayer type as input data and displays the appropriate notification.
 */
@HiltWorker
class PrayerNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "prayer_notification_channel"
        const val NOTIFICATION_ID_BASE = 2000
        const val KEY_PRAYER_TYPE = "prayer_type"

        // Prayer names in Arabic
        private val arabicPrayerNames = mapOf(
            PrayerType.FAJR to "الفجر",
            PrayerType.SUNRISE to "الشروق",
            PrayerType.DHUHR to "الظهر",
            PrayerType.ASR to "العصر",
            PrayerType.MAGHRIB to "المغرب",
            PrayerType.ISHA to "العشاء"
        )

        // Prayer names in English
        private val englishPrayerNames = mapOf(
            PrayerType.FAJR to "Fajr",
            PrayerType.SUNRISE to "Sunrise",
            PrayerType.DHUHR to "Dhuhr",
            PrayerType.ASR to "Asr",
            PrayerType.MAGHRIB to "Maghrib",
            PrayerType.ISHA to "Isha"
        )
    }

    override suspend fun doWork(): Result {
        return try {
            val settings = settingsRepository.getCurrentSettings()

            // Check if prayer notifications are enabled
            if (!settings.prayerNotificationEnabled) {
                Timber.d("Prayer notifications are disabled, skipping")
                return Result.success()
            }

            // Get prayer type from input data
            val prayerTypeName = inputData.getString(KEY_PRAYER_TYPE)
            if (prayerTypeName == null) {
                Timber.e("No prayer type provided")
                return Result.failure()
            }

            val prayerType = try {
                PrayerType.valueOf(prayerTypeName)
            } catch (e: IllegalArgumentException) {
                Timber.e("Invalid prayer type: $prayerTypeName")
                return Result.failure()
            }

            // Check if this prayer type is enabled for notifications
            val isEnabled = when (prayerType) {
                PrayerType.FAJR -> settings.notifyFajr
                PrayerType.DHUHR -> settings.notifyDhuhr
                PrayerType.ASR -> settings.notifyAsr
                PrayerType.MAGHRIB -> settings.notifyMaghrib
                PrayerType.ISHA -> settings.notifyIsha
                PrayerType.SUNRISE -> false  // Don't notify for sunrise
            }

            if (!isEnabled) {
                Timber.d("Notification for $prayerType is disabled")
                return Result.success()
            }

            // Get notification mode for this specific prayer
            val notificationMode = settingsRepository.getPrayerNotificationMode(prayerType.name)

            when (notificationMode) {
                PrayerNotificationMode.ATHAN -> {
                    // Play athan sound
                    val athanId = settingsRepository.getPrayerAthanId(prayerType.name)
                    Timber.d("Starting Athan service for $prayerType with athan ID: $athanId")
                    AthanService.startAthan(context, athanId, prayerType)
                }
                PrayerNotificationMode.NOTIFICATION -> {
                    // Show notification only
                    showPrayerNotification(
                        prayerType = prayerType,
                        language = settings.appLanguage,
                        minutesBefore = settings.prayerNotificationMinutesBefore,
                        withSound = settings.prayerNotificationSound,
                        withVibration = settings.prayerNotificationVibrate
                    )
                    Timber.d("Prayer notification sent for $prayerType")
                }
                PrayerNotificationMode.SILENT -> {
                    // Silent mode - do nothing
                    Timber.d("$prayerType is set to silent mode, skipping")
                }
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error in PrayerNotificationWorker")
            Result.failure()
        }
    }

    private fun showPrayerNotification(
        prayerType: PrayerType,
        language: AppLanguage,
        minutesBefore: Int,
        withSound: Boolean,
        withVibration: Boolean
    ) {
        createNotificationChannel(withSound, withVibration)

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
            putExtra("navigate_to", "prayer_times")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            prayerType.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification content based on language
        val prayerName = if (language == AppLanguage.ARABIC) {
            arabicPrayerNames[prayerType] ?: prayerType.nameArabic
        } else {
            englishPrayerNames[prayerType] ?: prayerType.nameEnglish
        }

        val title = if (language == AppLanguage.ARABIC) "الفرقان" else "Alfurqan"

        val message = if (minutesBefore > 0) {
            if (language == AppLanguage.ARABIC) {
                "صلاة $prayerName بعد $minutesBefore دقيقة"
            } else {
                "$prayerName prayer in $minutesBefore minutes"
            }
        } else {
            if (language == AppLanguage.ARABIC) {
                "حان وقت صلاة $prayerName"
            } else {
                "Time for $prayerName prayer"
            }
        }

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Set defaults based on settings
        var defaults = 0
        if (withSound) defaults = defaults or NotificationCompat.DEFAULT_SOUND
        if (withVibration) defaults = defaults or NotificationCompat.DEFAULT_VIBRATE
        if (defaults > 0) {
            notificationBuilder.setDefaults(defaults)
        }

        // Use unique notification ID for each prayer type
        val notificationId = NOTIFICATION_ID_BASE + prayerType.ordinal

        NotificationManagerCompat.from(context).notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannel(withSound: Boolean, withVibration: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Prayer Time Notifications"
            val descriptionText = "Notifications for prayer times"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(withVibration)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                if (!withSound) {
                    setSound(null, null)
                }
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
