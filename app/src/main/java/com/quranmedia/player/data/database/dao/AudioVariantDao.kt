package com.quranmedia.player.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranmedia.player.data.database.entity.AudioVariantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioVariantDao {

    @Query("""
        SELECT * FROM audio_variants
        WHERE reciterId = :reciterId AND surahNumber = :surahNumber
        ORDER BY bitrate DESC
    """)
    fun getAudioVariants(reciterId: String, surahNumber: Int): Flow<List<AudioVariantEntity>>

    @Query("""
        SELECT * FROM audio_variants
        WHERE reciterId = :reciterId AND surahNumber = :surahNumber
        ORDER BY bitrate DESC
        LIMIT 1
    """)
    suspend fun getAudioVariant(reciterId: String, surahNumber: Int): AudioVariantEntity?

    @Query("SELECT * FROM audio_variants WHERE id = :variantId")
    suspend fun getAudioVariantById(variantId: String): AudioVariantEntity?

    @Query("SELECT * FROM audio_variants WHERE localPath IS NOT NULL")
    fun getDownloadedAudioVariants(): Flow<List<AudioVariantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioVariant(audioVariant: AudioVariantEntity)

    @Query("DELETE FROM audio_variants WHERE id = :variantId")
    suspend fun deleteAudioVariant(variantId: String)
}
