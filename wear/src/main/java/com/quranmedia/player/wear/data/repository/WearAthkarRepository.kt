package com.quranmedia.player.wear.data.repository

import android.content.Context
import com.google.gson.Gson
import com.quranmedia.player.wear.data.model.BundledAthkarData
import com.quranmedia.player.wear.domain.model.AthkarCategory
import com.quranmedia.player.wear.domain.model.Thikr
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for loading Athkar data from bundled assets.
 * Uses in-memory caching for fast access.
 */
@Singleton
class WearAthkarRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private var cachedCategories: List<AthkarCategory>? = null
    private var cachedAthkar: Map<String, List<Thikr>>? = null

    /**
     * Get all athkar categories.
     */
    suspend fun getAllCategories(): List<AthkarCategory> {
        return cachedCategories ?: loadFromAssets().let {
            cachedCategories = it.first
            cachedAthkar = it.second
            it.first
        }
    }

    /**
     * Get athkar (thikrs) for a specific category.
     */
    suspend fun getAthkarByCategory(categoryId: String): List<Thikr> {
        if (cachedAthkar == null) {
            val data = loadFromAssets()
            cachedCategories = data.first
            cachedAthkar = data.second
        }
        return cachedAthkar?.get(categoryId) ?: emptyList()
    }

    /**
     * Get a specific thikr by ID.
     */
    suspend fun getThikrById(thikrId: String): Thikr? {
        if (cachedAthkar == null) {
            val data = loadFromAssets()
            cachedCategories = data.first
            cachedAthkar = data.second
        }
        return cachedAthkar?.values?.flatten()?.find { it.id == thikrId }
    }

    /**
     * Load athkar data from bundled assets.
     */
    private suspend fun loadFromAssets(): Pair<List<AthkarCategory>, Map<String, List<Thikr>>> {
        return withContext(Dispatchers.IO) {
            try {
                val json = context.assets.open("athkar_data.json")
                    .bufferedReader().use { it.readText() }

                val bundledData = gson.fromJson(json, BundledAthkarData::class.java)

                val categories = bundledData.categories.map { cat ->
                    AthkarCategory(
                        id = cat.id,
                        nameArabic = cat.nameArabic,
                        nameEnglish = cat.nameEnglish,
                        iconName = cat.iconName,
                        order = cat.order
                    )
                }.sortedBy { it.order }

                val athkarMap = bundledData.categories.associate { cat ->
                    cat.id to cat.athkar.mapIndexed { index, thikr ->
                        Thikr(
                            id = thikr.id,
                            categoryId = cat.id,
                            textArabic = thikr.textArabic,
                            repeatCount = thikr.repeatCount,
                            reference = thikr.reference,
                            order = index
                        )
                    }
                }

                Timber.d("Loaded ${categories.size} athkar categories")
                Pair(categories, athkarMap)
            } catch (e: Exception) {
                Timber.e(e, "Error loading athkar from assets")
                Pair(emptyList(), emptyMap())
            }
        }
    }
}
