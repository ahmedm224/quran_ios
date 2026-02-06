package com.quranmedia.player.wear.presentation.screens.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.quranmedia.player.wear.presentation.theme.ScheherazadeFont
import com.quranmedia.player.wear.presentation.theme.WearAthkarColors
import kotlinx.coroutines.launch

@Composable
fun QuranIndexScreen(
    onSurahClick: (Int) -> Unit,
    viewModel: QuranViewModel = hiltViewModel()
) {
    val uiState by viewModel.indexState.collectAsState()
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Page input state
    var pageNumber by remember { mutableStateOf(1) }

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Request focus after composition is complete
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            ScalingLazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(WearAthkarColors.Background)
                    .onRotaryScrollEvent { event ->
                        coroutineScope.launch {
                            listState.scrollBy(event.verticalScrollPixels)
                        }
                        true
                    }
                    .focusRequester(focusRequester)
                    .focusable(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                item {
                    ListHeader {
                        Text(
                            text = "القرآن الكريم",
                            fontFamily = ScheherazadeFont,
                            fontSize = 18.sp,
                            color = WearAthkarColors.Primary
                        )
                    }
                }

                // Page number input with +/- buttons
                item {
                    PageNumberInput(
                        pageNumber = pageNumber,
                        onPageChange = { newPage ->
                            pageNumber = newPage.coerceIn(1, 604)
                        },
                        onGoToPage = {
                            val surahNumber = viewModel.goToPage(pageNumber)
                            onSurahClick(surahNumber)
                        }
                    )
                }

                // Continue reading chip if there's a saved position
                if (uiState.lastPosition.surahNumber > 1) {
                    item {
                        val lastSurahInfo = uiState.surahs.getOrNull(uiState.lastPosition.surahNumber - 1)
                        if (lastSurahInfo != null) {
                            Chip(
                                onClick = { onSurahClick(uiState.lastPosition.surahNumber) },
                                label = {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "متابعة القراءة",
                                            fontFamily = ScheherazadeFont,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = lastSurahInfo.name,
                                            fontFamily = ScheherazadeFont,
                                            fontSize = 14.sp
                                        )
                                    }
                                },
                                colors = ChipDefaults.chipColors(
                                    backgroundColor = WearAthkarColors.Primary.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Surah list
                itemsIndexed(uiState.surahs) { index, surah ->
                    SurahItem(
                        surahNumber = surah.id,
                        surahName = surah.name,
                        surahNameTranslit = surah.transliteration,
                        versesCount = surah.totalVerses,
                        onClick = { onSurahClick(surah.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PageNumberInput(
    pageNumber: Int,
    onPageChange: (Int) -> Unit,
    onGoToPage: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "الذهاب إلى صفحة",
            fontFamily = ScheherazadeFont,
            fontSize = 12.sp,
            color = WearAthkarColors.OnSurfaceVariant
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            // Minus button
            Button(
                onClick = { onPageChange(pageNumber - 1) },
                modifier = Modifier.size(32.dp),
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Text(text = "-", fontSize = 16.sp)
            }

            // Page number display
            Box(
                modifier = Modifier
                    .background(WearAthkarColors.Surface, CircleShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pageNumber.toString(),
                    fontSize = 18.sp,
                    color = WearAthkarColors.Primary
                )
            }

            // Plus button
            Button(
                onClick = { onPageChange(pageNumber + 1) },
                modifier = Modifier.size(32.dp),
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Text(text = "+", fontSize = 16.sp)
            }
        }

        // Go button
        Chip(
            onClick = onGoToPage,
            label = {
                Text(
                    text = "اذهب",
                    fontFamily = ScheherazadeFont,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            colors = ChipDefaults.chipColors(
                backgroundColor = WearAthkarColors.Primary
            ),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun SurahItem(
    surahNumber: Int,
    surahName: String,
    surahNameTranslit: String,
    versesCount: Int,
    onClick: () -> Unit
) {
    Chip(
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Surah number in circle
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(WearAthkarColors.Primary.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = surahNumber.toString(),
                        fontSize = 10.sp,
                        color = WearAthkarColors.Primary
                    )
                }

                // Surah name
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = surahName,
                        fontFamily = ScheherazadeFont,
                        fontSize = 14.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "$versesCount آية",
                        fontFamily = ScheherazadeFont,
                        fontSize = 10.sp,
                        color = WearAthkarColors.OnSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        colors = ChipDefaults.secondaryChipColors(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
