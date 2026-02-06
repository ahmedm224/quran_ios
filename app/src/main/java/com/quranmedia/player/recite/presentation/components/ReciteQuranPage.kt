package com.quranmedia.player.recite.presentation.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ReplacementSpan
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.res.ResourcesCompat
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import androidx.compose.foundation.Image
import com.quranmedia.player.R
import com.quranmedia.player.domain.model.Ayah
import com.quranmedia.player.recite.domain.model.HighlightType
import com.quranmedia.player.recite.domain.model.WordHighlightState
import com.quranmedia.player.presentation.theme.ReadingThemeColors
import com.quranmedia.player.presentation.theme.ReadingThemes
import com.quranmedia.player.data.repository.ReadingTheme
import com.quranmedia.player.presentation.screens.reader.components.kfgqpcHafsFont
import timber.log.Timber

/**
 * Quran page component for Recite feature with word-level highlighting support.
 *
 * Based on QuranPageComposable from the reader, with:
 * - Word-level background highlighting for current/correct/error words
 * - Click detection on error words to show details
 * - Safe area handling for camera punchhole and navigation bar
 * - Special centered layout for pages 1-2 (Al-Fatiha and Al-Baqarah start)
 */
@Composable
fun ReciteQuranPage(
    pageNumber: Int,
    ayahs: List<Ayah>,
    wordHighlights: List<WordHighlightState>,
    selectedAyah: Ayah? = null,  // For long-press visual feedback
    isReciting: Boolean = false,  // Show recording indicator
    readingTheme: ReadingTheme = ReadingTheme.LIGHT,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
    onWordClick: ((WordHighlightState) -> Unit)? = null,
    onAyahLongPress: ((Ayah) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val themeColors = ReadingThemes.getTheme(readingTheme)
    val typeface = remember { ResourcesCompat.getFont(context, R.font.kfgqpc_hafs_uthmanic) }

    // Check if this is a centered page (pages 1 and 2 are special)
    val isCenteredPage = pageNumber <= 2

    // Safe area insets for camera punchhole and navigation bar
    val initialDisplayCutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val initialSafeBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val topInset = remember { maxOf(initialDisplayCutoutTop, 32.dp) }
    val bottomInset = remember { maxOf(initialSafeBottom, 24.dp) }

    val pageHeaderHeight = 20.dp

    // Group ayahs by surah
    val ayahsBySurah = remember(ayahs) { ayahs.groupBy { it.surahNumber }.toList() }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .clipToBounds()
                .background(themeColors.background)
        ) {
            val screenHeight = maxHeight
            val screenWidth = maxWidth

            val topPadding = topInset + 8.dp
            val bottomPadding = bottomInset + 8.dp
            val horizontalPadding = 12.dp
            val availableHeight = screenHeight - topPadding - bottomPadding - pageHeaderHeight
            val availableWidth = screenWidth - (horizontalPadding * 2)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = topPadding,
                        bottom = bottomPadding,
                        start = horizontalPadding,
                        end = horizontalPadding
                    )
            ) {
                // Page info header at the very top
                if (ayahs.isNotEmpty()) {
                    RecitePageInfoHeader(
                        ayahs = ayahs,
                        pageNumber = pageNumber,
                        themeColors = themeColors,
                        modifier = Modifier.height(pageHeaderHeight)
                    )
                }

                // Main content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (ayahs.isEmpty()) {
                        // Empty page placeholder
                        Text(
                            text = "جاري تحميل الصفحة...",
                            fontFamily = kfgqpcHafsFont,
                            fontSize = 18.sp,
                            color = themeColors.textSecondary
                        )
                    } else if (isCenteredPage) {
                        // Special centered layout for pages 1-2
                        ReciteCenteredContent(
                            ayahs = ayahs,
                            wordHighlights = wordHighlights,
                            selectedAyah = selectedAyah,
                            isReciting = isReciting,
                            availableHeight = availableHeight,
                            themeColors = themeColors,
                            typeface = typeface,
                            onTap = onTap,
                            onWordClick = onWordClick,
                            onAyahLongPress = onAyahLongPress
                        )
                    } else {
                        // Standard Mushaf layout for pages 3+
                        ReciteMushafContent(
                            pageNumber = pageNumber,
                            ayahs = ayahs,
                            ayahsBySurah = ayahsBySurah,
                            wordHighlights = wordHighlights,
                            selectedAyah = selectedAyah,
                            isReciting = isReciting,
                            availableHeight = availableHeight,
                            availableWidth = availableWidth,
                            themeColors = themeColors,
                            typeface = typeface,
                            onTap = onTap,
                            onWordClick = onWordClick,
                            onAyahLongPress = onAyahLongPress
                        )
                    }
                }
            }
        }
    }
}

