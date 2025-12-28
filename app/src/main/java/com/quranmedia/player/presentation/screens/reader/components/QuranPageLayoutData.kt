package com.quranmedia.player.presentation.screens.reader.components

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.util.LinkedHashMap

data class QuranPageLayout(
    @SerializedName("page_number")
    val pageNumber: Int,
    @SerializedName("lines")
    val lines: List<QuranPageLine>,
    @SerializedName("surah_headers")
    val surahHeaders: List<SurahHeaderPosition> = emptyList()
)

data class QuranPageLine(
    @SerializedName("line_number")
    val lineNumber: Int,
    @SerializedName("line_type")
    val lineType: String,
    @SerializedName("is_centered")
    val isCentered: Boolean,
    @SerializedName("words")
    val words: List<QuranPageWord>,
    @SerializedName("surah_number")
    val surahNumber: Int?
)

data class QuranPageWord(
    @SerializedName("word_id")
    val wordId: Long,
    @SerializedName("word_index")
    val wordIndex: Int,
    @SerializedName("position")
    val position: Int,
    @SerializedName("verse_key")
    val verseKey: String,
    @SerializedName("surah_number")
    val surahNumber: Int,
    @SerializedName("ayah_number")
    val ayahNumber: Int,
    @SerializedName("glyph_code")
    val glyphCode: String,
    @SerializedName("text_uthmani")
    val textUthmani: String,
    @SerializedName("char_type")
    val charType: String
)

data class SurahHeaderPosition(
    @SerializedName("surah_number")
    val surahNumber: Int,
    @SerializedName("line_number")
    val lineNumber: Int
)

object QuranPageLayoutStore {
    private val mutex = Mutex()
    private var cachedPages: Map<Int, QuranPageLayout>? = null

    suspend fun getPage(context: Context, pageNumber: Int): QuranPageLayout? {
        if (pageNumber <= 0) return null
        return ensureLoaded(context)[pageNumber]
    }

    private suspend fun ensureLoaded(context: Context): Map<Int, QuranPageLayout> {
        cachedPages?.let { return it }
        return mutex.withLock {
            cachedPages?.let { return it }
            val loaded = loadPages(context)
            cachedPages = loaded
            loaded
        }
    }

    private suspend fun loadPages(context: Context): Map<Int, QuranPageLayout> {
        return withContext(Dispatchers.IO) {
            try {
                context.assets.open("quran_layout.json").use { input ->
                    InputStreamReader(input).use { reader ->
                        val type = object : TypeToken<Map<String, QuranPageLayout>>() {}.type
                        val raw: Map<String, QuranPageLayout> =
                            Gson().fromJson(reader, type) ?: emptyMap()
                        raw.mapNotNull { (key, value) ->
                            key.toIntOrNull()?.let { pageNumber -> pageNumber to value }
                        }.toMap()
                    }
                }
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
}

data class QuranPageFont(
    val family: FontFamily,
    val typeface: Typeface
)

object QuranPageFontStore {
    private const val MAX_CACHED_FONTS = 10
    private val mutex = Mutex()
    private val cache = LinkedHashMap<Int, QuranPageFont>(MAX_CACHED_FONTS, 0.75f, true)

    suspend fun getFont(context: Context, pageNumber: Int): QuranPageFont? {
        if (pageNumber !in 1..604) return null
        return mutex.withLock {
            cache[pageNumber]?.let { return it }
            val font = withContext(Dispatchers.IO) {
                val fileName = "font/qcf/qcf_p${pageNumber.toString().padStart(3, '0')}.ttf"
                val typeface = Typeface.createFromAsset(context.assets, fileName)
                QuranPageFont(FontFamily(typeface), typeface)
            }
            cache[pageNumber] = font
            trimCache()
            font
        }
    }

    private fun trimCache() {
        while (cache.size > MAX_CACHED_FONTS) {
            val firstKey = cache.entries.firstOrNull()?.key ?: break
            cache.remove(firstKey)
        }
    }
}
