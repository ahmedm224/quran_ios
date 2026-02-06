package com.quranmedia.player.wear.presentation.screens.quran

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.quranmedia.player.wear.R
import com.quranmedia.player.wear.domain.model.Surah
import com.quranmedia.player.wear.domain.model.Verse
import com.quranmedia.player.wear.presentation.theme.ScheherazadeFont
import com.quranmedia.player.wear.presentation.theme.getQuranColors
import kotlinx.coroutines.launch

private const val BISMILLAH = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"
private const val SWIPE_THRESHOLD = 100f

@Composable
fun QuranReaderScreen(
    surahNumber: Int,
    onBackClick: () -> Unit,
    viewModel: QuranViewModel = hiltViewModel()
) {
    val uiState by viewModel.readerState.collectAsState()
    val colors = getQuranColors(uiState.isDarkMode)
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Swipe detection state
    var totalDragAmount by remember { mutableFloatStateOf(0f) }

    // Load the surah when screen opens
    LaunchedEffect(surahNumber) {
        viewModel.openSurah(surahNumber)
    }

    Scaffold(
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        if (uiState.isLoading || uiState.currentSurah == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Request focus after content is composed
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { totalDragAmount = 0f },
                            onDragEnd = {
                                when {
                                    totalDragAmount > SWIPE_THRESHOLD -> {
                                        // Swipe right - previous surah (RTL: next in reading order)
                                        viewModel.goToNextSurah()
                                    }
                                    totalDragAmount < -SWIPE_THRESHOLD -> {
                                        // Swipe left - next surah (RTL: previous in reading order)
                                        viewModel.goToPreviousSurah()
                                    }
                                }
                                totalDragAmount = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                totalDragAmount += dragAmount
                            }
                        )
                    }
            ) {
                SurahContent(
                    surah = uiState.currentSurah!!,
                    colors = colors,
                    isDarkMode = uiState.isDarkMode,
                    onThemeToggle = { viewModel.toggleTheme() },
                    listState = listState,
                    focusRequester = focusRequester,
                    coroutineScope = coroutineScope,
                    surahIndex = uiState.currentSurahIndex,
                    totalSurahs = uiState.totalSurahs
                )
            }
        }
    }
}

@Composable
private fun SurahContent(
    surah: Surah,
    colors: com.quranmedia.player.wear.presentation.theme.QuranColors,
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    listState: androidx.wear.compose.foundation.lazy.ScalingLazyListState,
    focusRequester: FocusRequester,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    surahIndex: Int,
    totalSurahs: Int
) {
    ScalingLazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .onRotaryScrollEvent { event ->
                coroutineScope.launch {
                    listState.scrollBy(event.verticalScrollPixels)
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Surah Header
        item {
            SurahHeader(
                surahName = surah.name,
                surahNumber = surah.id,
                versesCount = surah.totalVerses,
                colors = colors,
                isDarkMode = isDarkMode,
                onThemeToggle = onThemeToggle
            )
        }

        // Bismillah (except for Surah At-Tawbah)
        if (surah.id != 9) {
            item {
                Text(
                    text = BISMILLAH,
                    fontFamily = ScheherazadeFont,
                    fontSize = 14.sp,
                    color = colors.bismillah,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                )
            }
        }

        // Verses
        itemsIndexed(surah.verses) { index, verse ->
            AyahItem(
                verse = verse,
                colors = colors
            )
        }

        // Navigation hint at bottom
        item {
            Text(
                text = "← اسحب للتنقل →",
                fontFamily = ScheherazadeFont,
                fontSize = 10.sp,
                color = colors.ayahNumber,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SurahHeader(
    surahName: String,
    surahNumber: Int,
    versesCount: Int,
    colors: com.quranmedia.player.wear.presentation.theme.QuranColors,
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            // Theme toggle button
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onThemeToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isDarkMode) R.drawable.ic_light_mode else R.drawable.ic_dark_mode
                    ),
                    contentDescription = "Toggle theme",
                    tint = colors.ayahNumber,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Surah name
            Text(
                text = "سورة $surahName",
                fontFamily = ScheherazadeFont,
                fontSize = 16.sp,
                color = colors.surahHeader,
                textAlign = TextAlign.Center
            )

            // Surah number
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(colors.surahHeader.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = surahNumber.toString(),
                    fontSize = 10.sp,
                    color = colors.surahHeader
                )
            }
        }

        Text(
            text = "$versesCount آية",
            fontFamily = ScheherazadeFont,
            fontSize = 10.sp,
            color = colors.ayahNumber
        )
    }
}

@Composable
private fun AyahItem(
    verse: Verse,
    colors: com.quranmedia.player.wear.presentation.theme.QuranColors
) {
    // Use Arabic end of ayah sign (۝) with the number inside
    // This is the traditional Quran ayah marker that works correctly with RTL
    val ayahNumber = toArabicNumber(verse.id)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = "${verse.text} ۝$ayahNumber",
            fontFamily = ScheherazadeFont,
            fontSize = 16.sp,
            color = colors.ayahText,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Convert number to Arabic numerals
 */
private fun toArabicNumber(number: Int): String {
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return number.toString().map { arabicDigits[it - '0'] }.joinToString("")
}
