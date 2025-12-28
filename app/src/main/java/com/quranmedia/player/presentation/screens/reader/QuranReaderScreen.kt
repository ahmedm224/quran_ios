package com.quranmedia.player.presentation.screens.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.domain.model.Ayah
import com.quranmedia.player.domain.model.Reciter
import com.quranmedia.player.presentation.screens.reader.components.QuranPageComposable
import com.quranmedia.player.presentation.screens.reader.components.CustomRecitationDialog
import com.quranmedia.player.presentation.util.Strings
import com.quranmedia.player.presentation.util.layoutDirection
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.theme.ReadingThemeColors
import com.quranmedia.player.presentation.theme.ReadingThemes
import kotlinx.coroutines.launch

/**
 * Zoom modes for Quran reading:
 * - FIT_SCREEN: Smaller text, entire page fits on screen
 * - ZOOMED: Larger text (default), may require slight scrolling on dense pages
 * - SPLIT: Extra large text showing 50% of page, swipe to see remaining half
 */
enum class ZoomMode {
    FIT_SCREEN,  // All content fits on screen
    ZOOMED,      // Larger text (current default)
    SPLIT        // 50% of page per screen, swipe for rest
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    initialPage: Int = 1,
    initialSurahNumber: Int? = null,
    initialReciterId: String? = null,
    highlightSurahNumber: Int? = null,
    highlightAyahNumber: Int? = null,
    viewModel: QuranReaderViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToIndex: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val language = settings.appLanguage
    val readingTheme = settings.readingTheme

    // Get theme colors with custom colors if CUSTOM theme is selected
    val themeColors = if (readingTheme == com.quranmedia.player.data.repository.ReadingTheme.CUSTOM) {
        ReadingThemes.getTheme(
            readingTheme,
            com.quranmedia.player.presentation.theme.CustomThemeColors(
                backgroundColor = Color((settings.customBackgroundColor and 0xFFFFFFFF).toInt()),
                textColor = Color((settings.customTextColor and 0xFFFFFFFF).toInt()),
                headerColor = Color((settings.customHeaderColor and 0xFFFFFFFF).toInt())
            )
        )
    } else {
        ReadingThemes.getTheme(readingTheme)
    }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val density = LocalDensity.current

    // Immersive mode - controls hidden by default
    var showControls by remember { mutableStateOf(false) }

    // Enable immersive mode - hide system bars when controls are hidden
    val context = androidx.compose.ui.platform.LocalContext.current
    val window = (context as? android.app.Activity)?.window
    val insetsController = window?.let {
        androidx.core.view.WindowCompat.getInsetsController(it, it.decorView)
    }

