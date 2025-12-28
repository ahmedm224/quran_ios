package com.quranmedia.player.data.repository

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.quranmedia.player.data.database.dao.AyahDao
import com.quranmedia.player.data.database.dao.SurahDao
import com.quranmedia.player.data.database.entity.AyahEntity
import com.quranmedia.player.data.database.entity.SurahEntity
import com.quranmedia.player.data.worker.QuranDataPopulatorWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuranDataRepository @Inject constructor(
    private val surahDao: SurahDao,
    private val ayahDao: AyahDao,
    private val workManager: WorkManager
) {

    /**
     * Start downloading all Quran data from the API
     */
    fun startQuranDataPopulation() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<QuranDataPopulatorWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            QuranDataPopulatorWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Get the status of Quran data population
     */
    fun getPopulationWorkStatus(): Flow<WorkInfo?> {
        return workManager.getWorkInfosForUniqueWorkFlow(QuranDataPopulatorWorker.WORK_NAME)
            .map { workInfos -> workInfos.firstOrNull() }
    }

    /**
     * Check if Quran data is populated
     */
    suspend fun isQuranDataPopulated(): Boolean {
        val ayahCount = ayahDao.getAyahCount()
        return ayahCount >= 6236  // Total ayahs in Quran
    }

    /**
     * Get all Surahs
     */
    fun getAllSurahs(): Flow<List<SurahEntity>> {
        return surahDao.getAllSurahs()
    }

    /**
     * Get Ayahs for a specific Surah
     */
    fun getAyahsForSurah(surahNumber: Int): Flow<List<AyahEntity>> {
        return ayahDao.getAyahsBySurah(surahNumber)
    }

    /**
     * Get a specific Ayah
     */
    suspend fun getAyah(surahNumber: Int, ayahNumber: Int): AyahEntity? {
        return ayahDao.getAyah(surahNumber, ayahNumber)
    }

    /**
     * Get Ayahs by page number
     */
    fun getAyahsByPage(pageNumber: Int): Flow<List<AyahEntity>> {
        return ayahDao.getAyahsByPage(pageNumber)
    }

    /**
     * Get Ayahs by Juz number
     */
    fun getAyahsByJuz(juzNumber: Int): Flow<List<AyahEntity>> {
        return ayahDao.getAyahsByJuz(juzNumber)
    }

    /**
     * Get all Sajda ayahs
     */
    fun getSajdaAyahs(): Flow<List<AyahEntity>> {
        return ayahDao.getSajdaAyahs()
    }

    /**
     * Get current ayah count
     */
    suspend fun getAyahCount(): Int {
        return ayahDao.getAyahCount()
    }
}
