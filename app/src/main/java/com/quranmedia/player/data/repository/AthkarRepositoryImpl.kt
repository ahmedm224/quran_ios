package com.quranmedia.player.data.repository

import android.content.Context
import com.google.gson.Gson
import com.quranmedia.player.data.api.AthkarApi
import com.quranmedia.player.data.api.model.BundledAthkarData
import com.quranmedia.player.data.database.dao.AthkarDao
import com.quranmedia.player.data.database.entity.AthkarCategoryEntity
import com.quranmedia.player.data.database.entity.ThikrEntity
import com.quranmedia.player.domain.model.AthkarCategory
import com.quranmedia.player.domain.util.Resource
import com.quranmedia.player.domain.model.Thikr
import com.quranmedia.player.domain.repository.AthkarRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AthkarRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val athkarDao: AthkarDao,
    private val athkarApi: AthkarApi,
    private val gson: Gson
) : AthkarRepository {

    override fun getAllCategories(): Flow<List<AthkarCategory>> {
        return athkarDao.getAllCategories().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getAthkarByCategory(categoryId: String): Flow<List<Thikr>> {
        return athkarDao.getAthkarByCategory(categoryId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getThikrById(id: String): Thikr? {
        return athkarDao.getThikrById(id)?.toDomainModel()
    }

    override suspend fun refreshAthkarFromApi(): Resource<Unit> {
        return try {
            val response = athkarApi.getAllAthkar()
            val chapters = response.chapters ?: return Resource.Error("No data received")

            // Convert API response to entities
            val categories = mutableListOf<AthkarCategoryEntity>()
            val athkar = mutableListOf<ThikrEntity>()

            chapters.forEachIndexed { index, chapter ->
                val categoryId = "api_${chapter.id}"

                categories.add(
                    AthkarCategoryEntity(
                        id = categoryId,
                        nameArabic = chapter.arabic,
                        nameEnglish = chapter.english ?: chapter.arabic,
                        iconName = getIconForCategory(chapter.arabic),
                        order = index + 100 // Start after bundled data
                    )
                )

                chapter.contents?.forEachIndexed { contentIndex, content ->
                    athkar.add(
                        ThikrEntity(
                            id = "${categoryId}_${content.id}",
                            categoryId = categoryId,
                            textArabic = content.text,
                            transliteration = null,
                            translation = null,
                            repeatCount = content.count ?: 1,
                            reference = content.reference,
                            audioUrl = content.audio,
                            order = contentIndex
                        )
                    )
                }
            }

            // Insert into database
            athkarDao.insertCategories(categories)
            athkarDao.insertAthkar(athkar)

            Timber.d("Refreshed ${categories.size} categories and ${athkar.size} athkar from API")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh athkar from API")
            Resource.Error("Failed to refresh: ${e.message}")
        }
    }

    override suspend fun hasLocalData(): Boolean {
        return athkarDao.getCategoryCount() > 0
    }

    override suspend fun initializeFromAssets() {
        // Force re-initialization to pick up updated athkar data
        try {
            // Read bundled JSON from assets
            val jsonString = context.assets.open("athkar_data.json")
                .bufferedReader()
                .use { it.readText() }

            val bundledData = gson.fromJson(jsonString, BundledAthkarData::class.java)

            val categories = mutableListOf<AthkarCategoryEntity>()
            val athkar = mutableListOf<ThikrEntity>()

            bundledData.categories.forEach { category ->
                categories.add(
                    AthkarCategoryEntity(
                        id = category.id,
                        nameArabic = category.nameArabic,
                        nameEnglish = category.nameEnglish,
                        iconName = category.iconName,
                        order = category.order
                    )
                )

                category.athkar.forEachIndexed { index, thikr ->
                    athkar.add(
                        ThikrEntity(
                            id = thikr.id,
                            categoryId = category.id,
                            textArabic = thikr.textArabic,
                            transliteration = thikr.transliteration,
                            translation = thikr.translation,
                            repeatCount = thikr.repeatCount,
                            reference = thikr.reference,
                            audioUrl = null,
                            order = index
                        )
                    )
                }
            }

            // Delete existing bundled data (not API data)
            athkarDao.deleteCategoriesNotFromApi()
            athkarDao.deleteAthkarNotFromApi()

            // Insert updated data
            athkarDao.insertCategories(categories)
            athkarDao.insertAthkar(athkar)

            Timber.d("Re-initialized ${categories.size} categories and ${athkar.size} athkar from bundled assets")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize athkar from assets")
        }
    }

    private fun getIconForCategory(arabicName: String): String {
        return when {
            arabicName.contains("صباح") -> "WbSunny"
            arabicName.contains("مساء") -> "NightsStay"
            arabicName.contains("صلاة") -> "Mosque"
            arabicName.contains("نوم") -> "Bedtime"
            arabicName.contains("استيقاظ") -> "Alarm"
            arabicName.contains("منزل") || arabicName.contains("بيت") -> "Home"
            arabicName.contains("طعام") -> "Restaurant"
            arabicName.contains("سفر") -> "Flight"
            else -> "AutoAwesome"
        }
    }
}
