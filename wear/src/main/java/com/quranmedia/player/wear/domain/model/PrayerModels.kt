package com.quranmedia.player.wear.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * App language enum
 */
enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ARABIC("ar", "Arabic", "العربية"),
    ENGLISH("en", "English", "English");

    companion object {
        fun fromCode(code: String): AppLanguage = entries.find { it.code == code } ?: ARABIC
    }
}

/**
 * Domain model for Prayer Times
 */
data class PrayerTimes(
    val date: LocalDate,
    val fajr: LocalTime,
    val sunrise: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime,
    val locationName: String,
    val calculationMethod: CalculationMethod,
    val hijriDate: HijriDate
) {
    /**
     * Get prayer time by type
     */
    fun getTimeForPrayer(type: PrayerType): LocalTime = when (type) {
        PrayerType.FAJR -> fajr
        PrayerType.SUNRISE -> sunrise
        PrayerType.DHUHR -> dhuhr
        PrayerType.ASR -> asr
        PrayerType.MAGHRIB -> maghrib
        PrayerType.ISHA -> isha
    }
}

/**
 * Hijri (Islamic) date
 */
data class HijriDate(
    val day: Int,
    val month: String,
    val monthArabic: String,
    val year: Int,
    val monthNumber: Int = 1
)

/**
 * Prayer time calculation methods
 */
enum class CalculationMethod(
    val id: Int,
    val nameArabic: String,
    val nameEnglish: String
) {
    MAKKAH(4, "أم القرى", "Umm Al-Qura"),
    MWL(3, "رابطة العالم الإسلامي", "Muslim World League"),
    EGYPT(5, "الهيئة المصرية", "Egyptian"),
    KARACHI(1, "جامعة كراتشي", "Karachi"),
    ISNA(2, "أمريكا الشمالية", "ISNA"),
    TEHRAN(7, "طهران", "Tehran"),
    GULF(8, "الخليج", "Gulf"),
    KUWAIT(9, "الكويت", "Kuwait"),
    QATAR(10, "قطر", "Qatar"),
    DUBAI(16, "دبي", "Dubai"),
    SINGAPORE(11, "سنغافورة", "Singapore"),
    FRANCE(12, "فرنسا", "France"),
    TURKEY(13, "تركيا", "Turkey"),
    RUSSIA(14, "روسيا", "Russia");

    companion object {
        fun fromId(id: Int): CalculationMethod = entries.find { it.id == id } ?: MAKKAH
    }
}

/**
 * Asr juristic method
 */
enum class AsrJuristicMethod(val id: Int, val nameArabic: String) {
    SHAFI(0, "الشافعي"),
    HANAFI(1, "الحنفي");

    companion object {
        fun fromId(id: Int): AsrJuristicMethod = entries.find { it.id == id } ?: SHAFI
    }
}

/**
 * Prayer type enum
 */
enum class PrayerType(
    val nameArabic: String,
    val nameEnglish: String,
    val ordinal_: Int
) {
    FAJR("الفجر", "Fajr", 0),
    SUNRISE("الشروق", "Sunrise", 1),
    DHUHR("الظهر", "Dhuhr", 2),
    ASR("العصر", "Asr", 3),
    MAGHRIB("المغرب", "Maghrib", 4),
    ISHA("العشاء", "Isha", 5);

    companion object {
        fun fromOrdinal(ordinal: Int): PrayerType = entries.find { it.ordinal_ == ordinal } ?: FAJR
    }
}

/**
 * User location for prayer times
 */
data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String? = null,
    val countryName: String? = null,
    val isAutoDetected: Boolean = true
) {
    val displayName: String
        get() = buildString {
            cityName?.let { append(it) }
            countryName?.let {
                if (isNotEmpty()) append(", ")
                append(it)
            }
        }.ifEmpty { "%.2f, %.2f".format(latitude, longitude) }
}
