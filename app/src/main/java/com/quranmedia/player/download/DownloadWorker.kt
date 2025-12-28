package com.quranmedia.player.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.quranmedia.player.data.database.dao.AudioVariantDao
import com.quranmedia.player.data.database.dao.DownloadTaskDao
import com.quranmedia.player.data.database.entity.DownloadStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadTaskDao: DownloadTaskDao,
    private val audioVariantDao: AudioVariantDao,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_DOWNLOAD_TASK_ID = "download_task_id"
        const val KEY_AUDIO_VARIANT_ID = "audio_variant_id"
        const val KEY_URL = "url"
        const val KEY_DESTINATION_PATH = "destination_path"
        const val KEY_PROGRESS = "progress"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getString(KEY_DOWNLOAD_TASK_ID) ?: return@withContext Result.failure()
        val audioVariantId = inputData.getString(KEY_AUDIO_VARIANT_ID) ?: return@withContext Result.failure()
        val url = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val destinationPath = inputData.getString(KEY_DESTINATION_PATH) ?: return@withContext Result.failure()

        try {
            Timber.d("Starting download for task: $taskId")

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

            // Download file
            val success = downloadFile(url, destinationPath, taskId)

            if (success) {
                // Update audio variant with local path
                val audioVariant = audioVariantDao.getAudioVariantById(audioVariantId)
                audioVariant?.let { variant ->
                    audioVariantDao.insertAudioVariant(
                        variant.copy(localPath = destinationPath)
                    )
                }

                // Mark task as completed
                task?.let {
                    downloadTaskDao.updateDownloadTask(
                        it.copy(
                            status = DownloadStatus.COMPLETED.name,
                            progress = 1f,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }

                Timber.d("Download completed successfully: $taskId")
                Result.success()
            } else {
                // Mark as failed
                task?.let {
                    downloadTaskDao.updateDownloadTask(
                        it.copy(
                            status = DownloadStatus.FAILED.name,
                            errorMessage = "Download failed",
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }

                Timber.e("Download failed: $taskId")
                Result.failure()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during download: $taskId")

            val task = downloadTaskDao.getDownloadTaskById(taskId)
            task?.let {
                downloadTaskDao.updateDownloadTask(
                    it.copy(
                        status = DownloadStatus.FAILED.name,
                        errorMessage = e.message,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            Result.failure()
        }
    }

    private suspend fun downloadFile(url: String, destinationPath: String, taskId: String): Boolean {
        return try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Timber.e("Download failed with code: ${response.code}")
                return false
            }

            val body = response.body ?: return false
            val contentLength = body.contentLength()

            // Create destination file
            val file = File(destinationPath)
            file.parentFile?.mkdirs()

            // Download with progress tracking
            body.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // Update progress
                        if (contentLength > 0) {
                            val progress = (totalBytesRead.toFloat() / contentLength).coerceIn(0f, 1f)
                            updateProgress(taskId, totalBytesRead, contentLength, progress)
                        }
                    }
                }
            }

            true
        } catch (e: Exception) {
            Timber.e(e, "Error downloading file")
            false
        }
    }

    private suspend fun updateProgress(taskId: String, bytesDownloaded: Long, bytesTotal: Long, progress: Float) {
        val task = downloadTaskDao.getDownloadTaskById(taskId)
        task?.let {
            downloadTaskDao.updateDownloadTask(
                it.copy(
                    bytesDownloaded = bytesDownloaded,
                    bytesTotal = bytesTotal,
                    progress = progress,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        // Update work progress
        setProgress(workDataOf(KEY_PROGRESS to progress))
    }
}
