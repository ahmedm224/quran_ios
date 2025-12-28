package com.quranmedia.player.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quranmedia.player.data.api.AlQuranCloudApi
import com.quranmedia.player.data.database.dao.AyahDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Worker to fetch and populate page/juz/manzil/ruku/hizbQuarter/sajda metadata
 * from Al-Quran Cloud API. This worker runs after QuranDataPopulatorWorker
 * and updates existing ayah records with metadata.
 */
@HiltWorker
class QuranMetadataPopulatorWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val api: AlQuranCloudApi,
    private val ayahDao: AyahDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Starting Quran metadata population from API")

            // Check if metadata already exists (page > 0 means populated)
            val maxPage = ayahDao.getMaxPageNumber()
            if (maxPage != null && maxPage > 0) {
                Timber.d("Quran metadata already populated (max page: $maxPage)")
                return Result.success()
            }

            // Check if base ayah data exists first
            val ayahCount = ayahDao.getAyahCount()
            if (ayahCount < 6236) {
                Timber.d("Waiting for base Quran data to be populated first ($ayahCount ayahs)")
                return Result.retry()
            }

            Timber.d("Fetching full Quran metadata from API...")
            val response = api.getFullQuran(AlQuranCloudApi.EDITION_UTHMANI)

            if (response.code != 200) {
                Timber.e("API returned error code: ${response.code}")
                return Result.retry()
            }

            val surahs = response.data.surahs
            Timber.d("Received ${surahs.size} surahs from API")

            var updatedCount = 0

            // Process each surah and update ayah metadata
            for (surah in surahs) {
                try {
                    Timber.d("Processing metadata for Surah ${surah.number}: ${surah.englishName}")

                    for (ayah in surah.ayahs) {
                        // Parse sajda - can be boolean or object
                        val isSajda = when (ayah.sajda) {
                            is Boolean -> ayah.sajda
                            is Map<*, *> -> true  // If it's an object, there is a sajda
                            else -> false
                        }

                        ayahDao.updateAyahMetadata(
                            surahNumber = surah.number,
                            ayahNumber = ayah.numberInSurah,
                            juz = ayah.juz,
                            manzil = ayah.manzil,
                            page = ayah.page,
                            ruku = ayah.ruku,
                            hizbQuarter = ayah.hizbQuarter,
                            sajda = isSajda
                        )
                        updatedCount++
                    }

                    Timber.d("Updated ${surah.ayahs.size} ayahs for Surah ${surah.number}")
                } catch (e: Exception) {
                    Timber.e(e, "Error processing Surah ${surah.number}")
                    // Continue with next surah
                }
            }

            Timber.d("Quran metadata population complete: $updatedCount ayahs updated")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Failed to populate Quran metadata")
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "quran_metadata_populator"
    }
}
