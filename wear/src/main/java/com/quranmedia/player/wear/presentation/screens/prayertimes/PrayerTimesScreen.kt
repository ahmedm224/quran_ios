package com.quranmedia.player.wear.presentation.screens.prayertimes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.quranmedia.player.wear.domain.model.AppLanguage
import com.quranmedia.player.wear.domain.model.PrayerTimes
import com.quranmedia.player.wear.domain.model.PrayerType
import com.quranmedia.player.wear.presentation.theme.ScheherazadeFont
import com.quranmedia.player.wear.presentation.theme.WearAthkarColors
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun PrayerTimesScreen(
    onSettingsClick: () -> Unit = {},
    viewModel: PrayerTimesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberScalingLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh prayer times when screen becomes visible (e.g., returning from settings)
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "Error",
                        color = Color.Red,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            uiState.prayerTimes != null -> {
                PrayerTimesContent(
                    prayerTimes = uiState.prayerTimes!!,
                    nextPrayer = uiState.nextPrayer,
                    countdown = uiState.countdown,
                    listState = listState,
                    onSettingsClick = onSettingsClick,
                    isArabic = uiState.appLanguage == AppLanguage.ARABIC
                )
            }
        }
    }
}

@Composable
private fun PrayerTimesContent(
    prayerTimes: PrayerTimes,
    nextPrayer: PrayerType?,
    countdown: String,
    listState: androidx.wear.compose.foundation.lazy.ScalingLazyListState,
    onSettingsClick: () -> Unit,
    isArabic: Boolean
) {
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(WearAthkarColors.Background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Hijri Date Header
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            ) {
                Text(
                    text = if (isArabic) {
                        "${prayerTimes.hijriDate.day} ${prayerTimes.hijriDate.monthArabic} ${prayerTimes.hijriDate.year}"
                    } else {
                        "${prayerTimes.hijriDate.day} ${prayerTimes.hijriDate.month} ${prayerTimes.hijriDate.year}"
                    },
                    fontFamily = if (isArabic) ScheherazadeFont else null,
                    fontSize = 14.sp,
                    color = WearAthkarColors.Primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = prayerTimes.locationName,
                    fontSize = 10.sp,
                    color = WearAthkarColors.OnSurfaceVariant
                )
            }
        }

        // Next Prayer Countdown
        if (nextPrayer != null && countdown.isNotEmpty()) {
            item {
                NextPrayerCard(
                    prayerType = nextPrayer,
                    countdown = countdown,
                    time = prayerTimes.getTimeForPrayer(nextPrayer).format(timeFormatter),
                    isArabic = isArabic
                )
            }
        }

        // All Prayer Times
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Fajr
        item {
            PrayerTimeRow(
                prayerType = PrayerType.FAJR,
                time = prayerTimes.fajr,
                isNext = nextPrayer == PrayerType.FAJR,
                timeFormatter = timeFormatter,
                isArabic = isArabic
            )
        }

        // Sunrise
        item {
            PrayerTimeRow(
                prayerType = PrayerType.SUNRISE,
                time = prayerTimes.sunrise,
                isNext = nextPrayer == PrayerType.SUNRISE,
                timeFormatter = timeFormatter,
                isSunrise = true,
                isArabic = isArabic
            )
        }

        // Dhuhr
        item {
            PrayerTimeRow(
                prayerType = PrayerType.DHUHR,
                time = prayerTimes.dhuhr,
                isNext = nextPrayer == PrayerType.DHUHR,
                timeFormatter = timeFormatter,
                isArabic = isArabic
            )
        }

        // Asr
        item {
            PrayerTimeRow(
                prayerType = PrayerType.ASR,
                time = prayerTimes.asr,
                isNext = nextPrayer == PrayerType.ASR,
                timeFormatter = timeFormatter,
                isArabic = isArabic
            )
        }

        // Maghrib
        item {
            PrayerTimeRow(
                prayerType = PrayerType.MAGHRIB,
                time = prayerTimes.maghrib,
                isNext = nextPrayer == PrayerType.MAGHRIB,
                timeFormatter = timeFormatter,
                isArabic = isArabic
            )
        }

        // Isha
        item {
            PrayerTimeRow(
                prayerType = PrayerType.ISHA,
                time = prayerTimes.isha,
                isNext = nextPrayer == PrayerType.ISHA,
                timeFormatter = timeFormatter,
                isArabic = isArabic
            )
        }

        // Settings Button
        item {
            Chip(
                onClick = onSettingsClick,
                label = {
                    Text(
                        text = if (isArabic) "الإعدادات" else "Settings",
                        fontFamily = if (isArabic) ScheherazadeFont else null,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NextPrayerCard(
    prayerType: PrayerType,
    countdown: String,
    time: String,
    isArabic: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = WearAthkarColors.Surface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Text(
            text = if (isArabic) "الصلاة القادمة" else "Next Prayer",
            fontFamily = if (isArabic) ScheherazadeFont else null,
            fontSize = 10.sp,
            color = WearAthkarColors.OnSurfaceVariant
        )
        Text(
            text = if (isArabic) prayerType.nameArabic else prayerType.nameEnglish,
            fontFamily = if (isArabic) ScheherazadeFont else null,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = WearAthkarColors.Primary
        )
        Text(
            text = countdown,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = WearAthkarColors.LightGreen
        )
        Text(
            text = time,
            fontSize = 10.sp,
            color = WearAthkarColors.OnSurfaceVariant
        )
    }
}

@Composable
private fun PrayerTimeRow(
    prayerType: PrayerType,
    time: LocalTime,
    isNext: Boolean,
    timeFormatter: DateTimeFormatter,
    isSunrise: Boolean = false,
    isArabic: Boolean
) {
    val backgroundColor = when {
        isNext -> WearAthkarColors.Surface
        else -> Color.Transparent
    }

    val textColor = when {
        isSunrise -> WearAthkarColors.OnSurfaceVariant
        isNext -> WearAthkarColors.Primary
        else -> Color.White
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = if (isArabic) Arrangement.SpaceBetween else Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isArabic) {
            // Time on the left for Arabic
            Text(
                text = time.format(timeFormatter),
                fontSize = 12.sp,
                color = textColor
            )
            // Arabic name on the right
            Text(
                text = prayerType.nameArabic,
                fontFamily = ScheherazadeFont,
                fontSize = 14.sp,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
        } else {
            // English name on the left
            Text(
                text = prayerType.nameEnglish,
                fontSize = 14.sp,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            // Time on the right for English
            Text(
                text = time.format(timeFormatter),
                fontSize = 12.sp,
                color = textColor
            )
        }
    }
}
