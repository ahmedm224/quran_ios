package com.quranmedia.player.recite.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.data.repository.ReadingTheme
import com.quranmedia.player.domain.model.Ayah
import com.quranmedia.player.presentation.theme.ReadingThemes
import com.quranmedia.player.recite.domain.model.HighlightType
import com.quranmedia.player.recite.domain.model.WordHighlightState
import com.quranmedia.player.recite.presentation.components.ReciteQuranPage
import com.quranmedia.player.recite.streaming.TranscriptionProviderType
import kotlinx.coroutines.launch

/**
 * Full Mushaf experience for Recite feature.
 *
 * - Shows Quran pages like the regular reader
 * - Mic FAB in bottom right to start recitation
 * - Word-level highlighting during recitation
 * - No AI calls until user clicks Mic
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReciteMushafScreen(
    initialPage: Int = 1,
    viewModel: ReciteStreamingViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToIndex: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ViewModel state
    val streamingState by viewModel.streamingState.collectAsState()
    val wordHighlights by viewModel.wordHighlights.collectAsState()
    val errorHighlights by viewModel.errorHighlights.collectAsState()
    val allSurahs by viewModel.allSurahs.collectAsState()
    val language by viewModel.language.collectAsState()
    val currentProvider by viewModel.currentProviderType.collectAsState()

    // UI state
    var showControls by remember { mutableStateOf(true) }
    var selectedError by remember { mutableStateOf<WordHighlightState?>(null) }
    var selectedAyahForRecite by remember { mutableStateOf<Ayah?>(null) }  // Long-press feedback

    // Reading theme - use LIGHT for recite mode
    val readingTheme = ReadingTheme.LIGHT
    val themeColors = ReadingThemes.getTheme(readingTheme)

    // Total pages in Quran
    val totalPages = 604

    // Pager state - RTL for Arabic (page 1 on right)
    val pagerState = rememberPagerState(
        initialPage = initialPage - 1,
        pageCount = { totalPages }
    )

    // Current page number (1-indexed)
    val currentPage by remember { derivedStateOf { pagerState.currentPage + 1 } }

    // Track ayahs for current page
    var currentPageAyahs by remember { mutableStateOf<List<Ayah>>(emptyList()) }

    // Load ayahs when page changes
    LaunchedEffect(currentPage) {
        viewModel.loadAyahsForPage(currentPage).collect { ayahs ->
            currentPageAyahs = ayahs
        }
    }

    // Check if recitation is active
    val isReciting = streamingState is ReciteStreamingState.Streaming ||
            streamingState is ReciteStreamingState.Connecting

    // Clear selected ayah only when user dismisses the completion dialog
    // Not on state changes, as that causes issues during restart

    // Permission handling
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startStreaming()
        }
    }

    fun requestPermissionAndStart() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.startStreaming()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Start recitation for current page
    fun startRecitationForCurrentPage() {
        if (currentPageAyahs.isNotEmpty()) {
            // Update selection with page ayahs (handles multiple surahs on same page)
            viewModel.updateSelectionFromPage(currentPage, currentPageAyahs, allSurahs)

            // Request permission and start
            requestPermissionAndStart()
        }
    }

    // Start recitation from a specific ayah (long-press)
    fun startRecitationFromAyah(startAyah: Ayah, pageAyahs: List<Ayah>) {
        if (pageAyahs.isEmpty()) return

        // Reset any previous state first (before setting new feedback)
        viewModel.reset()

        // Get ayahs from startAyah to end of page
        val ayahsFromStart = pageAyahs.filter { ayah ->
            // Include ayahs from the same surah starting from startAyah
            // or ayahs from later surahs on the same page
            if (ayah.surahNumber == startAyah.surahNumber) {
                ayah.ayahNumber >= startAyah.ayahNumber
            } else {
                ayah.surahNumber > startAyah.surahNumber
            }
        }

        if (ayahsFromStart.isNotEmpty()) {
            // Show visual feedback for selected ayah (after reset to avoid being cleared)
            selectedAyahForRecite = startAyah

            // Update selection with ayahs from startAyah onwards
            viewModel.updateSelectionFromAyah(startAyah, ayahsFromStart, allSurahs)

            // Request permission and start
            requestPermissionAndStart()
        }
    }

    // Immersive mode
    val window = (context as? android.app.Activity)?.window
    val insetsController = window?.let {
        androidx.core.view.WindowCompat.getInsetsController(it, it.decorView)
    }

    LaunchedEffect(showControls) {
        insetsController?.let { controller ->
            if (showControls) {
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            } else {
                controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            insetsController?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            viewModel.stopStreaming()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(themeColors.background)
        ) {
            // Main content - Pager (RTL for Quran)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondBoundsPageCount = 1
                ) { pageIndex ->
                    val pageNumber = pageIndex + 1
                    var pageAyahs by remember { mutableStateOf<List<Ayah>>(emptyList()) }

                    LaunchedEffect(pageNumber) {
                        viewModel.loadAyahsForPage(pageNumber).collect { ayahs ->
                            pageAyahs = ayahs
                        }
                    }

                    // Use ReciteQuranPage with word highlighting
                    ReciteQuranPage(
                        pageNumber = pageNumber,
                        ayahs = pageAyahs,
                        wordHighlights = if (pageNumber == currentPage) wordHighlights else emptyList(),
                        selectedAyah = if (pageNumber == currentPage) selectedAyahForRecite else null,
                        isReciting = isReciting,
                        readingTheme = readingTheme,
                        modifier = Modifier.fillMaxSize(),
                        onTap = { showControls = !showControls },
                        onWordClick = { highlight ->
                            if (highlight.type == HighlightType.ERROR) {
                                selectedError = highlight
                            }
                        },
                        onAyahLongPress = { ayah ->
                            // Start recitation from this ayah (works even during/after recitation)
                            startRecitationFromAyah(ayah, pageAyahs)
                        }
                    )
                }
            }

            // Top bar
            AnimatedVisibility(
                visible = showControls,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (language == AppLanguage.ARABIC) "تسميع" else "Recite",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (language == AppLanguage.ARABIC)
                                    "صفحة $currentPage من $totalPages"
                                else
                                    "Page $currentPage of $totalPages",
                                fontSize = 12.sp,
                                color = themeColors.topBarContent.copy(alpha = 0.8f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = themeColors.topBarContent
                            )
                        }
                    },
                    actions = {
                        // Provider toggle - for development/testing
                        val providerLabel = when (currentProvider) {
                            TranscriptionProviderType.QURAN_SERVER -> "QSR"
                            TranscriptionProviderType.OPENAI_GPT4O_TRANSCRIBE -> "GPT"
                        }
                        TextButton(
                            onClick = {
                                if (!isReciting) {
                                    val nextProvider = when (currentProvider) {
                                        TranscriptionProviderType.QURAN_SERVER ->
                                            TranscriptionProviderType.OPENAI_GPT4O_TRANSCRIBE
                                        TranscriptionProviderType.OPENAI_GPT4O_TRANSCRIBE ->
                                            TranscriptionProviderType.QURAN_SERVER
                                    }
                                    viewModel.setTranscriptionProvider(nextProvider)
                                }
                            },
                            enabled = !isReciting
                        ) {
                            Text(
                                text = providerLabel,
                                fontSize = 12.sp,
                                color = if (isReciting)
                                    themeColors.topBarContent.copy(alpha = 0.5f)
                                else
                                    themeColors.topBarContent
                            )
                        }

                        // Go to Index
                        IconButton(onClick = onNavigateToIndex) {
                            Icon(
                                Icons.Default.List,
                                contentDescription = "Index",
                                tint = themeColors.topBarContent
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = themeColors.topBarBackground,
                        titleContentColor = themeColors.topBarContent
                    )
                )
            }

            // Recording indicator when reciting
            AnimatedVisibility(
                visible = isReciting,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (showControls) 80.dp else 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF44336).copy(alpha = 0.9f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pulsing recording indicator (red dot)
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Text(
                            text = if (language == AppLanguage.ARABIC) "جاري التسجيل..." else "Recording...",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Mic FAB - bottom right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                FloatingActionButton(
                    onClick = {
                        if (isReciting) {
                            viewModel.stopStreaming()
                        } else {
                            startRecitationForCurrentPage()
                        }
                    },
                    containerColor = if (isReciting) Color(0xFFF44336) else themeColors.accent,
                    contentColor = Color.White,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (isReciting) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isReciting) "Stop" else "Start Reciting",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Error count badge
            if (errorHighlights.isNotEmpty() && !isReciting) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 76.dp, bottom = 40.dp)
                        .navigationBarsPadding(),
                    shape = CircleShape,
                    color = Color(0xFFF44336)
                ) {
                    Text(
                        text = "${errorHighlights.size}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

        }
    }

    // Error detail dialog
    if (selectedError != null) {
        AlertDialog(
            onDismissRequest = { selectedError = null },
            title = { Text("Error at Ayah ${selectedError!!.ayahNumber}") },
            text = {
                Column {
                    Text("Expected:", fontSize = 12.sp, color = Color.Green)
                    Text(
                        selectedError!!.expectedWord,
                        fontSize = 24.sp,
                        color = Color.Green
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Detected:", fontSize = 12.sp, color = Color.Red)
                    Text(
                        selectedError!!.transcribedWord.ifEmpty { "(skipped)" },
                        fontSize = 24.sp,
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Please repeat this ayah correctly to continue.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedError = null }) { Text("OK") }
            }
        )
    }

    // Completion dialog
    if (streamingState is ReciteStreamingState.Completed) {
        val completed = streamingState as ReciteStreamingState.Completed
        AlertDialog(
            onDismissRequest = {
                selectedAyahForRecite = null
                viewModel.reset()
            },
            title = {
                Text(
                    if (language == AppLanguage.ARABIC) "اكتمل التسميع" else "Recitation Complete"
                )
            },
            text = {
                Column {
                    Text(
                        text = if (language == AppLanguage.ARABIC)
                            "الدقة: ${String.format("%.1f", completed.accuracy)}%"
                        else
                            "Accuracy: ${String.format("%.1f", completed.accuracy)}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (completed.errorCount > 0) {
                        Text(
                            text = if (language == AppLanguage.ARABIC)
                                "عدد الأخطاء: ${completed.errorCount}"
                            else
                                "Errors: ${completed.errorCount}",
                            fontSize = 14.sp,
                            color = Color.Red
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedAyahForRecite = null
                    viewModel.reset()
                }) {
                    Text(if (language == AppLanguage.ARABIC) "حسنا" else "OK")
                }
            }
        )
    }
}

private fun AppLanguage.layoutDirection(): LayoutDirection {
    return when (this) {
        AppLanguage.ARABIC -> LayoutDirection.Rtl
        else -> LayoutDirection.Ltr
    }
}
