package com.quranmedia.player.wear.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Theme colors for Quran reader - Dark mode
 */
object QuranDarkTheme {
    val Background = Color(0xFF1A1A2E)
    val Surface = Color(0xFF16213E)
    val AyahText = Color(0xFFE8E8E8)
    val AyahNumber = Color(0xFF94A3B8)
    val SurahHeader = Color(0xFF4ADE80)
    val Bismillah = Color(0xFFFFD700)
    val Divider = Color(0xFF334155)
}

/**
 * Theme colors for Quran reader - Light mode (bright background)
 */
object QuranLightTheme {
    val Background = Color(0xFFFFFBE6)  // Warm cream/paper color
    val Surface = Color(0xFFF5F0DC)
    val AyahText = Color(0xFF1F2937)
    val AyahNumber = Color(0xFF6B7280)
    val SurahHeader = Color(0xFF047857)
    val Bismillah = Color(0xFFB45309)
    val Divider = Color(0xFFD1D5DB)
}

/**
 * Quran theme holder
 */
data class QuranColors(
    val background: Color,
    val surface: Color,
    val ayahText: Color,
    val ayahNumber: Color,
    val surahHeader: Color,
    val bismillah: Color,
    val divider: Color
)

fun getQuranColors(isDarkMode: Boolean): QuranColors {
    return if (isDarkMode) {
        QuranColors(
            background = QuranDarkTheme.Background,
            surface = QuranDarkTheme.Surface,
            ayahText = QuranDarkTheme.AyahText,
            ayahNumber = QuranDarkTheme.AyahNumber,
            surahHeader = QuranDarkTheme.SurahHeader,
            bismillah = QuranDarkTheme.Bismillah,
            divider = QuranDarkTheme.Divider
        )
    } else {
        QuranColors(
            background = QuranLightTheme.Background,
            surface = QuranLightTheme.Surface,
            ayahText = QuranLightTheme.AyahText,
            ayahNumber = QuranLightTheme.AyahNumber,
            surahHeader = QuranLightTheme.SurahHeader,
            bismillah = QuranLightTheme.Bismillah,
            divider = QuranLightTheme.Divider
        )
    }
}
