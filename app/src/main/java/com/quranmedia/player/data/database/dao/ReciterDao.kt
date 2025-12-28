package com.quranmedia.player.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranmedia.player.data.database.entity.ReciterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReciterDao {

    @Query("SELECT * FROM reciters ORDER BY name ASC")
    fun getAllReciters(): Flow<List<ReciterEntity>>

    @Query("SELECT * FROM reciters WHERE id = :reciterId")
    suspend fun getReciterById(reciterId: String): ReciterEntity?

    @Query("SELECT COUNT(*) FROM reciters")
    suspend fun getReciterCount(): Int

    @Query("""
        SELECT * FROM reciters
        WHERE name LIKE '%' || :query || '%'
        OR nameArabic LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    suspend fun searchReciters(query: String): List<ReciterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReciter(reciter: ReciterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReciters(reciters: List<ReciterEntity>)

    @Query("DELETE FROM reciters WHERE id = :reciterId")
    suspend fun deleteReciterById(reciterId: String)

    @Query("DELETE FROM reciters")
    suspend fun deleteAllReciters()
}
