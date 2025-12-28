package com.quranmedia.player.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranmedia.player.data.database.entity.AthkarCategoryEntity
import com.quranmedia.player.data.database.entity.ThikrEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AthkarDao {

    // Category operations

    @Query("SELECT * FROM athkar_categories ORDER BY `order` ASC")
    fun getAllCategories(): Flow<List<AthkarCategoryEntity>>

    @Query("SELECT * FROM athkar_categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: String): AthkarCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<AthkarCategoryEntity>)

    @Query("SELECT COUNT(*) FROM athkar_categories")
    suspend fun getCategoryCount(): Int

    @Query("DELETE FROM athkar_categories")
    suspend fun deleteAllCategories()

    @Query("DELETE FROM athkar_categories WHERE id NOT LIKE 'api_%'")
    suspend fun deleteCategoriesNotFromApi()

    // Thikr operations

    @Query("SELECT * FROM athkar WHERE categoryId = :categoryId ORDER BY `order` ASC")
    fun getAthkarByCategory(categoryId: String): Flow<List<ThikrEntity>>

    @Query("SELECT * FROM athkar WHERE id = :id")
    suspend fun getThikrById(id: String): ThikrEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAthkar(athkar: List<ThikrEntity>)

    @Query("SELECT COUNT(*) FROM athkar")
    suspend fun getAthkarCount(): Int

    @Query("DELETE FROM athkar")
    suspend fun deleteAllAthkar()

    @Query("DELETE FROM athkar WHERE categoryId = :categoryId")
    suspend fun deleteAthkarByCategory(categoryId: String)

    @Query("DELETE FROM athkar WHERE categoryId NOT LIKE 'api_%'")
    suspend fun deleteAthkarNotFromApi()
}
