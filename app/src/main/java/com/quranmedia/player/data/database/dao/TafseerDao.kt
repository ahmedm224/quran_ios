package com.quranmedia.player.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranmedia.player.data.database.entity.TafseerContentEntity
import com.quranmedia.player.data.database.entity.TafseerDownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TafseerDao {

    // ==================== Download Tracking ====================

    @Query("SELECT * FROM tafseer_downloads ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<TafseerDownloadEntity>>

    @Query("SELECT * FROM tafseer_downloads WHERE tafseerId = :tafseerId")
    suspend fun getDownload(tafseerId: String): TafseerDownloadEntity?

    @Query("SELECT * FROM tafseer_downloads WHERE type = :type ORDER BY downloadedAt DESC")
    fun getDownloadsByType(type: String): Flow<List<TafseerDownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: TafseerDownloadEntity)

    @Query("DELETE FROM tafseer_downloads WHERE tafseerId = :tafseerId")
    suspend fun deleteDownload(tafseerId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM tafseer_downloads WHERE tafseerId = :tafseerId)")
    suspend fun isDownloaded(tafseerId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM tafseer_downloads WHERE tafseerId = :tafseerId)")
    fun isDownloadedFlow(tafseerId: String): Flow<Boolean>

    // ==================== Content ====================

    @Query("SELECT * FROM tafseer_content WHERE tafseerId = :tafseerId AND surah = :surah AND ayah = :ayah")
    suspend fun getContent(tafseerId: String, surah: Int, ayah: Int): TafseerContentEntity?

    @Query("SELECT * FROM tafseer_content WHERE tafseerId = :tafseerId AND surah = :surah ORDER BY ayah")
    suspend fun getContentForSurah(tafseerId: String, surah: Int): List<TafseerContentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContent(content: TafseerContentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllContent(content: List<TafseerContentEntity>)

    @Query("DELETE FROM tafseer_content WHERE tafseerId = :tafseerId")
    suspend fun deleteAllContent(tafseerId: String)

    @Query("DELETE FROM tafseer_content WHERE tafseerId = :tafseerId AND surah = :surah")
    suspend fun deleteContentForSurah(tafseerId: String, surah: Int)

    @Query("SELECT COUNT(*) FROM tafseer_content WHERE tafseerId = :tafseerId")
    suspend fun getContentCount(tafseerId: String): Int

    @Query("SELECT COUNT(DISTINCT surah) FROM tafseer_content WHERE tafseerId = :tafseerId")
    suspend fun getSurahCount(tafseerId: String): Int

    // ==================== Get available tafseers for an ayah ====================

    @Query("""
        SELECT td.* FROM tafseer_downloads td
        INNER JOIN tafseer_content tc ON td.tafseerId = tc.tafseerId
        WHERE tc.surah = :surah AND tc.ayah = :ayah
        ORDER BY td.downloadedAt DESC
    """)
    suspend fun getAvailableTafseersForAyah(surah: Int, ayah: Int): List<TafseerDownloadEntity>

    /**
     * Get available tafseers that have ANY content for a surah.
     * Used when tafseers provide surah-level interpretation (like Ibn Kathir)
     * rather than per-ayah interpretation.
     */
    @Query("""
        SELECT DISTINCT td.* FROM tafseer_downloads td
        INNER JOIN tafseer_content tc ON td.tafseerId = tc.tafseerId
        WHERE tc.surah = :surah
        ORDER BY td.downloadedAt DESC
    """)
    suspend fun getAvailableTafseersForSurah(surah: Int): List<TafseerDownloadEntity>

    /**
     * Get the first available content for a surah (for surah-level tafseers)
     */
    @Query("SELECT * FROM tafseer_content WHERE tafseerId = :tafseerId AND surah = :surah ORDER BY ayah ASC LIMIT 1")
    suspend fun getFirstContentForSurah(tafseerId: String, surah: Int): TafseerContentEntity?
}
