package com.quranmedia.player.presentation.screens.athkar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.domain.model.AthkarCategory
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.screens.reader.components.creamBackground
import com.quranmedia.player.presentation.screens.reader.components.islamicGreen
import com.quranmedia.player.presentation.screens.reader.components.darkGreen
import com.quranmedia.player.presentation.components.CommonOverflowMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthkarCategoriesScreen(
    onNavigateBack: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPrayerTimes: () -> Unit = {},
    onNavigateToTracker: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToReading: () -> Unit = {},
    onNavigateToImsakiya: () -> Unit = {},
    viewModel: AthkarCategoriesViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val language = settings.appLanguage

    // Always use RTL for Athkar since content is Arabic
    CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "الأذكار" else "Athkar",
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        CommonOverflowMenu(
                            language = language,
                            onNavigateToSettings = onNavigateToSettings,
                            onNavigateToReading = onNavigateToReading,
                            onNavigateToPrayerTimes = onNavigateToPrayerTimes,
                            onNavigateToImsakiya = onNavigateToImsakiya,
                            onNavigateToTracker = onNavigateToTracker,
                            onNavigateToDownloads = onNavigateToDownloads,
                            onNavigateToAbout = onNavigateToAbout,
                            hideAthkar = true  // Hide Athkar since we're on this screen
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = islamicGreen,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = creamBackground
        ) { paddingValues ->
            if (categories.isEmpty()) {
                // Loading or empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = islamicGreen)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories) { category ->
                        CategoryCard(
                            category = category,
                            language = language,
                            onClick = { onCategoryClick(category.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: AthkarCategory,
    language: AppLanguage,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(islamicGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForCategory(category.iconName),
                    contentDescription = null,
                    tint = islamicGreen,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category name
            Text(
                text = if (language == AppLanguage.ARABIC) category.nameArabic else category.nameEnglish,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun getIconForCategory(iconName: String): ImageVector {
    return when (iconName) {
        "WbSunny" -> Icons.Default.WbSunny
        "NightsStay" -> Icons.Default.NightsStay
        "Mosque" -> Icons.Default.Mosque
        "Bedtime" -> Icons.Default.Bedtime
        "Alarm" -> Icons.Default.Alarm
        "Home" -> Icons.Default.Home
        "ExitToApp" -> Icons.Default.ExitToApp
        "Restaurant" -> Icons.Default.Restaurant
        "Flight" -> Icons.Default.Flight
        "Shield" -> Icons.Default.Shield
        else -> Icons.Default.AutoAwesome
    }
}
