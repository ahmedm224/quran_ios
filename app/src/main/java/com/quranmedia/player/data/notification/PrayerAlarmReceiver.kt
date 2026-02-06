package com.quranmedia.player.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.quranmedia.player.R
import com.quranmedia.player.data.repository.AthanRepository
import com.quranmedia.player.data.repository.PrayerNotificationMode
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.domain.model.PrayerType
import com.quranmedia.player.media.service.AthanService
import com.quranmedia.player.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.time.LocalDate
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
    lateinit var athanRepository: AthanRepository

    @Inject
    lateinit var prayerTimesRepository: com.quranmedia.player.domain.repository.PrayerTimesRepository

    @Inject
    lateinit var prayerNotificationScheduler: PrayerNotificationScheduler

    companion object {
        const val ACTION_PRAYER_ALARM = "com.quranmedia.player.PRAYER_ALARM"
        const val ACTION_STOP_ATHAN = "com.quranmedia.player.STOP_ATHAN"
        const val EXTRA_PRAYER_TYPE = "prayer_type"
        private const val CHANNEL_ID = "prayer_notifications"
        private const val CHANNEL_ID_ATHAN = "athan_channel"
        private const val NOTIFICATION_ID_BASE = 2000
        private const val WAKELOCK_TAG = "AlFurqan:AthanWakeLock"

        // Static MediaPlayer for athan playback
        @Volatile
        private var mediaPlayer: MediaPlayer? = null

        // WakeLock to keep CPU running during playback
        @Volatile
        private var wakeLock: PowerManager.WakeLock? = null

        // Flip-to-silence sensors
        @Volatile
        private var sensorManager: SensorManager? = null
        @Volatile
        private var sensorsRegistered = false
        private var lastZ = 0f
        private var mainHandler: Handler? = null

        // Store context and notification ID for flip-to-silence dismissal
        @Volatile
        private var currentContext: Context? = null
        @Volatile
        private var currentNotificationId: Int? = null

        // Sensor listener for flip detection
        private val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                // Check if athan is playing - use mediaPlayer reference existence as backup
                val player = mediaPlayer
                if (player == null) return

                // Allow slight timing window - check if player exists even if not yet playing
                try {
                    if (!player.isPlaying) return
                } catch (e: IllegalStateException) {
                    // Player in invalid state
                    return
                }

                when (event?.sensor?.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        val z = event.values[2]
                        // Phone is face down when Z axis is significantly negative
                        // Z < -7 means gravity is pulling "up" relative to screen = face down
                        // Using -7 instead of -8 for better sensitivity
                        if (z < -7f && lastZ >= -7f) {
                            Timber.d("Phone flipped face down (z=$z, lastZ=$lastZ) - stopping athan")
                            stopFallbackAthanAndDismissNotification()
                        }
                        lastZ = z
                    }
                    Sensor.TYPE_PROXIMITY -> {
                        val distance = event.values[0]
                        val maxRange = event.sensor.maximumRange
                        // Near = face down on surface or in pocket
                        val isNear = distance < maxRange * 0.5f
                        if (isNear && kotlin.math.abs(lastZ) < 3f) {
                            // Phone is flat and proximity sensor triggered = lying face down
                            Timber.d("Proximity triggered while flat (dist=$distance, lastZ=$lastZ) - stopping athan")
                            stopFallbackAthanAndDismissNotification()
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        private fun stopFallbackAthanAndDismissNotification() {
            stopFallbackAthan()
            // Dismiss notification
            currentContext?.let { ctx ->
                currentNotificationId?.let { id ->
                    try {
                        val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(id)
                        Timber.d("Notification $id dismissed via flip-to-silence")
                    } catch (e: Exception) {
                        Timber.e(e, "Error dismissing notification")
                    }
                }
            }
        }

        private fun registerFlipToSilence(context: Context) {
            if (sensorsRegistered) {
                Timber.d("Flip-to-silence sensors already registered")
                return
            }

            try {
                sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                mainHandler = Handler(Looper.getMainLooper())

                val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

                if (accelerometer == null) {
                    Timber.w("Accelerometer sensor not available on this device")
                } else {
                    val registered = sensorManager?.registerListener(
                        sensorListener,
                        accelerometer,
                        SensorManager.SENSOR_DELAY_UI,  // Faster than NORMAL for quicker flip detection
                        mainHandler
                    )
                    Timber.d("Accelerometer registered: $registered")
                }

                if (proximitySensor == null) {
                    Timber.w("Proximity sensor not available on this device")
                } else {
                    val registered = sensorManager?.registerListener(
                        sensorListener,
                        proximitySensor,
                        SensorManager.SENSOR_DELAY_UI,
                        mainHandler
                    )
                    Timber.d("Proximity sensor registered: $registered")
                }

                sensorsRegistered = true
                Timber.d("Flip-to-silence sensors registered for athan")
            } catch (e: Exception) {
                Timber.e(e, "Error registering flip-to-silence sensors")
            }
        }

        private fun unregisterFlipToSilence() {
            if (!sensorsRegistered) return

            try {
                sensorManager?.unregisterListener(sensorListener)
                sensorsRegistered = false
                sensorManager = null
                mainHandler = null
                Timber.d("Flip-to-silence sensors unregistered")
            } catch (e: Exception) {
                Timber.e(e, "Error unregistering flip-to-silence sensors")
            }
        }

        fun stopFallbackAthan() {
            try {
                Timber.d("stopFallbackAthan called")

                // Unregister flip-to-silence sensors first
                unregisterFlipToSilence()

                mediaPlayer?.let {
                    try {
                        if (it.isPlaying) {
                            Timber.d("Stopping MediaPlayer")
                            it.stop()
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error stopping MediaPlayer")
                    }
                    try {
                        it.release()
                        Timber.d("MediaPlayer released")
                    } catch (e: Exception) {
                        Timber.e(e, "Error releasing MediaPlayer")
                    }
                }
                mediaPlayer = null

                // Release wake lock
                wakeLock?.let {
                    if (it.isHeld) {
                        it.release()
                        Timber.d("WakeLock released")
                    }
                }
                wakeLock = null

                // Clear context and notification ID references
                currentContext = null
                currentNotificationId = null

                Timber.d("Athan stopped and resources released")
            } catch (e: Exception) {
                Timber.e(e, "Error stopping athan")
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Handle stop athan action
        if (intent.action == ACTION_STOP_ATHAN) {
            Timber.d("Stop athan action received")
            // Stop both AthanService and fallback MediaPlayer
            AthanService.stopAthan(context)
            stopFallbackAthan()
            // Also dismiss the notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val prayerTypeName = intent.getStringExtra(EXTRA_PRAYER_TYPE)
            if (prayerTypeName != null) {
                try {
                    val prayerType = PrayerType.valueOf(prayerTypeName)
                    notificationManager.cancel(NOTIFICATION_ID_BASE + prayerType.ordinal)
                } catch (e: Exception) {
                    Timber.e(e, "Error cancelling notification")
                }
            }
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
                // Play athan using notification sound (more reliable than foreground service on Android 12+)
                val athanId = settingsRepository.getPrayerAthanId(prayerType.name)
                showAthanNotification(context, prayerType, athanId, settings.athanMaxVolume)
            }
            PrayerNotificationMode.NOTIFICATION -> {
                // Show notification only
                showNotification(context, prayerType, settings.prayerNotificationSound, settings.prayerNotificationVibrate)
            }
            PrayerNotificationMode.SILENT -> {
                // Do nothing
                Timber.d("$prayerType is set to silent, skipping")
            }
        }

        // Reschedule all prayer notifications (this will schedule for tomorrow if time has passed)
        rescheduleNotifications()
    }

    /**
     * Reschedule all prayer notifications after an alarm fires.
     * This ensures the next day's alarms are scheduled.
     */
    private fun rescheduleNotifications() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val location = prayerTimesRepository.getSavedLocationSync()
                if (location == null) {
                    Timber.d("No saved location, cannot reschedule prayer alarms")
                    return@launch
                }

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
                            Timber.d("Prayer alarms rescheduled after alarm fired")
                        }
                    }
                    is com.quranmedia.player.domain.util.Resource.Error -> {
                        Timber.e("Failed to reschedule prayer alarms: ${result.message}")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Timber.e(e, "Error rescheduling prayer alarms")
            }
        }
    }

    /**
     * Show notification and play athan audio.
     * Uses MediaPlayer directly since Android 12+ restricts foreground service starts from background.
     * This approach is more reliable across all Android versions.
     */
    private fun showAthanNotification(context: Context, prayerType: PrayerType, athanId: String, maxVolume: Boolean) {
        Timber.d("Showing athan notification for $prayerType with athanId: $athanId")

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

        val settings = settingsRepository.getCurrentSettings()
        val isArabic = settings.appLanguage.code == "ar"
        val (title, text) = getPrayerNotificationContent(prayerType, isArabic)

        // Create athan notification channel
        createAthanNotificationChannel(context)

        // Create full-screen intent for when device is locked (like alarm apps)
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "prayer_times")
            putExtra("prayer_type", prayerType.name)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            prayerType.ordinal + 200,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create intent to stop athan
        val stopIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_STOP_ATHAN
            putExtra(EXTRA_PRAYER_TYPE, prayerType.name)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            prayerType.ordinal + 100,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopText = if (isArabic) "إيقاف" else "Stop"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ATHAN)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, stopText, stopPendingIntent)
            .setDeleteIntent(stopPendingIntent)
            .setSilent(true)  // No notification sound - we play athan audio directly

        // Show notification
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(NOTIFICATION_ID_BASE + prayerType.ordinal, builder.build())
        Timber.d("Athan notification shown for $prayerType")

        // Play athan audio directly using MediaPlayer
        // This is more reliable than trying to start a foreground service on Android 12+
        // because startForegroundService() doesn't throw immediately - the exception
        // happens inside the service when calling startForeground(), which we can't catch here
        Timber.d("Playing athan directly with MediaPlayer for $prayerType")
        playAthanWithMediaPlayer(context, athanId, maxVolume, prayerType)
    }

    /**
     * Play athan directly using MediaPlayer with WakeLock.
     * Uses synchronous operations to ensure playback starts before receiver is killed.
     */
    private fun playAthanWithMediaPlayer(context: Context, athanId: String, maxVolume: Boolean, prayerType: PrayerType? = null) {
        Timber.d("=== playAthanWithMediaPlayer START === athanId: $athanId")

        // FIRST: Acquire wake lock immediately to keep CPU running
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKELOCK_TAG
            ).apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 1000L) // 10 minutes max (athan duration)
            }
            Timber.d("WakeLock acquired")
        } catch (e: Exception) {
            Timber.e(e, "Failed to acquire WakeLock")
        }

        // Use goAsync to extend receiver lifetime
        val pendingResult = goAsync()

        // Run on a background thread but use synchronous operations
        Thread {
            try {
                Timber.d("Background thread started for athan playback")

                // Get athan file path
                var athanPath: String? = null

                // Use runBlocking to call suspend functions synchronously
                kotlinx.coroutines.runBlocking {
                    athanPath = athanRepository.getAthanLocalPath(athanId)

                    if (athanPath == null) {
                        Timber.d("Athan file not found for $athanId, ensuring default is available")
                        athanRepository.ensureDefaultAthanAvailable()

                        // Try default athan if requested one isn't available
                        athanPath = if (athanId != AthanRepository.DEFAULT_ATHAN_ID) {
                            athanRepository.getAthanLocalPath(AthanRepository.DEFAULT_ATHAN_ID)
                        } else {
                            athanRepository.getAthanLocalPath(athanId)
                        }
                    }
                }

                if (athanPath == null) {
                    Timber.e("No athan file available - cannot play")
                    stopFallbackAthan()
                    pendingResult.finish()
                    return@Thread
                }

                // Verify file exists
                val athanFile = File(athanPath!!)
                if (!athanFile.exists()) {
                    Timber.e("Athan file does not exist: $athanPath")
                    stopFallbackAthan()
                    pendingResult.finish()
                    return@Thread
                }

                Timber.d("Athan file verified: $athanPath (size: ${athanFile.length()} bytes)")

                // Stop any existing playback
                stopFallbackAthan()

                // Maximize volume if requested
                if (maxVolume) {
                    try {
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
                        Timber.d("Alarm volume: was $currentVol, now set to max: $maxVol")
                    } catch (e: Exception) {
                        Timber.e(e, "Error setting volume")
                    }
                }

                // Notification manager for dismissing on completion
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notificationId = if (prayerType != null) NOTIFICATION_ID_BASE + prayerType.ordinal else null

                // Create MediaPlayer with wake mode
                Timber.d("Creating MediaPlayer...")
                val player = MediaPlayer()

                try {
                    // Set audio attributes for alarm stream (plays over silent mode)
                    player.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                            .build()
                    )

                    // Set wake mode to keep playing even when screen off
                    player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)

                    // Set data source
                    player.setDataSource(athanPath)
                    Timber.d("DataSource set: $athanPath")

                    // Set completion listener
                    player.setOnCompletionListener {
                        Timber.d("=== Athan playback COMPLETED ===")
                        stopFallbackAthan()
                        notificationId?.let { id ->
                            notificationManager.cancel(id)
                            Timber.d("Notification dismissed")
                        }
                    }

                    // Set error listener
                    player.setOnErrorListener { _, what, extra ->
                        Timber.e("=== Athan playback ERROR === what=$what, extra=$extra")
                        stopFallbackAthan()
                        notificationId?.let { id ->
                            notificationManager.cancel(id)
                        }
                        true
                    }

                    // Store reference BEFORE prepare
                    mediaPlayer = player

                    // Store context and notification ID for flip-to-silence dismissal
                    currentContext = context.applicationContext
                    currentNotificationId = notificationId

                    // SYNCHRONOUS prepare - blocks until ready
                    Timber.d("Calling prepare() synchronously...")
                    player.prepare()
                    Timber.d("MediaPlayer prepared successfully")

                    // Register flip-to-silence sensors BEFORE starting playback
                    // to ensure we don't miss any sensor events
                    val settings = settingsRepository.getCurrentSettings()
                    if (settings.flipToSilenceAthan) {
                        Timber.d("Flip-to-silence is enabled, registering sensors")
                        // Reset lastZ to current position to prevent false triggers
                        lastZ = 0f
                        registerFlipToSilence(context)
                    } else {
                        Timber.d("Flip-to-silence is disabled")
                    }

                    // Start playback immediately
                    player.start()
                    Timber.d("=== Athan playback STARTED === isPlaying: ${player.isPlaying}")

                    // Finish the pending result - MediaPlayer will continue playing
                    // because it has its own wake lock via setWakeMode
                    pendingResult.finish()
                    Timber.d("PendingResult finished, MediaPlayer should continue playing")

                } catch (e: Exception) {
                    Timber.e(e, "=== Error during MediaPlayer setup/playback ===")
                    try {
                        player.release()
                    } catch (releaseError: Exception) {
                        Timber.e(releaseError, "Error releasing player after failure")
                    }
                    mediaPlayer = null
                    stopFallbackAthan()
                    pendingResult.finish()
                }

            } catch (e: Exception) {
                Timber.e(e, "=== Fatal error in playAthanWithMediaPlayer ===")
                stopFallbackAthan()
                pendingResult.finish()
            }
        }.start()
    }

    /**
     * Create notification channel for athan.
     * Sound is handled by AthanService, not the notification.
     */
    private fun createAthanNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Athan"
            val descriptionText = "Prayer call (Athan) notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHANNEL_ID_ATHAN, name, importance).apply {
                description = descriptionText
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(null, null)  // No sound from notification - AthanService plays audio
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Timber.d("Athan notification channel created")
        }
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
