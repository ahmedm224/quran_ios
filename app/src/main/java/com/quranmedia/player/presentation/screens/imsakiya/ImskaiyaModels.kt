package com.quranmedia.player.presentation.screens.imsakiya

import java.time.LocalDate
import java.time.LocalTime

/**
 * Represents a single day in the Imsakiya (Ramadan prayer times calendar)
 */
data class ImskaiyaDay(
    val dayNumber: Int,           // Day of Ramadan (1-30)
    val gregorianDate: LocalDate,
    val hijriDate: String,        // e.g., "1 Ramadan 1446"
    val fajr: LocalTime,
    val sunrise: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime
)

/**
 * Represents the full Imsakiya for Ramadan
 */
data class ImskaiyaMonth(
    val days: List<ImskaiyaDay>,
    val hijriYear: Int,
    val gregorianYear: Int,
    val locationName: String,
    val calculationMethod: String
)

/**
 * UI state for the Imsakiya screen
 */
data class ImskaiyaUiState(
    val imsakiya: ImskaiyaMonth? = null,
    val currentDayIndex: Int = -1,  // Index of today in the list, -1 if not in Ramadan
    val currentHijriDate: String? = null,  // Today's Hijri date for the title
    val currentHijriDateArabic: String? = null,  // Today's Hijri date in Arabic
    val daysUntilRamadan: Int = 0,  // Days remaining until Ramadan starts (0 if in Ramadan)
    val isLoading: Boolean = false,
    val error: String? = null,
    val offlineWarning: String? = null  // Non-blocking warning shown as snackbar
)
