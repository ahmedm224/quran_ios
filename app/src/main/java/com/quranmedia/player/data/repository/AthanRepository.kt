package com.quranmedia.player.data.repository

import android.content.Context
import com.quranmedia.player.data.api.AthanApi
import com.quranmedia.player.data.api.model.AthanDto
import com.quranmedia.player.data.database.dao.DownloadedAthanDao
import com.quranmedia.player.data.database.entity.DownloadedAthanEntity
import com.quranmedia.player.domain.model.Athan
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing Athan (call to prayer) audio files.
 * Handles API calls, caching, and local storage of downloaded athans.
 */
@Singleton
class AthanRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val athanApi: AthanApi,
    private val downloadedAthanDao: DownloadedAthanDao,
    private val okHttpClient: OkHttpClient
) {
    // Directory for storing downloaded athans
    private val athansDir: File by lazy {
        File(context.filesDir, "athans").apply {
            if (!exists()) mkdirs()
        }
    }

    // Cache for athans list from API
    private var cachedAthans: List<Athan>? = null

    /**
     * Get list of all available athans from API
     */
    suspend fun getAllAthans(): List<Athan> {
        return cachedAthans ?: try {
            val response = athanApi.getAthans()
            response.athans.map { it.toDomain() }.also {
                cachedAthans = it
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching athans from API")
            emptyList()
        }
    }

    /**
     * Get list of downloaded athans
     */
    fun getDownloadedAthans(): Flow<List<Athan>> {
        return downloadedAthanDao.getAllDownloadedAthans().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Check if an athan is downloaded
     */
    suspend fun isAthanDownloaded(athanId: String): Boolean {
        return downloadedAthanDao.isAthanDownloaded(athanId)
    }

    /**
     * Get local file path for an athan (if downloaded)
     */
    suspend fun getAthanLocalPath(athanId: String): String? {
        val localPath = downloadedAthanDao.getAthanLocalPath(athanId)
        // Verify file exists
        if (localPath != null && File(localPath).exists()) {
            return localPath
        }
        // File doesn't exist, remove from database
        if (localPath != null) {
            downloadedAthanDao.deleteDownloadedAthan(athanId)
        }
        return null
    }

    /**
     * Get the audio path for an athan - returns local path if downloaded, otherwise streaming URL
     */
    suspend fun getAthanAudioPath(athanId: String): String {
        val localPath = getAthanLocalPath(athanId)
        if (localPath != null) {
            Timber.d("Using local athan file: $localPath")
            return localPath
        }
        // Fallback to streaming URL (not recommended, but as backup)
        val url = AthanApi.getAthanAudioUrl(athanId)
        Timber.d("Athan not downloaded, using streaming URL: $url")
        return url
    }

    /**
     * Download an athan file
     * @param athan The athan to download
     * @param onProgress Progress callback (0.0 to 1.0)
     * @return true if download was successful
     */
    suspend fun downloadAthan(
        athan: Athan,
        onProgress: ((Float) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.d("Starting download for athan: ${athan.id} - ${athan.name}")

            val audioUrl = AthanApi.getAthanAudioUrl(athan.id)
            val outputFile = File(athansDir, "${athan.id}.mp3")

            // Create request
            val request = Request.Builder()
                .url(audioUrl)
                .build()

            // Execute request
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.e("Download failed: ${response.code}")
                    return@withContext false
                }

                val body = response.body ?: return@withContext false
                val contentLength = body.contentLength()
                var bytesRead = 0L

                // Write to file
                FileOutputStream(outputFile).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytes: Int
                        while (input.read(buffer).also { bytes = it } != -1) {
                            output.write(buffer, 0, bytes)
                            bytesRead += bytes
                            if (contentLength > 0) {
                                onProgress?.invoke(bytesRead.toFloat() / contentLength)
                            }
                        }
                    }
                }

                // Save to database
                val entity = DownloadedAthanEntity(
                    id = athan.id,
                    name = athan.name,
                    muezzin = athan.muezzin,
                    location = athan.location,
                    localPath = outputFile.absolutePath,
                    fileSize = outputFile.length()
                )
                downloadedAthanDao.insertDownloadedAthan(entity)

                Timber.d("Athan downloaded successfully: ${outputFile.absolutePath}")
                true
            }
        } catch (e: Exception) {
            Timber.e(e, "Error downloading athan: ${athan.id}")
            false
        }
    }

    /**
     * Delete a downloaded athan
     */
    suspend fun deleteDownloadedAthan(athanId: String) = withContext(Dispatchers.IO) {
        try {
            // Get local path
            val localPath = downloadedAthanDao.getAthanLocalPath(athanId)
            if (localPath != null) {
                // Delete file
                val file = File(localPath)
                if (file.exists()) {
                    file.delete()
                    Timber.d("Deleted athan file: $localPath")
                }
            }
            // Remove from database
            downloadedAthanDao.deleteDownloadedAthan(athanId)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting athan: $athanId")
        }
    }

    /**
     * Get athan by ID from API
     */
    suspend fun getAthanById(athanId: String): Athan? {
        return getAllAthans().find { it.id == athanId }
    }

    /**
     * Extension to convert DTO to domain model
     */
    private fun AthanDto.toDomain() = Athan(
        id = id,
        name = name,
        muezzin = muezzin,
        location = location,
        audioUrl = AthanApi.getAthanAudioUrl(id)
    )

    /**
     * Extension to convert Entity to domain model
     */
    private fun DownloadedAthanEntity.toDomain() = Athan(
        id = id,
        name = name,
        muezzin = muezzin,
        location = location,
        audioUrl = localPath  // Use local path for downloaded athans
    )
}
