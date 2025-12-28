package com.quranmedia.player.presentation.theme

import androidx.compose.ui.graphics.Color
import com.quranmedia.player.data.repository.ReadingTheme

/**
 * Color scheme for reading themes
 */
data class ReadingThemeColors(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentLight: Color,
    val divider: Color,
    val cardBackground: Color,
    val highlightBackground: Color,
    val highlight: Color,  // Highlight color for currently playing ayah text
    val ayahMarker: Color,
    val topBarBackground: Color,
    val topBarContent: Color,
    val bottomBarBackground: Color,
    val isDark: Boolean
)

object ReadingThemes {
    // Light theme - Classic cream/white
    val Light = ReadingThemeColors(
        background = Color(0xFFFAF8F3),      // Cream background
        surface = Color.White,
        textPrimary = Color(0xFF1B5E20),     // Dark green
        textSecondary = Color(0xFF666666),
        accent = Color(0xFF2E7D32),          // Islamic green
        accentLight = Color(0xFF66BB6A),     // Light green
        divider = Color(0xFFE0E0E0),
        cardBackground = Color.White,
        highlightBackground = Color(0xFFFFF8E1),  // Light amber/gold highlight - contrasts with green text
        highlight = Color(0xFFFF6F00),       // Orange for playing ayah text
        ayahMarker = Color(0xFFD4AF37),      // Gold
        topBarBackground = Color(0xFF2E7D32),
        topBarContent = Color.White,
        bottomBarBackground = Color.White,
        isDark = false
    )

    // Sepia theme - Warm paper-like
    val Sepia = ReadingThemeColors(
        background = Color(0xFFF5E6D3),      // Warm sepia
        surface = Color(0xFFFAF0E6),         // Linen
        textPrimary = Color(0xFF3E2723),     // Brown text
        textSecondary = Color(0xFF5D4037),
        accent = Color(0xFF6D4C41),          // Brown accent
        accentLight = Color(0xFFA1887F),
        divider = Color(0xFFD7CCC8),
        cardBackground = Color(0xFFFAF0E6),
        highlightBackground = Color(0xFFEFEBE9),
        highlight = Color(0xFFBF360C),       // Deep orange for playing ayah
        ayahMarker = Color(0xFF8D6E63),
        topBarBackground = Color(0xFF5D4037),
        topBarContent = Color(0xFFFAF0E6),
        bottomBarBackground = Color(0xFFFAF0E6),
        isDark = false
    )

    // Night theme - True dark for OLED screens
    val Night = ReadingThemeColors(
        background = Color(0xFF000000),      // Pure black
        surface = Color(0xFF121212),
        textPrimary = Color(0xFFB0B0B0),     // Dimmed text for eye comfort
        textSecondary = Color(0xFF757575),
        accent = Color(0xFF4A6B4D),          // Dimmed muted green for header
        accentLight = Color(0xFF3D5C40),     // Even more dimmed for accents
        divider = Color(0xFF2D2D2D),
        cardBackground = Color(0xFF121212),
        highlightBackground = Color(0xFF1B3D1E),  // Very dark green
        highlight = Color(0xFFE6A500),       // Gold for playing ayah
        ayahMarker = Color(0xFFCDAD00),      // Dimmed gold
        topBarBackground = Color(0xFF121212),
        topBarContent = Color(0xFFB0B0B0),
        bottomBarBackground = Color(0xFF121212),
        isDark = true
    )

    // Ocean theme - Calm blue tones
    val Ocean = ReadingThemeColors(
        background = Color(0xFFE3F2FD),      // Light blue background
        surface = Color(0xFFBBDEFB),
        textPrimary = Color(0xFF0D47A1),     // Dark blue text
        textSecondary = Color(0xFF1565C0),
        accent = Color(0xFF1976D2),          // Blue accent
        accentLight = Color(0xFF64B5F6),
        divider = Color(0xFF90CAF9),
        cardBackground = Color(0xFFE3F2FD),
        highlightBackground = Color(0xFFB3E5FC),  // Light cyan highlight
        highlight = Color(0xFFFF6D00),       // Orange for playing ayah
        ayahMarker = Color(0xFF0288D1),      // Blue marker
        topBarBackground = Color(0xFF1565C0),
        topBarContent = Color(0xFFFFFFFF),
        bottomBarBackground = Color(0xFFE3F2FD),
        isDark = false
    )

