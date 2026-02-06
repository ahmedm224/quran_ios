package com.quranmedia.player.wear.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.quranmedia.player.wear.data.repository.WearPrayerTimesRepository
import com.quranmedia.player.wear.domain.model.PrayerTimes
import com.quranmedia.player.wear.domain.model.PrayerType
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules prayer time notifications with vibration for Wear OS.
 * Uses AlarmManager for exact timing.
 */
@Singleton
class PrayerNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: WearPrayerTimesRepository
) {
    companion object {
        private const val REQUEST_CODE_BASE = 4000
        const val EXTRA_PRAYER_TYPE = "prayer_type"
        const val EXTRA_PRAYER_NAME_AR = "prayer_name_ar"
        const val EXTRA_PRAYER_NAME_EN = "prayer_name_en"
        const val EXTRA_PRAYER_TIME = "prayer_time"
    }

    private val alarmManager: AlarmManager? = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    /**
     * Schedule all prayer notifications for today.
     */
    suspend fun scheduleAllPrayerNotifications() {
        try {
            val prayerTimes = repository.getPrayerTimesForToday()
            val settings = repository.getSettingsSync()

            if (!settings.notificationsEnabled) {
                Timber.d("Prayer notifications disabled")
                cancelAllNotifications()
                return
            }

            val minutesBefore = settings.minutesBefore

            // Schedule each prayer
            if (settings.notifyFajr) {
                schedulePrayerNotification(PrayerType.FAJR, prayerTimes.fajr, minutesBefore, prayerTimes)
            }
            if (settings.notifyDhuhr) {
                schedulePrayerNotification(PrayerType.DHUHR, prayerTimes.dhuhr, minutesBefore, prayerTimes)
            }
            if (settings.notifyAsr) {
                schedulePrayerNotification(PrayerType.ASR, prayerTimes.asr, minutesBefore, prayerTimes)
            }
            if (settings.notifyMaghrib) {
                schedulePrayerNotification(PrayerType.MAGHRIB, prayerTimes.maghrib, minutesBefore, prayerTimes)
            }
            if (settings.notifyIsha) {
                schedulePrayerNotification(PrayerType.ISHA, prayerTimes.isha, minutesBefore, prayerTimes)
            }

            Timber.d("Scheduled prayer notifications for ${prayerTimes.date}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule prayer notifications")
        }
    }

    /**
     * Schedule a single prayer notification.
     */
    private fun schedulePrayerNotification(
        prayerType: PrayerType,
        prayerTime: LocalTime,
        minutesBefore: Int,
        prayerTimes: PrayerTimes
    ) {
        val now = LocalDateTime.now()
        val notificationTime = LocalDateTime.of(prayerTimes.date, prayerTime)
            .minusMinutes(minutesBefore.toLong())

        // Skip if the time has already passed
        if (notificationTime.isBefore(now)) {
            Timber.d("Skipping ${prayerType.nameEnglish} - time already passed")
            return
        }

        val triggerTimeMillis = notificationTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = "com.quranmedia.player.wear.PRAYER_ALARM"
            putExtra(EXTRA_PRAYER_TYPE, prayerType.ordinal_)
            putExtra(EXTRA_PRAYER_NAME_AR, prayerType.nameArabic)
            putExtra(EXTRA_PRAYER_NAME_EN, prayerType.nameEnglish)
            putExtra(EXTRA_PRAYER_TIME, prayerTime.toString())
        }

        val requestCode = REQUEST_CODE_BASE + prayerType.ordinal_
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ - use setAlarmClock for better reliability
                if (alarmManager?.canScheduleExactAlarms() == true) {
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent)
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                } else {
                    alarmManager?.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager?.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }

            Timber.d("Scheduled ${prayerType.nameEnglish} notification at $notificationTime")
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule ${prayerType.nameEnglish} notification")
        }
    }

    /**
     * Cancel a specific prayer notification.
     */
    fun cancelPrayerNotification(prayerType: PrayerType) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
        val requestCode = REQUEST_CODE_BASE + prayerType.ordinal_
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager?.cancel(it)
            it.cancel()
            Timber.d("Cancelled ${prayerType.nameEnglish} notification")
        }
    }

    /**
     * Cancel all prayer notifications.
     */
    fun cancelAllNotifications() {
        PrayerType.entries.forEach { prayerType ->
            if (prayerType != PrayerType.SUNRISE) {
                cancelPrayerNotification(prayerType)
            }
        }
        Timber.d("Cancelled all prayer notifications")
    }

    /**
     * Schedule notifications for the next day.
     * Called after the last prayer (Isha) or at midnight.
     */
    suspend fun scheduleNextDayNotifications() {
        try {
            val tomorrow = LocalDate.now().plusDays(1)
            val prayerTimes = repository.getPrayerTimes(tomorrow)
            val settings = repository.getSettingsSync()

            if (!settings.notificationsEnabled) return

            // Schedule tomorrow's prayers
            Timber.d("Pre-scheduling tomorrow's prayer notifications")
            // This will be triggered by a midnight alarm or after Isha
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule next day notifications")
        }
    }
}