/**
 * Page info header showing Surah name, Juz and Hizb
 */
@Composable
private fun RecitePageInfoHeader(
    ayahs: List<Ayah>,
    pageNumber: Int,
    themeColors: ReadingThemeColors,
    modifier: Modifier = Modifier
) {
    if (ayahs.isEmpty()) return

    val firstAyah = ayahs.first()
    val surahName = surahNamesArabic[firstAyah.surahNumber] ?: ""

    // Calculate Hizb from hizbQuarter (60 hizb, each with 4 quarters)
    val hizb = (firstAyah.hizbQuarter + 3) / 4
    val juz = firstAyah.juz

    val juzText = "الجزء ${convertToArabicNumerals(juz)}"
    val hizbText = "الحزب ${convertToArabicNumerals(hizb)}"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Right side in RTL (Surah name)
        Text(
            text = surahName,
            fontSize = 10.sp,
            color = themeColors.textSecondary,
            fontFamily = kfgqpcHafsFont,
            maxLines = 1
        )

        // Left side in RTL (Juz and Hizb)
        Text(
            text = "$juzText - $hizbText",
            fontSize = 10.sp,
            color = themeColors.textSecondary,
            maxLines = 1
        )
    }
}

/**
 * Centered content for pages 1 and 2 (Al-Fatiha and Al-Baqarah start)
 */
@Composable
private fun ReciteCenteredContent(
    ayahs: List<Ayah>,
    wordHighlights: List<WordHighlightState>,
    selectedAyah: Ayah?,
    isReciting: Boolean,
    availableHeight: androidx.compose.ui.unit.Dp,
    themeColors: ReadingThemeColors,
    typeface: Typeface?,
    onTap: (() -> Unit)?,
    onWordClick: ((WordHighlightState) -> Unit)?,
    onAyahLongPress: ((Ayah) -> Unit)?
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val totalAyahs = ayahs.size
    val hasSurahHeader = ayahs.any { it.ayahNumber == 1 }
    val headerUnits = if (hasSurahHeader) 4f else 0f
    val ayahUnits = totalAyahs.toFloat()
    val totalUnits = ayahUnits + headerUnits

    val baseFontSize = with(density) {
        val availableHeightPx = availableHeight.toPx()
        val unitHeight = availableHeightPx / totalUnits
        val fontSize = unitHeight / 2.5f
        fontSize.coerceIn(16f, 24f).sp
    }
    val lineHeight = baseFontSize * 2.2f

    val ayahsBySurah = ayahs.groupBy { it.surahNumber }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = availableHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTap?.invoke() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        ayahsBySurah.forEach { (surahNumber, surahAyahs) ->
            val startsOnThisPage = surahAyahs.any { it.ayahNumber == 1 }

            if (startsOnThisPage) {
                ReciteCompactSurahHeader(
                    surahNumber = surahNumber,
                    fontSize = baseFontSize,
                    themeColors = themeColors,
                    onTap = onTap
                )
            }

            surahAyahs.forEach { ayah ->
                // Check if this ayah is selected for recitation
                val isSelected = selectedAyah?.let {
                    it.surahNumber == ayah.surahNumber && it.ayahNumber == ayah.ayahNumber
                } ?: false

                // Build text with word highlights for this ayah
                val builder = buildSingleAyahWithHighlights(
                    ayah = ayah,
                    wordHighlights = wordHighlights,
                    themeColors = themeColors
                )

                // Build word position map for click detection
                val wordPositions = buildSingleAyahWordPositions(ayah, wordHighlights)

                // Capture ayah reference for long-press
                val currentAyah = ayah

                // Background modifier for selected ayah
                val bgModifier = if (isSelected && isReciting) {
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFF4CAF50).copy(alpha = 0.15f),  // Green tint for recording
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                } else if (isSelected) {
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFF2196F3).copy(alpha = 0.15f),  // Blue tint for selected
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                } else {
                    Modifier.fillMaxWidth()
                }

                AndroidView(
                    factory = { ctx ->
                        TextView(ctx).apply {
                            gravity = Gravity.CENTER
                            this.typeface = typeface
                            includeFontPadding = false
                        }
                    },
                    update = { textView ->
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseFontSize.value)
                        textView.text = builder
                        textView.setTextColor(themeColors.textPrimary.toArgb())
                        textView.setLineSpacing(0f, 1.4f)

                        // Handle clicks
                        textView.setOnClickListener { view ->
                            val tv = view as TextView
                            val layout = tv.layout
                            val touchPos = view.tag as? Pair<Float, Float>

                            if (layout != null && touchPos != null && onWordClick != null) {
                                val x = touchPos.first
                                val y = touchPos.second
                                val line = layout.getLineForVertical(y.toInt())
                                val offset = layout.getOffsetForHorizontal(line, x)

                                val clickedHighlight = wordPositions.find { wp ->
                                    offset in wp.startOffset until wp.endOffset
                                }?.highlight

                                if (clickedHighlight != null && clickedHighlight.type == HighlightType.ERROR) {
                                    onWordClick(clickedHighlight)
                                    return@setOnClickListener
                                }
                            }

                            onTap?.invoke()
                        }

                        // Handle long-press to start recitation from this ayah
                        textView.setOnLongClickListener {
                            onAyahLongPress?.invoke(currentAyah)
                            true
                        }

                        textView.setOnTouchListener { view, event ->
                            view.tag = Pair(event.x, event.y)
                            false
                        }
                    },
                    modifier = bgModifier
                )
            }
        }
    }
}

