package com.quranmedia.player.data.repository

import com.quranmedia.player.data.database.dao.AyahDao
import com.quranmedia.player.data.database.dao.SurahDao
import com.quranmedia.player.data.database.entity.toDomainModel
import com.quranmedia.player.domain.model.SearchResult
import com.quranmedia.player.domain.repository.SearchRepository
import com.quranmedia.player.domain.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor(
    private val ayahDao: AyahDao,
    private val surahDao: SurahDao
) : SearchRepository {

    override suspend fun searchAyahs(query: String): Resource<List<SearchResult>> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("Searching for: $query")

                // Search in database
                val ayahEntities = ayahDao.searchAyahs(query)
                Timber.d("Found ${ayahEntities.size} ayahs")

                // Convert to SearchResult with surah info
                val results = ayahEntities.mapNotNull { ayahEntity ->
                    val surahEntity = surahDao.getSurahByNumber(ayahEntity.surahNumber)
                    if (surahEntity != null) {
                        SearchResult(
                            ayah = ayahEntity.toDomainModel(),
                            surah = surahEntity.toDomainModel()
                        )
                    } else {
                        Timber.w("Surah not found for ayah: ${ayahEntity.surahNumber}:${ayahEntity.ayahNumber}")
                        null
                    }
                }

                Resource.Success(results)
            } catch (e: Exception) {
                Timber.e(e, "Search error")
                Resource.Error(e.message ?: "Search failed")
            }
        }
    }
}
