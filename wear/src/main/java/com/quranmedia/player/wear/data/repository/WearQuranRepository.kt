package com.quranmedia.player.wear.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quranmedia.player.wear.domain.model.QuranReadingPosition
import com.quranmedia.player.wear.domain.model.Surah
import com.quranmedia.player.wear.domain.model.Verse
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Quran data on Wear OS.
 * Loads Quran text from bundled JSON asset.
 */
@Singleton
class WearQuranRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private var cachedSurahs: List<Surah>? = null

    companion object {
        private const val QURAN_JSON_FILE = "tanzil_quran.json"
        private const val PREFS_NAME = "quran_prefs"
        private const val KEY_LAST_SURAH = "last_surah"
        private const val KEY_LAST_VERSE = "last_verse"
    }

    /**
     * Load all surahs from the JSON file.
     * Uses caching to avoid repeated file reads.
     */
    fun getAllSurahs(): List<Surah> {
        cachedSurahs?.let { return it }

        return try {
            val jsonString = context.assets.open(QURAN_JSON_FILE)
                .bufferedReader()
                .use { it.readText() }

            val listType = object : TypeToken<List<SurahJson>>() {}.type
            val surahsJson: List<SurahJson> = gson.fromJson(jsonString, listType)

            val surahs = surahsJson.map { it.toDomainModel() }
            cachedSurahs = surahs

            Timber.d("Loaded ${surahs.size} surahs from Quran JSON")
            surahs
        } catch (e: Exception) {
            Timber.e(e, "Failed to load Quran JSON")
            emptyList()
        }
    }

    /**
     * Get a specific surah by number (1-114).
     */
    fun getSurah(surahNumber: Int): Surah? {
        return getAllSurahs().find { it.id == surahNumber }
    }

    /**
     * Get surah names for index/selection.
     */
    fun getSurahNames(): List<Pair<Int, String>> {
        return getAllSurahs().map { it.id to it.name }
    }

    /**
     * Save reading position.
     */
    fun saveReadingPosition(position: QuranReadingPosition) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_LAST_SURAH, position.surahNumber)
            .putInt(KEY_LAST_VERSE, position.verseNumber)
            .apply()
    }

    /**
     * Get last reading position.
     */
    fun getReadingPosition(): QuranReadingPosition {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return QuranReadingPosition(
            surahNumber = prefs.getInt(KEY_LAST_SURAH, 1),
            verseNumber = prefs.getInt(KEY_LAST_VERSE, 1)
        )
    }
}

/**
 * JSON model for parsing
 */
private data class SurahJson(
    val id: Int,
    val name: String,
    val transliteration: String,
    val type: String,
    val total_verses: Int,
    val verses: List<VerseJson>
) {
    fun toDomainModel() = Surah(
        id = id,
        name = name,
        transliteration = transliteration,
        type = type,
        totalVerses = total_verses,
        verses = verses.map { it.toDomainModel() }
    )
}

private data class VerseJson(
    val id: Int,
    val text: String
) {
    fun toDomainModel() = Verse(id = id, text = text)
}
