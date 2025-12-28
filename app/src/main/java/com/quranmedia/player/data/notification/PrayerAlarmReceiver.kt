package com.quranmedia.player.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.quranmedia.player.R
import com.quranmedia.player.data.repository.PrayerNotificationMode
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.domain.model.PrayerType
import com.quranmedia.player.media.player.AthanPlayer
import com.quranmedia.player.media.service.AthanService
import com.quranmedia.player.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * BroadcastReceiver for exact prayer time alarms.
 * This ensures notifications/athan trigger at the exact scheduled time,
 * even when the device is in Doze mode.
 */
@AndroidEntryPoint
class PrayerAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var athanPlayer: AthanPlayer

    companion object {
        const val ACTION_PRAYER_ALARM = "com.quranmedia.player.PRAYER_ALARM"
        const val ACTION_STOP_ATHAN = "com.quranmedia.player.STOP_ATHAN"
        const val EXTRA_PRAYER_TYPE = "prayer_type"
        private const val CHANNEL_ID = "prayer_notifications"
        private const val NOTIFICATION_ID_BASE = 2000
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Handle stop athan action
        if (intent.action == ACTION_STOP_ATHAN) {
            Timber.d("Stop athan action received")
            // Stop the AthanService which handles all athan playback
            AthanService.stopAthan(context)
            return
        }

        if (intent.action != ACTION_PRAYER_ALARM) return

        val prayerTypeName = intent.getStringExtra(EXTRA_PRAYER_TYPE) ?: return
        val prayerType = try {
            PrayerType.valueOf(prayerTypeName)
        } catch (e: Exception) {
            Timber.e("Invalid prayer type: $prayerTypeName")
            return
        }

        Timber.d("Prayer alarm received for: $prayerType")

        // Get notification mode for this prayer
        val mode = settingsRepository.getPrayerNotificationMode(prayerType.name)
        val settings = settingsRepository.getCurrentSettings()

        when (mode) {
            PrayerNotificationMode.ATHAN -> {
                // Play athan
                val athanId = settingsRepository.getPrayerAthanId(prayerType.name)
                playAthan(context, prayerType, athanId, settings.athanMaxVolume)
            }
            PrayerNotificationMode.NOTIFICATION -> {
                // Show notification
                showNotification(context, prayerType, settings.prayerNotificationSound, settings.prayerNotificationVibrate)
            }
            PrayerNotificationMode.SILENT -> {
                // Do nothing
                Timber.d("$prayerType is set to silent, skipping")
            }
        }

        // Reschedule for tomorrow
        val scheduler = PrayerNotificationScheduler(context, settingsRepository)
        scheduler.scheduleNextDayNotification(prayerType)
    }

    private fun playAthan(context: Context, prayerType: PrayerType, athanId: String, maxVolume: Boolean) {
        Timber.d("Playing athan for $prayerType with athanId: $athanId via AthanService")

        // Use AthanService which handles:
        // - Foreground notification with stop button
        // - Flip-to-silence via accelerometer/proximity sensors
        // - Volume button press detection
        // - Proper lifecycle management
        AthanService.startAthan(context, athanId, prayerType)
    }

    private fun showNotification(context: Context, prayerType: PrayerType, sound: Boolean, vibrate: Boolean, isAthanPlaying: Boolean = false) {
        // Check notification permission on Android 13+
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

        createNotificationChannel(context)

        val settings = settingsRepository.getCurrentSettings()
        val isArabic = settings.appLanguage.code == "ar"

        val (title, text) = getPrayerNotificationContent(prayerType, isArabic)

        // Create intent to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            prayerType.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setOngoing(isAthanPlaying)  // Make ongoing while athan plays so user can't accidentally dismiss

        // Add stop action if athan is playing
        if (isAthanPlaying) {
            val stopIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = ACTION_STOP_ATHAN
                putExtra(EXTRA_PRAYER_TYPE, prayerType.name)
            }
            val stopPendingIntent = PendingIntent.getBroadcast(
                context,
                prayerType.ordinal + 100,  // Different request code
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_media_pause,
                if (isArabic) "إيقاف" else "Stop",
                stopPendingIntent
            )
            // Set delete intent to stop athan when notification is dismissed
            builder.setDeleteIntent(stopPendingIntent)
        }

        if (!sound) {
            builder.setSilent(true)
        }

        if (!vibrate) {
            builder.setVibrate(null)
        }

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(NOTIFICATION_ID_BASE + prayerType.ordinal, builder.build())

        Timber.d("Notification shown for $prayerType (athan playing: $isAthanPlaying)")
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Prayer Times"
            val descriptionText = "Prayer time notifications and athan"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getPrayerNotificationContent(prayerType: PrayerType, isArabic: Boolean): Pair<String, String> {
        return when (prayerType) {
            PrayerType.FAJR -> if (isArabic)
                Pair("صلاة الفجر", "حان وقت صلاة الفجر")
            else
                Pair("Fajr Prayer", "It's time for Fajr prayer")
            PrayerType.SUNRISE -> if (isArabic)
                Pair("الشروق", "وقت الشروق")
            else
                Pair("Sunrise", "Sunrise time")
            PrayerType.DHUHR -> if (isArabic)
                Pair("صلاة الظهر", "حان وقت صلاة الظهر")
            else
                Pair("Dhuhr Prayer", "It's time for Dhuhr prayer")
            PrayerType.ASR -> if (isArabic)
                Pair("صلاة العصر", "حان وقت صلاة العصر")
            else
                Pair("Asr Prayer", "It's time for Asr prayer")
            PrayerType.MAGHRIB -> if (isArabic)
                Pair("صلاة المغرب", "حان وقت صلاة المغرب")
            else
                Pair("Maghrib Prayer", "It's time for Maghrib prayer")
            PrayerType.ISHA -> if (isArabic)
                Pair("صلاة العشاء", "حان وقت صلاة العشاء")
            else
                Pair("Isha Prayer", "It's time for Isha prayer")
        }
    }
}
