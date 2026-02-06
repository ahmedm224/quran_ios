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
     * Check if exact alarms can be scheduled (Android 12+ requires permission)
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Pre-Android 12 doesn't need permission
        }
    }

    /**
     * Check and log alarm permission status
     */
    fun logAlarmPermissionStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val canSchedule = alarmManager.canScheduleExactAlarms()
            Timber.d("=== ALARM PERMISSION STATUS ===")
            Timber.d("Android version: ${Build.VERSION.SDK_INT}")
            Timber.d("canScheduleExactAlarms: $canSchedule")
            if (!canSchedule) {
                Timber.e("EXACT ALARM PERMISSION NOT GRANTED - Alarms may be unreliable!")
            }
        } else {
            Timber.d("=== ALARM PERMISSION STATUS ===")
            Timber.d("Android version: ${Build.VERSION.SDK_INT} (pre-S, no permission needed)")
        }
    }

    /**
     * Schedule notifications for all enabled prayer times.
     * Should be called when prayer times are fetched or settings change.
     */
    fun schedulePrayerNotifications(prayerTimes: PrayerTimes) {
        // Log permission status
        logAlarmPermissionStatus()

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

        // Schedule alarm - use setAlarmClock for better foreground service exemptions
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ - use setAlarmClock for better exemptions
                // This allows starting foreground services from the alarm receiver
                if (alarmManager.canScheduleExactAlarms()) {
                    // Create show intent for when user taps the alarm icon
                    val showIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                        action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
                        putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TYPE, prayerType.name)
                    }
                    val showPendingIntent = PendingIntent.getBroadcast(
                        context,
                        REQUEST_CODE_BASE + prayerType.ordinal + 100,
                        showIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val alarmInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, showPendingIntent)
                    alarmManager.setAlarmClock(alarmInfo, pendingIntent)
                    Timber.d("Scheduled ${prayerType.name} using setAlarmClock (Android 12+)")
                } else {
                    // Fallback to inexact alarm if permission not granted
                    Timber.w("Exact alarm permission not granted, using inexact alarm")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Android 5-11 - setAlarmClock also works here
                val showIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TYPE, prayerType.name)
                }
                val showPendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_BASE + prayerType.ordinal + 100,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val alarmInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, showPendingIntent)
                alarmManager.setAlarmClock(alarmInfo, pendingIntent)
                Timber.d("Scheduled ${prayerType.name} using setAlarmClock")
            } else {
                // Older Android versions
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }

            val delayMinutes = (triggerTimeMs - System.currentTimeMillis()) / 1000 / 60
            Timber.d("Scheduled ${prayerType.name} alarm in $delayMinutes minutes (at $notificationDateTime)")
        } catch (e: SecurityException) {
            Timber.e(e, "Failed to schedule alarm for ${prayerType.name}")
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

    /**
     * DEBUG: Schedule a test athan at a specific time chosen by user.
     * Uses FAJR prayer type for the test.
     */
    fun scheduleTestAthanAt(hour: Int, minute: Int, athanId: String, maxVolume: Boolean) {
        val today = LocalDate.now()
        var testTime = LocalDateTime.of(today, LocalTime.of(hour, minute))

        // If time has passed today, schedule for tomorrow
        if (testTime.isBefore(LocalDateTime.now())) {
            testTime = testTime.plusDays(1)
        }

        val triggerTimeMs = testTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // Use FAJR for test (request code 3000)
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TYPE, PrayerType.FAJR.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + PrayerType.FAJR.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    val showIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                        action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
                        putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TYPE, PrayerType.FAJR.name)
                    }
                    val showPendingIntent = PendingIntent.getBroadcast(
                        context,
                        REQUEST_CODE_BASE + PrayerType.FAJR.ordinal + 100,
                        showIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val alarmInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, showPendingIntent)
                    alarmManager.setAlarmClock(alarmInfo, pendingIntent)
                }
            } else {
                val showIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TYPE, PrayerType.FAJR.name)
                }
                val showPendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_BASE + PrayerType.FAJR.ordinal + 100,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, showPendingIntent)
                alarmManager.setAlarmClock(alarmInfo, pendingIntent)
            }

            Timber.d("=== TEST ATHAN SCHEDULED FOR %02d:%02d (in ${(triggerTimeMs - System.currentTimeMillis()) / 1000} seconds) ===", hour, minute)
        } catch (e: SecurityException) {
            Timber.e(e, "Failed to schedule test athan")
        }
    }
}
