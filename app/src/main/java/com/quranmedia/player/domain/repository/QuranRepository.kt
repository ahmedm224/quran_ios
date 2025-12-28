package com.quranmedia.player.domain.repository

import com.quranmedia.player.data.api.model.SurahData
import com.quranmedia.player.domain.model.Ayah
import com.quranmedia.player.domain.model.AyahIndex
import com.quranmedia.player.domain.model.AudioVariant
import com.quranmedia.player.domain.model.Bookmark
import com.quranmedia.player.domain.model.Reciter
import com.quranmedia.player.domain.model.Surah
import kotlinx.coroutines.flow.Flow

interface QuranRepository {

    // Reciters
    fun getAllReciters(): Flow<List<Reciter>>
    suspend fun getReciterById(reciterId: String): Reciter?
    suspend fun insertReciters(reciters: List<Reciter>)

    // API fetching
    suspend fun getSurahWithAudio(surahNumber: Int, reciterId: String): SurahData?

    // Surahs
    fun getAllSurahs(): Flow<List<Surah>>
    suspend fun getSurahByNumber(surahNumber: Int): Surah?
    suspend fun insertSurahs(surahs: List<Surah>)

    // Audio Variants
    fun getAudioVariants(reciterId: String, surahNumber: Int): Flow<List<AudioVariant>>
    suspend fun getAudioVariant(reciterId: String, surahNumber: Int): AudioVariant?
    suspend fun insertAudioVariant(audioVariant: AudioVariant)

    // Ayah Index
    fun getAyahIndices(reciterId: String, surahNumber: Int): Flow<List<AyahIndex>>
    suspend fun getAyahIndexAt(reciterId: String, surahNumber: Int, positionMs: Long): AyahIndex?
    suspend fun insertAyahIndices(indices: List<AyahIndex>)

    // Bookmarks
    fun getAllBookmarks(): Flow<List<Bookmark>>
    suspend fun insertBookmark(bookmark: Bookmark)
    suspend fun deleteBookmark(bookmarkId: String)

    // Search
    suspend fun searchSurahs(query: String): List<Surah>
    suspend fun searchReciters(query: String): List<Reciter>

    // Ayahs by Page/Juz/Surah (for Quran Reader and offline playback)
    fun getAyahsByPage(pageNumber: Int): Flow<List<Ayah>>
    fun getAyahsByJuz(juzNumber: Int): Flow<List<Ayah>>
    fun getAyahsBySurah(surahNumber: Int): Flow<List<Ayah>>
    suspend fun getAyah(surahNumber: Int, ayahNumber: Int): Ayah?
    suspend fun getFirstPageOfSurah(surahNumber: Int): Int?
    suspend fun getFirstPageOfJuz(juzNumber: Int): Int?
    suspend fun getPageForAyah(surahNumber: Int, ayahNumber: Int): Int?
    fun getAllJuzNumbers(): Flow<List<Int>>
}
