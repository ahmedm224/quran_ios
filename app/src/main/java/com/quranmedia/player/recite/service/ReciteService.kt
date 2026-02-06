package com.quranmedia.player.recite.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.quranmedia.player.R
import com.quranmedia.player.presentation.MainActivity
import timber.log.Timber

/**
 * Foreground service for Recite feature to support screen-off recording.
 *
 * This service:
 * - Runs as a foreground service with a notification
 * - Acquires a wake lock to keep the CPU active
 * - Allows audio recording to continue when screen is off
 */
class ReciteService : Service() {

    companion object {
        private const val CHANNEL_ID = "recite_channel"
        private const val NOTIFICATION_ID = 3001
        private const val WAKE_LOCK_TAG = "QuranMedia:ReciteWakeLock"

        const val ACTION_START = "com.quranmedia.player.recite.START"
        const val ACTION_STOP = "com.quranmedia.player.recite.STOP"

        @Volatile
        private var isRunning = false

        fun start(context: Context) {
            if (isRunning) {
                Timber.d("ReciteService already running, skipping start")
                return
            }
            val intent = Intent(context, ReciteService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            if (!isRunning) {
                Timber.d("ReciteService not running, skipping stop")
                return
            }
            context.stopService(Intent(context, ReciteService::class.java))
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private val binder = LocalBinder()
    private var isForegroundStarted = false

    inner class LocalBinder : Binder() {
        fun getService(): ReciteService = this@ReciteService
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("ReciteService created")
        createNotificationChannel()
        // Immediately start foreground to avoid ForegroundServiceDidNotStartInTimeException
        startForeground(NOTIFICATION_ID, createNotification())
        isForegroundStarted = true
        isRunning = true
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                Timber.d("ReciteService starting")
                if (!isForegroundStarted) {
                    startForeground(NOTIFICATION_ID, createNotification())
                    isForegroundStarted = true
                }
                acquireWakeLock()
            }
            ACTION_STOP -> {
                Timber.d("ReciteService stopping via action")
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                // Service started without action, just ensure foreground
                if (!isForegroundStarted) {
                    startForeground(NOTIFICATION_ID, createNotification())
                    isForegroundStarted = true
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        isRunning = false
        isForegroundStarted = false
        Timber.d("ReciteService destroyed")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recitation Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification for recitation recording"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        // Intent to open the app
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Stop action
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, ReciteService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording Recitation")
            .setContentText("Listening to your recitation...")
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKE_LOCK_TAG
            ).apply {
                // 10 minute timeout as a safety measure
                acquire(10 * 60 * 1000L)
            }
            Timber.d("Wake lock acquired")
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Timber.d("Wake lock released")
            }
        }
        wakeLock = null
    }
}
