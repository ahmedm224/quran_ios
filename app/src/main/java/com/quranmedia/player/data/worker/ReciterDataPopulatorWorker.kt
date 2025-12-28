package com.quranmedia.player.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quranmedia.player.data.api.QuranApi
import com.quranmedia.player.data.database.dao.AudioVariantDao
import com.quranmedia.player.data.database.dao.ReciterDao
import com.quranmedia.player.data.database.entity.AudioVariantEntity
import com.quranmedia.player.data.database.entity.ReciterEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Worker to download all audio reciters from CloudLinqed Quran API
 * and create audio variants for streaming playback
 */
@HiltWorker
class ReciterDataPopulatorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val quranApi: QuranApi,
    private val reciterDao: ReciterDao,
    private val audioVariantDao: AudioVariantDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Starting reciter data population from CloudLinqed Quran API")

            // Check if reciters already exist
            val existingReciterCount = reciterDao.getReciterCount()
            if (existingReciterCount > 0) {
                Timber.d("Reciter data already populated ($existingReciterCount reciters)")
                return Result.success()
            }

            // Fetch all reciters from CloudLinqed Quran API
            val response = quranApi.getReciters()

            Timber.d("Fetched ${response.count} reciters from CloudLinqed API")

            var reciterCount = 0
            var audioVariantCount = 0

            // Process each reciter
            for (apiReciter in response.reciters) {
                try {
                    // Extract style from name if present (e.g., "Mohamed Siddiq Al-Minshawi (Murattal)")
                    val style = when {
                        apiReciter.name.contains("Murattal", ignoreCase = true) -> "Murattal"
                        apiReciter.name.contains("Mujawwad", ignoreCase = true) -> "Mujawwad"
                        apiReciter.name.contains("Warsh", ignoreCase = true) -> "Warsh"
                        else -> "Murattal" // Default
                    }

                    // Insert reciter
                    val reciterEntity = ReciterEntity(
                        id = apiReciter.id,              // Use slug ID (e.g., "minshawy-murattal")
                        name = apiReciter.name,
                        nameArabic = apiReciter.arabicName,
                        style = style,
                        version = "2025",
                        imageUrl = null
                    )
                    reciterDao.insertReciter(reciterEntity)
                    reciterCount++

                    // Create audio variants for all 114 Surahs
                    // Store the r2Path in the URL field for later use
                    for (surahNumber in 1..114) {
                        val audioVariant = AudioVariantEntity(
                            reciterId = apiReciter.id,
                            surahNumber = surahNumber,
                            bitrate = 128,
                            format = "MP3",
                            // Store R2 path as base URL for ayah URL construction
                            url = "quranapi:${apiReciter.r2Path}/", // Custom scheme to indicate CloudLinqed API
                            localPath = null,
                            durationMs = 0,
                            fileSizeBytes = null,
                            hash = null
                        )
                        audioVariantDao.insertAudioVariant(audioVariant)
                        audioVariantCount++
                    }

                    Timber.d("Added reciter: ${apiReciter.name} (${apiReciter.id})")

                    // Small delay to avoid overwhelming the database
                    delay(10)
                } catch (e: Exception) {
                    Timber.e(e, "Error adding reciter: ${apiReciter.id}")
                    // Continue with next reciter
                }
            }

            Timber.d("CloudLinqed API population complete: $reciterCount reciters, $audioVariantCount audio variants")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Failed to populate reciter data from CloudLinqed API")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "reciter_data_populator"
    }
}