/**
 * Standard Mushaf content for pages 3+
 */
@Composable
private fun ReciteMushafContent(
    pageNumber: Int,
    ayahs: List<Ayah>,
    ayahsBySurah: List<Pair<Int, List<Ayah>>>,
    wordHighlights: List<WordHighlightState>,
    selectedAyah: Ayah?,
    isReciting: Boolean,
    availableHeight: androidx.compose.ui.unit.Dp,
    availableWidth: androidx.compose.ui.unit.Dp,
    themeColors: ReadingThemeColors,
    typeface: Typeface?,
    onTap: (() -> Unit)?,
    onWordClick: ((WordHighlightState) -> Unit)?,
    onAyahLongPress: ((Ayah) -> Unit)?
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Build text with word highlights for each surah block
    val surahBlocks = remember(ayahsBySurah, wordHighlights) {
        ayahsBySurah.map { (surahNum, surahAyahs) ->
            val hasHeader = surahAyahs.any { it.ayahNumber == 1 }
            val builder = buildAyahTextWithHighlights(
                surahAyahs = surahAyahs,
                wordHighlights = wordHighlights,
                themeColors = themeColors
            )
            SurahBlockData(builder, surahNum, hasHeader, surahAyahs)
        }
    }

    // Calculate container dimensions with aspect ratio
    val mushafAspectRatio = 0.65f
    val screenWidthPx = with(density) { availableWidth.toPx() }
    val screenHeightPx = with(density) { availableHeight.toPx() }

    val (containerWidthPx, containerHeightPx) = remember(screenWidthPx, screenHeightPx) {
        val widthFromHeight = screenHeightPx * mushafAspectRatio
        if (widthFromHeight <= screenWidthPx) {
            Pair(widthFromHeight.toInt(), screenHeightPx.toInt())
        } else {
            Pair(screenWidthPx.toInt(), (screenWidthPx / mushafAspectRatio).toInt())
        }
    }

    val containerWidthDp = with(density) { containerWidthPx.toDp() }
    val containerHeightDp = with(density) { containerHeightPx.toDp() }

    // Calculate optimal font size using binary search
    val textSettings = remember(surahBlocks, containerWidthPx, containerHeightPx) {
        findOptimalTextSettings(
            blocks = surahBlocks.map { SurahTextBlock(it.text, it.hasHeader, it.surahNumber) },
            typeface = typeface,
            targetWidth = containerWidthPx,
            targetHeight = containerHeightPx,
            density = density
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTap?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(containerWidthDp)
                .height(containerHeightDp)
        ) {
            surahBlocks.forEachIndexed { index, block ->
                // Surah header
                if (block.hasHeader) {
                    ReciteCompactSurahHeader(
                        surahNumber = block.surahNumber,
                        fontSize = (textSettings.fontSizePx / density.density).sp,
                        themeColors = themeColors,
                        onTap = onTap
                    )
                }

                // Build word position map for click detection
                val wordPositions = remember(block.ayahs, wordHighlights) {
                    buildWordPositionMap(block.ayahs, wordHighlights)
                }

                // Build ayah position map for long-press detection
                val ayahPositions = remember(block.ayahs) {
                    buildAyahPositionMap(block.ayahs)
                }

                // Render text with word highlighting
                AndroidView(
                    factory = { ctx ->
                        TextView(ctx).apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                justificationMode = Layout.JUSTIFICATION_MODE_INTER_WORD
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
                                breakStrategy = Layout.BREAK_STRATEGY_HIGH_QUALITY
                            }
                            gravity = Gravity.START or Gravity.TOP
                            textDirection = android.view.View.TEXT_DIRECTION_RTL
                            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
                            includeFontPadding = false
                        }
                    },
                    update = { textView ->
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSettings.fontSizePx)
                        textView.text = block.text
                        textView.typeface = typeface
                        textView.setTextColor(themeColors.textPrimary.toArgb())
                        textView.setLineSpacing(0f, textSettings.lineSpacingMultiplier)

                        // Handle clicks - toggle controls or show error details
                        textView.setOnClickListener { view ->
                            val tv = view as TextView
                            val layout = tv.layout
                            val touchPos = view.tag as? Pair<Float, Float>

                            if (layout != null && touchPos != null && onWordClick != null) {
                                val x = touchPos.first
                                val y = touchPos.second
                                val line = layout.getLineForVertical(y.toInt())
                                val offset = layout.getOffsetForHorizontal(line, x)

                                // Find which word was clicked
                                val clickedHighlight = wordPositions.find { wp ->
                                    offset in wp.startOffset until wp.endOffset
                                }?.highlight

                                if (clickedHighlight != null && clickedHighlight.type == HighlightType.ERROR) {
                                    onWordClick(clickedHighlight)
                                    return@setOnClickListener
                                }
                            }

                            // No error word clicked - trigger general tap (toggle controls)
                            onTap?.invoke()
                        }

                        // Handle long-press to start recitation from selected ayah
                        textView.setOnLongClickListener { view ->
                            val tv = view as TextView
                            val layout = tv.layout
                            val touchPos = view.tag as? Pair<Float, Float>

                            if (layout != null && touchPos != null && onAyahLongPress != null) {
                                val x = touchPos.first
                                val y = touchPos.second
                                val line = layout.getLineForVertical(y.toInt())
                                val offset = layout.getOffsetForHorizontal(line, x)

                                // Find which ayah was long-pressed
                                val pressedAyah = ayahPositions.find { ap ->
                                    offset in ap.startOffset until ap.endOffset
                                }?.ayah

                                if (pressedAyah != null) {
                                    onAyahLongPress(pressedAyah)
                                    return@setOnLongClickListener true
                                }
                            }
                            false
                        }

                        textView.setOnTouchListener { view, event ->
                            view.tag = Pair(event.x, event.y)
                            false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                )
            }
        }
    }
}

