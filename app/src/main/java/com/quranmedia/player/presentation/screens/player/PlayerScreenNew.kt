package com.quranmedia.player.presentation.screens.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.R

// Arabic Quranic font
private val quranFontFamily = FontFamily(
    Font(R.font.scheherazade_regular, FontWeight.Normal)
)

// Playback speed options
private val speedOptions = listOf(1.0f, 1.25f, 1.5f, 2.0f)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerScreenNew(
    reciterId: String,
    surahNumber: Int,
    resumeFromSaved: Boolean = false,
    startFromAyah: Int? = null,
    viewModel: PlayerViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val bookmarkSaved by viewModel.bookmarkSaved.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()

    LaunchedEffect(reciterId, surahNumber, startFromAyah) {
        val resumePosition = if (resumeFromSaved) {
            viewModel.getSavedPosition(reciterId, surahNumber)
        } else null

        viewModel.loadAudio(reciterId, surahNumber, resumePosition, startFromAyah)
        viewModel.checkDownloadStatus(reciterId, surahNumber)
    }

    // Islamic green theme colors
    val islamicGreen = Color(0xFF2E7D32)
    val lightGreen = Color(0xFF66BB6A)
    val darkGreen = Color(0xFF1B5E20)
    val goldAccent = Color(0xFFFFD700)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = playbackState.currentSurahNameEnglish ?: "Alfurqan",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 16.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            playbackState.currentSurahNameArabic?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 14.sp
                                )
                            }
                            playbackState.currentReciterName?.let {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Download button
                    IconButton(
                        onClick = { viewModel.downloadCurrentSurah() },
                        enabled = downloadState !is DownloadButtonState.Downloading
                    ) {
                        when (downloadState) {
                            is DownloadButtonState.NotDownloaded -> {
                                Icon(
                                    Icons.Default.CloudDownload,
                                    contentDescription = "Download Surah",
                                    tint = Color.White
                                )
                            }
                            is DownloadButtonState.Downloading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            }
                            is DownloadButtonState.Downloaded -> {
                                Icon(
                                    Icons.Default.CloudDone,
                                    contentDescription = "Downloaded",
                                    tint = goldAccent
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = islamicGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        var showSpeedDialog by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main content area (scrollable) - takes remaining space
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ayah display card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Ayah selector dropdown - more compact
                        var ayahDropdownExpanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = ayahDropdownExpanded,
                            onExpandedChange = { ayahDropdownExpanded = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .clickable { ayahDropdownExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = islamicGreen.copy(alpha = 0.05f)
                                ),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(islamicGreen.copy(alpha = 0.3f))
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (playbackState.currentAyah != null && playbackState.totalAyahs != null) {
                                            "آية ${playbackState.currentAyah} من ${playbackState.totalAyahs}"
                                        } else {
                                            "جاري التحميل..."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = islamicGreen,
                                        fontSize = 14.sp
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Ayah",
                                        tint = islamicGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (playbackState.totalAyahs != null && playbackState.totalAyahs!! > 0) {
                                ExposedDropdownMenu(
                                    expanded = ayahDropdownExpanded,
                                    onDismissRequest = { ayahDropdownExpanded = false },
                                    modifier = Modifier
                                        .heightIn(max = 300.dp)
                                        .background(Color.White)
                                ) {
                                    for (ayahNum in 1..playbackState.totalAyahs!!) {
                                        val isCurrentAyah = ayahNum == playbackState.currentAyah
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "آية $ayahNum",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = if (isCurrentAyah) Color.White else darkGreen,
                                                        fontWeight = if (isCurrentAyah) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 14.sp
                                                    )
                                                )
                                            },
                                            onClick = {
                                                viewModel.seekToAyah(ayahNum)
                                                ayahDropdownExpanded = false
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = darkGreen,
                                                leadingIconColor = darkGreen,
                                                disabledTextColor = Color.Gray
                                            ),
                                            modifier = Modifier.background(
                                                if (isCurrentAyah) islamicGreen else Color.Transparent
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Arabic ayah text - Adaptive sizing for different ayah lengths
                        if (playbackState.currentAyahText != null) {
                            val ayahText = playbackState.currentAyahText!!

                            // Add ayah end marker
                            val ayahEndMarker = " ۝ "
                            val displayText = "$ayahText$ayahEndMarker"

                            val textLength = ayahText.length

                            // Adaptive font size based on ayah length
                            val fontSize = when {
                                textLength < 50 -> 26.sp
                                textLength < 100 -> 22.sp
                                textLength < 200 -> 19.sp
                                textLength < 400 -> 17.sp
                                else -> 15.sp
                            }

                            val lineHeight = fontSize * 1.6f

                            // For very long ayahs, use single-line marquee
                            if (textLength > 300) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp, horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = displayText,
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontFamily = quranFontFamily,
                                                letterSpacing = 0.sp,
                                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                                    includeFontPadding = false
                                                )
                                            ),
                                            fontSize = fontSize,
                                            textAlign = TextAlign.Center,
                                            color = Color.Black,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .basicMarquee(
                                                    iterations = Int.MAX_VALUE,
                                                    delayMillis = 500,
                                                    initialDelayMillis = 1000,
                                                    velocity = 40.dp,
                                                    spacing = androidx.compose.foundation.MarqueeSpacing(48.dp)
                                                )
                                        )
                                    }
                                }
                            } else {
                                // Multiline scrollable for shorter ayahs
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 260.dp)
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 16.dp, horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = displayText,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontFamily = quranFontFamily,
                                            letterSpacing = 0.sp,
                                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                                includeFontPadding = false
                                            )
                                        ),
                                        fontSize = fontSize,
                                        lineHeight = lineHeight,
                                        textAlign = TextAlign.Center,
                                        color = Color.Black,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(24.dp),
                                color = islamicGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Fixed bottom control bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Progress slider with time
                    Slider(
                        value = if (playbackState.duration > 0) {
                            playbackState.currentPosition.toFloat().coerceIn(0f, playbackState.duration.toFloat())
                        } else 0f,
                        onValueChange = { newValue ->
                            if (playbackState.duration > 0 && !playbackState.isBuffering) {
                                viewModel.seekTo(newValue.toLong().coerceIn(0, playbackState.duration))
                            }
                        },
                        valueRange = 0f..playbackState.duration.toFloat().coerceAtLeast(1f),
                        enabled = playbackState.duration > 0 && !playbackState.isBuffering,
                        colors = SliderDefaults.colors(
                            thumbColor = islamicGreen,
                            activeTrackColor = lightGreen,
                            inactiveTrackColor = lightGreen.copy(alpha = 0.3f),
                            disabledThumbColor = islamicGreen.copy(alpha = 0.5f),
                            disabledActiveTrackColor = lightGreen.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.height(24.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatTime(playbackState.currentPosition),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                        Text(
                            formatTime(playbackState.duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Compact playback controls row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speed button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(lightGreen.copy(alpha = 0.15f))
                                .clickable { showSpeedDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${playbackState.playbackSpeed}x",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = islamicGreen
                            )
                        }

                        // Previous ayah
                        IconButton(
                            onClick = { viewModel.previousAyah() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous Ayah",
                                tint = islamicGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Play/Pause (larger, prominent)
                        FilledIconButton(
                            onClick = { viewModel.togglePlayPause() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = islamicGreen,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Next ayah
                        IconButton(
                            onClick = { viewModel.nextAyah() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next Ayah",
                                tint = islamicGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Bookmark button (icon only)
                        IconButton(
                            onClick = { viewModel.createBookmark() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (bookmarkSaved) lightGreen.copy(alpha = 0.3f)
                                    else goldAccent.copy(alpha = 0.15f)
                                )
                        ) {
                            Icon(
                                if (bookmarkSaved) Icons.Default.Check else Icons.Default.Bookmark,
                                contentDescription = if (bookmarkSaved) "Bookmark saved" else "Add bookmark",
                                tint = if (bookmarkSaved) islamicGreen else Color(0xFFF57C00),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Speed selection dialog
        if (showSpeedDialog) {
            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                title = {
                    Text(
                        "سرعة التشغيل",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        speedOptions.forEach { speed ->
                            val isSelected = playbackState.playbackSpeed == speed
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) islamicGreen.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable {
                                        viewModel.setPlaybackSpeed(speed)
                                        showSpeedDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${speed}x",
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) islamicGreen else Color.Black
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = islamicGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSpeedDialog = false }) {
                        Text("إغلاق")
                    }
                }
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
