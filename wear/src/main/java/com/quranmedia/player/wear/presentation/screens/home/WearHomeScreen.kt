package com.quranmedia.player.wear.presentation.screens.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.quranmedia.player.wear.R
import com.quranmedia.player.wear.domain.model.AthkarCategory
import com.quranmedia.player.wear.presentation.theme.ScheherazadeFont
import com.quranmedia.player.wear.presentation.theme.WearAthkarColors

@Composable
fun WearHomeScreen(
    onCategoryClick: (String) -> Unit,
    onPrayerTimesClick: () -> Unit,
    onQuranClick: () -> Unit,
    viewModel: WearHomeViewModel = hiltViewModel()
) {
    val categories by viewModel.categories
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            // App Header
            item {
                ListHeader {
                    Text(
                        text = "الفرقان",
                        fontFamily = ScheherazadeFont,
                        fontSize = 20.sp,
                        color = WearAthkarColors.Primary
                    )
                }
            }

            // Quran Button
            item {
                Chip(
                    onClick = onQuranClick,
                    label = {
                        Text(
                            text = "القرآن الكريم",
                            fontFamily = ScheherazadeFont,
                            fontSize = 16.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "Quran",
                            fontSize = 10.sp,
                            color = WearAthkarColors.OnSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_quran),
                            contentDescription = "Quran",
                            tint = WearAthkarColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = WearAthkarColors.Surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Prayer Times Button with Icon
            item {
                Chip(
                    onClick = onPrayerTimesClick,
                    label = {
                        Text(
                            text = "مواقيت الصلاة",
                            fontFamily = ScheherazadeFont,
                            fontSize = 16.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "Prayer Times",
                            fontSize = 10.sp,
                            color = WearAthkarColors.OnSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_prayer_time),
                            contentDescription = "Prayer Times",
                            tint = WearAthkarColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = WearAthkarColors.Surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Athkar Header with Icon
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Chip(
                    onClick = { /* Header chip, no action */ },
                    enabled = false,
                    label = {
                        Text(
                            text = "الأذكار",
                            fontFamily = ScheherazadeFont,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_athkar),
                            contentDescription = "Athkar",
                            tint = WearAthkarColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = WearAthkarColors.Surface.copy(alpha = 0.5f),
                        disabledBackgroundColor = WearAthkarColors.Surface.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Athkar Categories
            items(categories) { category ->
                CategoryChip(
                    category = category,
                    onClick = { onCategoryClick(category.id) }
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    category: AthkarCategory,
    onClick: () -> Unit
) {
    Chip(
        onClick = onClick,
        label = {
            Text(
                text = category.nameArabic,
                fontFamily = ScheherazadeFont,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        },
        colors = ChipDefaults.secondaryChipColors(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
