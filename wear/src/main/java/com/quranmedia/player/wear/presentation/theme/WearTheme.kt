package com.quranmedia.player.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import com.quranmedia.player.wear.R

/**
 * Islamic-themed colors optimized for OLED displays.
 */
object WearAthkarColors {
    val IslamicGreen = Color(0xFF1B5E20)
    val LightGreen = Color(0xFF4CAF50)
    val CompletedGreen = Color(0xFF81C784)
    val Background = Color(0xFF000000)  // Pure black for OLED
    val Surface = Color(0xFF1E2228)
    val OnSurface = Color.White
    val OnSurfaceVariant = Color(0xFFB0B0B0)
    val Primary = Color(0xFF4CAF50)
    val OnPrimary = Color.White
    val Secondary = Color(0xFF81C784)
    val Error = Color(0xFFCF6679)
}

/**
 * Arabic font family using Scheherazade.
 */
val ScheherazadeFont = FontFamily(
    Font(R.font.scheherazade_regular)
)

/**
 * Wear Material color scheme.
 */
private val WearColorPalette = Colors(
    primary = WearAthkarColors.Primary,
    primaryVariant = WearAthkarColors.IslamicGreen,
    secondary = WearAthkarColors.Secondary,
    secondaryVariant = WearAthkarColors.LightGreen,
    background = WearAthkarColors.Background,
    surface = WearAthkarColors.Surface,
    error = WearAthkarColors.Error,
    onPrimary = WearAthkarColors.OnPrimary,
    onSecondary = Color.Black,
    onBackground = WearAthkarColors.OnSurface,
    onSurface = WearAthkarColors.OnSurface,
    onSurfaceVariant = WearAthkarColors.OnSurfaceVariant,
    onError = Color.Black
)

/**
 * Al-Furqan Wear OS theme.
 */
@Composable
fun AlFurqanWearTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = WearColorPalette,
        content = content
    )
}
