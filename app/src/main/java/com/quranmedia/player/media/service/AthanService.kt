package com.quranmedia.player.media.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import kotlin.math.abs
import com.quranmedia.player.R
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.data.repository.AthanRepository
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.domain.model.PrayerType
import com.quranmedia.player.media.player.AthanPlayer
import com.quranmedia.player.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground service for playing Athan (call to prayer).
 * Started when a prayer time notification triggers.
 * Shows a notification while playing with a stop button.
 */
@AndroidEntryPoint
class AthanService : Service() {

    @Inject
    lateinit var athanPlayer: AthanPlayer

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var athanRepository: AthanRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Sensors for flip-to-silence
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var proximitySensor: Sensor? = null
    private var isFlipToSilenceEnabled = true
    private var isPhoneFaceDown = false
    private var lastZ = 0f
    private var sensorsRegistered = false  // Track if sensors are currently registered
    private var isPlayingAthan = false  // Track if athan is currently playing

    // Media button receiver for volume buttons
    private val mediaButtonReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
                val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_VOLUME_UP,
                        KeyEvent.KEYCODE_VOLUME_DOWN,
                        KeyEvent.KEYCODE_HEADSETHOOK -> {
                            if (isFlipToSilenceEnabled && athanPlayer.isPlaying()) {
                                Timber.d("Volume/headset button pressed - stopping athan")
                                stopAthanPlayback()
                            }
                        }
                    }
                }
            }
        }
    }

    // Sensor listener for flip detection
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (!isFlipToSilenceEnabled || !athanPlayer.isPlaying()) return

            when (event?.sensor?.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val z = event.values[2]
                    // Phone is face down when Z axis is significantly negative
                    // Z < -8 means gravity is pulling "up" relative to screen = face down
                    if (z < -8f && lastZ >= -8f) {
                        Timber.d("Phone flipped face down - stopping athan")
                        stopAthanPlayback()
                    }
                    lastZ = z
                }
                Sensor.TYPE_PROXIMITY -> {
                    val distance = event.values[0]
                    val maxRange = event.sensor.maximumRange
                    // Near = face down on surface or in pocket
                    isPhoneFaceDown = distance < maxRange * 0.5f
                    if (isPhoneFaceDown && abs(lastZ) < 3f) {
                        // Phone is flat and proximity sensor triggered = lying face down
                        Timber.d("Proximity sensor triggered while flat - stopping athan")
                        stopAthanPlayback()
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    companion object {
        const val CHANNEL_ID = "athan_playback_channel"
        const val NOTIFICATION_ID = 3000

        const val ACTION_PLAY = "com.quranmedia.player.ACTION_PLAY_ATHAN"
        const val ACTION_STOP = "com.quranmedia.player.ACTION_STOP_ATHAN"

        const val EXTRA_ATHAN_ID = "athan_id"
        const val EXTRA_PRAYER_TYPE = "prayer_type"

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

        /**
         * Start the Athan service to play athan
         */
        fun startAthan(context: Context, athanId: String, prayerType: PrayerType) {
            val intent = Intent(context, AthanService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_ATHAN_ID, athanId)
                putExtra(EXTRA_PRAYER_TYPE, prayerType.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop the Athan service
         */
        fun stopAthan(context: Context) {
            val intent = Intent(context, AthanService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("AthanService created")
        createNotificationChannel()

        // Initialize sensor manager for flip-to-silence
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val athanId = intent.getStringExtra(EXTRA_ATHAN_ID)
                val prayerTypeName = intent.getStringExtra(EXTRA_PRAYER_TYPE)

                if (athanId != null && prayerTypeName != null) {
                    val prayerType = try {
                        PrayerType.valueOf(prayerTypeName)
                    } catch (e: IllegalArgumentException) {
                        PrayerType.FAJR
                    }
                    playAthan(athanId, prayerType)
                } else {
                    Timber.e("Missing athan ID or prayer type")
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopAthanPlayback()
            }
            else -> {
                Timber.w("Unknown action: ${intent?.action}")
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun playAthan(athanId: String, prayerType: PrayerType) {
        // Prevent multiple simultaneous playback attempts
        if (isPlayingAthan) {
            Timber.d("Athan already playing, ignoring duplicate playback request")
            return
        }

        Timber.d("Playing athan $athanId for $prayerType")
        isPlayingAthan = true

        val settings = settingsRepository.getCurrentSettings()
        isFlipToSilenceEnabled = settings.flipToSilenceAthan

        // Register sensors for flip-to-silence
        if (isFlipToSilenceEnabled) {
            registerFlipToSilence()
        }

        // Start foreground with notification
        val notification = createPlayingNotification(prayerType, settings.appLanguage)
        startForeground(NOTIFICATION_ID, notification)

        // Get local file path - athan MUST be downloaded for prayer notifications
        serviceScope.launch {
            val localPath = athanRepository.getAthanLocalPath(athanId)
            if (localPath != null) {
                Timber.d("Playing athan from local file: $localPath")
                athanPlayer.playAthan(
                    path = localPath,
                    athanId = athanId,
                    maximizeVolume = settings.athanMaxVolume
                ) {
                    // On completion, stop the service
                    Timber.d("Athan playback completed")
                    isPlayingAthan = false
                    unregisterFlipToSilence()
                    stopSelf()
                }
            } else {
                // Athan not downloaded - should not happen, but fallback to just notification
                Timber.e("Athan $athanId not downloaded! Cannot play.")
                // Keep notification visible for a few seconds then stop
                kotlinx.coroutines.delay(3000)
                isPlayingAthan = false
                unregisterFlipToSilence()
                stopSelf()
            }
        }
    }

    private fun registerFlipToSilence() {
        // Prevent multiple registrations
        if (sensorsRegistered) {
            Timber.d("Flip-to-silence sensors already registered, skipping")
            return
        }

        try {
            // Register accelerometer for flip detection
            accelerometer?.let {
                sensorManager?.registerListener(
                    sensorListener,
                    it,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }

            // Register proximity sensor
            proximitySensor?.let {
                sensorManager?.registerListener(
                    sensorListener,
                    it,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }

            // Register media button receiver
            val filter = IntentFilter(Intent.ACTION_MEDIA_BUTTON)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(mediaButtonReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(mediaButtonReceiver, filter)
            }

            sensorsRegistered = true
            Timber.d("Flip-to-silence sensors registered")
        } catch (e: Exception) {
            Timber.e(e, "Error registering flip-to-silence")
        }
    }

    private fun unregisterFlipToSilence() {
        // Only unregister if currently registered
        if (!sensorsRegistered) {
            Timber.d("Flip-to-silence sensors not registered, skipping unregister")
            return
        }

        try {
            sensorManager?.unregisterListener(sensorListener)
            try {
                unregisterReceiver(mediaButtonReceiver)
            } catch (e: IllegalArgumentException) {
                // Receiver not registered
            }
            sensorsRegistered = false
            Timber.d("Flip-to-silence sensors unregistered")
        } catch (e: Exception) {
            Timber.e(e, "Error unregistering flip-to-silence")
        }
    }

    private fun stopAthanPlayback() {
        Timber.d("Stopping athan playback")
        isPlayingAthan = false
        unregisterFlipToSilence()
        athanPlayer.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createPlayingNotification(prayerType: PrayerType, language: AppLanguage): Notification {
        // Intent to open app
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "prayer_times")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to stop athan
        val stopIntent = Intent(this, AthanService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prayerName = if (language == AppLanguage.ARABIC) {
            arabicPrayerNames[prayerType] ?: prayerType.nameArabic
        } else {
            englishPrayerNames[prayerType] ?: prayerType.nameEnglish
        }

        val title = if (language == AppLanguage.ARABIC) "أذان $prayerName" else "$prayerName Athan"
        val message = if (language == AppLanguage.ARABIC) "حان وقت صلاة $prayerName" else "Time for $prayerName prayer"
        val stopText = if (language == AppLanguage.ARABIC) "إيقاف" else "Stop"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                stopText,
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Athan Playback"
            val descriptionText = "Shows while Athan is playing"
            val importance = NotificationManager.IMPORTANCE_LOW  // Normal notification, no sound/vibration from channel
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                setSound(null, null)  // No sound from notification - athan audio handles sound
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        unregisterFlipToSilence()
        serviceScope.cancel()
        athanPlayer.stop()
        Timber.d("AthanService destroyed")
    }
}
