package com.quranmedia.player

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.quranmedia.player.data.repository.AthanRepository
import com.quranmedia.player.data.repository.TafseerRepository
import com.quranmedia.player.data.worker.QuranDataPopulatorWorker
import com.quranmedia.player.data.worker.ReciterDataPopulatorWorker
import com.quranmedia.player.domain.util.TajweedDataLoader
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class QuranMediaApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var athanRepository: AthanRepository

    @Inject
    lateinit var tafseerRepository: TafseerRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Create notification channels
        createNotificationChannels()

        // Start Quran data population (runs once, includes text + metadata from bundled files)
        startQuranDataPopulation()

        // Start reciter data population (API-based streaming)
        startReciterDataPopulation()

        // Load Tajweed data from bundled JSON (static data, no API needed)
        TajweedDataLoader.loadTajweedData(this)

        // Initialize default bundled athan (ensures athan works out of the box)
        initializeDefaultAthan()

        // Initialize bundled tafseers (Muyassar, Irab) - auto-loads from assets
        initializeBundledTafseers()

        Timber.d("QuranMediaApplication initialized - Workers enqueued, Tajweed data loaded")
    }

    private fun startQuranDataPopulation() {
        val workRequest = OneTimeWorkRequestBuilder<QuranDataPopulatorWorker>()
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            QuranDataPopulatorWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP, // Only run if not already queued/running
            workRequest
        )

        Timber.d("Quran data population work enqueued")
    }

    private fun startReciterDataPopulation() {
        val workRequest = OneTimeWorkRequestBuilder<ReciterDataPopulatorWorker>()
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            ReciterDataPopulatorWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE, // Replace existing work to ensure fresh data
            workRequest
        )

        Timber.d("Reciter data population work enqueued")
    }

    private fun initializeDefaultAthan() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = athanRepository.ensureDefaultAthanAvailable()
                if (success) {
                    Timber.d("Default athan initialized successfully")
                } else {
                    Timber.w("Default athan not available (asset may be missing)")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error initializing default athan")
            }
        }
    }

    /**
     * Initialize bundled tafseers (Muyassar, Irab) from assets.
     * These are pre-loaded so users don't need to download them.
     */
    private fun initializeBundledTafseers() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // List of bundled tafseer IDs
                val bundledTafseerIds = listOf("muyassar", "quran-irab")

                for (tafseerId in bundledTafseerIds) {
                    // Check if already loaded
                    val isLoaded = tafseerRepository.isDownloaded(tafseerId)
                    if (!isLoaded) {
                        Timber.d("Loading bundled tafseer: $tafseerId")
                        val success = tafseerRepository.downloadTafseer(tafseerId)
                        if (success) {
                            Timber.d("Bundled tafseer loaded: $tafseerId")
                        } else {
                            Timber.w("Failed to load bundled tafseer: $tafseerId")
                        }
                    } else {
                        Timber.d("Bundled tafseer already loaded: $tafseerId")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error initializing bundled tafseers")
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Playback notification channel
            val playbackChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_PLAYBACK,
                getString(R.string.notification_channel_playback),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media playback controls"
                setShowBadge(false)
            }

            // Download notification channel
            val downloadChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_DOWNLOADS,
                getString(R.string.notification_channel_downloads),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Download progress notifications"
                setShowBadge(false)
            }

            notificationManager?.createNotificationChannel(playbackChannel)
            notificationManager?.createNotificationChannel(downloadChannel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_PLAYBACK = "playback_channel"
        const val NOTIFICATION_CHANNEL_DOWNLOADS = "downloads_channel"
    }
}
