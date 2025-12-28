package com.quranmedia.player.domain.repository

import com.quranmedia.player.domain.model.AthkarCategory
import com.quranmedia.player.domain.util.Resource
import com.quranmedia.player.domain.model.Thikr
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Athkar data
 */
interface AthkarRepository {
    /**
     * Get all athkar categories
     */
    fun getAllCategories(): Flow<List<AthkarCategory>>

    /**
     * Get athkar by category ID
     */
    fun getAthkarByCategory(categoryId: String): Flow<List<Thikr>>

    /**
     * Get a specific thikr by ID
     */
    suspend fun getThikrById(id: String): Thikr?

    /**
     * Refresh athkar data from API
     */
    suspend fun refreshAthkarFromApi(): Resource<Unit>

    /**
     * Check if athkar data is available locally
     */
    suspend fun hasLocalData(): Boolean

    /**
     * Initialize athkar from bundled assets if not already loaded
     */
    suspend fun initializeFromAssets()
}
