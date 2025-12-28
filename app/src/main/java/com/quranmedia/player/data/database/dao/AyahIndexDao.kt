package com.quranmedia.player.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranmedia.player.data.database.entity.AyahIndexEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AyahIndexDao {

    @Query("""
        SELECT * FROM ayah_index
        WHERE reciterId = :reciterId AND surahNumber = :surahNumber
        ORDER BY ayahNumber ASC
    """)
    fun getAyahIndices(reciterId: String, surahNumber: Int): Flow<List<AyahIndexEntity>>

    @Query("""
        SELECT * FROM ayah_index
        WHERE reciterId = :reciterId
        AND surahNumber = :surahNumber
        AND startMs <= :positionMs
        AND endMs >= :positionMs
        LIMIT 1
    """)
    suspend fun getAyahIndexAt(
        reciterId: String,
        surahNumber: Int,
        positionMs: Long
    ): AyahIndexEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahIndices(indices: List<AyahIndexEntity>)

    @Query("""
        DELETE FROM ayah_index
        WHERE reciterId = :reciterId AND surahNumber = :surahNumber
    """)
    suspend fun deleteAyahIndices(reciterId: String, surahNumber: Int)
}
