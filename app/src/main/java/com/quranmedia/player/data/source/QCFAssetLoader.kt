package com.quranmedia.player.data.source

import android.content.Context
import android.graphics.Typeface
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.compose.ui.text.font.FontFamily
import com.quranmedia.player.data.model.QCFFontMode
import com.quranmedia.player.data.model.QCFPageData
import com.quranmedia.player.data.model.getFontFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loader for QCF (Quran Complex Fonts) assets.
 * Handles loading page JSON data and per-page fonts.
 *
 * Font Loading Priority:
 * 1. Downloaded fonts (from QCFFontDownloadManager)
 * 2. Bundled assets (fallback if no download)
 *
 * Font Modes:
 * - V2 (PLAIN): Plain fonts that accept custom text colors
 * - V4 (TAJWEED): COLRv1 fonts with embedded Tajweed colors
 *
 * Both font types use qpcV2 glyph codes for rendering.
 */
@Singleton
class QCFAssetLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fontDownloadManager: QCFFontDownloadManager
) {
    companion object {
        const val TOTAL_PAGES = 604
        private const val PAGES_FOLDER = "qcf-pages"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Font cache with LRU-like behavior
    // Key format: "${fontMode.name}_$pageNumber"
    private val fontCache = mutableMapOf<String, FontFamily>()
    private val fontCacheMutex = Mutex()
    private val maxCacheSize = 20 // Keep ~20 fonts in memory

    // Page data cache
    private val pageDataCache = mutableMapOf<Int, QCFPageData>()
    private val pageDataMutex = Mutex()
    private val maxPageDataCacheSize = 30

    /**
     * Load page JSON data by page number.
     * Files are at: assets/qcf-pages/page-001.json to page-604.json
     */
    suspend fun loadPageData(pageNumber: Int): QCFPageData? {
        if (pageNumber !in 1..TOTAL_PAGES) return null

        // Check cache first
        pageDataMutex.withLock {
            pageDataCache[pageNumber]?.let { return it }
        }

        return withContext(Dispatchers.IO) {
            try {
                val fileName = "$PAGES_FOLDER/page-${pageNumber.toString().padStart(3, '0')}.json"
                val jsonContent = context.assets.open(fileName).bufferedReader().use { it.readText() }
                val pageData = json.decodeFromString<QCFPageData>(jsonContent)

                // Cache the result
                pageDataMutex.withLock {
                    // Evict oldest entries if cache is full
                    if (pageDataCache.size >= maxPageDataCacheSize) {
                        val keysToRemove = pageDataCache.keys.take(pageDataCache.size - maxPageDataCacheSize + 1)
                        keysToRemove.forEach { pageDataCache.remove(it) }
                    }
                    pageDataCache[pageNumber] = pageData
                }

                pageData
            } catch (e: Exception) {
                Timber.e(e, "Failed to load QCF page data for page $pageNumber")
                null
            }
        }
    }

    /**
     * Load font for a specific page and mode.
     * Priority: Downloaded fonts > Bundled assets
     * - PLAIN mode: loads from qcf-v2/p{page}.ttf
     * - TAJWEED mode: loads from qcf-v4/p{page}.ttf
     */
    suspend fun loadFont(pageNumber: Int, fontMode: QCFFontMode): FontFamily? {
        if (pageNumber !in 1..TOTAL_PAGES) return null

        val cacheKey = "${fontMode.name}_$pageNumber"

        // Check cache first
        fontCacheMutex.withLock {
            fontCache[cacheKey]?.let { return it }
        }

        return withContext(Dispatchers.IO) {
            try {
                val isTajweed = fontMode == QCFFontMode.TAJWEED

                // Try downloaded fonts first
                val downloadedFile = fontDownloadManager.getFontFile(pageNumber, isTajweed)
                val typeface = if (downloadedFile != null && downloadedFile.exists()) {
                    Timber.d("Loading downloaded font for page $pageNumber, mode $fontMode")
                    Typeface.createFromFile(downloadedFile)
                } else {
                    // Fall back to bundled assets
                    val fontFolder = fontMode.getFontFolder()
                    val fileName = "$fontFolder/p$pageNumber.ttf"
                    Timber.d("Loading asset font for page $pageNumber, mode $fontMode")
                    Typeface.createFromAsset(context.assets, fileName)
                }

                val fontFamily = FontFamily(
                    androidx.compose.ui.text.font.Typeface(typeface)
                )

                // Cache the result
                fontCacheMutex.withLock {
                    // Evict oldest entries if cache is full
                    if (fontCache.size >= maxCacheSize) {
                        val keysToRemove = fontCache.keys.take(fontCache.size - maxCacheSize + 1)
                        keysToRemove.forEach { fontCache.remove(it) }
                    }
                    fontCache[cacheKey] = fontFamily
                }

                fontFamily
            } catch (e: Exception) {
                Timber.e(e, "Failed to load QCF font for page $pageNumber, mode $fontMode")
                // Fallback: try loading from the other font mode (downloaded or asset)
                if (fontMode == QCFFontMode.PLAIN) {
                    try {
                        // Try downloaded V4 first
                        val downloadedFallback = fontDownloadManager.getFontFile(pageNumber, true)
                        val typeface = if (downloadedFallback != null && downloadedFallback.exists()) {
                            Typeface.createFromFile(downloadedFallback)
                        } else {
                            val fallbackFileName = "${QCFFontMode.TAJWEED.getFontFolder()}/p$pageNumber.ttf"
                            Typeface.createFromAsset(context.assets, fallbackFileName)
                        }
                        FontFamily(androidx.compose.ui.text.font.Typeface(typeface))
                    } catch (e2: Exception) {
                        Timber.e(e2, "Fallback font load also failed for page $pageNumber")
                        null
                    }
                } else {
                    null
                }
            }
        }
    }

    /**
     * Check if fonts are available for a given mode (either downloaded or bundled)
     */
    fun isFontAvailable(fontMode: QCFFontMode): Boolean {
        val isTajweed = fontMode == QCFFontMode.TAJWEED

        // Check downloaded fonts first
        if (isTajweed && fontDownloadManager.isV4Downloaded()) return true
        if (!isTajweed && fontDownloadManager.isV2Downloaded()) return true

        // Check bundled assets
        return try {
            val fontFolder = fontMode.getFontFolder()
            context.assets.list(fontFolder)?.isNotEmpty() == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Preload fonts for pages around the current page.
     * Useful for smoother page transitions.
     */
    suspend fun preloadFonts(currentPage: Int, fontMode: QCFFontMode, range: Int = 2) {
        val startPage = (currentPage - range).coerceAtLeast(1)
        val endPage = (currentPage + range).coerceAtMost(TOTAL_PAGES)

        for (page in startPage..endPage) {
            loadFont(page, fontMode)
        }
    }

    /**
     * Preload page data for pages around the current page.
     */
    suspend fun preloadPageData(currentPage: Int, range: Int = 2) {
        val startPage = (currentPage - range).coerceAtLeast(1)
        val endPage = (currentPage + range).coerceAtMost(TOTAL_PAGES)

        for (page in startPage..endPage) {
            loadPageData(page)
        }
    }

    /**
     * Clear all caches. Useful when switching font modes or low memory.
     */
    suspend fun clearCache() {
        fontCacheMutex.withLock { fontCache.clear() }
        pageDataMutex.withLock { pageDataCache.clear() }
    }

    /**
     * Get the QPC V2 glyph string for rendering a line.
     * Words are joined with spaces for proper QCF rendering.
     * CRITICAL: Must use " " (space), NOT "" (empty string).
     */
    fun getLineGlyphs(line: com.quranmedia.player.data.model.QCFLineData): String {
        return when (line.type) {
            com.quranmedia.player.data.model.QCFLineType.BASMALA -> {
                // Basmala lines have incorrect qpcV2 codes in JSON
                // Return empty to trigger fallback to Arabic Unicode
                ""
            }
            com.quranmedia.player.data.model.QCFLineType.TEXT -> {
                line.words?.joinToString(" ") { it.qpcV2 } ?: ""
            }
            com.quranmedia.player.data.model.QCFLineType.SURAH_HEADER -> {
                // Headers use different rendering (custom surah header)
                ""
            }
        }
    }

    /**
     * Get Arabic text for a line (fallback if font rendering fails).
     */
    fun getLineText(line: com.quranmedia.player.data.model.QCFLineData): String {
        return line.text ?: line.words?.joinToString(" ") { it.word } ?: ""
    }

    /**
     * Check if page number is valid.
     */
    fun isValidPage(pageNumber: Int): Boolean = pageNumber in 1..TOTAL_PAGES

    /**
     * Check if SVG Quran pages are available (downloaded or bundled).
     */
    fun isSVGAvailable(): Boolean {
        // Check downloaded SVG pages first
        if (fontDownloadManager.isSVGDownloaded()) return true

        // Fall back to bundled assets
        return try {
            val list = context.assets.list("quran-svg")
            (list?.size ?: 0) >= 600
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Load SVG content for a specific page.
     * Priority: Downloaded files > Bundled assets
     * Downloaded: context.filesDir/fonts/quran-svg/001.svg
     * Bundled: assets/quran-svg/001.svg
     */
    suspend fun loadSVG(pageNumber: Int): String? {
        if (pageNumber !in 1..TOTAL_PAGES) return null

        return withContext(Dispatchers.IO) {
            try {
                // Try downloaded SVG first
                val downloadedFile = fontDownloadManager.getSVGFile(pageNumber)
                if (downloadedFile != null && downloadedFile.exists()) {
                    return@withContext downloadedFile.readText()
                }

                // Fall back to bundled assets
                val fileName = "quran-svg/${pageNumber.toString().padStart(3, '0')}.svg"
                context.assets.open(fileName).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load SVG for page $pageNumber")
                null
            }
        }
    }
}
