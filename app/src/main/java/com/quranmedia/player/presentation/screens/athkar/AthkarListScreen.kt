package com.quranmedia.player.presentation.screens.athkar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.domain.model.Thikr
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.screens.reader.components.creamBackground
import com.quranmedia.player.presentation.screens.reader.components.lightGreen
import com.quranmedia.player.presentation.screens.reader.components.islamicGreen
import com.quranmedia.player.presentation.screens.reader.components.darkGreen
import com.quranmedia.player.presentation.util.layoutDirection
import kotlinx.coroutines.launch
import com.quranmedia.player.presentation.components.CommonOverflowMenu
import com.quranmedia.player.domain.util.ArabicNumeralUtils

private val completedColor = lightGreen  // Use the shared light green for completed state

/**
 * Cleans Arabic text by removing problematic Unicode characters that render as black circles.
 * Removes ornamental marks, unusual punctuation, and isolated diacritics while preserving
 * Arabic letters, standard diacritics, and common punctuation.
 */
private fun String.cleanArabicText(): String {
    // Characters to completely remove (ornamental/problematic)
    val charsToRemove = setOf(
        '\u060C', // Arabic Comma - causes rendering issues in some fonts
        '\u06DD', // Arabic End of Ayah
        '\u06DE', // Arabic Start of Rub El Hizb
        '\u06E9', // Arabic Place of Sajdah
        '\u25CC', // Dotted Circle
        '\u2022', // Bullet
        '\u2023', // Triangular Bullet
        '\u25E6', // White Bullet
        '\u2219', // Bullet Operator
        '\u2055', // Flower Punctuation Mark
        '\u00B7', // Middle Dot
        '\u0640', // Arabic Tatweel (kashida) - can cause rendering issues
        '\u200B', // Zero Width Space
        '\u200C', // Zero Width Non-Joiner
        '\u200D', // Zero Width Joiner
        '\u200E', // Left-to-Right Mark
        '\u200F', // Right-to-Left Mark
        '\uFEFF'  // Zero Width No-Break Space
    )

    val combiningDiacritics = setOf(
        '\u064B', '\u064C', '\u064D', '\u064E', '\u064F', '\u0650',
        '\u0651', '\u0652', '\u0653', '\u0654', '\u0655', '\u0656',
        '\u0657', '\u0658', '\u0670', '\u06DC', '\u06DF', '\u06E0',
        '\u06E1', '\u06E2', '\u06E3', '\u06E4', '\u06E7', '\u06E8',
        '\u06EA', '\u06EB', '\u06EC', '\u06ED'
    )

    val result = StringBuilder()
    var i = 0
    while (i < this.length) {
        val char = this[i]

        when {
            // Skip characters that should be removed entirely
            char in charsToRemove -> {}

            // Handle combining diacritics - only keep if they have a base character
            char in combiningDiacritics -> {
                if (i > 0 && this[i - 1] !in combiningDiacritics &&
                    !this[i - 1].isWhitespace() && this[i - 1] !in charsToRemove) {
                    result.append(char)
                }
            }

            // Keep all other characters
            else -> result.append(char)
        }
        i++
    }

    return result.toString()
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AthkarListScreen(
    categoryId: String,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPrayerTimes: () -> Unit = {},
    onNavigateToTracker: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToReading: () -> Unit = {},
    onNavigateToImsakiya: () -> Unit = {},
    viewModel: AthkarListViewModel = hiltViewModel()
) {
    val athkar by viewModel.athkar.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val category by viewModel.category.collectAsState()
    val remainingCounts by viewModel.remainingCounts.collectAsState()
    val language = settings.appLanguage

    // Initialize remaining counts when athkar list is loaded
    LaunchedEffect(athkar) {
        if (athkar.isNotEmpty()) {
            viewModel.initializeRemainingCounts(athkar)
        }
    }

    // Pager state for swipeable tiles
    val pagerState = rememberPagerState(pageCount = { athkar.size })

    // Always use RTL for Athkar since content is Arabic
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = category?.let {
                                if (language == AppLanguage.ARABIC) it.nameArabic else it.nameEnglish
                            } ?: "",
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
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
                        IconButton(onClick = { viewModel.resetAllCounts() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reset",
                                tint = Color.White
                            )
                        }

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
            if (athkar.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = islamicGreen)
                }
            } else {
                // Check if all completed
                val allCompleted = remainingCounts.values.all { it == 0 } && remainingCounts.isNotEmpty()
                val useIndoArabic = language == AppLanguage.ARABIC && settings.useIndoArabicNumerals

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Progress indicator at top
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${ArabicNumeralUtils.formatNumber(pagerState.currentPage + 1, useIndoArabic)} / ${ArabicNumeralUtils.formatNumber(athkar.size, useIndoArabic)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = islamicGreen
                            )
                        }

                        // Horizontal Pager for swipeable tiles
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            beyondBoundsPageCount = 1
                            // No reverseLayout needed - RTL layout direction handles swipe direction automatically
                        ) { page ->
                            val thikr = athkar[page]
                            val remaining = remainingCounts[thikr.id] ?: thikr.repeatCount

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ThikrCard(
                                    thikr = thikr,
                                    index = page + 1,
                                    remainingCount = remaining,
                                    language = language,
                                    onTap = { viewModel.decrementCount(thikr.id) },
                                    onReset = { viewModel.resetCount(thikr.id, thikr.repeatCount) },
                                    useIndoArabic = useIndoArabic
                                )
                            }
                        }

                        // Page indicators (dots)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(athkar.size) { index ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(if (isSelected) 10.dp else 8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) islamicGreen
                                            else islamicGreen.copy(alpha = 0.3f)
                                        )
                                )
                            }
                        }
                    }

                    // Completion overlay
                    if (allCompleted) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f))
                                .clickable { viewModel.resetAllCounts() },
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .padding(32.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "✓",
                                        fontSize = 48.sp,
                                        color = completedColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (language == AppLanguage.ARABIC)
                                            "تقبّل الله منك"
                                        else
                                            "May Allah accept from you",
                                        fontFamily = scheherazadeFont,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = completedColor,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (language == AppLanguage.ARABIC)
                                            "اضغط للإعادة"
                                        else
                                            "Tap to reset",
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThikrCard(
    thikr: Thikr,
    index: Int,
    remainingCount: Int,
    language: AppLanguage,
    onTap: () -> Unit,
    onReset: () -> Unit,
    useIndoArabic: Boolean = false
) {
    val isCompleted = remainingCount == 0
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Animation states
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    val cardColor by animateColorAsState(
        targetValue = if (isPressed && !isCompleted) {
            islamicGreen.copy(alpha = 0.1f)
        } else if (isCompleted) {
            completedColor.copy(alpha = 0.1f)
        } else {
            Color.White
        },
        animationSpec = tween(durationMillis = 100),
        label = "cardColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)  // Use most of screen height for card
            .scale(scale)
            .clickable(
                enabled = !isCompleted,
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material.ripple.rememberRipple(
                    bounded = true,
                    color = islamicGreen
                )
            ) {
                // Haptic feedback
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                // Visual press animation
                isPressed = true

                // Call the tap action
                onTap()

                // Reset animation
                scope.launch {
                    kotlinx.coroutines.delay(200)
                    isPressed = false
                }
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header row with index and count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Index badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isCompleted) completedColor else islamicGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ArabicNumeralUtils.formatNumber(index, useIndoArabic),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Repeat count badge
                if (thikr.repeatCount > 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (isCompleted) completedColor.copy(alpha = 0.2f)
                                else islamicGreen.copy(alpha = 0.15f)
                            )
                            .clickable { onReset() }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isCompleted) {
                                if (language == AppLanguage.ARABIC) "مكتمل" else "Done"
                            } else {
                                "${ArabicNumeralUtils.formatNumber(remainingCount, useIndoArabic)} / ${ArabicNumeralUtils.formatNumber(thikr.repeatCount, useIndoArabic)}"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCompleted) completedColor else islamicGreen
                        )
                    }
                }
            }

            // Arabic text - centered vertically
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = thikr.textArabic.cleanArabicText(),
                    fontFamily = scheherazadeFont,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isCompleted) Color.Gray else Color.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 44.sp,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                // Reference with decorative separator
                if (!thikr.reference.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Decorative divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(1.dp)
                                .background(islamicGreen.copy(alpha = 0.3f))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = thikr.reference,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = islamicGreen.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(1.dp)
                                .background(islamicGreen.copy(alpha = 0.3f))
                        )
                    }
                }
            }

            // Tap hint at bottom
            if (!isCompleted && thikr.repeatCount > 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(islamicGreen.copy(alpha = 0.1f))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (language == AppLanguage.ARABIC)
                            "اضغط للعد"
                        else
                            "Tap to count",
                        fontSize = 12.sp,
                        color = islamicGreen,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Empty spacer to maintain layout balance
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
