package com.quranmedia.player.wear.domain.util

import com.quranmedia.player.wear.domain.model.HijriDate
import java.time.LocalDate

/**
 * Utility object for Hijri (Islamic) calendar calculations
 */
object HijriCalendarUtils {

    private val HIJRI_MONTHS = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Ula", "Jumada al-Thani", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    private val HIJRI_MONTHS_ARABIC = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    /**
     * Convert Gregorian date to Hijri date using mathematical approximation
     */
    fun gregorianToHijri(gregorianDate: LocalDate): HijriDate {
        val a = (14 - gregorianDate.monthValue) / 12
        val y = gregorianDate.year + 4800 - a
        val m = gregorianDate.monthValue + 12 * a - 3

        val jdn = gregorianDate.dayOfMonth + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045

        val l = jdn - 1948440 + 10632
        val n = (l - 1) / 10631
        val l2 = l - 10631 * n + 354
        val j = ((10985 - l2) / 5316) * ((50 * l2) / 17719) + (l2 / 5670) * ((43 * l2) / 15238)
        val l3 = l2 - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29

        val month = (24 * l3) / 709
        val day = l3 - (709 * month) / 24
        val year = (30 * n + j - 30).toInt()

        val monthIndex = (month - 1).coerceIn(0, 11)

        return HijriDate(
            day = day,
            month = HIJRI_MONTHS[monthIndex],
            monthArabic = HIJRI_MONTHS_ARABIC[monthIndex],
            year = year,
            monthNumber = month
        )
    }

    fun getMonthNumber(monthName: String): Int {
        val index = HIJRI_MONTHS.indexOfFirst {
            it.equals(monthName, ignoreCase = true)
        }
        return if (index >= 0) index + 1 else 1
    }
}