/**
 * Compact surah header with SVG ornate frame
 */
@Composable
private fun ReciteCompactSurahHeader(
    surahNumber: Int,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    themeColors: ReadingThemeColors,
    onTap: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val surahName = surahNamesArabic[surahNumber] ?: "$surahNumber"
    val textColor = themeColors.ayahMarker

    val surahNameFontSize = (fontSize.value * 0.9f).coerceIn(14f, 24f).sp
    val bismillahFontSize = (fontSize.value * 0.85f).coerceIn(16f, 26f).sp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTap?.invoke() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Surah Header with SVG ornate frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.Center
        ) {
            // Create ImageLoader with SVG decoder support
            val imageLoader = remember(context) {
                ImageLoader.Builder(context)
                    .components {
                        add(SvgDecoder.Factory())
                    }
                    .build()
            }

            // Load SVG from assets
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/surah_header_green.svg")
                    .build(),
                imageLoader = imageLoader
            )

            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            )

            // Surah name text centered over the SVG frame
            Text(
                text = "سُورَةُ $surahName",
                fontFamily = kfgqpcHafsFont,
                fontSize = surahNameFontSize,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }

        // Bismillah (except for Surah 1 and 9)
        if (surahNumber != 9 && surahNumber != 1) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                fontFamily = kfgqpcHafsFont,
                fontSize = bismillahFontSize,
                color = themeColors.textPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helper functions

