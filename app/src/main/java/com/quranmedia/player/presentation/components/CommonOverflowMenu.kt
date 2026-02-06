package com.quranmedia.player.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.screens.reader.components.islamicGreen
import com.quranmedia.player.presentation.screens.reader.components.darkGreen

/**
 * Centralized three-dot overflow menu used across all screens.
 *
 * This composable provides a consistent menu experience throughout the app.
 * Each screen can hide its own menu item by setting the corresponding hide parameter.
 *
 * Example usage:
 * ```
 * CommonOverflowMenu(
 *     language = language,
 *     onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
 *     onNavigateToReading = { navController.navigate(Screen.QuranIndex.route) },
 *     // ... other callbacks
 *     hidePrayerTimes = true  // Hide if this IS the Prayer Times screen
 * )
 * ```
 */
@Composable
fun CommonOverflowMenu(
    language: AppLanguage,
    // Navigation callbacks
    onNavigateToSettings: () -> Unit = {},
    onNavigateToReading: () -> Unit = {},
    onNavigateToPrayerTimes: () -> Unit = {},
    onNavigateToImsakiya: () -> Unit = {},
    onNavigateToAthkar: () -> Unit = {},
    onNavigateToTracker: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    // Hide options for when the current screen shouldn't show its own menu item
    hideSettings: Boolean = false,
    hideReading: Boolean = false,
    hidePrayerTimes: Boolean = false,
    hideImsakiya: Boolean = false,
    hideAthkar: Boolean = false,
    hideTracker: Boolean = false,
    hideDownloads: Boolean = false,
    hideAbout: Boolean = false,
    // Icon tint color (some screens use different colors)
    iconTint: Color = Color.White
) {
    var showMenu by remember { mutableStateOf(false) }
    val isArabic = language == AppLanguage.ARABIC

    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Menu",
                tint = iconTint
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            // Settings
            if (!hideSettings) {
                OverflowMenuItem(
                    text = if (isArabic) "الإعدادات" else "Settings",
                    icon = Icons.Default.Settings,
                    isArabic = isArabic,
                    onClick = {
                        showMenu = false
                        onNavigateToSettings()
                    }
                )
            }

            // Reading (Quran)
            if (!hideReading) {
                OverflowMenuItem(
                    text = if (isArabic) "القرآن" else "Reading",
                    icon = Icons.Default.MenuBook,
                    isArabic = isArabic,
                    onClick = {
                        showMenu = false
                        onNavigateToReading()
                    }
                )
            }

            // Prayer Times
            if (!hidePrayerTimes) {
                OverflowMenuItem(
                    text = if (isArabic) "مواقيت الصلاة" else "Prayer Times",
                    icon = Icons.Default.Schedule,
                    isArabic = isArabic,
                    onClick = {
                        showMenu = false
                        onNavigateToPrayerTimes()
                    }
                )
            }

            // Ramadan Imsakiya (TODO: Remove after Ramadan)
            if (!hideImsakiya) {
                OverflowMenuItem(
                    text = if (isArabic) "إمساكية رمضان" else "Ramadan Imsakiya",
                    icon = Icons.Default.NightsStay,
                    isArabic = isArabic,
                    onClick = {
                        showMenu = false
                        onNavigateToImsakiya()
                    }
                )
            }

            // Athkar
            if (!hideAthkar) {
                OverflowMenuItem(
                    text = if (isArabic) "الأذكار" else "Athkar",
                    icon = Icons.Default.WbSunny,
                    isArabic = isArabic,
                    onClick = {
                        showMenu = false
                        onNavigateToAthkar()
                    }
                )
            }

            // Daily Tracker
            if (!hideTracker) {
                OverflowMenuItem(
                    text = if (isArabic) "المتابعة اليومية" else "Daily Tracker",
                    icon = Icons.Default.CheckCircle,
                    isArabic = isArabic,
                    onClick = {
                        showMenu = false
                        onNavigateToTracker()
                    }
                )
            }

            // Downloads
            if (!hideDownloads) {
                OverflowMenuItem(
                    text = if (isArabic) "التحميلات" else "Downloads",
                    icon = Icons.Default.CloudDownload,
                    isArabic = isArabic,
                    onClick = {
                        showMenu = false
                        onNavigateToDownloads()
                    }
                )
            }

            // About
            if (!hideAbout) {
                OverflowMenuItem(
                    text = if (isArabic) "حول التطبيق" else "About",
                    icon = Icons.Default.Info,
                    isArabic = isArabic,
                    onClick = {
                        showMenu = false
                        onNavigateToAbout()
                    }
                )
            }
        }
    }
}

@Composable
private fun OverflowMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                fontFamily = if (isArabic) scheherazadeFont else null,
                color = darkGreen
            )
        },
        onClick = onClick,
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = islamicGreen)
        }
    )
}
