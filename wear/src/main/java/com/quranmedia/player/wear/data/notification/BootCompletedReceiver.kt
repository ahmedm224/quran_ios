package com.quranmedia.player.wear.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Reschedules prayer notifications after device boot.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationScheduler: PrayerNotificationScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Timber.d("Device booted, rescheduling prayer notifications")

            scope.launch {
                try {
                    notificationScheduler.scheduleAllPrayerNotifications()
                    Timber.d("Prayer notifications rescheduled after boot")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to reschedule prayer notifications after boot")
                }
            }
        }
    }
}
