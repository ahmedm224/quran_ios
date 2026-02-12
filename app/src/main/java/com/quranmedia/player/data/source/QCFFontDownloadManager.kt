package com.quranmedia.player.data.source

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Font download state
 */
enum class FontDownloadState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    ERROR
}

/**
 * Download progress info
 */
data class FontDownloadProgress(
    val state: FontDownloadState = FontDownloadState.NOT_DOWNLOADED,
    val progress: Float = 0f,  // 0.0 to 1.0
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val errorMessage: String? = null
)

/**
 * Manager for downloading QCF font packs.
 * Handles downloading ZIP archives from the API and extracting fonts to app storage.
 *
 * Font packs:
 * - V2 (Plain): ~198 MB - Plain fonts that accept custom text colors
 * - V4 (Tajweed): ~159 MB - COLRv1 fonts with embedded Tajweed colors
 */
@Singleton
class QCFFontDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        // Base URL for font downloads - user can configure this
        private const val DEFAULT_BASE_URL = "https://alfurqan.online/api/v1/fonts"

        // Font pack ZIP filenames
        private const val V2_FONT_PACK = "qcf-v2.zip"
        private const val V4_FONT_PACK = "qcf-v4.zip"
        private const val SVG_FONT_PACK = "quran-svg.zip"

        // Local folder names for extracted fonts
        const val V2_FONTS_FOLDER = "qcf-v2"
        const val V4_FONTS_FOLDER = "qcf-v4"
        const val SVG_FONTS_FOLDER = "quran-svg"

        // Total number of font files expected (one per page)
        const val TOTAL_PAGES = 604
    }

    private val fontsDir = File(context.filesDir, "fonts")

    // Download state flows
    private val _v2DownloadProgress = MutableStateFlow(FontDownloadProgress())
    val v2DownloadProgress: StateFlow<FontDownloadProgress> = _v2DownloadProgress.asStateFlow()

    private val _v4DownloadProgress = MutableStateFlow(FontDownloadProgress())
    val v4DownloadProgress: StateFlow<FontDownloadProgress> = _v4DownloadProgress.asStateFlow()

    private val _svgDownloadProgress = MutableStateFlow(FontDownloadProgress())
    val svgDownloadProgress: StateFlow<FontDownloadProgress> = _svgDownloadProgress.asStateFlow()

    init {
        // Check existing downloads on init
        checkExistingDownloads()
    }

    /**
     * Check if fonts are already downloaded and update states
     */
    private fun checkExistingDownloads() {
        val v2Folder = File(fontsDir, V2_FONTS_FOLDER)
        val v4Folder = File(fontsDir, V4_FONTS_FOLDER)
        val svgFolder = File(fontsDir, SVG_FONTS_FOLDER)

        if (isValidFontFolder(v2Folder)) {
            _v2DownloadProgress.value = FontDownloadProgress(
                state = FontDownloadState.DOWNLOADED,
                progress = 1f
            )
        }

        if (isValidFontFolder(v4Folder)) {
            _v4DownloadProgress.value = FontDownloadProgress(
                state = FontDownloadState.DOWNLOADED,
                progress = 1f
            )
        }

        if (isValidSvgFolder(svgFolder)) {
            _svgDownloadProgress.value = FontDownloadProgress(
                state = FontDownloadState.DOWNLOADED,
                progress = 1f
            )
        }
    }

    /**
     * Check if a font folder contains valid font files
     */
    private fun isValidFontFolder(folder: File): Boolean {
        if (!folder.exists() || !folder.isDirectory) return false

        // Count .ttf files in folder
        val ttfFiles = folder.listFiles { file ->
            file.isFile && file.name.endsWith(".ttf", ignoreCase = true)
        }
        val fileCount = ttfFiles?.size ?: 0

        Timber.d("Font folder ${folder.name} has $fileCount .ttf files")

        // Consider valid if we have at least 600 font files (allowing for some missing)
        // Or check for specific files p1.ttf and p604.ttf
        val p1 = File(folder, "p1.ttf")
        val p604 = File(folder, "p604.ttf")

        return (p1.exists() && p604.exists()) || fileCount >= 600
    }

    /**
     * Check if an SVG folder contains valid SVG files
     */
    private fun isValidSvgFolder(folder: File): Boolean {
        if (!folder.exists() || !folder.isDirectory) return false

        val svgFiles = folder.listFiles { file ->
            file.isFile && file.name.endsWith(".svg", ignoreCase = true)
        }
        val fileCount = svgFiles?.size ?: 0

        Timber.d("SVG folder ${folder.name} has $fileCount .svg files")

        val p1 = File(folder, "001.svg")
        val p604 = File(folder, "604.svg")

        return (p1.exists() && p604.exists()) || fileCount >= 600
    }

    /**
     * Get the path to a specific font file if downloaded
     */
    fun getFontFile(pageNumber: Int, isTajweed: Boolean): File? {
        val folder = if (isTajweed) V4_FONTS_FOLDER else V2_FONTS_FOLDER
        val fontFile = File(fontsDir, "$folder/p$pageNumber.ttf")
        return if (fontFile.exists()) fontFile else null
    }

    /**
     * Get the path to a specific SVG file if downloaded
     */
    fun getSVGFile(pageNumber: Int): File? {
        val fileName = "${pageNumber.toString().padStart(3, '0')}.svg"
        val svgFile = File(fontsDir, "$SVG_FONTS_FOLDER/$fileName")
        return if (svgFile.exists()) svgFile else null
    }

    /**
     * Check if V2 fonts are downloaded
     */
    fun isV2Downloaded(): Boolean = _v2DownloadProgress.value.state == FontDownloadState.DOWNLOADED

    /**
     * Check if V4 fonts are downloaded
     */
    fun isV4Downloaded(): Boolean = _v4DownloadProgress.value.state == FontDownloadState.DOWNLOADED

    /**
     * Check if SVG pages are downloaded
     */
    fun isSVGDownloaded(): Boolean = _svgDownloadProgress.value.state == FontDownloadState.DOWNLOADED

    /**
     * Download V2 (Plain) fonts
     */
    suspend fun downloadV2Fonts(baseUrl: String = DEFAULT_BASE_URL): Boolean {
        return downloadFontPack(
            url = "$baseUrl/$V2_FONT_PACK",
            targetFolder = V2_FONTS_FOLDER,
            progressFlow = _v2DownloadProgress
        )
    }

    /**
     * Download V4 (Tajweed) fonts
     */
    suspend fun downloadV4Fonts(baseUrl: String = DEFAULT_BASE_URL): Boolean {
        return downloadFontPack(
            url = "$baseUrl/$V4_FONT_PACK",
            targetFolder = V4_FONTS_FOLDER,
            progressFlow = _v4DownloadProgress
        )
    }

    /**
     * Download SVG Quran pages
     */
    suspend fun downloadSVGFonts(baseUrl: String = DEFAULT_BASE_URL): Boolean {
        return downloadFontPack(
            url = "$baseUrl/$SVG_FONT_PACK",
            targetFolder = SVG_FONTS_FOLDER,
            progressFlow = _svgDownloadProgress,
            fileExtensions = listOf(".svg")
        )
    }

    /**
     * Download and extract a font pack
     */
    private suspend fun downloadFontPack(
        url: String,
        targetFolder: String,
        progressFlow: MutableStateFlow<FontDownloadProgress>,
        fileExtensions: List<String> = listOf(".ttf")
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            progressFlow.value = FontDownloadProgress(
                state = FontDownloadState.DOWNLOADING,
                progress = 0f
            )

            val request = Request.Builder()
                .url(url)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                progressFlow.value = FontDownloadProgress(
                    state = FontDownloadState.ERROR,
                    errorMessage = "Download failed: ${response.code}"
                )
                return@withContext false
            }

            val body = response.body ?: run {
                progressFlow.value = FontDownloadProgress(
                    state = FontDownloadState.ERROR,
                    errorMessage = "Empty response"
                )
                return@withContext false
            }

            val totalBytes = body.contentLength()
            var bytesDownloaded = 0L

            // Create temp file for download (unique per target to avoid conflicts)
            val cacheDir = context.cacheDir
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val tempFile = File(cacheDir, "font_download_${targetFolder}.zip")

            // Download to temp file
            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead

                        val progress = if (totalBytes > 0) {
                            bytesDownloaded.toFloat() / totalBytes.toFloat()
                        } else {
                            0f
                        }

                        progressFlow.value = FontDownloadProgress(
                            state = FontDownloadState.DOWNLOADING,
                            progress = progress * 0.9f,  // Reserve 10% for extraction
                            bytesDownloaded = bytesDownloaded,
                            totalBytes = totalBytes
                        )
                    }
                }
            }

            // Extract ZIP
            progressFlow.value = progressFlow.value.copy(progress = 0.9f)

            if (!fontsDir.exists()) fontsDir.mkdirs()
            val targetDir = File(fontsDir, targetFolder)
            extractZip(tempFile, targetDir, fileExtensions)

            // Clean up temp file
            tempFile.delete()

            // Verify extraction
            val isValid = if (fileExtensions.contains(".svg")) {
                isValidSvgFolder(targetDir)
            } else {
                isValidFontFolder(targetDir)
            }
            if (!isValid) {
                progressFlow.value = FontDownloadProgress(
                    state = FontDownloadState.ERROR,
                    errorMessage = "Extraction failed or incomplete"
                )
                return@withContext false
            }

            progressFlow.value = FontDownloadProgress(
                state = FontDownloadState.DOWNLOADED,
                progress = 1f
            )

            Timber.i("Font pack downloaded and extracted: $targetFolder")
            true

        } catch (e: Exception) {
            Timber.e(e, "Failed to download font pack")
            progressFlow.value = FontDownloadProgress(
                state = FontDownloadState.ERROR,
                errorMessage = e.message ?: "Unknown error"
            )
            false
        }
    }

    /**
     * Extract a ZIP file to target directory.
     * Handles nested folders by extracting font files directly to target directory.
     */
    private fun extractZip(zipFile: File, targetDir: File, fileExtensions: List<String> = listOf(".ttf")) {
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
        targetDir.mkdirs()

        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    // Get just the filename, ignoring any folder structure in ZIP
                    val fileName = File(entry.name).name

                    // Only extract files matching the specified extensions
                    if (fileExtensions.any { ext -> fileName.endsWith(ext, ignoreCase = true) }) {
                        val outFile = File(targetDir, fileName)
                        Timber.d("Extracting: ${entry.name} -> ${outFile.absolutePath}")
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // Log extracted files count
        val extractedCount = targetDir.listFiles()?.size ?: 0
        Timber.i("Extracted $extractedCount files to ${targetDir.absolutePath}")
    }

    /**
     * Delete V2 fonts
     */
    suspend fun deleteV2Fonts() = withContext(Dispatchers.IO) {
        val folder = File(fontsDir, V2_FONTS_FOLDER)
        if (folder.exists()) {
            folder.deleteRecursively()
        }
        _v2DownloadProgress.value = FontDownloadProgress(state = FontDownloadState.NOT_DOWNLOADED)
    }

    /**
     * Delete V4 fonts
     */
    suspend fun deleteV4Fonts() = withContext(Dispatchers.IO) {
        val folder = File(fontsDir, V4_FONTS_FOLDER)
        if (folder.exists()) {
            folder.deleteRecursively()
        }
        _v4DownloadProgress.value = FontDownloadProgress(state = FontDownloadState.NOT_DOWNLOADED)
    }

    /**
     * Delete SVG pages
     */
    suspend fun deleteSVGFonts() = withContext(Dispatchers.IO) {
        val folder = File(fontsDir, SVG_FONTS_FOLDER)
        if (folder.exists()) {
            folder.deleteRecursively()
        }
        _svgDownloadProgress.value = FontDownloadProgress(state = FontDownloadState.NOT_DOWNLOADED)
    }

    /**
     * Get size of downloaded V2 fonts in bytes
     */
    fun getV2FontsSize(): Long {
        val folder = File(fontsDir, V2_FONTS_FOLDER)
        return if (folder.exists()) getFolderSize(folder) else 0
    }

    /**
     * Get size of downloaded V4 fonts in bytes
     */
    fun getV4FontsSize(): Long {
        val folder = File(fontsDir, V4_FONTS_FOLDER)
        return if (folder.exists()) getFolderSize(folder) else 0
    }

    /**
     * Get size of downloaded SVG pages in bytes
     */
    fun getSVGFontsSize(): Long {
        val folder = File(fontsDir, SVG_FONTS_FOLDER)
        return if (folder.exists()) getFolderSize(folder) else 0
    }

    /**
     * Get total size of a folder
     */
    private fun getFolderSize(folder: File): Long {
        var size = 0L
        folder.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                getFolderSize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    /**
     * Format bytes to human readable string
     */
    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
            bytes >= 1_024 -> String.format("%.1f KB", bytes / 1_024.0)
            else -> "$bytes B"
        }
    }
}
