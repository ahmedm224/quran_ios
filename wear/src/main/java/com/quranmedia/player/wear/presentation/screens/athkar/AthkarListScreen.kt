package com.quranmedia.player.wear.presentation.screens.athkar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.HorizontalPageIndicator
import androidx.wear.compose.material.PageIndicatorState
import androidx.wear.compose.material.Text
import com.quranmedia.player.wear.domain.model.Thikr
import com.quranmedia.player.wear.presentation.theme.ScheherazadeFont
import com.quranmedia.player.wear.presentation.theme.WearAthkarColors
import com.quranmedia.player.wear.presentation.util.AirGestureDetector
import com.quranmedia.player.wear.presentation.util.HapticFeedbackManager
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun AthkarListScreen(
    categoryId: String,
    viewModel: ThikrViewModel = hiltViewModel()
) {
    val athkar by viewModel.athkar
    val remainingCounts = viewModel.remainingCounts
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Initialize haptic feedback manager
    val hapticManager = remember { HapticFeedbackManager(context) }

    // Initialize air gesture detector
    val airGestureDetector = remember { AirGestureDetector(context) }

    if (athkar.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { athkar.size })

    // Page indicator state
    val pageIndicatorState = remember(pagerState) {
        object : PageIndicatorState {
            override val pageCount: Int get() = athkar.size
            override val pageOffset: Float get() = pagerState.currentPageOffsetFraction
            override val selectedPage: Int get() = pagerState.currentPage
        }
    }

    // Collect air gestures
    LaunchedEffect(airGestureDetector, pagerState.currentPage) {
        if (!airGestureDetector.isSupported()) {
            Timber.w("Air gestures not supported on this device")
            return@LaunchedEffect
        }

        airGestureDetector.gestureFlow()
            .catch { e -> Timber.e(e, "Error in air gesture detection") }
            .collect { gesture ->
                val currentThikr = athkar.getOrNull(pagerState.currentPage)
                if (currentThikr != null) {
                    val remaining = remainingCounts[currentThikr.id] ?: currentThikr.repeatCount
                    if (remaining > 0) {
                        // Perform haptic feedback
                        hapticManager.performCountFeedback(remaining, currentThikr.repeatCount)

                        // Decrement counter
                        val completed = viewModel.decrementCount(currentThikr.id)
                        Timber.d("Air gesture detected: $gesture, remaining: ${remaining - 1}")

                        // Auto-advance if complete
                        if (completed && pagerState.currentPage < athkar.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val thikr = athkar[page]
            val remaining = remainingCounts[thikr.id] ?: thikr.repeatCount
            val progress = viewModel.getProgress(thikr.id)
            val isComplete = viewModel.isComplete(thikr.id)

            ThikrPage(
                thikr = thikr,
                remaining = remaining,
                progress = progress,
                isComplete = isComplete,
                currentPage = page + 1,
                totalPages = athkar.size,
                hapticManager = hapticManager,
                onCount = {
                    val completed = viewModel.decrementCount(thikr.id)
                    if (completed && page < athkar.size - 1) {
                        // Auto-advance to next thikr when complete
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(page + 1)
                        }
                    }
                },
                onReset = {
                    viewModel.resetCount(thikr.id)
                }
            )
        }

        // Page indicator at bottom
        HorizontalPageIndicator(
            pageIndicatorState = pageIndicatorState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ThikrPage(
    thikr: Thikr,
    remaining: Int,
    progress: Float,
    isComplete: Boolean,
    currentPage: Int,
    totalPages: Int,
    hapticManager: HapticFeedbackManager,
    onCount: () -> Unit,
    onReset: () -> Unit
) {
    var lastPinchScale by remember { mutableFloatStateOf(1f) }
    var accumulatedRotation by remember { mutableFloatStateOf(0f) }
    val rotationThreshold = 30f // Degrees of rotation needed to trigger a count

    // Focus requester for rotary input
    val focusRequester = remember { FocusRequester() }

    // Request focus when this page becomes visible
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WearAthkarColors.Background)
            // Rotary input for bezel/crown rotation
            .focusRequester(focusRequester)
            .onRotaryScrollEvent { event ->
                if (!isComplete) {
                    // Accumulate rotation
                    accumulatedRotation += event.verticalScrollPixels

                    // Check if accumulated rotation exceeds threshold (either direction)
                    if (kotlin.math.abs(accumulatedRotation) >= rotationThreshold) {
                        // Perform haptic feedback and count
                        hapticManager.performCountFeedback(remaining, thikr.repeatCount)
                        onCount()
                        // Reset accumulated rotation
                        accumulatedRotation = 0f
                    }
                }
                true // Consume the event
            }
            // Hardware button support (side buttons on Samsung watches)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && !isComplete) {
                    when (keyEvent.key) {
                        // Primary button (usually top/back button on Samsung watches)
                        Key.Stem1,
                        Key.StemPrimary,
                        // Secondary button
                        Key.Stem2,
                        Key.Stem3,
                        // Navigation keys that might be mapped
                        Key.NavigateNext,
                        Key.NavigatePrevious,
                        // Enter/confirm
                        Key.Enter,
                        Key.NumPadEnter,
                        // D-pad center (some watches)
                        Key.DirectionCenter -> {
                            hapticManager.performCountFeedback(remaining, thikr.repeatCount)
                            onCount()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .focusable()
            // Tap gestures
            .pointerInput(isComplete, remaining) {
                detectTapGestures(
                    onTap = {
                        if (!isComplete) {
                            hapticManager.performCountFeedback(remaining, thikr.repeatCount)
                            onCount()
                        }
                    },
                    onLongPress = {
                        // Long press to reset with strong feedback
                        hapticManager.strongFeedback()
                        onReset()
                    }
                )
            }
            // Pinch gestures
            .pointerInput(isComplete, remaining) {
                detectTransformGestures { _, _, zoom, _ ->
                    // Detect pinch gesture
                    // When pinching in (zoom < 1), count
                    if (zoom < 0.95f && lastPinchScale >= 0.95f && !isComplete) {
                        hapticManager.performCountFeedback(remaining, thikr.repeatCount)
                        onCount()
                    }
                    lastPinchScale = zoom
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Circular progress indicator around the edge
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxSize(),
            indicatorColor = if (isComplete) WearAthkarColors.CompletedGreen else WearAthkarColors.LightGreen,
            trackColor = WearAthkarColors.Surface,
            strokeWidth = 4.dp
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            // Page indicator text
            Text(
                text = "$currentPage / $totalPages",
                fontSize = 10.sp,
                color = WearAthkarColors.OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Arabic text
            Text(
                text = cleanArabicText(thikr.textArabic),
                fontFamily = ScheherazadeFont,
                fontSize = 16.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 24.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Counter display
            Text(
                text = if (isComplete) "✓" else "$remaining",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (isComplete) WearAthkarColors.CompletedGreen else WearAthkarColors.Primary
            )

            // Reference (if available)
            if (!thikr.reference.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = thikr.reference,
                    fontFamily = ScheherazadeFont,
                    fontSize = 10.sp,
                    color = WearAthkarColors.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Clean Arabic text by removing problematic Unicode characters.
 */
private fun cleanArabicText(text: String): String {
    return text
        .replace("\u06DF", "") // Remove small high rounded zero
        .replace("\u06E0", "") // Remove small high upright rectangular zero
        .trim()
}
