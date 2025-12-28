package com.quranmedia.player.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.domain.model.PrayerTimes
import com.quranmedia.player.domain.model.PrayerType
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules prayer time notifications using AlarmManager for exact timing.
 * Uses setExactAndAllowWhileIdle to ensure alarms fire even in Doze mode.
 */
@Singleton
class PrayerNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val REQUEST_CODE_BASE = 3000
    }

    /**
     * Schedule notifications for all enabled prayer times.
     * Should be called when prayer times are fetched or settings change.
     */
    fun schedulePrayerNotifications(prayerTimes: PrayerTimes) {
        val settings = settingsRepository.getCurrentSettings()

        if (!settings.prayerNotificationEnabled) {
            Timber.d("Prayer notifications disabled, cancelling all")
            cancelAllNotifications()
            return
        }

        val minutesBefore = settings.prayerNotificationMinutesBefore

        // Schedule each prayer notification
        if (settings.notifyFajr) {
            scheduleNotification(PrayerType.FAJR, prayerTimes.fajr, minutesBefore)
        } else {
            cancelNotification(PrayerType.FAJR)
        }

        if (settings.notifyDhuhr) {
            scheduleNotification(PrayerType.DHUHR, prayerTimes.dhuhr, minutesBefore)
        } else {
            cancelNotification(PrayerType.DHUHR)
        }

        if (settings.notifyAsr) {
            scheduleNotification(PrayerType.ASR, prayerTimes.asr, minutesBefore)
        } else {
            cancelNotification(PrayerType.ASR)
        }

        if (settings.notifyMaghrib) {
            scheduleNotification(PrayerType.MAGHRIB, prayerTimes.maghrib, minutesBefore)
        } else {
            cancelNotification(PrayerType.MAGHRIB)
        }

        if (settings.notifyIsha) {
            scheduleNotification(PrayerType.ISHA, prayerTimes.isha, minutesBefore)
        } else {
            cancelNotification(PrayerType.ISHA)
        }

        Timber.d("Prayer notifications scheduled with exact alarms")
    }

    /**
     * Schedule a notification for a specific prayer time using exact alarm.
     */
    private fun scheduleNotification(
        prayerType: PrayerType,
        prayerTime: LocalTime,
        minutesBefore: Int
    ) {
        val now = LocalDateTime.now()
        val today = LocalDate.now()

        // Calculate notification time (prayer time minus minutesBefore)
        val notificationTime = prayerTime.minusMinutes(minutesBefore.toLong())
        var notificationDateTime = LocalDateTime.of(today, notificationTime)

        // If the notification time has passed, schedule for tomorrow
        if (notificationDateTime.isBefore(now) || notificationDateTime.isEqual(now)) {
            notificationDateTime = notificationDateTime.plusDays(1)
            Timber.d("${prayerType.name} notification time passed, scheduling for tomorrow")
        }

        // Convert to epoch millis
        val triggerTimeMs = notificationDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // Create pending intent
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TYPE, prayerType.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + prayerType.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule exact alarm that works in Doze mode
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ requires checking if exact alarms are allowed
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm if permission not granted
                    Timber.w("Exact alarm permission not granted, using inexact alarm")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                }
            } else {
                // Android 6-11
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }

            val delayMinutes = (triggerTimeMs - System.currentTimeMillis()) / 1000 / 60
            Timber.d("Scheduled ${prayerType.name} exact alarm in $delayMinutes minutes (at $notificationDateTime)")
        } catch (e: SecurityException) {
            Timber.e(e, "Failed to schedule exact alarm for ${prayerType.name}")
        }
    }

    /**
     * Schedule notification for next day (called after alarm fires).
     */
    fun scheduleNextDayNotification(prayerType: PrayerType) {
        // This will be called by the receiver to reschedule for tomorrow
        // The actual prayer time needs to be fetched from the repository
        Timber.d("Rescheduling ${prayerType.name} for next day")
        // Note: The PrayerTimesViewModel will call schedulePrayerNotifications
        // when it refreshes prayer times at midnight or on app start
    }

    /**
     * Cancel notification for a specific prayer type.
     */
    fun cancelNotification(prayerType: PrayerType) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + prayerType.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Timber.d("Cancelled ${prayerType.name} alarm")
    }

    /**
     * Cancel all prayer notifications.
     */
    fun cancelAllNotifications() {
        PrayerType.entries.forEach { prayerType ->
            cancelNotification(prayerType)
        }
        Timber.d("Cancelled all prayer alarms")
    }
}
