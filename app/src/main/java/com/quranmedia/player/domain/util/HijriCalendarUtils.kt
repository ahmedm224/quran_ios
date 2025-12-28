package com.quranmedia.player.domain.util

import com.quranmedia.player.domain.model.HijriDate
import java.time.LocalDate
import kotlin.math.abs

/**
 * Utility object for Hijri (Islamic) calendar calculations
 */
object HijriCalendarUtils {

    /**
     * List of Hijri months in order
     */
    private val HIJRI_MONTHS = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Ula", "Jumada al-Thani", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    /**
     * Arabic month names
     */
    private val HIJRI_MONTHS_ARABIC = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    /**
     * Convert Gregorian date to Hijri date
     * Uses mathematical approximation algorithm
     */
    fun gregorianToHijri(gregorianDate: LocalDate): HijriDate {
        // Julian day number calculation
        val a = (14 - gregorianDate.monthValue) / 12
        val y = gregorianDate.year + 4800 - a
        val m = gregorianDate.monthValue + 12 * a - 3

        val jdn = gregorianDate.dayOfMonth + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045

        // Hijri calendar calculation
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

    /**
     * Get Hijri month number (1-12) from month name
     */
    fun getMonthNumber(monthName: String): Int {
        val index = HIJRI_MONTHS.indexOfFirst {
            it.equals(monthName, ignoreCase = true)
        }
        return if (index >= 0) index + 1 else 1
    }

    /**
     * Get month length (29 or 30 days) - simplified estimation
     * Odd months have 30 days, even months have 29 days
     * This is an approximation - actual lengths vary by lunar observation
     */
    fun getHijriMonthLength(monthNumber: Int, year: Int = 0): Int {
        // Simplified rule: odd months = 30 days, even months = 29 days
        // Exception: Dhu al-Hijjah (month 12) alternates between 29 and 30
        return if (monthNumber % 2 == 1) {
            30
        } else if (monthNumber == 12) {
            // Simplified: assume 30 days for Dhu al-Hijjah
            30
        } else {
            29
        }
    }

    /**
     * Calculate approximate days between two Hijri dates
     * Note: This is a simplified calculation and may not be perfectly accurate
     */
    fun daysBetweenHijriDates(start: HijriDate, end: HijriDate): Int {
        val startMonthNum = getMonthNumber(start.month)
        val endMonthNum = getMonthNumber(end.month)

        // Same month and year - simple subtraction
        if (start.year == end.year && startMonthNum == endMonthNum) {
            return abs(end.day - start.day)
        }

        // Different months/years - approximate calculation
        var days = 0

        // Days remaining in start month
        days += getHijriMonthLength(startMonthNum, start.year) - start.day

        // Full months between start and end
        var currentMonth = startMonthNum + 1
        var currentYear = start.year

        while (currentYear < end.year ||
               (currentYear == end.year && currentMonth < endMonthNum)) {
            days += getHijriMonthLength(currentMonth, currentYear)
            currentMonth++
            if (currentMonth > 12) {
                currentMonth = 1
                currentYear++
            }
        }

        // Days in end month
        days += end.day

        return abs(days)
    }

    /**
     * Get current Hijri day of month
     */
    fun getCurrentHijriDay(hijriDate: HijriDate): Int = hijriDate.day

    /**
     * Calculate expected Khatmah page for given Hijri day
     * Example: On day 15 of a 30-day month, expected page is ~302 (50% of 604)
     */
    fun expectedPageForHijriDay(
        currentDay: Int,
        monthLength: Int,
        totalPages: Int = 604
    ): Int {
        return ((currentDay.toFloat() / monthLength) * totalPages).toInt()
    }

    /**
     * Get month length for current Hijri date
     */
    fun getMonthLength(hijriDate: HijriDate): Int {
        val monthNumber = getMonthNumber(hijriDate.month)
        return getHijriMonthLength(monthNumber, hijriDate.year)
    }

    /**
     * Calculate if user is on track based on expected progress
     * Returns positive number if ahead of schedule, negative if behind
     */
    fun calculateScheduleVariance(
        currentPage: Int,
        hijriDay: Int,
        monthLength: Int,
        totalPages: Int = 604
    ): Int {
        val expectedPage = expectedPageForHijriDay(hijriDay, monthLength, totalPages)
        return currentPage - expectedPage
    }

    /**
     * Get days remaining in current Hijri month
     */
    fun getDaysRemainingInMonth(hijriDate: HijriDate): Int {
        val monthLength = getMonthLength(hijriDate)
        return monthLength - hijriDate.day
    }

    /**
     * Get end date of current Hijri month in Gregorian calendar (approximate)
     */
    fun getEndOfHijriMonth(currentGregorianDate: LocalDate, currentHijriDate: HijriDate): LocalDate {
        val daysRemaining = getDaysRemainingInMonth(currentHijriDate)
        return currentGregorianDate.plusDays(daysRemaining.toLong())
    }
}
