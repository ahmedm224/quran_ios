package com.quranmedia.player.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranmedia.player.data.database.entity.SurahEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SurahDao {

    @Query("SELECT * FROM surahs ORDER BY number ASC")
    fun getAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs WHERE number = :surahNumber")
    suspend fun getSurahByNumber(surahNumber: Int): SurahEntity?

    @Query("""
        SELECT * FROM surahs
        WHERE nameEnglish LIKE '%' || :query || '%'
        OR nameArabic LIKE '%' || :query || '%'
        OR nameTransliteration LIKE '%' || :query || '%'
        OR CAST(number AS TEXT) = :query
        ORDER BY number ASC
    """)
    suspend fun searchSurahs(query: String): List<SurahEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurah(surah: SurahEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahs(surahs: List<SurahEntity>)

    @Query("DELETE FROM surahs")
    suspend fun deleteAllSurahs()
}