    // Toggle system bars based on showControls
    LaunchedEffect(showControls) {
        insetsController?.let { controller ->
            if (showControls) {
                // Show system bars when controls are visible
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            } else {
                // Hide system bars for immersive reading
                controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    // Restore system bars when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            insetsController?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    // Context menu state for long-press on ayah
    var selectedAyah by remember { mutableStateOf<Ayah?>(null) }
    var showAyahMenu by remember { mutableStateOf(false) }
    var menuPosition by remember { mutableStateOf(Offset.Zero) }
    var showCustomRecitationDialog by remember { mutableStateOf(false) }

    // Screen size for clamping menu position
    var screenWidth by remember { mutableStateOf(0) }
    var screenHeight by remember { mutableStateOf(0) }

    var showGoToPageDialog by remember { mutableStateOf(false) }
    var zoomMode by rememberSaveable { mutableStateOf(ZoomMode.ZOOMED) }  // Default to zoomed mode (larger text)
    // Track pending page navigation after zoom mode change (to ensure scroll happens after recomposition)
    var pendingPageAfterZoomChange by remember { mutableStateOf<Int?>(null) }

    // In SPLIT mode, each real page becomes 2 virtual pages (first half, second half)
    val virtualPageCount = when (zoomMode) {
        ZoomMode.SPLIT -> state.totalPages * 2
        else -> state.totalPages
    }

    // Helper functions to convert between virtual and real pages
    fun virtualToRealPage(virtualPage: Int): Int = when (zoomMode) {
        ZoomMode.SPLIT -> (virtualPage / 2) + 1  // Each real page has 2 virtual pages
        else -> virtualPage + 1
    }

    fun isSecondHalf(virtualPage: Int): Boolean = when (zoomMode) {
        ZoomMode.SPLIT -> virtualPage % 2 == 1
        else -> false
    }

    fun realToVirtualPage(realPage: Int, secondHalf: Boolean = false): Int = when (zoomMode) {
        ZoomMode.SPLIT -> (realPage - 1) * 2 + (if (secondHalf) 1 else 0)
        else -> realPage - 1
    }

    // Pager state - uses virtual pages in SPLIT mode
    val validInitialPage = maxOf(1, initialPage)
    val pagerState = rememberPagerState(
        initialPage = validInitialPage - 1,
        pageCount = { maxOf(1, virtualPageCount) }
    )

    // Track the page where playback is happening
    var playbackPage by remember { mutableStateOf<Int?>(null) }
    // Track if user manually navigated away from playback
    var userNavigatedAway by remember { mutableStateOf(false) }
    // Show floating back button when user is away from playing page
    var showBackToPlayingButton by remember { mutableStateOf(false) }

    // Load initial page data
    LaunchedEffect(Unit) {
        viewModel.loadPage(validInitialPage)
        viewModel.refreshSelectedReciter()
    }

    // Set initial highlight from search result
    LaunchedEffect(highlightSurahNumber, highlightAyahNumber) {
        if (highlightSurahNumber != null && highlightAyahNumber != null) {
            viewModel.setHighlightedAyah(highlightSurahNumber, highlightAyahNumber)
        }
    }

    // Start playback if opened from Downloads with surah/reciter
    LaunchedEffect(initialSurahNumber, initialReciterId) {
        if (initialSurahNumber != null && initialReciterId != null) {
            viewModel.playFromDownload(initialReciterId, initialSurahNumber)
        }
    }

    // Check download status when playback changes
    LaunchedEffect(playbackState.currentSurah, playbackState.currentReciter) {
        if (playbackState.currentSurah != null && playbackState.currentReciter != null) {
            viewModel.checkDownloadStatus()
        }
    }

    // Track previous page for page-left detection
    var previousRealPage by remember { mutableStateOf<Int?>(null) }

    // Update view model when page changes
    LaunchedEffect(pagerState.currentPage, zoomMode) {
        val realPageNumber = virtualToRealPage(pagerState.currentPage)

        // Track page left (for reading progress tracking)
        previousRealPage?.let { prevPage ->
            if (prevPage != realPageNumber) {
                viewModel.onPageLeft(prevPage)
            }
        }

        viewModel.loadPage(realPageNumber)
        viewModel.updateCurrentPageBookmarkStatus(realPageNumber)
        previousRealPage = realPageNumber
    }

    // Track playback page and auto-follow when user is on the playing page
    LaunchedEffect(state.highlightedAyah, zoomMode) {
        state.highlightedAyah?.let { highlighted ->
            val realPage = viewModel.getPageForAyah(highlighted.surahNumber, highlighted.ayahNumber)
            if (realPage != null) {
                val previousPlaybackPage = playbackPage
                playbackPage = realPage

                // Auto-follow: if user hasn't navigated away, turn the page with recitation
                val currentRealPage = virtualToRealPage(pagerState.currentPage)
                if (!userNavigatedAway && previousPlaybackPage != null && realPage != currentRealPage) {
                    val targetVirtualPage = realToVirtualPage(realPage)
                    pagerState.animateScrollToPage(targetVirtualPage)
                }
            }
        }
    }

    // Detect when user manually swipes to a different page
    LaunchedEffect(pagerState.currentPage, zoomMode) {
        val currentRealPage = virtualToRealPage(pagerState.currentPage)
        if (playbackPage != null && currentRealPage != playbackPage && playbackState.isPlaying) {
            userNavigatedAway = true
            showBackToPlayingButton = true
        }
    }

    // Reset userNavigatedAway when user comes back to the playing page
    LaunchedEffect(pagerState.currentPage, playbackPage, zoomMode) {
        val currentRealPage = virtualToRealPage(pagerState.currentPage)
        if (playbackPage != null && currentRealPage == playbackPage) {
            userNavigatedAway = false
            showBackToPlayingButton = false
        }
    }

    // Show/hide back button based on playback state
    LaunchedEffect(playbackState.isPlaying, userNavigatedAway) {
        showBackToPlayingButton = playbackState.isPlaying && userNavigatedAway
    }

    // Reset userNavigatedAway when playback stops
    LaunchedEffect(playbackState.isPlaying) {
        if (!playbackState.isPlaying) {
            userNavigatedAway = false
            showBackToPlayingButton = false
            playbackPage = null
        }
    }

    // Handle pending page navigation after zoom mode change
    // This ensures scroll happens AFTER pagerState has recomposed with new pageCount
    LaunchedEffect(pendingPageAfterZoomChange, virtualPageCount) {
        pendingPageAfterZoomChange?.let { targetPage ->
            // Only navigate if target is within the new page count
            if (targetPage in 0 until virtualPageCount) {
                pagerState.scrollToPage(targetPage)
            }
            pendingPageAfterZoomChange = null
        }
    }

    // Keep screen on during reading if enabled in settings
    val keepScreenOn = settings.keepScreenOn
    DisposableEffect(keepScreenOn) {
        val window = (context as? android.app.Activity)?.window
        if (keepScreenOn) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(themeColors.background)
                .onGloballyPositioned { coordinates ->
                    screenWidth = coordinates.size.width
                    screenHeight = coordinates.size.height
                }
        ) {
            // Main content - Pager (always RTL for Quran reading regardless of app language)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(0.dp)), // Clip pager content
                    beyondBoundsPageCount = 1,
                    reverseLayout = false
                ) { pageIndex ->
                // Calculate real page number and whether this is second half (for SPLIT mode)
                val realPageNumber = when (zoomMode) {
                    ZoomMode.SPLIT -> (pageIndex / 2) + 1
                    else -> pageIndex + 1
                }
                val showSecondHalf = zoomMode == ZoomMode.SPLIT && pageIndex % 2 == 1

                // Wrap each page in a clipping box to prevent overflow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(0.dp))
                ) {
                    var pageAyahs by remember { mutableStateOf<List<Ayah>>(emptyList()) }

                    LaunchedEffect(realPageNumber) {
                        viewModel.getAyahsForPage(realPageNumber).collect { ayahs ->
                            pageAyahs = ayahs
                        }
                    }

                    QuranPageComposable(
                        pageNumber = realPageNumber,
                        ayahs = pageAyahs,
                        highlightedAyah = state.highlightedAyah,
                        modifier = Modifier.fillMaxSize(),
                        zoomMode = zoomMode,
                        splitHalf = if (showSecondHalf) 1 else 0,  // 0 = first half, 1 = second half
                        readingTheme = readingTheme,
                        customBackgroundColor = if (readingTheme == com.quranmedia.player.data.repository.ReadingTheme.CUSTOM)
                            Color((settings.customBackgroundColor and 0xFFFFFFFF).toInt()) else null,
                        customTextColor = if (readingTheme == com.quranmedia.player.data.repository.ReadingTheme.CUSTOM)
                            Color((settings.customTextColor and 0xFFFFFFFF).toInt()) else null,
                        customHeaderColor = if (readingTheme == com.quranmedia.player.data.repository.ReadingTheme.CUSTOM)
                            Color((settings.customHeaderColor and 0xFFFFFFFF).toInt()) else null,
                        onTap = {
                            showControls = !showControls
                        },
                        onAyahLongPress = { ayah: Ayah, position: Offset ->
                            selectedAyah = ayah
                            menuPosition = position
                            showAyahMenu = true
                        }
                    )
                }
                }
            }

            // Top bar - animated visibility
            AnimatedVisibility(
                visible = showControls,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                TopAppBar(
                    title = {
                        val currentRealPage = virtualToRealPage(pagerState.currentPage)
                        val halfIndicator = if (zoomMode == ZoomMode.SPLIT) {
                            if (isSecondHalf(pagerState.currentPage)) " (٢/٢)" else " (١/٢)"
                        } else ""

                        Column {
                            Text(
                                text = if (language == AppLanguage.ARABIC) "القرآن الكريم" else "Quran",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )

                            // Page number with daily target on same line
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (language == AppLanguage.ARABIC)
                                        "صفحة $currentRealPage من ${state.totalPages}$halfIndicator"
                                    else
                                        "Page $currentRealPage of ${state.totalPages}$halfIndicator",
                                    fontSize = 12.sp,
                                    color = themeColors.topBarContent.copy(alpha = 0.8f),
                                    maxLines = 1
                                )

                                // Daily target indicator
                                state.dailyTargetPages?.let { target ->
                                    Text(
                                        text = if (language == AppLanguage.ARABIC)
                                            "• ${String.format("%.0f", target)} ص/يوم"
                                        else
                                            "• ${String.format("%.0f", target)} p/day",
                                        fontSize = 11.sp,
                                        color = themeColors.ayahMarker,
                                        maxLines = 1,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
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
                        // Download button - only show when there's active playback
                        if (playbackState.currentSurah != null) {
                            val currentDownloadState = downloadState
                            IconButton(
                                onClick = { viewModel.downloadCurrentSurah() },
                                enabled = currentDownloadState !is DownloadState.Downloading
                            ) {
                                when (currentDownloadState) {
                                    is DownloadState.Downloaded -> Icon(
                                        Icons.Default.CloudDone,
                                        contentDescription = "Downloaded",
                                        tint = themeColors.ayahMarker
                                    )
                                    is DownloadState.Downloading -> CircularProgressIndicator(
                                        progress = { currentDownloadState.progress },
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = themeColors.topBarContent
                                    )
                                    else -> Icon(
                                        Icons.Default.CloudDownload,
                                        contentDescription = "Download",
                                        tint = themeColors.topBarContent
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { viewModel.toggleBookmarkCurrentPage() }) {
                            Icon(
                                if (state.isCurrentPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (state.isCurrentPageBookmarked) themeColors.ayahMarker else themeColors.topBarContent
                            )
                        }
                        IconButton(onClick = {
                            // Remember current real page before changing zoom mode
                            val currentRealPage = virtualToRealPage(pagerState.currentPage)

                            // Toggle between ZOOMED and SPLIT modes
                            // Pages 1-2 stay in ZOOMED mode (too little text to split)
                            val newZoomMode = when (zoomMode) {
                                ZoomMode.ZOOMED -> if (currentRealPage <= 2) ZoomMode.ZOOMED else ZoomMode.SPLIT
                                ZoomMode.SPLIT -> ZoomMode.ZOOMED
                                ZoomMode.FIT_SCREEN -> ZoomMode.ZOOMED  // Fallback, not used
                            }

                            // Calculate target virtual page for new mode
                            // Store it for navigation AFTER recomposition (fixes page 302+ bug)
                            pendingPageAfterZoomChange = when (newZoomMode) {
                                ZoomMode.SPLIT -> (currentRealPage - 1) * 2  // First half of same page
                                else -> currentRealPage - 1
                            }

                            // Change zoom mode - this triggers recomposition with new pageCount
                            zoomMode = newZoomMode
                        }) {
                            Icon(
                                when (zoomMode) {
                                    ZoomMode.ZOOMED -> Icons.Default.ZoomIn           // Can zoom to split
                                    ZoomMode.SPLIT -> Icons.Default.ZoomOut           // Can zoom out
                                    ZoomMode.FIT_SCREEN -> Icons.Default.ZoomIn       // Fallback
                                },
                                contentDescription = "Zoom",
                                tint = themeColors.topBarContent
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
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

            // Bottom bar - controlled by showControls (user tap toggles visibility)
            AnimatedVisibility(
                visible = showControls,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                EnhancedPlaybackControlBar(
                    selectedReciter = state.selectedReciter,
                    reciters = state.reciters,
                    language = language,
                    isPlaying = playbackState.isPlaying,
                    currentSurah = playbackState.currentSurahNameArabic,
                    currentAyah = playbackState.currentAyah,
                    totalAyahs = playbackState.totalAyahs,
                    currentPosition = playbackState.currentPosition,
                    duration = playbackState.duration,
                    playbackSpeed = playbackState.playbackSpeed,
                    ayahRepeatCount = settings.ayahRepeatCount,
                    themeColors = themeColors,
                    onReciterSelected = { reciter -> viewModel.selectReciter(reciter) },
                    onPlayPauseClick = {
                        if (playbackState.currentSurah != null) {
                            viewModel.togglePlayPause()
                        } else {
                            val ayahs = state.ayahsOnPage
                            if (ayahs.isNotEmpty()) {
                                val firstAyah = ayahs.first()
                                viewModel.playFromAyah(firstAyah.surahNumber, firstAyah.ayahNumber)
                            }
                        }
                    },
                    onStopClick = { viewModel.stopPlayback() },
                    onPreviousAyah = { viewModel.previousAyah() },
                    onNextAyah = { viewModel.nextAyah() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onSpeedClick = {
                        val nextSpeed = when (playbackState.playbackSpeed) {
                            1.0f -> 1.25f
                            1.25f -> 1.5f
                            1.5f -> 2.0f
                            else -> 1.0f
                        }
                        viewModel.setPlaybackSpeed(nextSpeed)
                    },
                    onRepeatClick = {
                        val nextRepeat = when (settings.ayahRepeatCount) {
                            1 -> 2
                            2 -> 3
                            else -> 1
                        }
                        viewModel.setAyahRepeatCount(nextRepeat)
                    }
                )
            }

            // Loading indicator
            if (state.isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = themeColors.accent)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "جاري تحميل القرآن...",
                        fontFamily = scheherazadeFont,
                        color = themeColors.textPrimary,
                        fontSize = 14.sp
                    )
                }
            }

            // Floating "back to playing page" button
            AnimatedVisibility(
                visible = showBackToPlayingButton && playbackPage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        playbackPage?.let { realPage ->
                            coroutineScope.launch {
                                val targetVirtualPage = realToVirtualPage(realPage)
                                pagerState.animateScrollToPage(targetVirtualPage)
                            }
                        }
                    },
                    containerColor = themeColors.accent,
                    contentColor = Color.White,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Headphones,
                        contentDescription = if (language == AppLanguage.ARABIC) "العودة للتلاوة" else "Back to playing",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Compact icon-only context menu at touch position
            if (showAyahMenu && selectedAyah != null) {
                // Menu dimensions (approximate): ~180dp wide (4 icons), ~48dp tall
                val menuWidthPx = with(density) { 180.dp.toPx().toInt() }
                val menuHeightPx = with(density) { 48.dp.toPx().toInt() }
                val padding = with(density) { 8.dp.toPx().toInt() }

                // Position menu near touch point, clamped to screen bounds
                val rawX = menuPosition.x.toInt() - (menuWidthPx / 2)
                val rawY = menuPosition.y.toInt() - menuHeightPx - padding

                // Clamp X to stay within screen (with padding)
                val menuX = rawX.coerceIn(padding, (screenWidth - menuWidthPx - padding).coerceAtLeast(padding))
                // Clamp Y - if too close to top, show below the touch point instead
                val menuY = if (rawY < padding) {
                    (menuPosition.y.toInt() + padding).coerceAtMost(screenHeight - menuHeightPx - padding)
                } else {
                    rawY.coerceAtMost(screenHeight - menuHeightPx - padding)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showAyahMenu = false
                            selectedAyah = null
                        }
                ) {
                    Surface(
                        modifier = Modifier
                            .offset { IntOffset(menuX, menuY) },
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            // Play button
                            IconButton(
                                onClick = {
                                    selectedAyah?.let { ayah ->
                                        timber.log.Timber.d("Playing from context menu: surah ${ayah.surahNumber}, ayah ${ayah.ayahNumber}")
                                        viewModel.playFromAyah(ayah.surahNumber, ayah.ayahNumber)
                                    }
                                    showAyahMenu = false
                                    selectedAyah = null
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = themeColors.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Copy button
                            IconButton(
                                onClick = {
                                    selectedAyah?.let { ayah ->
                                        clipboardManager.setText(AnnotatedString(ayah.textArabic))
                                    }
                                    showAyahMenu = false
                                    selectedAyah = null
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = themeColors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Bookmark button
                            IconButton(
                                onClick = {
                                    viewModel.toggleBookmarkCurrentPage()
                                    showAyahMenu = false
                                    selectedAyah = null
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    if (state.isCurrentPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = if (state.isCurrentPageBookmarked) themeColors.ayahMarker else themeColors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Custom Recitation button
                            IconButton(
                                onClick = {
                                    showAyahMenu = false
                                    showCustomRecitationDialog = true
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = "Custom Recitation",
                                    tint = themeColors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Go to page dialog (always shows real page numbers 1-604)
    if (showGoToPageDialog) {
        val currentRealPage = virtualToRealPage(pagerState.currentPage)
        GoToPageDialog(
            currentPage = currentRealPage,
            totalPages = state.totalPages,
            onDismiss = { showGoToPageDialog = false },
            onGoToPage = { realPage ->
                coroutineScope.launch {
                    val targetVirtualPage = realToVirtualPage(realPage)
                    pagerState.animateScrollToPage(targetVirtualPage)
                }
                showGoToPageDialog = false
            }
        )
    }

    // Custom Recitation Dialog
    if (showCustomRecitationDialog && selectedAyah != null) {
        val surahs by viewModel.getAllSurahs().collectAsState(initial = emptyList())
        val reciters by viewModel.getAllReciters().collectAsState(initial = emptyList())
        val presets by viewModel.getRecitationPresets().collectAsState(initial = emptyList())
        val appLanguage by viewModel.getAppLanguage().collectAsState(initial = com.quranmedia.player.data.repository.AppLanguage.ENGLISH)

        // Use the selected reciter from settings (same as media controller default)
        val selectedReciterId by viewModel.getSelectedReciterId().collectAsState(initial = "")
        val currentReciterId = selectedReciterId

        CustomRecitationDialog(
            initialStartSurah = selectedAyah!!.surahNumber,
            initialStartAyah = selectedAyah!!.ayahNumber,
            currentReciterId = currentReciterId,
            language = appLanguage,
            reciters = reciters,
            surahs = surahs,
            savedPresets = presets,
            onDismiss = {
                showCustomRecitationDialog = false
                selectedAyah = null
            },
            onConfirm = { reciterId, settings ->
                viewModel.startCustomRecitation(reciterId, settings)
                showCustomRecitationDialog = false
                selectedAyah = null
            },
            onSavePreset = { name, settings ->
                viewModel.saveRecitationPreset(name, settings)
            }
        )
    }

}

@Composable
private fun EnhancedPlaybackControlBar(
    selectedReciter: Reciter?,
    reciters: List<Reciter>,
    language: AppLanguage,
    isPlaying: Boolean,
    currentSurah: String?,
    currentAyah: Int?,
    totalAyahs: Int?,
    currentPosition: Long,
    duration: Long,
    playbackSpeed: Float,
    ayahRepeatCount: Int,
    themeColors: ReadingThemeColors,
    onReciterSelected: (Reciter) -> Unit,
    onPlayPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    onPreviousAyah: () -> Unit,
    onNextAyah: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSpeedClick: () -> Unit,
    onRepeatClick: () -> Unit
) {
    val hasActivePlayback = currentSurah != null

    val reciterDisplayName = if (selectedReciter != null) {
        if (language == AppLanguage.ARABIC) {
            // Use Arabic name if available and valid (not just dots or empty)
            val arabicName = selectedReciter.nameArabic
            if (!arabicName.isNullOrBlank() && !arabicName.all { it == '.' || it == ' ' }) {
                arabicName
            } else {
                selectedReciter.name
            }
        } else {
            selectedReciter.name
        }
    } else {
        Strings.selectReciter.get(language)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = themeColors.bottomBarBackground,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (hasActivePlayback && duration > 0) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Slider(
                        value = currentPosition.toFloat().coerceIn(0f, duration.toFloat()),
                        onValueChange = { onSeekTo(it.toLong().coerceIn(0, duration)) },
                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = themeColors.accent,
                            activeTrackColor = themeColors.accentLight,
                            inactiveTrackColor = themeColors.accentLight.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.height(20.dp)
                    )
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(currentPosition), fontSize = 10.sp, color = themeColors.textSecondary)
                        Text(formatTime(duration), fontSize = 10.sp, color = themeColors.textSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var showReciterMenu by remember { mutableStateOf(false) }

                val sortedReciters = remember(reciters, language) {
                    reciters.sortedBy { if (language == AppLanguage.ARABIC) it.nameArabic ?: it.name else it.name }
                }

                // Reciter selector - follows app language direction (right in Arabic, left in English)
                Box(modifier = Modifier.weight(1f).widthIn(min = 100.dp)) {
                    OutlinedCard(
                        onClick = { showReciterMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = themeColors.accent.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, null, tint = themeColors.accent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == AppLanguage.ARABIC) "القارئ" else "Reciter",
                                    fontSize = 8.sp,
                                    color = themeColors.textSecondary,
                                    maxLines = 1
                                )
                                Text(
                                    reciterDisplayName,
                                    fontFamily = null,  // Use system default - KFGQPC font doesn't support all Arabic letters
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = themeColors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (currentSurah != null && currentAyah != null) {
                                    Text(
                                        "$currentSurah • ${if (language == AppLanguage.ARABIC) "آية" else "Ayah"} $currentAyah${totalAyahs?.let { "/$it" } ?: ""}",
                                        fontFamily = scheherazadeFont,
                                        fontSize = 9.sp,
                                        color = themeColors.textSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                            Icon(
                                if (showReciterMenu) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                null, tint = themeColors.accent, modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showReciterMenu,
                        onDismissRequest = { showReciterMenu = false },
                        modifier = Modifier.heightIn(max = 300.dp).background(Color.White)
                    ) {
                        sortedReciters.forEach { reciter ->
                            val isSelected = reciter.id == selectedReciter?.id
                            DropdownMenuItem(
                                text = {
                                    val displayName = if (language == AppLanguage.ARABIC) {
                                        val arabicName = reciter.nameArabic
                                        if (!arabicName.isNullOrBlank() && !arabicName.all { it == '.' || it == ' ' }) arabicName else reciter.name
                                    } else reciter.name
                                    Text(
                                        displayName,
                                        fontFamily = null,  // Use system default - KFGQPC font doesn't support all Arabic letters
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) themeColors.accent else Color.DarkGray
                                    )
                                },
                                onClick = { onReciterSelected(reciter); showReciterMenu = false },
                                leadingIcon = if (isSelected) {{ Icon(Icons.Default.Check, null, tint = themeColors.accent, modifier = Modifier.size(18.dp)) }} else null
                            )
                        }
                    }
                }

                // Media control buttons - always LTR for universal prev/play/next order
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (hasActivePlayback) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(themeColors.accentLight.copy(alpha = 0.15f)).clickable { onSpeedClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${playbackSpeed}x", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            }
                        }

                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(if (ayahRepeatCount > 1) themeColors.accent.copy(alpha = 0.2f) else themeColors.accentLight.copy(alpha = 0.15f)).clickable { onRepeatClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${ayahRepeatCount}×", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (ayahRepeatCount > 1) themeColors.accent else themeColors.textSecondary)
                        }

                        // =============================================================
                        // MEDIA CONTROLS - RTL HANDLING
                        // =============================================================
                        // IMPORTANT: Icons must always point OUTWARD from play button:
                        //   - Left button: SkipPrevious (<<) pointing left
                        //   - Right button: SkipNext (>>) pointing right
                        //
                        // ACTIONS swap for RTL (Arabic):
                        //   - LTR (English): Left = Previous, Right = Next
                        //   - RTL (Arabic): Left = Next, Right = Previous
                        //
                        // In Arabic, "forward" in Quran is LEFT (RTL reading direction).
                        // =============================================================
                        val isRtl = language == AppLanguage.ARABIC

                        if (hasActivePlayback) {
                            // Left button - SkipPrevious icon (<<), action swaps for RTL
                            IconButton(
                                onClick = if (isRtl) onNextAyah else onPreviousAyah,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.SkipPrevious,
                                    if (isRtl) "Next Ayah" else "Previous Ayah",
                                    tint = themeColors.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        FilledIconButton(
                            onClick = onPlayPauseClick,
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = themeColors.accent, contentColor = if (themeColors.isDark) Color.Black else Color.White),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", modifier = Modifier.size(26.dp))
                        }

                        if (hasActivePlayback) {
                            // Right button - SkipNext icon (>>), action swaps for RTL
                            IconButton(
                                onClick = if (isRtl) onPreviousAyah else onNextAyah,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.SkipNext,
                                    if (isRtl) "Previous Ayah" else "Next Ayah",
                                    tint = themeColors.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            FilledIconButton(
                                onClick = onStopClick,
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = themeColors.divider, contentColor = themeColors.textPrimary),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Stop, "Stop", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoToPageDialog(
    currentPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onGoToPage: (Int) -> Unit
) {
    var pageInput by remember { mutableStateOf(currentPage.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الذهاب إلى صفحة", fontFamily = scheherazadeFont, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("أدخل رقم الصفحة (1-$totalPages):")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it.filter { c -> c.isDigit() } },
                    singleLine = true,
                    label = { Text("الصفحة") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val page = pageInput.toIntOrNull()
                if (page != null && page in 1..totalPages) onGoToPage(page)
            }) { Text("اذهب") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
