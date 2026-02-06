package com.quranmedia.player.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.quranmedia.player.data.database.dao.TafseerDao
import com.quranmedia.player.data.database.entity.TafseerContentEntity
import com.quranmedia.player.data.database.entity.TafseerDownloadEntity
import com.quranmedia.player.domain.model.AvailableTafseers
import com.quranmedia.player.domain.model.TafseerContent
import com.quranmedia.player.domain.model.TafseerDownload
import com.quranmedia.player.domain.model.TafseerInfo
import com.quranmedia.player.domain.model.TafseerType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing Tafseer (Quran interpretation) data.
 * Handles downloading, caching, and retrieval of tafseer content.
 *
 * Uses alfurqan.online API:
 * - GET /api/v1/tafseer/downloads - List available tafseers
 * - GET /api/v1/tafseer/download/{id} - Download tafseer as ZIP file
 */
@Singleton
class TafseerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tafseerDao: TafseerDao,
    private val okHttpClient: OkHttpClient
) {
    private val gson = Gson()

    companion object {
        private const val API_BASE = "https://alfurqan.online"
        private const val TOTAL_SURAHS = 114
    }

    // Cache for Quran words from assets (used for mufradat Arabic words lookup)
    private var quranWordsCache: Map<String, String>? = null

    /**
     * Load Quran words from assets for Arabic word lookup.
     * Returns a map of "surah:ayah:wordIndex" -> "arabicWord"
     */
    private fun loadQuranWords(): Map<String, String> {
        quranWordsCache?.let { return it }

        return try {
            val jsonString = context.assets.open("quran_words_all.json").bufferedReader().use { it.readText() }
            val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)
            val wordsMap = mutableMapOf<String, String>()

            for (surahKey in jsonObject.keySet()) {
                val surahNum = surahKey.toIntOrNull() ?: continue
                val surahObj = jsonObject.getAsJsonObject(surahKey)
                val ayahsObj = surahObj.getAsJsonObject("ayahs") ?: continue

                for (ayahKey in ayahsObj.keySet()) {
                    val ayahNum = ayahKey.toIntOrNull() ?: continue
                    val ayahObj = ayahsObj.getAsJsonObject(ayahKey)
                    val wordsArray = ayahObj.getAsJsonArray("words") ?: continue

                    for (wordElement in wordsArray) {
                        val wordObj = wordElement.asJsonObject
                        val index = wordObj.get("index")?.asInt ?: continue
                        val arabic = wordObj.get("arabic")?.asString ?: continue
                        wordsMap["$surahNum:$ayahNum:$index"] = arabic
                    }
                }
            }

            quranWordsCache = wordsMap
            Timber.d("Loaded ${wordsMap.size} Quran words from assets")
            wordsMap
        } catch (e: Exception) {
            Timber.e(e, "Error loading Quran words from assets")
            emptyMap()
        }
    }

    /**
     * Get Arabic word from cache by surah, ayah, and word index.
     */
    private fun getArabicWord(surah: Int, ayah: Int, wordIndex: Int): String? {
        val wordsMap = loadQuranWords()
        return wordsMap["$surah:$ayah:$wordIndex"]
    }

    // ==================== Download Status ====================

    /**
     * Get all downloaded tafseers
     */
    fun getDownloadedTafseers(): Flow<List<TafseerDownload>> {
        return tafseerDao.getAllDownloads().map { entities ->
            entities.mapNotNull { entity ->
                AvailableTafseers.getById(entity.tafseerId)?.let { info ->
                    TafseerDownload(
                        tafseerInfo = info,
                        isDownloaded = true,
                        downloadedAt = entity.downloadedAt,
                        totalSizeBytes = entity.totalSizeBytes
                    )
                }
            }
        }
    }

    /**
     * Check if a specific tafseer is downloaded
     */
    suspend fun isDownloaded(tafseerId: String): Boolean {
        return tafseerDao.isDownloaded(tafseerId)
    }

    /**
     * Get download status as Flow
     */
    fun isDownloadedFlow(tafseerId: String): Flow<Boolean> {
        return tafseerDao.isDownloadedFlow(tafseerId)
    }

    /**
     * Get available tafseers for an ayah (ones that are downloaded)
     */
    suspend fun getAvailableTafseersForAyah(surah: Int, ayah: Int): List<TafseerInfo> {
        val downloads = tafseerDao.getAvailableTafseersForSurah(surah)
        return downloads.mapNotNull { entity ->
            AvailableTafseers.getById(entity.tafseerId)
        }
    }

    // ==================== Content Retrieval ====================

    /**
     * Get tafseer content for a specific ayah.
     */
    suspend fun getTafseerForAyah(tafseerId: String, surah: Int, ayah: Int): TafseerContent? {
        val entity = tafseerDao.getContent(tafseerId, surah, ayah)
        return entity?.let {
            TafseerContent(
                tafseerId = it.tafseerId,
                surah = it.surah,
                ayah = it.ayah,
                text = it.text
            )
        }
    }

    /**
     * Get all available tafseer content for an ayah (from all downloaded tafseers)
     */
    suspend fun getAllTafseersForAyah(surah: Int, ayah: Int): List<Pair<TafseerInfo, TafseerContent>> {
        val result = mutableListOf<Pair<TafseerInfo, TafseerContent>>()

        val availableTafseers = getAvailableTafseersForAyah(surah, ayah)
        for (tafseerInfo in availableTafseers) {
            val content = getTafseerForAyah(tafseerInfo.id, surah, ayah)
            if (content != null) {
                result.add(tafseerInfo to content)
            }
        }

        return result
    }

    // ==================== Download Operations ====================

    /**
     * Download a tafseer from alfurqan.online API or load from bundled assets.
     * Downloads ZIP file, extracts JSON, and saves to database.
     *
     * @param tafseerId The tafseer ID to download
     * @param onProgress Progress callback (0.0 to 1.0)
     * @return true if download was successful
     */
    suspend fun downloadTafseer(
        tafseerId: String,
        onProgress: ((Float) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val tafseerInfo = AvailableTafseers.getById(tafseerId)
        if (tafseerInfo == null) {
            Timber.e("Unknown tafseer ID: $tafseerId")
            return@withContext false
        }

        // Check if this is a bundled tafseer (loaded from assets)
        if (tafseerInfo.downloadUrl.startsWith("bundled:")) {
            return@withContext loadBundledTafseer(tafseerId, tafseerInfo, onProgress)
        }

        try {
            Timber.d("Starting download for tafseer: $tafseerId")
            onProgress?.invoke(0.05f)

            // Download ZIP file
            val downloadUrl = "$API_BASE${tafseerInfo.downloadUrl}"
            Timber.d("Downloading from: $downloadUrl")

            val request = Request.Builder()
                .url(downloadUrl)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Timber.e("Failed to download tafseer: ${response.code}")
                return@withContext false
            }

            val responseBody = response.body ?: return@withContext false
            val totalBytes = responseBody.contentLength()
            onProgress?.invoke(0.1f)

            // Save ZIP to temp file
            val tempZipFile = File(context.cacheDir, "tafseer_$tafseerId.zip")
            responseBody.byteStream().use { input ->
                FileOutputStream(tempZipFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            // Download progress: 10% to 40%
                            onProgress?.invoke(0.1f + (totalRead.toFloat() / totalBytes) * 0.3f)
                        }
                    }
                }
            }
            Timber.d("ZIP downloaded: ${tempZipFile.length()} bytes")
            onProgress?.invoke(0.4f)

            // Extract JSON from ZIP to a temp file (instead of loading into memory)
            val tempJsonFile = File(context.cacheDir, "tafseer_$tafseerId.json")
            val extracted = extractJsonFromZipToFile(tempZipFile, tempJsonFile)
            tempZipFile.delete()

            if (!extracted) {
                Timber.e("Failed to extract JSON from ZIP")
                tempJsonFile.delete()
                return@withContext false
            }
            Timber.d("JSON extracted to file: ${tempJsonFile.length()} bytes")
            onProgress?.invoke(0.5f)

            // Parse JSON using streaming and save to database
            val success = parseTafseerJsonStreaming(tafseerId, tempJsonFile, onProgress)
            tempJsonFile.delete()

            if (success) {
                val contentCount = tafseerDao.getContentCount(tafseerId)
                val downloadEntity = TafseerDownloadEntity(
                    tafseerId = tafseerId,
                    nameArabic = tafseerInfo.nameArabic ?: "",
                    nameEnglish = tafseerInfo.nameEnglish,
                    language = tafseerInfo.language,
                    type = tafseerInfo.type.name.lowercase(),
                    downloadedAt = System.currentTimeMillis(),
                    totalSizeBytes = totalBytes,
                    surahsDownloaded = TOTAL_SURAHS
                )
                tafseerDao.insertDownload(downloadEntity)
                Timber.d("Tafseer download completed: $tafseerId ($contentCount ayahs)")
                onProgress?.invoke(1f)
                return@withContext true
            }

            false
        } catch (e: Exception) {
            Timber.e(e, "Error downloading tafseer: $tafseerId")
            false
        }
    }

    /**
     * Load a bundled tafseer from assets folder.
     * Used for pre-installed tafseers like Irab Al-Quran.
     */
    private suspend fun loadBundledTafseer(
        tafseerId: String,
        tafseerInfo: TafseerInfo,
        onProgress: ((Float) -> Unit)?
    ): Boolean {
        return try {
            Timber.d("Loading bundled tafseer: $tafseerId")
            onProgress?.invoke(0.1f)

            // Get asset file name from downloadUrl (format: "bundled:filename.json")
            val assetFileName = tafseerInfo.downloadUrl.removePrefix("bundled:")

            // Copy asset to temp file for streaming parser
            val tempJsonFile = File(context.cacheDir, "tafseer_$tafseerId.json")
            context.assets.open(assetFileName).use { input ->
                FileOutputStream(tempJsonFile).use { output ->
                    input.copyTo(output)
                }
            }
            Timber.d("Copied asset to temp file: ${tempJsonFile.length()} bytes")
            onProgress?.invoke(0.3f)

            // Parse JSON using streaming and save to database
            val success = parseTafseerJsonStreaming(tafseerId, tempJsonFile, onProgress)
            tempJsonFile.delete()

            if (success) {
                val contentCount = tafseerDao.getContentCount(tafseerId)
                val downloadEntity = TafseerDownloadEntity(
                    tafseerId = tafseerId,
                    nameArabic = tafseerInfo.nameArabic ?: "",
                    nameEnglish = tafseerInfo.nameEnglish,
                    language = tafseerInfo.language,
                    type = tafseerInfo.type.name.lowercase(),
                    downloadedAt = System.currentTimeMillis(),
                    totalSizeBytes = 0L,  // Bundled, no download size
                    surahsDownloaded = TOTAL_SURAHS
                )
                tafseerDao.insertDownload(downloadEntity)
                Timber.d("Bundled tafseer loaded: $tafseerId ($contentCount ayahs)")
                onProgress?.invoke(1f)
                return true
            }

            false
        } catch (e: Exception) {
            Timber.e(e, "Error loading bundled tafseer: $tafseerId")
            false
        }
    }

    /**
     * Extract JSON from ZIP to a file (memory-efficient).
     * Expects a single JSON file inside the ZIP.
     */
    private fun extractJsonFromZipToFile(zipFile: File, outputFile: File): Boolean {
        return try {
            ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith(".json")) {
                        FileOutputStream(outputFile).use { fos ->
                            val buffer = ByteArray(8192)
                            var len: Int
                            while (zis.read(buffer).also { len = it } != -1) {
                                fos.write(buffer, 0, len)
                            }
                        }
                        zis.closeEntry()
                        return@use true
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error extracting ZIP to file")
            false
        }
    }

    /**
     * Parse tafseer JSON using streaming (memory-efficient).
     * Handles the main format: Array of objects with surah/ayah/text
     */
    private suspend fun parseTafseerJsonStreaming(
        tafseerId: String,
        jsonFile: File,
        onProgress: ((Float) -> Unit)?
    ): Boolean {
        return try {
            // Delete existing content first
            tafseerDao.deleteAllContent(tafseerId)

            val fileSize = jsonFile.length()
            var bytesRead = 0L
            val batch = mutableListOf<TafseerContentEntity>()
            var totalCount = 0

            JsonReader(jsonFile.bufferedReader()).use { reader ->
                // Check if it starts with array or object
                val token = reader.peek()

                if (token == JsonToken.BEGIN_ARRAY) {
                    // Format: Array of objects with surah/ayah/text
                    reader.beginArray()

                    while (reader.hasNext()) {
                        val entity = parseAyahObject(reader, tafseerId)
                        if (entity != null) {
                            batch.add(entity)
                            totalCount++

                            // Insert in batches of 500
                            if (batch.size >= 500) {
                                tafseerDao.insertAllContent(batch)
                                batch.clear()

                                // Estimate progress based on count (assuming ~6236 ayahs)
                                val progress = 0.5f + (totalCount.toFloat() / 6500f) * 0.4f
                                onProgress?.invoke(minOf(progress, 0.9f))
                            }
                        }
                    }

                    reader.endArray()
                } else if (token == JsonToken.BEGIN_OBJECT) {
                    // Format: Object - need to handle different structures
                    reader.beginObject()

                    while (reader.hasNext()) {
                        val name = reader.nextName()

                        when {
                            name == "surahs" || name == "data" -> {
                                // Array inside object
                                reader.beginArray()
                                while (reader.hasNext()) {
                                    if (name == "surahs") {
                                        // Parse surah object with ayahs
                                        parseSurahObject(reader, tafseerId, batch)
                                    } else {
                                        // Parse direct ayah object
                                        val entity = parseAyahObject(reader, tafseerId)
                                        if (entity != null) batch.add(entity)
                                    }
                                    totalCount++

                                    if (batch.size >= 500) {
                                        tafseerDao.insertAllContent(batch)
                                        batch.clear()
                                        onProgress?.invoke(0.5f + (totalCount.toFloat() / 6500f) * 0.4f)
                                    }
                                }
                                reader.endArray()
                            }
                            name.toIntOrNull() != null -> {
                                // Numeric surah key format
                                val surahNum = name.toInt()
                                parseNumericSurahData(reader, tafseerId, surahNum, batch)
                                totalCount += batch.size

                                if (batch.size >= 500) {
                                    tafseerDao.insertAllContent(batch)
                                    batch.clear()
                                    onProgress?.invoke(0.5f + (surahNum.toFloat() / 114f) * 0.4f)
                                }
                            }
                            else -> reader.skipValue()
                        }
                    }

                    reader.endObject()
                }
            }

            // Insert remaining batch
            if (batch.isNotEmpty()) {
                tafseerDao.insertAllContent(batch)
            }

            onProgress?.invoke(0.95f)

            val finalCount = tafseerDao.getContentCount(tafseerId)
            Timber.d("Streaming parse completed: $finalCount ayahs for tafseer $tafseerId")

            finalCount > 0
        } catch (e: Exception) {
            Timber.e(e, "Error streaming tafseer JSON")
            false
        }
    }

    /**
     * Parse a single ayah object from JsonReader
     */
    private fun parseAyahObject(reader: JsonReader, tafseerId: String): TafseerContentEntity? {
        var surah: Int? = null
        var ayah: Int? = null
        var text: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "surah", "sura", "chapter", "surah_id" -> surah = reader.nextInt()
                "ayah", "aya", "verse", "ayah_number" -> ayah = reader.nextInt()
                "text", "tafseer", "content", "translation", "irab" -> text = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return if (surah != null && ayah != null && text != null) {
            TafseerContentEntity(
                tafseerId = tafseerId,
                surah = surah,
                ayah = ayah,
                text = cleanHtmlTags(text)
            )
        } else null
    }

    /**
     * Parse a surah object containing ayahs array
     */
    private fun parseSurahObject(
        reader: JsonReader,
        tafseerId: String,
        batch: MutableList<TafseerContentEntity>
    ) {
        var surahId: Int? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (val name = reader.nextName()) {
                "surah_id", "surah", "number" -> surahId = reader.nextInt()
                "ayahs", "verses" -> {
                    if (surahId != null) {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            parseAyahInSurah(reader, tafseerId, surahId, batch)
                        }
                        reader.endArray()
                    } else {
                        reader.skipValue()
                    }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
    }

    /**
     * Parse an ayah within a surah object
     */
    private fun parseAyahInSurah(
        reader: JsonReader,
        tafseerId: String,
        surahId: Int,
        batch: MutableList<TafseerContentEntity>
    ) {
        var ayahNumber: Int? = null
        var text: String? = null
        val wordMeanings = StringBuilder()

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "ayah_number", "ayah", "verse" -> ayahNumber = reader.nextInt()
                "text", "tafseer", "content" -> text = reader.nextString()
                "words" -> {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        parseWord(reader, surahId, ayahNumber ?: 0, wordMeanings)
                    }
                    reader.endArray()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        val finalText = if (wordMeanings.isNotBlank()) {
            wordMeanings.toString().trim()
        } else {
            text?.let { cleanHtmlTags(it) }
        }

        if (ayahNumber != null && finalText != null) {
            batch.add(TafseerContentEntity(
                tafseerId = tafseerId,
                surah = surahId,
                ayah = ayahNumber,
                text = finalText
            ))
        }
    }

    /**
     * Parse a word object for word-by-word format
     */
    private fun parseWord(
        reader: JsonReader,
        surahId: Int,
        ayahNumber: Int,
        wordMeanings: StringBuilder
    ) {
        var arabic: String? = null
        var translation: String? = null
        var meaning: String? = null
        var wordNumber: Int? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "arabic" -> arabic = reader.nextString()
                "translation" -> translation = reader.nextString()
                "meaning" -> meaning = reader.nextString()
                "word_number" -> wordNumber = reader.nextInt()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        when {
            arabic != null && translation != null -> {
                wordMeanings.append("$arabic: $translation\n\n")
            }
            wordNumber != null && meaning != null -> {
                val arabicWord = getArabicWord(surahId, ayahNumber, wordNumber)
                if (arabicWord != null) {
                    wordMeanings.append("$arabicWord: $meaning\n\n")
                } else {
                    wordMeanings.append("$meaning\n\n")
                }
            }
            meaning != null -> wordMeanings.append("$meaning\n\n")
            translation != null -> wordMeanings.append("$translation\n\n")
        }
    }

    /**
     * Parse numeric surah data (format 4)
     */
    private fun parseNumericSurahData(
        reader: JsonReader,
        tafseerId: String,
        surahNum: Int,
        batch: MutableList<TafseerContentEntity>
    ) {
        when (reader.peek()) {
            JsonToken.BEGIN_ARRAY -> {
                reader.beginArray()
                while (reader.hasNext()) {
                    var ayahNum: Int? = null
                    var text: String? = null

                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "ayah", "aya" -> ayahNum = reader.nextInt()
                            "text", "tafseer", "irab" -> text = reader.nextString()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()

                    if (ayahNum != null && text != null) {
                        batch.add(TafseerContentEntity(
                            tafseerId = tafseerId,
                            surah = surahNum,
                            ayah = ayahNum,
                            text = cleanHtmlTags(text)
                        ))
                    }
                }
                reader.endArray()
            }
            JsonToken.BEGIN_OBJECT -> {
                reader.beginObject()
                while (reader.hasNext()) {
                    val ayahKey = reader.nextName()
                    val ayahNum = ayahKey.toIntOrNull()

                    if (ayahNum != null) {
                        val text = when (reader.peek()) {
                            JsonToken.STRING -> reader.nextString()
                            JsonToken.BEGIN_OBJECT -> {
                                var t: String? = null
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    when (reader.nextName()) {
                                        "text", "tafseer", "irab" -> t = reader.nextString()
                                        else -> reader.skipValue()
                                    }
                                }
                                reader.endObject()
                                t
                            }
                            else -> {
                                reader.skipValue()
                                null
                            }
                        }

                        if (text != null) {
                            batch.add(TafseerContentEntity(
                                tafseerId = tafseerId,
                                surah = surahNum,
                                ayah = ayahNum,
                                text = cleanHtmlTags(text)
                            ))
                        }
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }
            else -> reader.skipValue()
        }
    }

    /**
     * Clean HTML tags from text
     */
    private fun cleanHtmlTags(text: String): String {
        return text
            .replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .trim()
    }

    /**
     * Delete a downloaded tafseer
     */
    suspend fun deleteTafseer(tafseerId: String) = withContext(Dispatchers.IO) {
        try {
            tafseerDao.deleteAllContent(tafseerId)
            tafseerDao.deleteDownload(tafseerId)
            Timber.d("Deleted tafseer: $tafseerId")
        } catch (e: Exception) {
            Timber.e(e, "Error deleting tafseer: $tafseerId")
        }
    }
}
