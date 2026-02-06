package com.quranmedia.player.wear.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.quranmedia.player.wear.R
import com.quranmedia.player.wear.domain.model.PrayerType
import com.quranmedia.player.wear.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Receives prayer time alarms and shows notification with vibration.
 */
@AndroidEntryPoint
class PrayerAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationScheduler: PrayerNotificationScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val CHANNEL_ID = "prayer_times_channel"
        private const val NOTIFICATION_ID_BASE = 5000
    }

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Prayer alarm received: ${intent.action}")

        val prayerTypeOrdinal = intent.getIntExtra(PrayerNotificationScheduler.EXTRA_PRAYER_TYPE, -1)
        val prayerNameAr = intent.getStringExtra(PrayerNotificationScheduler.EXTRA_PRAYER_NAME_AR) ?: ""
        val prayerNameEn = intent.getStringExtra(PrayerNotificationScheduler.EXTRA_PRAYER_NAME_EN) ?: ""
        val prayerTime = intent.getStringExtra(PrayerNotificationScheduler.EXTRA_PRAYER_TIME) ?: ""

        if (prayerTypeOrdinal < 0) {
            Timber.w("Invalid prayer type in alarm")
            return
        }

        val prayerType = PrayerType.fromOrdinal(prayerTypeOrdinal)

        // Show notification
        showPrayerNotification(context, prayerType, prayerNameAr, prayerNameEn, prayerTime)

        // Vibrate
        performVibration(context)

        // Reschedule next day's notifications after Isha
        if (prayerType == PrayerType.ISHA) {
            scope.launch {
                try {
                    notificationScheduler.scheduleAllPrayerNotifications()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to reschedule notifications")
                }
            }
        }
    }

    private fun showPrayerNotification(
        context: Context,
        prayerType: PrayerType,
        nameAr: String,
        nameEn: String,
        time: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (required for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Prayer Times",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Prayer time notifications"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500, 200, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open the app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "prayer_times")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_prayer_notification)
            .setContentTitle("حان وقت الصلاة")
            .setContentText("$nameAr - $nameEn ($time)")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("حان وقت صلاة $nameAr\n$nameEn Prayer Time: $time"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500, 200, 500))
            .build()

        val notificationId = NOTIFICATION_ID_BASE + prayerType.ordinal_
        notificationManager.notify(notificationId, notification)

        Timber.d("Showed notification for $nameEn")
    }

    private fun performVibration(context: Context) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let { vib ->
            // Longer vibration pattern for prayer notification (3 seconds total)
            // Pattern: vibrate 500ms, pause 200ms, vibrate 500ms, pause 200ms, vibrate 500ms, pause 200ms, vibrate 500ms
            val pattern = longArrayOf(0, 500, 200, 500, 200, 500, 200, 500)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255)
                vib.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, -1)
            }

            Timber.d("Performed prayer vibration")
        }
    }
}