/**
 * Build spannable text with word-level highlighting for a single ayah (centered pages)
 */
private fun buildSingleAyahWithHighlights(
    ayah: Ayah,
    wordHighlights: List<WordHighlightState>,
    themeColors: ReadingThemeColors
): SpannableStringBuilder {
    val builder = SpannableStringBuilder()
    val primaryColor = themeColors.textPrimary.toArgb()
    val markerBgColor = themeColors.ayahMarker.copy(alpha = 0.15f).toArgb()
    val markerTextColor = themeColors.ayahMarker.toArgb()

    val ayahText = ayah.textArabic.stripKashida()
    val words = ayahText.split(Regex("\\s+"))
    var currentPos = 0

    words.forEachIndexed { wordIdx, word ->
        val wordStart = currentPos
        builder.append(word)
        val wordEnd = builder.length

        // Check if this word has a highlight
        val highlight = wordHighlights.find {
            it.surahNumber == ayah.surahNumber &&
                    it.ayahNumber == ayah.ayahNumber &&
                    it.wordIndex == wordIdx
        }

        if (highlight != null) {
            // Apply background highlight
            val bgColor = highlight.type.getBackgroundColor().toArgb()
            builder.setSpan(
                BackgroundColorSpan(bgColor),
                wordStart,
                wordEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Apply text color for errors
            if (highlight.type == HighlightType.ERROR) {
                builder.setSpan(
                    ForegroundColorSpan(highlight.type.getTextColor().toArgb()),
                    wordStart,
                    wordEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        // Add space after word (except last)
        if (wordIdx < words.size - 1) {
            builder.append(" ")
        }

        currentPos = builder.length
    }

    // Add ayah marker
    builder.append(" ")
    val markerStart = builder.length
    builder.append(convertToArabicNumerals(ayah.ayahNumber))
    builder.setSpan(
        CircleBackgroundSpan(markerBgColor, markerTextColor, 1.2f),
        markerStart,
        builder.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    return builder
}

/**
 * Build word positions for a single ayah (centered pages)
 */
private fun buildSingleAyahWordPositions(
    ayah: Ayah,
    wordHighlights: List<WordHighlightState>
): List<WordPosition> {
    val positions = mutableListOf<WordPosition>()
    val ayahText = ayah.textArabic.stripKashida()
    val words = ayahText.split(Regex("\\s+"))
    var currentOffset = 0

    words.forEachIndexed { wordIdx, word ->
        val highlight = wordHighlights.find {
            it.surahNumber == ayah.surahNumber &&
                    it.ayahNumber == ayah.ayahNumber &&
                    it.wordIndex == wordIdx
        }

        if (highlight != null) {
            positions.add(
                WordPosition(
                    startOffset = currentOffset,
                    endOffset = currentOffset + word.length,
                    highlight = highlight
                )
            )
        }

        currentOffset += word.length + 1  // +1 for space
    }

    return positions
}

/**
 * Build spannable text with word-level highlighting.
 */
private fun buildAyahTextWithHighlights(
    surahAyahs: List<Ayah>,
    wordHighlights: List<WordHighlightState>,
    themeColors: ReadingThemeColors
): SpannableStringBuilder {
    val builder = SpannableStringBuilder()
    val primaryColor = themeColors.textPrimary.toArgb()
    val markerBgColor = themeColors.ayahMarker.copy(alpha = 0.15f).toArgb()
    val markerTextColor = themeColors.ayahMarker.toArgb()

    surahAyahs.forEachIndexed { ayahIdx, ayah ->
        val ayahTextStart = builder.length
        val ayahText = ayah.textArabic.stripKashida()

        // Split text into words and track positions
        val words = ayahText.split(Regex("\\s+"))
        var currentPos = ayahTextStart

        words.forEachIndexed { wordIdx, word ->
            val wordStart = currentPos
            builder.append(word)
            val wordEnd = builder.length

            // Check if this word has a highlight
            val highlight = wordHighlights.find {
                it.surahNumber == ayah.surahNumber &&
                        it.ayahNumber == ayah.ayahNumber &&
                        it.wordIndex == wordIdx
            }

            if (highlight != null) {
                // Apply background highlight
                val bgColor = highlight.type.getBackgroundColor().toArgb()
                builder.setSpan(
                    BackgroundColorSpan(bgColor),
                    wordStart,
                    wordEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                // Apply text color for errors
                if (highlight.type == HighlightType.ERROR) {
                    builder.setSpan(
                        ForegroundColorSpan(highlight.type.getTextColor().toArgb()),
                        wordStart,
                        wordEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            // Add space after word (except last)
            if (wordIdx < words.size - 1) {
                builder.append(" ")
            }

            currentPos = builder.length
        }

        // Add ayah marker
        builder.append(" ")
        val markerStart = builder.length
        builder.append(convertToArabicNumerals(ayah.ayahNumber))
        builder.setSpan(
            CircleBackgroundSpan(markerBgColor, markerTextColor, 1.2f),
            markerStart,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Add space between ayahs
        if (ayahIdx < surahAyahs.size - 1) {
            builder.append(" ")
        }
    }

    return builder
}

/**
 * Build map of word positions for click detection.
 */
private fun buildWordPositionMap(
    ayahs: List<Ayah>,
    wordHighlights: List<WordHighlightState>
): List<WordPosition> {
    val positions = mutableListOf<WordPosition>()
    var currentOffset = 0

    ayahs.forEach { ayah ->
        val ayahText = ayah.textArabic.stripKashida()
        val words = ayahText.split(Regex("\\s+"))

        words.forEachIndexed { wordIdx, word ->
            val highlight = wordHighlights.find {
                it.surahNumber == ayah.surahNumber &&
                        it.ayahNumber == ayah.ayahNumber &&
                        it.wordIndex == wordIdx
            }

            if (highlight != null) {
                positions.add(
                    WordPosition(
                        startOffset = currentOffset,
                        endOffset = currentOffset + word.length,
                        highlight = highlight
                    )
                )
            }

            currentOffset += word.length + 1  // +1 for space
        }

        // Account for ayah marker (space + digits + space)
        val markerLength = 1 + convertToArabicNumerals(ayah.ayahNumber).length + 1
        currentOffset += markerLength
    }

    return positions
}

/**
 * Build map of ayah positions for long-press detection.
 */
private fun buildAyahPositionMap(ayahs: List<Ayah>): List<AyahPosition> {
    val positions = mutableListOf<AyahPosition>()
    var currentOffset = 0

    ayahs.forEach { ayah ->
        val startOffset = currentOffset
        val ayahText = ayah.textArabic.stripKashida()
        val words = ayahText.split(Regex("\\s+"))

        // Calculate length: all words + spaces between them
        words.forEachIndexed { wordIdx, word ->
            currentOffset += word.length
            if (wordIdx < words.size - 1) {
                currentOffset += 1 // space
            }
        }

        // Add ayah marker length (space + digits + space)
        currentOffset += 1 // space before marker
        currentOffset += convertToArabicNumerals(ayah.ayahNumber).length
        currentOffset += 1 // space after marker (between ayahs)

        positions.add(
            AyahPosition(
                startOffset = startOffset,
                endOffset = currentOffset,
                ayah = ayah
            )
        )
    }

    return positions
}

// Data classes

private data class SurahBlockData(
    val text: SpannableStringBuilder,
    val surahNumber: Int,
    val hasHeader: Boolean,
    val ayahs: List<Ayah>
)

private data class SurahTextBlock(
    val text: CharSequence,
    val hasHeader: Boolean,
    val surahNumber: Int
)

private data class WordPosition(
    val startOffset: Int,
    val endOffset: Int,
    val highlight: WordHighlightState
)

private data class AyahPosition(
    val startOffset: Int,
    val endOffset: Int,
    val ayah: Ayah
)

private data class OptimalTextSettings(
    val fontSizePx: Float,
    val lineSpacingMultiplier: Float
)

// Text measurement and optimal font size calculation

private fun buildStaticLayout(
    text: CharSequence,
    paint: TextPaint,
    targetWidth: Int,
    lineSpacingMultiplier: Float
): StaticLayout {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, targetWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, lineSpacingMultiplier)
            .setIncludePad(false)
            .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .setTextDirection(TextDirectionHeuristics.RTL)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
        }

        builder.build()
    } else {
        @Suppress("DEPRECATION")
        StaticLayout(text, paint, targetWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0f, false)
    }
}

private fun estimateHeaderHeightPx(fontSizePx: Float, surahNumber: Int, density: Density): Int {
    val verticalPadding = with(density) { 8.dp.toPx() }
    val bannerHeight = with(density) { 44.dp.toPx() }

    var height = verticalPadding + bannerHeight

    // Add Bismillah height if present
    if (surahNumber != 1 && surahNumber != 9) {
        val spacer = with(density) { 4.dp.toPx() }
        val bismillahHeight = fontSizePx * 1.6f
        height += spacer + bismillahHeight
    }
    return height.toInt()
}

private fun measureTotalContent(
    blocks: List<SurahTextBlock>,
    paint: TextPaint,
    targetWidth: Int,
    lineSpacingMultiplier: Float,
    density: Density
): TextBlockMetrics {
    var totalHeight = 0
    var totalLines = 0

    blocks.forEach { block ->
        if (block.text.isNotEmpty()) {
            val layout = buildStaticLayout(block.text, paint, targetWidth, lineSpacingMultiplier)
            totalHeight += layout.height
            totalLines += layout.lineCount
        }

        if (block.hasHeader) {
            totalHeight += estimateHeaderHeightPx(paint.textSize, block.surahNumber, density)
        }
    }

    return TextBlockMetrics(totalHeight, totalLines)
}

private data class TextBlockMetrics(
    val totalHeightPx: Int,
    val totalLines: Int
)

private fun findLineSpacingMultiplier(
    blocks: List<SurahTextBlock>,
    paint: TextPaint,
    targetWidth: Int,
    targetHeight: Int,
    density: Density,
    minMultiplier: Float = 1.0f,
    maxMultiplier: Float = 1.5f
): Float {
    if (targetHeight <= 0) return minMultiplier
    var low = minMultiplier
    var high = maxMultiplier
    var best = minMultiplier

    while (high - low > 0.005f) {
        val mid = (low + high) / 2f
        val metrics = measureTotalContent(blocks, paint, targetWidth, mid, density)
        if (metrics.totalHeightPx <= targetHeight) {
            best = mid
            low = mid
        } else {
            high = mid
        }
    }
    return best
}

private fun findOptimalTextSettings(
    blocks: List<SurahTextBlock>,
    typeface: Typeface?,
    targetWidth: Int,
    targetHeight: Int,
    density: Density,
    minSizePx: Float = 20f,
    maxSizePx: Float = 200f,
    maxLineSpacing: Float = 1.5f
): OptimalTextSettings {
    val paint = TextPaint().apply {
        isAntiAlias = true
        this.typeface = typeface
    }

    var low = minSizePx
    var high = maxSizePx
    var bestSize = low

    // Safety buffer: Target 98% of height to prevent cropping
    val safeTargetHeight = (targetHeight * 0.98f).toInt()

    // Binary search for largest font size where content fits
    while (high - low > 0.5f) {
        val mid = (low + high) / 2f
        paint.textSize = mid

        val metrics = measureTotalContent(blocks, paint, targetWidth, 1.0f, density)

        if (metrics.totalHeightPx <= safeTargetHeight) {
            bestSize = mid
            low = mid
        } else {
            high = mid
        }
    }

    // Calculate line spacing to fill remaining height
    paint.textSize = bestSize
    val baseMetrics = measureTotalContent(blocks, paint, targetWidth, 1.0f, density)

    val lineSpacingMultiplier = if (baseMetrics.totalLines > 1 && baseMetrics.totalHeightPx > 0) {
        findLineSpacingMultiplier(
            blocks = blocks,
            paint = paint,
            targetWidth = targetWidth,
            targetHeight = safeTargetHeight,
            density = density,
            maxMultiplier = maxLineSpacing
        )
    } else {
        1.0f
    }

    return OptimalTextSettings(bestSize, lineSpacingMultiplier)
}

private fun String.stripKashida(): String = this.replace("\u0640", "")

private fun convertToArabicNumerals(number: Int): String {
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return number.toString().map { arabicDigits[it - '0'] }.joinToString("")
}

private val surahNamesArabic = mapOf(
    1 to "الفاتحة", 2 to "البقرة", 3 to "آل عمران", 4 to "النساء", 5 to "المائدة",
    6 to "الأنعام", 7 to "الأعراف", 8 to "الأنفال", 9 to "التوبة", 10 to "يونس",
    11 to "هود", 12 to "يوسف", 13 to "الرعد", 14 to "إبراهيم", 15 to "الحجر",
    16 to "النحل", 17 to "الإسراء", 18 to "الكهف", 19 to "مريم", 20 to "طه",
    21 to "الأنبياء", 22 to "الحج", 23 to "المؤمنون", 24 to "النور", 25 to "الفرقان",
    26 to "الشعراء", 27 to "النمل", 28 to "القصص", 29 to "العنكبوت", 30 to "الروم",
    31 to "لقمان", 32 to "السجدة", 33 to "الأحزاب", 34 to "سبأ", 35 to "فاطر",
    36 to "يس", 37 to "الصافات", 38 to "ص", 39 to "الزمر", 40 to "غافر",
    41 to "فصلت", 42 to "الشورى", 43 to "الزخرف", 44 to "الدخان", 45 to "الجاثية",
    46 to "الأحقاف", 47 to "محمد", 48 to "الفتح", 49 to "الحجرات", 50 to "ق",
    51 to "الذاريات", 52 to "الطور", 53 to "النجم", 54 to "القمر", 55 to "الرحمن",
    56 to "الواقعة", 57 to "الحديد", 58 to "المجادلة", 59 to "الحشر", 60 to "الممتحنة",
    61 to "الصف", 62 to "الجمعة", 63 to "المنافقون", 64 to "التغابن", 65 to "الطلاق",
    66 to "التحريم", 67 to "الملك", 68 to "القلم", 69 to "الحاقة", 70 to "المعارج",
    71 to "نوح", 72 to "الجن", 73 to "المزمل", 74 to "المدثر", 75 to "القيامة",
    76 to "الإنسان", 77 to "المرسلات", 78 to "النبأ", 79 to "النازعات", 80 to "عبس",
    81 to "التكوير", 82 to "الانفطار", 83 to "المطففين", 84 to "الانشقاق", 85 to "البروج",
    86 to "الطارق", 87 to "الأعلى", 88 to "الغاشية", 89 to "الفجر", 90 to "البلد",
    91 to "الشمس", 92 to "الليل", 93 to "الضحى", 94 to "الشرح", 95 to "التين",
    96 to "العلق", 97 to "القدر", 98 to "البينة", 99 to "الزلزلة", 100 to "العاديات",
    101 to "القارعة", 102 to "التكاثر", 103 to "العصر", 104 to "الهمزة", 105 to "الفيل",
    106 to "قريش", 107 to "الماعون", 108 to "الكوثر", 109 to "الكافرون", 110 to "النصر",
    111 to "المسد", 112 to "الإخلاص", 113 to "الفلق", 114 to "الناس"
)

/**
 * Circle background span for ayah markers.
 */
private class CircleBackgroundSpan(
    private val backgroundColor: Int,
    private val textColor: Int,
    private val sizeMultiplier: Float
) : ReplacementSpan() {
    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        val sizePx = paint.textSize * sizeMultiplier
        if (fm != null) {
            val half = (sizePx / 2f).toInt()
            fm.ascent = -half
            fm.descent = half
            fm.top = fm.ascent
            fm.bottom = fm.descent
        }
        return sizePx.toInt()
    }

    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        val sizePx = paint.textSize * sizeMultiplier
        val radius = sizePx / 2f
        val centerX = x + radius
        val centerY = (top + bottom) / 2f
        val originalColor = paint.color
        val originalStyle = paint.style
        val originalTextSize = paint.textSize
        val originalAlign = paint.textAlign
        val originalTypeface = paint.typeface

        paint.color = backgroundColor
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, radius, paint)

        paint.color = textColor
        paint.textSize = sizePx * 0.55f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT

        val fm = paint.fontMetrics
        val textY = centerY - (fm.ascent + fm.descent) / 2f
        canvas.drawText(text, start, end, centerX, textY, paint)

        paint.color = originalColor
        paint.style = originalStyle
        paint.textSize = originalTextSize
        paint.textAlign = originalAlign
        paint.typeface = originalTypeface
    }
}
