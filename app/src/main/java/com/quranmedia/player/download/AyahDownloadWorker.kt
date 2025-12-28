package com.quranmedia.player.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.quranmedia.player.data.database.dao.AudioVariantDao
import com.quranmedia.player.data.database.dao.DownloadTaskDao
import com.quranmedia.player.data.database.entity.DownloadStatus
import com.quranmedia.player.domain.repository.QuranRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/**
 * Worker that downloads all ayah audio files for a surah from the respective provider.
 * This enables offline playback with ayah-by-ayah audio.
 */
@HiltWorker
class AyahDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadTaskDao: DownloadTaskDao,
    private val audioVariantDao: AudioVariantDao,
    private val quranRepository: QuranRepository,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_DOWNLOAD_TASK_ID = "download_task_id"
        const val KEY_AUDIO_VARIANT_ID = "audio_variant_id"
        const val KEY_RECITER_ID = "reciter_id"
        const val KEY_SURAH_NUMBER = "surah_number"
        const val KEY_PROGRESS = "progress"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getString(KEY_DOWNLOAD_TASK_ID) ?: return@withContext Result.failure()
        val audioVariantId = inputData.getString(KEY_AUDIO_VARIANT_ID) ?: return@withContext Result.failure()
        val reciterId = inputData.getString(KEY_RECITER_ID) ?: return@withContext Result.failure()
        val surahNumber = inputData.getInt(KEY_SURAH_NUMBER, -1)

        if (surahNumber == -1) return@withContext Result.failure()

        try {
            Timber.d("Starting ayah download for task: $taskId, surah: $surahNumber, reciter: $reciterId")

            // Update status to IN_PROGRESS
            val task = downloadTaskDao.getDownloadTaskById(taskId)
            task?.let {
                downloadTaskDao.updateDownloadTask(
                    it.copy(
                        status = DownloadStatus.IN_PROGRESS.name,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            // Get surah data with ayah audio URLs from the respective provider
            val surahData = quranRepository.getSurahWithAudio(surahNumber, reciterId)
            if (surahData == null) {
                Timber.e("Failed to get surah data for download")
                markAsFailed(taskId, "Failed to get surah data")
                return@withContext Result.failure()
            }

            val ayahs = surahData.ayahs
            if (ayahs.isEmpty()) {
                Timber.e("No ayahs found for surah $surahNumber")
                markAsFailed(taskId, "No ayahs found")
                return@withContext Result.failure()
            }

            // Create download directory for this reciter/surah
            val downloadDir = File(
                applicationContext.getExternalFilesDir(null),
                "quran/ayahs/$reciterId/$surahNumber"
            )
            downloadDir.mkdirs()

            var downloadedCount = 0
            var totalBytes = 0L

            // Download each ayah
            for ((index, ayah) in ayahs.withIndex()) {
                val audioUrl = ayah.audio
                if (audioUrl.isNullOrBlank()) {
                    Timber.w("Ayah ${ayah.numberInSurah} has no audio URL, skipping")
                    continue
                }

                val ayahFile = File(downloadDir, "${ayah.numberInSurah}.mp3")

                // Skip if already downloaded
                if (ayahFile.exists() && ayahFile.length() > 0) {
                    Timber.d("Ayah ${ayah.numberInSurah} already downloaded, skipping")
                    downloadedCount++
                    totalBytes += ayahFile.length()
                } else {
                    // Download the ayah file
                    val success = downloadAyahFile(audioUrl, ayahFile)
                    if (success) {
                        downloadedCount++
                        totalBytes += ayahFile.length()
                    } else {
                        Timber.e("Failed to download ayah ${ayah.numberInSurah}")
                    }
                }

                // Update progress
                val progress = (index + 1).toFloat() / ayahs.size
                updateProgress(taskId, downloadedCount.toLong(), ayahs.size.toLong(), progress)
            }

            // Check if all ayahs were downloaded
            if (downloadedCount >= ayahs.size * 0.9) { // Allow 90% success rate
                // Update audio variant with local path (directory containing ayah files)
                val audioVariant = audioVariantDao.getAudioVariantById(audioVariantId)
                audioVariant?.let { variant ->
                    audioVariantDao.insertAudioVariant(
                        variant.copy(localPath = downloadDir.absolutePath)
                    )
                }

                // Mark task as completed
                task?.let {
                    downloadTaskDao.updateDownloadTask(
                        it.copy(
                            status = DownloadStatus.COMPLETED.name,
                            progress = 1f,
                            bytesDownloaded = totalBytes,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }

                Timber.d("Ayah download completed: $downloadedCount/${ayahs.size} ayahs for surah $surahNumber")
                Result.success()
            } else {
                markAsFailed(taskId, "Only $downloadedCount/${ayahs.size} ayahs downloaded")
                Result.failure()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during ayah download: $taskId")
            markAsFailed(taskId, e.message)
            Result.failure()
        }
    }

    private suspend fun downloadAyahFile(url: String, destFile: File): Boolean {
        return try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Timber.e("Download failed with code: ${response.code} for $url")
                return false
            }

            val body = response.body ?: return false

            destFile.parentFile?.mkdirs()
            body.byteStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            true
        } catch (e: Exception) {
            Timber.e(e, "Error downloading ayah file: $url")
            false
        }
    }

    private suspend fun markAsFailed(taskId: String, errorMessage: String?) {
        val task = downloadTaskDao.getDownloadTaskById(taskId)
        task?.let {
            downloadTaskDao.updateDownloadTask(
                it.copy(
                    status = DownloadStatus.FAILED.name,
                    errorMessage = errorMessage,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun updateProgress(taskId: String, downloaded: Long, total: Long, progress: Float) {
        val task = downloadTaskDao.getDownloadTaskById(taskId)
        task?.let {
            downloadTaskDao.updateDownloadTask(
                it.copy(
                    bytesDownloaded = downloaded,
                    bytesTotal = total,
                    progress = progress,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        setProgress(workDataOf(KEY_PROGRESS to progress))
    }
}
