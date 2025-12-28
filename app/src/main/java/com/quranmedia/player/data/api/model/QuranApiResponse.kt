package com.quranmedia.player.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * API response models for Al-Quran Cloud API
 * Documentation: https://alquran.cloud/api
 */

data class SurahResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("status")
    val status: String,
    @SerializedName("data")
    val data: SurahData
)

data class SurahData(
    @SerializedName("number")
    val number: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("englishName")
    val englishName: String,
    @SerializedName("englishNameTranslation")
    val englishNameTranslation: String,
    @SerializedName("numberOfAyahs")
    val numberOfAyahs: Int,
    @SerializedName("revelationType")
    val revelationType: String,
    @SerializedName("ayahs")
    val ayahs: List<AyahData>
)

data class AyahData(
    @SerializedName("number")
    val number: Int,              // Global ayah number
    @SerializedName("text")
    val text: String,             // Arabic text
    @SerializedName("numberInSurah")
    val numberInSurah: Int,       // Ayah number within the surah
    @SerializedName("juz")
    val juz: Int,
    @SerializedName("manzil")
    val manzil: Int,
    @SerializedName("page")
    val page: Int,
    @SerializedName("ruku")
    val ruku: Int,
    @SerializedName("hizbQuarter")
    val hizbQuarter: Int,
    @SerializedName("sajda")
    val sajda: Any,  // Can be boolean or object, we'll handle both
    @SerializedName("audio")
    val audio: String? = null,    // Primary audio URL (e.g., 128kbps)
    @SerializedName("audioSecondary")
    val audioSecondary: List<String>? = null  // Alternative bitrate URLs
)

data class AllSurahsResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("status")
    val status: String,
    @SerializedName("data")
    val data: List<SurahMetadata>
)

data class SurahMetadata(
    @SerializedName("number")
    val number: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("englishName")
    val englishName: String,
    @SerializedName("englishNameTranslation")
    val englishNameTranslation: String,
    @SerializedName("numberOfAyahs")
    val numberOfAyahs: Int,
    @SerializedName("revelationType")
    val revelationType: String
)

/**
 * Audio editions response
 */
data class EditionsResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("status")
    val status: String,
    @SerializedName("data")
    val data: List<EditionData>
)

data class EditionData(
    @SerializedName("identifier")
    val identifier: String,         // e.g., "ar.alafasy"
    @SerializedName("language")
    val language: String,            // e.g., "ar"
    @SerializedName("name")
    val name: String,                // e.g., "Alafasy"
    @SerializedName("englishName")
    val englishName: String,         // e.g., "Mishari Rashid al-`Afasy"
    @SerializedName("format")
    val format: String,              // "audio"
    @SerializedName("type")
    val type: String?                // e.g., "versebyverse"
)

/**
 * Search response
 */
data class SearchResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("status")
    val status: String,
    @SerializedName("data")
    val data: SearchData
)

data class SearchData(
    @SerializedName("count")
    val count: Int,
    @SerializedName("matches")
    val matches: List<SearchMatch>
)

data class SearchMatch(
    @SerializedName("number")
    val number: Int,              // Global ayah number
    @SerializedName("text")
    val text: String,             // Arabic text with search term highlighted
    @SerializedName("edition")
    val edition: String,
    @SerializedName("surah")
    val surah: SurahInfo,
    @SerializedName("numberInSurah")
    val numberInSurah: Int
)

data class SurahInfo(
    @SerializedName("number")
    val number: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("englishName")
    val englishName: String,
    @SerializedName("englishNameTranslation")
    val englishNameTranslation: String,
    @SerializedName("revelationType")
    val revelationType: String,
    @SerializedName("numberOfAyahs")
    val numberOfAyahs: Int
)

/**
 * Full Quran response (all 114 surahs with ayahs and metadata)
 * Used for populating page/juz/manzil/ruku/hizbQuarter/sajda data
 */
data class FullQuranResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("status")
    val status: String,
    @SerializedName("data")
    val data: FullQuranData
)

data class FullQuranData(
    @SerializedName("surahs")
    val surahs: List<SurahData>
)

/**
 * ======================================================================
 * Alfurqan Quran API Models
 * Base URL: https://api.alfurqan.online/api/v1/
 * ======================================================================
 */

/**
 * Response from /api/v1/reciters
 */
data class QuranApiRecitersResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("reciters")
    val reciters: List<QuranApiReciter>
)

/**
 * Reciter model from CloudLinqed API
 */
data class QuranApiReciter(
    @SerializedName("id")
    val id: String,                    // e.g., "minshawy-murattal"
    @SerializedName("name")
    val name: String,                  // e.g., "Mohamed Siddiq Al-Minshawi (Murattal)"
    @SerializedName("arabicName")
    val arabicName: String,            // e.g., "محمد صديق المنشاوي (مرتل)"
    @SerializedName("r2Path")
    val r2Path: String                 // e.g., "Minshawy_Murattal_128kbps"
)

/**
 * Response from /api/v1/surahs/{number}
 */
data class QuranApiSurahResponse(
    @SerializedName("surah")
    val surah: QuranApiSurah
)

/**
 * Surah model from CloudLinqed API
 */
data class QuranApiSurah(
    @SerializedName("number")
    val number: Int,                   // 1-114
    @SerializedName("name")
    val name: String,                  // Arabic name
    @SerializedName("transliteration")
    val transliteration: String,       // e.g., "Al-Fatihah"
    @SerializedName("translation")
    val translation: String,           // English translation
    @SerializedName("ayahCount")
    val ayahCount: Int,
    @SerializedName("startAyah")
    val startAyah: Int,
    @SerializedName("endAyah")
    val endAyah: Int,
    @SerializedName("revelationType")
    val revelationType: String         // "Meccan" or "Medinan"
)
