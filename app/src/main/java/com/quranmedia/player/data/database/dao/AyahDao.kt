package com.quranmedia.player.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranmedia.player.data.database.entity.AyahEntity
import kotlinx.coroutines.flow.Flow

data class SurahPageInfo(
    val surahNumber: Int,
    val page: Int
)

data class JuzStartInfo(
    val juz: Int,
    val firstSurah: Int,
    val page: Int
)

data class HizbQuarterInfo(
    val hizbQuarter: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val page: Int,
    val juz: Int,
    val textArabic: String
)

@Dao
interface AyahDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyah(ayah: AyahEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(ayahs: List<AyahEntity>)

    @Query("SELECT * FROM ayahs WHERE surahNumber = :surahNumber ORDER BY ayahNumber ASC")
    fun getAyahsBySurah(surahNumber: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE surahNumber = :surahNumber ORDER BY ayahNumber ASC")
    suspend fun getAyahsBySurahSync(surahNumber: Int): List<AyahEntity>

    @Query("SELECT * FROM ayahs WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber")
    suspend fun getAyah(surahNumber: Int, ayahNumber: Int): AyahEntity?

    @Query("SELECT * FROM ayahs WHERE surahNumber = :surahNumber AND ayahNumber BETWEEN :startAyah AND :endAyah ORDER BY ayahNumber ASC")
    suspend fun getAyahRange(surahNumber: Int, startAyah: Int, endAyah: Int): List<AyahEntity>

    @Query("SELECT * FROM ayahs WHERE globalAyahNumber = :globalNumber")
    suspend fun getAyahByGlobalNumber(globalNumber: Int): AyahEntity?

    @Query("SELECT * FROM ayahs WHERE page = :pageNumber ORDER BY surahNumber, ayahNumber")
    fun getAyahsByPage(pageNumber: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE page = :pageNumber ORDER BY surahNumber, ayahNumber")
    suspend fun getAyahsByPageSync(pageNumber: Int): List<AyahEntity>

    @Query("SELECT * FROM ayahs WHERE juz = :juzNumber ORDER BY surahNumber, ayahNumber")
    fun getAyahsByJuz(juzNumber: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE sajda = 1 ORDER BY surahNumber, ayahNumber")
    fun getSajdaAyahs(): Flow<List<AyahEntity>>

    @Query("SELECT COUNT(*) FROM ayahs")
    suspend fun getAyahCount(): Int

    @Query("SELECT COUNT(*) FROM ayahs WHERE surahNumber = :surahNumber")
    suspend fun getAyahCountForSurah(surahNumber: Int): Int

    @Query("DELETE FROM ayahs")
    suspend fun deleteAllAyahs()

    @Query("DELETE FROM ayahs WHERE surahNumber = :surahNumber")
    suspend fun deleteAyahsForSurah(surahNumber: Int)

    @Query("SELECT * FROM ayahs ORDER BY globalAyahNumber ASC")
    fun getAllAyahs(): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE textArabic LIKE '%' || :query || '%' ORDER BY globalAyahNumber ASC")
    suspend fun searchAyahs(query: String): List<AyahEntity>

    @Query("SELECT * FROM ayahs WHERE juz = :juzNumber ORDER BY globalAyahNumber ASC LIMIT 1")
    suspend fun getFirstAyahOfJuz(juzNumber: Int): AyahEntity?

    @Query("SELECT MIN(page) FROM ayahs WHERE surahNumber = :surahNumber")
    suspend fun getFirstPageOfSurah(surahNumber: Int): Int?

    @Query("SELECT surahNumber, MIN(page) as page FROM ayahs GROUP BY surahNumber ORDER BY surahNumber")
    suspend fun getAllSurahStartPages(): List<SurahPageInfo>

    @Query("SELECT juz, MIN(surahNumber) as firstSurah, MIN(page) as page FROM ayahs WHERE juz > 0 GROUP BY juz ORDER BY juz")
    suspend fun getAllJuzStartInfo(): List<JuzStartInfo>

    @Query("""
        SELECT a.hizbQuarter, a.surahNumber, a.ayahNumber, a.page, a.juz, a.textArabic
        FROM ayahs a
        INNER JOIN (
            SELECT hizbQuarter, MIN(globalAyahNumber) as minGlobal
            FROM ayahs
            WHERE hizbQuarter > 0
            GROUP BY hizbQuarter
        ) b ON a.hizbQuarter = b.hizbQuarter AND a.globalAyahNumber = b.minGlobal
        ORDER BY a.hizbQuarter
    """)
    suspend fun getAllHizbQuartersInfo(): List<HizbQuarterInfo>

    @Query("SELECT MIN(page) FROM ayahs WHERE juz = :juzNumber")
    suspend fun getFirstPageOfJuz(juzNumber: Int): Int?

    @Query("SELECT DISTINCT juz FROM ayahs WHERE juz > 0 ORDER BY juz ASC")
    fun getAllJuzNumbers(): Flow<List<Int>>

    @Query("SELECT MAX(page) FROM ayahs WHERE page > 0")
    suspend fun getMaxPageNumber(): Int?

    @Query("SELECT page FROM ayahs WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber")
    suspend fun getPageForAyah(surahNumber: Int, ayahNumber: Int): Int?

    @Query("UPDATE ayahs SET juz = :juz, manzil = :manzil, page = :page, ruku = :ruku, hizbQuarter = :hizbQuarter, sajda = :sajda WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber")
    suspend fun updateAyahMetadata(
        surahNumber: Int,
        ayahNumber: Int,
        juz: Int,
        manzil: Int,
        page: Int,
        ruku: Int,
        hizbQuarter: Int,
        sajda: Boolean
    )

}
