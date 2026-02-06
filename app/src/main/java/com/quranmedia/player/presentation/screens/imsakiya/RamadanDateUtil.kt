package com.quranmedia.player.presentation.screens.imsakiya

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Utility for calculating Ramadan dates.
 *
 * Note: Islamic months are based on lunar calendar and moon sighting.
 * These are approximate dates - actual dates may vary by 1-2 days based on moon sighting.
 *
 * TODO: After Ramadan, this file can be removed along with the entire imsakiya package.
 */
object RamadanDateUtil {

    // Approximate Ramadan start dates (based on astronomical calculations)
    // Format: Year to approximate start date
    private val ramadanStartDates = mapOf(
        2025 to LocalDate.of(2025, 3, 1),   // Ramadan 1446
        2026 to LocalDate.of(2026, 2, 18),  // Ramadan 1447
        2027 to LocalDate.of(2027, 2, 8),   // Ramadan 1448
        2028 to LocalDate.of(2028, 1, 28),  // Ramadan 1449
    )

    // Hijri years corresponding to Gregorian years
    private val hijriYears = mapOf(
        2025 to 1446,
        2026 to 1447,
        2027 to 1448,
        2028 to 1449,
    )

    /**
     * Get the Ramadan start date for a given year.
     * Returns the estimated start date or null if not available.
     */
    fun getRamadanStartDate(year: Int): LocalDate? {
        return ramadanStartDates[year]
    }

    /**
     * Get the current or upcoming Ramadan dates (30 days).
     * Returns pair of (start date, hijri year) or null if not available.
     */
    fun getCurrentOrUpcomingRamadan(): Pair<LocalDate, Int>? {
        val today = LocalDate.now()
        val currentYear = today.year

        // Check current year's Ramadan
        val currentYearRamadan = ramadanStartDates[currentYear]
        if (currentYearRamadan != null) {
            val ramadanEnd = currentYearRamadan.plusDays(29)
            // If we're before or during Ramadan this year
            if (today <= ramadanEnd) {
                return Pair(currentYearRamadan, hijriYears[currentYear] ?: 1446)
            }
        }

        // Check next year's Ramadan
        val nextYear = currentYear + 1
        val nextYearRamadan = ramadanStartDates[nextYear]
        if (nextYearRamadan != null) {
            return Pair(nextYearRamadan, hijriYears[nextYear] ?: 1447)
        }

        return null
    }

    /**
     * Get all 30 days of Ramadan as LocalDate list.
     */
    fun getRamadanDates(startDate: LocalDate): List<LocalDate> {
        return (0 until 30).map { startDate.plusDays(it.toLong()) }
    }

    /**
     * Check if a given date is during Ramadan.
     */
    fun isInRamadan(date: LocalDate, ramadanStart: LocalDate): Boolean {
        val daysDiff = ChronoUnit.DAYS.between(ramadanStart, date)
        return daysDiff in 0..29
    }

    /**
     * Get the day number of Ramadan (1-30) for a given date.
     * Returns -1 if not in Ramadan.
     */
    fun getRamadanDayNumber(date: LocalDate, ramadanStart: LocalDate): Int {
        val daysDiff = ChronoUnit.DAYS.between(ramadanStart, date).toInt()
        return if (daysDiff in 0..29) daysDiff + 1 else -1
    }

    /**
     * Format Hijri date string for display.
     */
    fun formatHijriDate(dayOfRamadan: Int, hijriYear: Int, isArabic: Boolean): String {
        return if (isArabic) {
            "$dayOfRamadan رمضان $hijriYear"
        } else {
            "$dayOfRamadan Ramadan $hijriYear"
        }
    }
}
