package com.quranmedia.player.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quranmedia.player.data.api.model.TanzilSurah
import com.quranmedia.player.data.database.dao.AyahDao
import com.quranmedia.player.data.database.dao.SurahDao
import com.quranmedia.player.data.database.entity.AyahEntity
import com.quranmedia.player.data.database.entity.SurahEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber

/**
 * Worker to load all 114 Surahs with Ayah data from Tanzil JSON file
 * and metadata from Tanzil XML, then populate the local database
 */
@HiltWorker
class QuranDataPopulatorWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val surahDao: SurahDao,
    private val ayahDao: AyahDao
) : CoroutineWorker(context, workerParams) {

    // Data classes for metadata parsing
    private data class PageInfo(val index: Int, val sura: Int, val aya: Int)
    private data class JuzInfo(val index: Int, val sura: Int, val aya: Int)
    private data class ManzilInfo(val index: Int, val sura: Int, val aya: Int)
    private data class HizbInfo(val index: Int, val sura: Int, val aya: Int)
    private data class RukuInfo(val index: Int, val sura: Int, val aya: Int)
    private data class SajdaInfo(val sura: Int, val aya: Int, val type: String)

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Starting Quran data population from Tanzil JSON + XML metadata")

            // Check if data already exists with metadata (page > 0)
            val existingAyahCount = ayahDao.getAyahCount()
            val maxPage = ayahDao.getMaxPageNumber()
            if (existingAyahCount >= 6236 && maxPage != null && maxPage > 0) {
                Timber.d("Quran data already populated with metadata (${existingAyahCount} ayahs, max page: $maxPage)")
                return Result.success()
            }

            // Parse metadata XML first
            val metadata = parseMetadataXml()
            Timber.d("Parsed metadata: ${metadata.pages.size} pages, ${metadata.juzs.size} juzs, ${metadata.sajdas.size} sajdas")

            // Load Tanzil JSON from assets
            val jsonString = context.assets.open("tanzil_quran.json").bufferedReader().use { it.readText() }
            val gson = Gson()
            val type = object : TypeToken<List<TanzilSurah>>() {}.type
            val surahs: List<TanzilSurah> = gson.fromJson(jsonString, type)

            Timber.d("Loaded ${surahs.size} surahs from Tanzil JSON")

            var globalAyahNumber = 1

            // Process all 114 Surahs
            for (surah in surahs) {
                try {
                    Timber.d("Processing Surah ${surah.id}/114: ${surah.transliteration}")

                    // Save Surah metadata
                    val surahEntity = SurahEntity(
                        number = surah.id,
                        nameArabic = surah.nameArabic,
                        nameEnglish = surah.translation ?: surah.transliteration,
                        nameTransliteration = surah.transliteration,
                        ayahCount = surah.totalVerses,
                        revelationType = surah.type.uppercase()
                    )
                    surahDao.insertSurah(surahEntity)

                    // Save all Ayahs for this Surah with metadata
                    val ayahEntities = surah.verses.map { verse ->
                        val page = metadata.getPageForAyah(surah.id, verse.id)
                        val juz = metadata.getJuzForAyah(surah.id, verse.id)
                        val manzil = metadata.getManzilForAyah(surah.id, verse.id)
                        val hizbQuarter = metadata.getHizbQuarterForAyah(surah.id, verse.id)
                        val ruku = metadata.getRukuForAyah(surah.id, verse.id)
                        val sajda = metadata.isSajdaAyah(surah.id, verse.id)

                        AyahEntity(
                            surahNumber = surah.id,
                            ayahNumber = verse.id,
                            globalAyahNumber = globalAyahNumber++,
                            textArabic = verse.text,
                            juz = juz,
                            manzil = manzil,
                            page = page,
                            ruku = ruku,
                            hizbQuarter = hizbQuarter,
                            sajda = sajda
                        )
                    }
                    ayahDao.insertAyahs(ayahEntities)

                    Timber.d("Saved Surah ${surah.id} with ${ayahEntities.size} ayahs")
                } catch (e: Exception) {
                    Timber.e(e, "Error processing Surah ${surah.id}")
                    // Continue with next surah instead of failing completely
                }
            }

            val finalCount = ayahDao.getAyahCount()
            val finalMaxPage = ayahDao.getMaxPageNumber()
            Timber.d("Quran data population complete: $finalCount ayahs, max page: $finalMaxPage")

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Failed to populate Quran data from Tanzil")
            Result.retry()
        }
    }

    private fun parseMetadataXml(): QuranMetadata {
        val pages = mutableListOf<PageInfo>()
        val juzs = mutableListOf<JuzInfo>()
        val manzils = mutableListOf<ManzilInfo>()
        val hizbs = mutableListOf<HizbInfo>()
        val rukus = mutableListOf<RukuInfo>()
        val sajdas = mutableListOf<SajdaInfo>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            context.assets.open("quran-metadata.xml").use { inputStream ->
                parser.setInput(inputStream, "UTF-8")

                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        when (parser.name) {
                            "page" -> {
                                val index = parser.getAttributeValue(null, "index")?.toIntOrNull() ?: 0
                                val sura = parser.getAttributeValue(null, "sura")?.toIntOrNull() ?: 0
                                val aya = parser.getAttributeValue(null, "aya")?.toIntOrNull() ?: 0
                                if (index > 0) pages.add(PageInfo(index, sura, aya))
                            }
                            "juz" -> {
                                val index = parser.getAttributeValue(null, "index")?.toIntOrNull() ?: 0
                                val sura = parser.getAttributeValue(null, "sura")?.toIntOrNull() ?: 0
                                val aya = parser.getAttributeValue(null, "aya")?.toIntOrNull() ?: 0
                                if (index > 0) juzs.add(JuzInfo(index, sura, aya))
                            }
                            "manzil" -> {
                                val index = parser.getAttributeValue(null, "index")?.toIntOrNull() ?: 0
                                val sura = parser.getAttributeValue(null, "sura")?.toIntOrNull() ?: 0
                                val aya = parser.getAttributeValue(null, "aya")?.toIntOrNull() ?: 0
                                if (index > 0) manzils.add(ManzilInfo(index, sura, aya))
                            }
                            "quarter" -> {
                                val index = parser.getAttributeValue(null, "index")?.toIntOrNull() ?: 0
                                val sura = parser.getAttributeValue(null, "sura")?.toIntOrNull() ?: 0
                                val aya = parser.getAttributeValue(null, "aya")?.toIntOrNull() ?: 0
                                if (index > 0) hizbs.add(HizbInfo(index, sura, aya))
                            }
                            "ruku" -> {
                                val index = parser.getAttributeValue(null, "index")?.toIntOrNull() ?: 0
                                val sura = parser.getAttributeValue(null, "sura")?.toIntOrNull() ?: 0
                                val aya = parser.getAttributeValue(null, "aya")?.toIntOrNull() ?: 0
                                if (index > 0) rukus.add(RukuInfo(index, sura, aya))
                            }
                            "sajda" -> {
                                val sura = parser.getAttributeValue(null, "sura")?.toIntOrNull() ?: 0
                                val aya = parser.getAttributeValue(null, "aya")?.toIntOrNull() ?: 0
                                val type = parser.getAttributeValue(null, "type") ?: "recommended"
                                if (sura > 0) sajdas.add(SajdaInfo(sura, aya, type))
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing metadata XML")
        }

        return QuranMetadata(pages, juzs, manzils, hizbs, rukus, sajdas)
    }

    private inner class QuranMetadata(
        val pages: List<PageInfo>,
        val juzs: List<JuzInfo>,
        val manzils: List<ManzilInfo>,
        val hizbs: List<HizbInfo>,
        val rukus: List<RukuInfo>,
        val sajdas: List<SajdaInfo>
    ) {
        // Binary search helper to find the current division for an ayah
        private fun <T> findDivision(
            list: List<T>,
            surah: Int,
            ayah: Int,
            getSura: (T) -> Int,
            getAya: (T) -> Int,
            getIndex: (T) -> Int
        ): Int {
            if (list.isEmpty()) return 1

            var result = 1
            for (item in list) {
                val itemSura = getSura(item)
                val itemAya = getAya(item)

                // Check if this division starts at or before our ayah
                if (itemSura < surah || (itemSura == surah && itemAya <= ayah)) {
                    result = getIndex(item)
                } else {
                    break
                }
            }
            return result
        }

        fun getPageForAyah(surah: Int, ayah: Int): Int {
            return findDivision(pages, surah, ayah, { it.sura }, { it.aya }, { it.index })
        }

        fun getJuzForAyah(surah: Int, ayah: Int): Int {
            return findDivision(juzs, surah, ayah, { it.sura }, { it.aya }, { it.index })
        }

        fun getManzilForAyah(surah: Int, ayah: Int): Int {
            return findDivision(manzils, surah, ayah, { it.sura }, { it.aya }, { it.index })
        }

        fun getHizbQuarterForAyah(surah: Int, ayah: Int): Int {
            return findDivision(hizbs, surah, ayah, { it.sura }, { it.aya }, { it.index })
        }

        fun getRukuForAyah(surah: Int, ayah: Int): Int {
            return findDivision(rukus, surah, ayah, { it.sura }, { it.aya }, { it.index })
        }

        fun isSajdaAyah(surah: Int, ayah: Int): Boolean {
            return sajdas.any { it.sura == surah && it.aya == ayah }
        }
    }

    companion object {
        const val WORK_NAME = "quran_data_populator"
    }
}