    // Paper theme - Classic book feel
    val Paper = ReadingThemeColors(
        background = Color(0xFFFFFDF7),      // Off-white paper
        surface = Color(0xFFFFFEFA),
        textPrimary = Color(0xFF2C2C2C),     // Near black
        textSecondary = Color(0xFF555555),
        accent = Color(0xFF1565C0),          // Blue ink
        accentLight = Color(0xFF42A5F5),
        divider = Color(0xFFE8E8E8),
        cardBackground = Color(0xFFFFFEFA),
        highlightBackground = Color(0xFFFFF9C4),  // Light yellow highlight
        highlight = Color(0xFFD84315),       // Deep orange for playing ayah
        ayahMarker = Color(0xFF1565C0),      // Blue
        topBarBackground = Color(0xFF37474F),  // Blue gray
        topBarContent = Color(0xFFFFFDF7),
        bottomBarBackground = Color(0xFFFFFEFA),
        isDark = false
    )

    // Tajweed theme - Pure white background with Tajweed rule colors
    val Tajweed = ReadingThemeColors(
        background = Color.White,            // Pure white background
        surface = Color.White,
        textPrimary = Color(0xFF000000),     // Black text (will be colored by Tajweed rules)
        textSecondary = Color(0xFF666666),
        accent = Color(0xFF2E7D32),          // Islamic green
        accentLight = Color(0xFF66BB6A),
        divider = Color(0xFFE0E0E0),
        cardBackground = Color.White,
        highlightBackground = Color(0xFFFFF8E1),
        highlight = Color(0xFFFF6F00),       // Orange for playing ayah
        ayahMarker = Color(0xFFD4AF37),      // Gold
        topBarBackground = Color(0xFF2E7D32),
        topBarContent = Color.White,
        bottomBarBackground = Color.White,
        isDark = false
    )

    fun getTheme(theme: ReadingTheme, customColors: CustomThemeColors? = null): ReadingThemeColors = when (theme) {
        ReadingTheme.LIGHT -> Light
        ReadingTheme.SEPIA -> Sepia
        ReadingTheme.NIGHT -> Night
        ReadingTheme.PAPER -> Paper
        ReadingTheme.OCEAN -> Ocean
        ReadingTheme.TAJWEED -> Tajweed
        ReadingTheme.CUSTOM -> customColors?.toReadingThemeColors() ?: Light
    }
}

/**
 * Custom theme colors from user settings
 */
data class CustomThemeColors(
    val backgroundColor: Color,
    val textColor: Color,
    val headerColor: Color
) {
    fun toReadingThemeColors(): ReadingThemeColors {
        // Determine if dark based on background luminance
        val isDark = backgroundColor.luminance() < 0.5f

        // Generate complementary colors by creating new Color instances
        val textSecondary = Color(
            red = textColor.red,
            green = textColor.green,
            blue = textColor.blue,
            alpha = 0.7f
        )
        val accentLight = Color(
            red = headerColor.red,
            green = headerColor.green,
            blue = headerColor.blue,
            alpha = 0.7f
        )
        val dividerColor = if (isDark) {
            Color(red = 1f, green = 1f, blue = 1f, alpha = 0.12f)
        } else {
            Color(red = 0f, green = 0f, blue = 0f, alpha = 0.12f)
        }
        val highlightBg = Color(
            red = headerColor.red,
            green = headerColor.green,
            blue = headerColor.blue,
            alpha = 0.1f
        )

        return ReadingThemeColors(
            background = backgroundColor,
            surface = backgroundColor,
            textPrimary = textColor,
            textSecondary = textSecondary,
            accent = headerColor,
            accentLight = accentLight,
            divider = dividerColor,
            cardBackground = backgroundColor,
            highlightBackground = highlightBg,
            highlight = if (isDark) Color(0xFFFFB300) else Color(0xFFFF6F00),
            ayahMarker = headerColor,
            topBarBackground = headerColor,
            topBarContent = if (headerColor.luminance() < 0.5f) Color.White else Color.Black,
            bottomBarBackground = backgroundColor,
            isDark = isDark
        )
    }
}

/**
 * Extension to calculate color luminance
 */
private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
