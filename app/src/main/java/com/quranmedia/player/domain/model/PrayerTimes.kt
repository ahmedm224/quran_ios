package com.quranmedia.player.domain.model

import java.time.LocalDate
import java.time.LocalTime

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
)

/**
 * Hijri (Islamic) date
 */
data class HijriDate(
    val day: Int,
    val month: String,
    val monthArabic: String,
    val year: Int,
    val monthNumber: Int = 1  // 1-12, defaults to 1 for backward compatibility
)

/**
 * Prayer time calculation methods
 */
enum class CalculationMethod(
    val id: Int,
    val nameArabic: String,
    val nameEnglish: String
) {
    MAKKAH(4, "أم القرى - مكة", "Umm Al-Qura, Makkah"),
    MWL(3, "رابطة العالم الإسلامي", "Muslim World League"),
    EGYPT(5, "الهيئة المصرية", "Egyptian General Authority"),
    KARACHI(1, "جامعة كراتشي", "Univ. of Karachi"),
    ISNA(2, "أمريكا الشمالية", "ISNA, North America"),
    TEHRAN(7, "طهران", "Tehran"),
    GULF(8, "الخليج", "Gulf Region"),
    KUWAIT(9, "الكويت", "Kuwait"),
    QATAR(10, "قطر", "Qatar"),
    DUBAI(16, "دبي", "Dubai"),
    SINGAPORE(11, "سنغافورة", "Singapore"),
    FRANCE(12, "فرنسا", "France"),
    TURKEY(13, "تركيا", "Turkey"),
    RUSSIA(14, "روسيا", "Russia");

    companion object {
        fun fromId(id: Int): CalculationMethod = entries.find { it.id == id } ?: MWL
    }
}

/**
 * Asr juristic method (school of thought)
 */
enum class AsrJuristicMethod(
    val id: Int,
    val nameArabic: String,
    val nameEnglish: String
) {
    SHAFI(0, "الشافعي / المالكي / الحنبلي", "Shafi'i, Maliki, Hanbali"),
    HANAFI(1, "الحنفي", "Hanafi");

    companion object {
        fun fromId(id: Int): AsrJuristicMethod = entries.find { it.id == id } ?: SHAFI
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
)

/**
 * Prayer type enum for UI
 */
enum class PrayerType(
    val nameArabic: String,
    val nameEnglish: String
) {
    FAJR("الفجر", "Fajr"),
    SUNRISE("الشروق", "Sunrise"),
    DHUHR("الظهر", "Dhuhr"),
    ASR("العصر", "Asr"),
    MAGHRIB("المغرب", "Maghrib"),
    ISHA("العشاء", "Isha")
}
