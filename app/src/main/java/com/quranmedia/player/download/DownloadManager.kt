package com.quranmedia.player.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.quranmedia.player.data.database.dao.AudioVariantDao
import com.quranmedia.player.data.database.dao.DownloadTaskDao
import com.quranmedia.player.data.database.entity.DownloadStatus
import com.quranmedia.player.data.database.entity.DownloadTaskEntity
import com.quranmedia.player.data.database.entity.toDomainModel
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.domain.model.AudioVariant
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
    private val downloadTaskDao: DownloadTaskDao,
    private val audioVariantDao: AudioVariantDao,
    private val settingsRepository: SettingsRepository
) {

    fun getAllDownloads(): Flow<List<DownloadTaskEntity>> {
        return downloadTaskDao.getAllDownloadTasks()
    }

    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadTaskEntity>> {
        return downloadTaskDao.getDownloadTasksByStatus(status.name)
    }

    /**
     * Download audio for a surah using ayah-by-ayah download.
     * This downloads each ayah file from the respective provider (Al-Quran Cloud or EveryAyah).
     */
    suspend fun downloadAudio(
        reciterId: String,
        surahNumber: Int,
        audioVariant: AudioVariant
    ): String {
        // Check if already downloaded
        val existingTask = downloadTaskDao.getDownloadTaskForSurah(reciterId, surahNumber)
        if (existingTask?.status == DownloadStatus.COMPLETED.name) {
            Timber.d("Audio already downloaded for surah $surahNumber")
            return existingTask.id
        }

        // Create download task
        val taskId = java.util.UUID.randomUUID().toString()
        val task = DownloadTaskEntity(
            id = taskId,
            audioVariantId = audioVariant.id,
            reciterId = reciterId,
            surahNumber = surahNumber,
            status = DownloadStatus.PENDING.name,
            bytesTotal = audioVariant.fileSizeBytes ?: 0L
        )

        downloadTaskDao.insertDownloadTask(task)

        // Get settings for WiFi-only option
        val settings = settingsRepository.settings.first()
        val wifiOnly = settings.wifiOnlyDownloads

        // Create work request with constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

        // Use AyahDownloadWorker for ayah-by-ayah download
        val workData = workDataOf(
            AyahDownloadWorker.KEY_DOWNLOAD_TASK_ID to taskId,
            AyahDownloadWorker.KEY_AUDIO_VARIANT_ID to audioVariant.id,
            AyahDownloadWorker.KEY_RECITER_ID to reciterId,
            AyahDownloadWorker.KEY_SURAH_NUMBER to surahNumber
        )

        val downloadWork = OneTimeWorkRequestBuilder<AyahDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(workData)
            .addTag("download_$taskId")
            .build()

        workManager.enqueue(downloadWork)

        Timber.d("Ayah download queued for surah $surahNumber, reciter: $reciterId, task: $taskId")
        return taskId
    }

    suspend fun pauseDownload(taskId: String) {
        workManager.cancelAllWorkByTag("download_$taskId")
        val task = downloadTaskDao.getDownloadTaskById(taskId)
        task?.let {
            downloadTaskDao.updateDownloadTask(
                it.copy(
                    status = DownloadStatus.PAUSED.name,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        Timber.d("Download paused: $taskId")
    }

    suspend fun resumeDownload(taskId: String) {
        val task = downloadTaskDao.getDownloadTaskById(taskId) ?: return
        val audioVariant = audioVariantDao.getAudioVariantById(task.audioVariantId) ?: return

        downloadAudio(task.reciterId, task.surahNumber, audioVariant.toDomainModel())
    }

    suspend fun cancelDownload(taskId: String) {
        workManager.cancelAllWorkByTag("download_$taskId")
        downloadTaskDao.deleteDownloadTask(taskId)
        Timber.d("Download cancelled: $taskId")
    }

    suspend fun deleteDownloadedAudio(reciterId: String, surahNumber: Int) {
        val task = downloadTaskDao.getDownloadTaskForSurah(reciterId, surahNumber)
        task?.let {
            val audioVariant = audioVariantDao.getAudioVariantById(it.audioVariantId)
            audioVariant?.localPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    if (file.isDirectory) {
                        // Delete all ayah files in the directory
                        file.listFiles()?.forEach { ayahFile ->
                            ayahFile.delete()
                        }
                        file.delete()
                        Timber.d("Deleted ayah directory: $path")
                    } else {
                        file.delete()
                        Timber.d("Deleted audio file: $path")
                    }
                }

                // Update audio variant
                audioVariantDao.insertAudioVariant(audioVariant.copy(localPath = null))
            }

            downloadTaskDao.deleteDownloadTask(it.id)
        }
    }

    /**
     * Download the full Quran (all 114 surahs) for a specific reciter.
     * Downloads are chained sequentially to respect API rate limits.
     */
    suspend fun downloadFullQuran(reciterId: String): List<String> {
        val taskIds = mutableListOf<String>()
        val workRequests = mutableListOf<androidx.work.OneTimeWorkRequest>()

        // Get settings for WiFi-only option
        val settings = settingsRepository.settings.first()
        val wifiOnly = settings.wifiOnlyDownloads

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

        for (surahNumber in 1..114) {
            // Check if already downloaded
            val existingTask = downloadTaskDao.getDownloadTaskForSurah(reciterId, surahNumber)
            if (existingTask?.status == DownloadStatus.COMPLETED.name) {
                Timber.d("Surah $surahNumber already downloaded for $reciterId")
                continue
            }

            // Create download task entry in database
            val taskId = java.util.UUID.randomUUID().toString()
            val audioVariantId = "${reciterId}_${surahNumber}"

            val task = DownloadTaskEntity(
                id = taskId,
                audioVariantId = audioVariantId,
                reciterId = reciterId,
                surahNumber = surahNumber,
                status = DownloadStatus.PENDING.name,
                bytesTotal = 0L
            )

            downloadTaskDao.insertDownloadTask(task)

            // Create work request
            val workData = workDataOf(
                AyahDownloadWorker.KEY_DOWNLOAD_TASK_ID to taskId,
                AyahDownloadWorker.KEY_AUDIO_VARIANT_ID to audioVariantId,
                AyahDownloadWorker.KEY_RECITER_ID to reciterId,
                AyahDownloadWorker.KEY_SURAH_NUMBER to surahNumber
            )

            val downloadWork = OneTimeWorkRequestBuilder<AyahDownloadWorker>()
                .setConstraints(constraints)
                .setInputData(workData)
                .addTag("full_quran_$reciterId")
                .addTag("download_$taskId")
                .build()

            workRequests.add(downloadWork)
            taskIds.add(taskId)
        }

        // Chain all workers to run sequentially
        if (workRequests.isNotEmpty()) {
            var workContinuation = workManager.beginUniqueWork(
                "full_quran_$reciterId",
                ExistingWorkPolicy.REPLACE,
                workRequests.first()
            )

            for (i in 1 until workRequests.size) {
                workContinuation = workContinuation.then(workRequests[i])
            }

            workContinuation.enqueue()
        }

        Timber.d("Full Quran download chained for $reciterId: ${taskIds.size} surahs (sequential)")
        return taskIds
    }

    /**
     * Get download progress for a reciter (for full Quran downloads).
     * Returns a pair of (completed surahs, total surahs).
     */
    suspend fun getReciterDownloadProgress(reciterId: String): Pair<Int, Int> {
        val tasks = downloadTaskDao.getDownloadTasksForReciterSync(reciterId)
        val completed = tasks.count { it.status == DownloadStatus.COMPLETED.name }
        return Pair(completed, 114)
    }

    /**
     * Check if a reciter has any downloads (partial or complete).
     */
    suspend fun hasDownloadsForReciter(reciterId: String): Boolean {
        val tasks = downloadTaskDao.getDownloadTasksForReciterSync(reciterId)
        return tasks.isNotEmpty()
    }

    /**
     * Delete all downloaded audio for a reciter.
     */
    suspend fun deleteAllDownloadsForReciter(reciterId: String) {
        for (surahNumber in 1..114) {
            deleteDownloadedAudio(reciterId, surahNumber)
        }
        Timber.d("Deleted all downloads for reciter: $reciterId")
    }

    /**
     * Cancel all pending/in-progress downloads for a reciter.
     */
    suspend fun cancelAllDownloadsForReciter(reciterId: String) {
        // Cancel the unique work chain if it exists
        workManager.cancelUniqueWork("full_quran_$reciterId")

        // Also cancel individual downloads
        val tasks = downloadTaskDao.getDownloadTasksForReciterSync(reciterId)
        tasks.filter { it.status != DownloadStatus.COMPLETED.name }
            .forEach { task ->
                cancelDownload(task.id)
            }
        Timber.d("Cancelled all pending downloads for reciter: $reciterId")
    }
}
