package com.quranmedia.player.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.domain.repository.PrayerTimesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

/**
 * Receiver that reschedules prayer alarms after device boot.
 * This ensures prayer notifications continue to work after device restart.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var prayerTimesRepository: PrayerTimesRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var prayerNotificationScheduler: PrayerNotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Timber.d("Boot completed, rescheduling prayer alarms")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get saved location
                val location = prayerTimesRepository.getSavedLocationSync()
                if (location == null) {
                    Timber.d("No saved location, skipping prayer alarm reschedule")
                    pendingResult.finish()
                    return@launch
                }

                // Fetch prayer times for today
                val method = prayerTimesRepository.getSavedCalculationMethod()
                val settings = settingsRepository.getCurrentSettings()
                val asrMethod = com.quranmedia.player.domain.model.AsrJuristicMethod.fromId(settings.asrJuristicMethod)

                val result = prayerTimesRepository.getPrayerTimes(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    date = LocalDate.now(),
                    method = method,
                    asrMethod = asrMethod
                )

                when (result) {
                    is com.quranmedia.player.domain.util.Resource.Success -> {
                        result.data?.let { prayerTimes ->
                            prayerNotificationScheduler.schedulePrayerNotifications(prayerTimes)
                            Timber.d("Prayer alarms rescheduled after boot")
                        }
                    }
                    is com.quranmedia.player.domain.util.Resource.Error -> {
                        Timber.e("Failed to fetch prayer times after boot: ${result.message}")
                    }
                    is com.quranmedia.player.domain.util.Resource.Loading -> {
                        // Ignore
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error rescheduling prayer alarms after boot")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
