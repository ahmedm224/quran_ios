package com.quranmedia.player.presentation.screens.reader.components

import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextDirectionHeuristics
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import android.text.style.ForegroundColorSpan
import android.text.style.ReplacementSpan
import android.view.Gravity
import android.widget.TextView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.res.ResourcesCompat
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import androidx.compose.foundation.Image
import com.quranmedia.player.R
import com.quranmedia.player.data.repository.ReadingTheme
import com.quranmedia.player.domain.model.Ayah
import com.quranmedia.player.presentation.screens.reader.HighlightedAyah
import com.quranmedia.player.presentation.screens.reader.ZoomMode
import com.quranmedia.player.presentation.theme.ReadingThemeColors
import com.quranmedia.player.presentation.theme.ReadingThemes

// KFGQPC Hafs Uthmanic Script - Official King Fahd Complex font for accurate Quran rendering
val kfgqpcHafsFont = FontFamily(
    Font(R.font.kfgqpc_hafs_uthmanic, FontWeight.Normal),
    Font(R.font.kfgqpc_hafs_uthmanic, FontWeight.Bold)
)

val scheherazadeFont = kfgqpcHafsFont

// Islamic theme colors
val islamicGreen = Color(0xFF2E7D32)
val lightGreen = Color(0xFF66BB6A)
val darkGreen = Color(0xFF1B5E20)
val goldAccent = Color(0xFFD4AF37)
val creamBackground = Color(0xFFFFFBF0)

// Arabic surah names map (shared)
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

// Page info header showing Surah name, Juz and Hizb
@Composable
private fun PageInfoHeader(
    ayahs: List<Ayah>,
    pageNumber: Int,
    themeColors: ReadingThemeColors,
    modifier: Modifier = Modifier
) {
    if (ayahs.isEmpty()) return

    // Get surah info from first ayah on this page
    val firstAyah = ayahs.first()
    val surahName = surahNamesArabic[firstAyah.surahNumber] ?: ""

    // Calculate Hizb from hizbQuarter (60 hizb, each with 4 quarters)
    val hizb = (firstAyah.hizbQuarter + 3) / 4
    val juz = firstAyah.juz

    // Format in Arabic
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
            fontFamily = FontFamily.Default,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuranPageComposable(
    pageNumber: Int,
    ayahs: List<Ayah>,
    highlightedAyah: HighlightedAyah?,
    modifier: Modifier = Modifier,
    zoomMode: ZoomMode = ZoomMode.ZOOMED,
    splitHalf: Int = 0,  // 0 = first half (or full page), 1 = second half
    readingTheme: ReadingTheme = ReadingTheme.LIGHT,
    customBackgroundColor: Color? = null,
    customTextColor: Color? = null,
    customHeaderColor: Color? = null,
    onTap: (() -> Unit)? = null,
    onAyahLongPress: ((Ayah, Offset) -> Unit)? = null
) {
    val isCenteredPage = pageNumber <= 2

    // Get theme colors with custom colors if provided
    val themeColors = if (readingTheme == ReadingTheme.CUSTOM &&
        customBackgroundColor != null && customTextColor != null && customHeaderColor != null) {
        ReadingThemes.getTheme(
            readingTheme,
            com.quranmedia.player.presentation.theme.CustomThemeColors(
                backgroundColor = customBackgroundColor,
                textColor = customTextColor,
                headerColor = customHeaderColor
            )
        )
    } else {
        ReadingThemes.getTheme(readingTheme)
    }

    // Convert zoomMode to fitScreen for existing logic (FIT_SCREEN = fitScreen true)
    val fitScreen = zoomMode == ZoomMode.FIT_SCREEN

    // In SPLIT mode, filter ayahs to show only the requested half
    // Split by TEXT LENGTH (not ayah count) for more even visual distribution
    val displayAyahs = if (zoomMode == ZoomMode.SPLIT && ayahs.isNotEmpty()) {
        // Calculate total text length of all ayahs
        val totalTextLength = ayahs.sumOf { it.textArabic.length }
        val targetHalfLength = totalTextLength / 2

        // Find split point where cumulative length reaches ~50%
        var cumulativeLength = 0
        var splitIndex = 0
        for ((index, ayah) in ayahs.withIndex()) {
            val ayahLength = ayah.textArabic.length
            // If adding this ayah would exceed half, check if it's closer to include or exclude it
            if (cumulativeLength + ayahLength > targetHalfLength) {
                // If we're closer to target by including this ayah, include it
                val withoutThis = kotlin.math.abs(cumulativeLength - targetHalfLength)
                val withThis = kotlin.math.abs(cumulativeLength + ayahLength - targetHalfLength)
                splitIndex = if (withThis < withoutThis) index + 1 else index
                break
            }
            cumulativeLength += ayahLength
            splitIndex = index + 1
        }

        // Ensure at least one ayah in each half
        splitIndex = splitIndex.coerceIn(1, ayahs.size - 1)

        if (splitHalf == 0) {
            ayahs.take(splitIndex)  // First half
        } else {
            ayahs.drop(splitIndex)  // Second half
        }
    } else {
        ayahs
    }

    val initialDisplayCutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val initialSafeBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val topInset = remember { maxOf(initialDisplayCutoutTop, 32.dp) }
    val bottomInset = remember { maxOf(initialSafeBottom, 24.dp) }

    val pageHeaderHeight = 20.dp

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
                // Page info header at the very top (use original ayahs for page info)
                PageInfoHeader(
                    ayahs = ayahs,  // Use full ayahs list for accurate page info
                    pageNumber = pageNumber,
                    themeColors = themeColors,
                    modifier = Modifier.height(pageHeaderHeight)
                )

                // Main content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (displayAyahs.isEmpty()) {
                        EmptyPageContent(themeColors = themeColors)
                    } else {
                        if (isCenteredPage) {
                            FullScreenCenteredContent(
                                ayahs = displayAyahs,
                                highlightedAyah = highlightedAyah,
                                availableHeight = availableHeight,
                                themeColors = themeColors,
                                readingTheme = readingTheme,
                                onTap = onTap,
                                onAyahLongPress = onAyahLongPress
                            )
                        } else {
                            FullScreenMushafContent(
                                pageNumber = pageNumber,
                                ayahs = displayAyahs,
                                highlightedAyah = highlightedAyah,
                                availableHeight = availableHeight,
                                availableWidth = availableWidth,
                                themeColors = themeColors,
                                readingTheme = readingTheme,
                                fitScreen = fitScreen,
                                isSplitMode = zoomMode == ZoomMode.SPLIT,
                                onTap = onTap,
                                onAyahLongPress = onAyahLongPress
                            )
                        }
                    }
                }
            }
        }
    }
}

// ... FullScreenCenteredContent remains unchanged ...
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FullScreenCenteredContent(
    ayahs: List<Ayah>,
    highlightedAyah: HighlightedAyah?,
    availableHeight: androidx.compose.ui.unit.Dp,
    themeColors: ReadingThemeColors,
    readingTheme: ReadingTheme = ReadingTheme.LIGHT,
    onTap: (() -> Unit)?,
    onAyahLongPress: ((Ayah, Offset) -> Unit)?
) {
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
    val circleSize = 24.sp
    
    val ayahsBySurah = ayahs.groupBy { it.surahNumber }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = availableHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        ayahsBySurah.forEach { (surahNumber, surahAyahs) ->
            val startsOnThisPage = surahAyahs.any { it.ayahNumber == 1 }

            if (startsOnThisPage) {
                CompactSurahHeader(surahNumber = surahNumber, fontSize = baseFontSize, themeColors = themeColors, readingTheme = readingTheme)
            }

            surahAyahs.forEach { ayah ->
                val isHighlighted = highlightedAyah != null &&
                        highlightedAyah.surahNumber == ayah.surahNumber &&
                        highlightedAyah.ayahNumber == ayah.ayahNumber

                val textColor = if (isHighlighted) themeColors.highlight else themeColors.textPrimary
                val markerBgColor = if (isHighlighted) themeColors.highlight.copy(alpha = 0.2f) else themeColors.ayahMarker.copy(alpha = 0.15f)
                val markerTextColor = if (isHighlighted) themeColors.highlight else themeColors.ayahMarker

                var textPosition by remember { mutableStateOf(Offset.Zero) }

                // Use AndroidView for Tajweed theme to prevent ligature breaking
                if (readingTheme == ReadingTheme.TAJWEED) {
                    val context = LocalContext.current
                    AndroidView(
                        factory = { ctx ->
                            TextView(ctx).apply {
                                gravity = Gravity.CENTER
                                textSize = baseFontSize.value
                                typeface = ResourcesCompat.getFont(ctx, R.font.kfgqpc_hafs_uthmanic)
                            }
                        },
                        update = { textView ->
                            val builder = SpannableStringBuilder()
                            val ayahText = ayah.textArabic.stripKashida()
                            val textStart = 0

                            builder.append(ayahText)
                            builder.append(" ")

                            // Apply Tajweed colors using SpannableString (like pages 3+)
                            val annotations = com.quranmedia.player.domain.util.TajweedRuleEngine.analyzeText(ayahText)

                            if (annotations.isNotEmpty()) {
                                builder.setSpan(
                                    ForegroundColorSpan(textColor.toArgb()),
                                    textStart,
                                    ayahText.length,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )

                                annotations.forEach { annotation ->
                                    val annotStart = textStart + annotation.start
                                    val annotEnd = textStart + annotation.end

                                    if (annotStart >= textStart && annotEnd <= ayahText.length && annotStart < annotEnd) {
                                        val color = com.quranmedia.player.domain.util.TajweedColors.getColor(annotation.rule).toArgb()
                                        builder.setSpan(
                                            ForegroundColorSpan(color),
                                            annotStart,
                                            annotEnd,
                                            Spanned.SPAN_INCLUSIVE_INCLUSIVE
                                        )
                                    }
                                }
                            } else {
                                builder.setSpan(
                                    ForegroundColorSpan(textColor.toArgb()),
                                    textStart,
                                    ayahText.length,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            }

                            // Add ayah number marker
                            val markerStart = builder.length
                            builder.append(convertToArabicNumerals(ayah.ayahNumber))
                            builder.setSpan(
                                CircleBackgroundSpan(markerBgColor.toArgb(), markerTextColor.toArgb(), 1.2f),
                                markerStart,
                                builder.length,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            textView.text = builder
                            textView.setTextColor(textColor.toArgb())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                textPosition = coordinates.positionInRoot()
                            }
                            .pointerInput(ayah) {
                                detectTapGestures(
                                    onTap = { onTap?.invoke() },
                                    onLongPress = { localOffset ->
                                        val globalOffset = Offset(
                                            x = textPosition.x + localOffset.x,
                                            y = textPosition.y + localOffset.y
                                        )
                                        onAyahLongPress?.invoke(ayah, globalOffset)
                                    }
                                )
                            }
                    )
                } else {
                    // Non-Tajweed themes use Compose Text
                    val markerId = "ayah_${ayah.ayahNumber}"

                    val annotatedText = buildAnnotatedString {
                        withStyle(SpanStyle(color = textColor)) {
                            append(ayah.textArabic.stripKashida())
                        }
                        append(" ")
                        appendInlineContent(markerId, "[${ayah.ayahNumber}]")
                    }

                    val inlineContent = mapOf(
                        markerId to InlineTextContent(
                            placeholder = Placeholder(
                                width = circleSize,
                                height = circleSize,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(with(LocalDensity.current) { circleSize.toDp() })
                                    .background(color = markerBgColor, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = convertToArabicNumerals(ayah.ayahNumber),
                                    fontSize = 12.sp,
                                    color = markerTextColor,
                                    fontFamily = FontFamily.Default
                                )
                            }
                        }
                    )

                    Text(
                        text = annotatedText,
                        inlineContent = inlineContent,
                        fontFamily = kfgqpcHafsFont,
                        fontSize = baseFontSize,
                        lineHeight = lineHeight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                textPosition = coordinates.positionInRoot()
                            }
                            .pointerInput(ayah) {
                                detectTapGestures(
                                    onTap = { onTap?.invoke() },
                                    onLongPress = { localOffset ->
                                        val globalOffset = Offset(
                                            x = textPosition.x + localOffset.x,
                                            y = textPosition.y + localOffset.y
                                        )
                                        onAyahLongPress?.invoke(ayah, globalOffset)
                                    }
                                )
                            }
                    )
                }
            }
        }
    }
}

// Data class to associate text with its header requirements
private data class SurahBlockData(
    val text: CharSequence,
    val surahNumber: Int,
    val hasHeader: Boolean
)

private data class OptimalTextSettings(
    val fontSizePx: Float,
    val lineSpacingMultiplier: Float
)

private data class TextBlockMetrics(
    val totalHeightPx: Int,
    val totalLines: Int
)

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
            .setIncludePad(false) // Matches AndroidView includeFontPadding=false
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

// Helper to estimate header height precisely in pixels based on font size
private fun estimateHeaderHeightPx(fontSizePx: Float, surahNumber: Int, density: Density): Int {
    val verticalPadding = with(density) { 8.dp.toPx() } // 4dp top + 4dp bottom
    val bannerHeight = with(density) { 44.dp.toPx() }
    
    var height = verticalPadding + bannerHeight
    
    // Add Bismillah height if present
    if (surahNumber != 1 && surahNumber != 9) {
        val spacer = with(density) { 4.dp.toPx() }
        // Bismillah font scales with main text. 
        // Text height is approx fontSize * 1.6 (safe line height estimate for Arabic)
        val bismillahHeight = fontSizePx * 1.6f
        height += spacer + bismillahHeight
    }
    return height.toInt()
}

// Revised to calculate Total Height (Headers + Text) for every font size check
private fun measureTotalContent(
    blocks: List<SurahBlockData>,
    paint: TextPaint,
    targetWidth: Int,
    lineSpacingMultiplier: Float,
    density: Density
): TextBlockMetrics {
    var totalHeight = 0
    var totalLines = 0

    blocks.forEach { block ->
        // 1. Measure Text
        if (block.text.isNotEmpty()) {
            val layout = buildStaticLayout(block.text, paint, targetWidth, lineSpacingMultiplier)
            totalHeight += layout.height
            totalLines += layout.lineCount
        }
        
        // 2. Add Header Height if this block has one
        if (block.hasHeader) {
            totalHeight += estimateHeaderHeightPx(paint.textSize, block.surahNumber, density)
        }
    }

    return TextBlockMetrics(totalHeight, totalLines)
}

private fun findLineSpacingMultiplier(
    blocks: List<SurahBlockData>,
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
    blocks: List<SurahBlockData>, // Accepts enriched data
    typeface: Typeface?,
    targetWidth: Int,
    targetHeight: Int,
    density: Density,
    minSizePx: Float = 20f,
    maxSizePx: Float = 200f,
    maxLineSpacing: Float = 1.5f  // Max line spacing multiplier
): OptimalTextSettings {
    val paint = TextPaint().apply {
        isAntiAlias = true
        this.typeface = typeface
    }

    var low = minSizePx
    var high = maxSizePx
    var bestSize = low

    // Safety buffer: Target 98% of height to prevent cropping from rounding errors
    val safeTargetHeight = (targetHeight * 0.98f).toInt()

    // Binary search for largest font size where (Text + Headers) fits
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

    // Calculate line spacing to fill the remaining height
    paint.textSize = bestSize
    val baseMetrics = measureTotalContent(blocks, paint, targetWidth, 1.0f, density)

    // We target the Safe Height for spacing too, to ensure we don't accidentally push
    // the last line into the 2% buffer zone.
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

// Logic for Zoom Mode (Target Lines) - also accounting for headers
private fun findSettingsForTargetLines(
    blocks: List<SurahBlockData>,
    typeface: Typeface?,
    targetWidth: Int,
    targetHeight: Int,
    density: Density,
    targetLines: Int = 15,
    minSizePx: Float = 30f,
    maxSizePx: Float = 150f
): OptimalTextSettings {
    val paint = TextPaint().apply {
        isAntiAlias = true
        this.typeface = typeface
    }

    var low = minSizePx
    var high = maxSizePx
    var bestSize = minSizePx
    val safeTargetHeight = (targetHeight * 0.98f).toInt()

    while (high - low > 0.5f) {
        val mid = (low + high) / 2f
        paint.textSize = mid

        val metrics = measureTotalContent(blocks, paint, targetWidth, 1.0f, density)
        val fitsLines = metrics.totalLines <= targetLines
        val fitsHeight = metrics.totalHeightPx <= safeTargetHeight
        
        if (fitsLines && fitsHeight) {
            bestSize = mid
            low = mid
        } else {
            high = mid
        }
    }

    paint.textSize = bestSize
    val baseMetrics = measureTotalContent(blocks, paint, targetWidth, 1.0f, density)
    
    val lineSpacingMultiplier = if (baseMetrics.totalLines > 1 && baseMetrics.totalHeightPx > 0) {
        findLineSpacingMultiplier(
            blocks = blocks,
            paint = paint,
            targetWidth = targetWidth,
            targetHeight = safeTargetHeight,
            density = density
        )
    } else {
        1.0f
    }

    return OptimalTextSettings(bestSize, lineSpacingMultiplier)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FullScreenMushafContent(
    pageNumber: Int,
    ayahs: List<Ayah>,
    highlightedAyah: HighlightedAyah?,
    availableHeight: androidx.compose.ui.unit.Dp,
    availableWidth: androidx.compose.ui.unit.Dp,
    themeColors: ReadingThemeColors,
    readingTheme: ReadingTheme = ReadingTheme.LIGHT,
    fitScreen: Boolean = true,
    isSplitMode: Boolean = false,
    onTap: (() -> Unit)?,
    onAyahLongPress: ((Ayah, Offset) -> Unit)?
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // In split mode, use full screen (no aspect ratio constraint)
    val mushafAspectRatio = when {
        isSplitMode -> 1.0f   // Full screen for split mode - no constraint
        fitScreen -> 0.65f    // Normal fit screen
        else -> 0.5f          // Zoomed mode
    }
    val typeface = remember { ResourcesCompat.getFont(context, R.font.kfgqpc_hafs_uthmanic) }

    // Colors preparation...
    val primaryColorInt = themeColors.textPrimary.toArgb()
    val markerBgColorInt = themeColors.ayahMarker.copy(alpha = 0.15f).toArgb()
    val markerTextColorInt = themeColors.ayahMarker.toArgb()
    val highlightColorInt = themeColors.highlight.toArgb()
    val highlightBgColorInt = themeColors.highlight.copy(alpha = 0.2f).toArgb()
    val highlightTextColorInt = themeColors.highlight.toArgb()

    val ayahsBySurah = remember(ayahs) { ayahs.groupBy { it.surahNumber }.toList() }

    // Prepare rich data blocks for calculation
    val surahBlocks = remember(ayahsBySurah, highlightedAyah) {
        ayahsBySurah.map { (surahNum, surahAyahs) ->
            val hasHeader = surahAyahs.any { it.ayahNumber == 1 }
            
            // Build the text
            val builder = SpannableStringBuilder()
            surahAyahs.forEachIndexed { idx, ayah ->
                val isHighlighted = highlightedAyah != null &&
                    highlightedAyah.surahNumber == ayah.surahNumber &&
                    highlightedAyah.ayahNumber == ayah.ayahNumber

                val textColor = if (isHighlighted) highlightColorInt else primaryColorInt
                val circleBgColor = if (isHighlighted) highlightBgColorInt else markerBgColorInt
                val circleTextColor = if (isHighlighted) highlightTextColorInt else markerTextColorInt

                val textStart = builder.length
                // Strip Kashida for cleaner display
                val ayahText = ayah.textArabic.stripKashida()
                builder.append(ayahText)

                // Apply Tajweed colors if Tajweed theme is active
                if (readingTheme == ReadingTheme.TAJWEED) {
                    // Generate annotations using Tajweed Rule Engine
                    val annotations = com.quranmedia.player.domain.util.TajweedRuleEngine.analyzeText(ayahText)

                    // Apply colors directly to the text in builder using annotations
                    // This avoids creating multiple span segments that can displace tashkeel
                    if (annotations.isNotEmpty()) {
                        // First, apply base color to entire ayah text
                        builder.setSpan(
                            ForegroundColorSpan(textColor),
                            textStart,
                            builder.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        // Then overlay Tajweed colors for specific positions
                        annotations.forEach { annotation ->
                            val annotStart = textStart + annotation.start
                            val annotEnd = textStart + annotation.end

                            // Ensure positions are within bounds
                            if (annotStart >= textStart && annotEnd <= builder.length && annotStart < annotEnd) {
                                val color = com.quranmedia.player.domain.util.TajweedColors.getColor(annotation.rule).toArgb()
                                builder.setSpan(
                                    ForegroundColorSpan(color),
                                    annotStart,
                                    annotEnd,
                                    Spanned.SPAN_INCLUSIVE_INCLUSIVE
                                )
                            }
                        }
                    } else {
                        // No annotations - use base color
                        builder.setSpan(ForegroundColorSpan(textColor), textStart, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                } else {
                    // Single color for other themes
                    builder.setSpan(ForegroundColorSpan(textColor), textStart, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                builder.append(" ")
                val markerStart = builder.length
                builder.append(convertToArabicNumerals(ayah.ayahNumber))
                builder.setSpan(
                    CircleBackgroundSpan(circleBgColor, circleTextColor, 1.2f),
                    markerStart,
                    builder.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (idx < surahAyahs.size - 1) builder.append(" ")
            }
            
            SurahBlockData(builder, surahNum, hasHeader)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap?.invoke() }) },
        contentAlignment = Alignment.Center
    ) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        val (containerWidthPx, containerHeightPx) = remember(screenWidthPx, screenHeightPx, mushafAspectRatio, isSplitMode) {
            if (isSplitMode) {
                // SPLIT mode: Use full screen width and height for maximum text size
                Pair(screenWidthPx.toInt(), screenHeightPx.toInt())
            } else {
                // Normal modes: Apply aspect ratio constraint
                val widthFromHeight = screenHeightPx * mushafAspectRatio
                if (widthFromHeight <= screenWidthPx) {
                    Pair(widthFromHeight.toInt(), screenHeightPx.toInt())
                } else {
                    Pair(screenWidthPx.toInt(), (screenWidthPx / mushafAspectRatio).toInt())
                }
            }
        }

        val containerWidthDp = with(density) { containerWidthPx.toDp() }
        val containerHeightDp = with(density) { containerHeightPx.toDp() }

        // REMOVED Phase 1 Header Measurement. We pass full constraints to calculator.
        
        // Calculate settings using TOTAL available height
        val textSettings = remember(surahBlocks, containerWidthPx, containerHeightPx, fitScreen, isSplitMode) {
            when {
                isSplitMode -> {
                    // Split mode: Show half the content with large text filling the full screen
                    // Use very large font and high line spacing to fill the screen
                    findOptimalTextSettings(
                        blocks = surahBlocks,
                        typeface = typeface,
                        targetWidth = containerWidthPx,
                        targetHeight = containerHeightPx,
                        density = density,
                        minSizePx = 80f,
                        maxSizePx = 200f,
                        maxLineSpacing = 3.0f  // High line spacing to spread text across full screen
                    )
                }
                fitScreen -> {
                    findOptimalTextSettings(
                        blocks = surahBlocks,
                        typeface = typeface,
                        targetWidth = containerWidthPx,
                        targetHeight = containerHeightPx, // Pass FULL height
                        density = density,
                        minSizePx = 48f,
                        maxSizePx = 62f
                    )
                }
                else -> {
                    // Zoom logic (ZOOMED mode)
                    val headerCount = surahBlocks.count { it.hasHeader }
                    val hasHeaderInMiddle = headerCount == 1 && !surahBlocks.first().hasHeader

                    val zoomTargetLines = when {
                        headerCount >= 3 -> 9
                        headerCount == 2 -> 11
                        hasHeaderInMiddle -> 13
                        else -> 15
                    }
                    findSettingsForTargetLines(
                        blocks = surahBlocks,
                        typeface = typeface,
                        targetWidth = containerWidthPx,
                        targetHeight = containerHeightPx,
                        density = density,
                        targetLines = zoomTargetLines,
                        minSizePx = 50f,
                        maxSizePx = 65f
                    )
                }
            }
        }

        // Render Content
        Column(
            modifier = Modifier
                .width(containerWidthDp)
                .height(containerHeightDp)
        ) {
            surahBlocks.forEachIndexed { index, block ->
                // 1. Dynamic Header
                if (block.hasHeader) {
                    CompactSurahHeader(
                        surahNumber = block.surahNumber,
                        fontSize = (textSettings.fontSizePx / density.density).sp,
                        themeColors = themeColors,
                        readingTheme = readingTheme
                    )
                }

                // 2. TextView
                val ayahPositions = remember(ayahsBySurah[index]) {
                    // ... ayah position logic unchanged ...
                    val surahAyahs = ayahsBySurah[index].second
                    val positions = mutableListOf<Triple<Int, Int, Ayah>>()
                    var currentPos = 0
                    surahAyahs.forEachIndexed { idx, ayah ->
                        val start = currentPos
                        currentPos += ayah.textArabic.stripKashida().length
                        currentPos += 1 + convertToArabicNumerals(ayah.ayahNumber).length
                        if (idx < surahAyahs.size - 1) currentPos += 1
                        positions.add(Triple(start, currentPos, ayah))
                    }
                    positions
                }

                AndroidView(
                    factory = { ctx ->
                        TextView(ctx).apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                justificationMode = android.text.Layout.JUSTIFICATION_MODE_INTER_WORD
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                hyphenationFrequency = android.text.Layout.HYPHENATION_FREQUENCY_NONE
                                breakStrategy = android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY
                            }
                            gravity = Gravity.START or Gravity.TOP
                            textDirection = android.view.View.TEXT_DIRECTION_RTL
                            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
                            includeFontPadding = false
                        }
                    },
                    update = { textView ->
                        // CRITICAL FIX: Use Pixel units directly to match StaticLayout
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSettings.fontSizePx)
                        
                        textView.text = block.text
                        textView.typeface = typeface
                        textView.setTextColor(primaryColorInt)
                        textView.setLineSpacing(0f, textSettings.lineSpacingMultiplier)

                        // ... Listeners unchanged ...
                        textView.setOnLongClickListener { view ->
                            val touchPos = view.tag as? Pair<Float, Float>
                            if (touchPos != null && onAyahLongPress != null) {
                                val tv = view as TextView
                                val layout = tv.layout
                                if (layout != null) {
                                    val x = touchPos.first
                                    val y = touchPos.second
                                    val line = layout.getLineForVertical(y.toInt())
                                    val offset = layout.getOffsetForHorizontal(line, x)

                                    val ayah = ayahPositions.find { (start, end, _) ->
                                        offset in start until end
                                    }?.third

                                    if (ayah != null) {
                                        val location = IntArray(2)
                                        tv.getLocationOnScreen(location)
                                        val screenOffset = Offset(x = location[0] + x, y = location[1] + y)
                                        onAyahLongPress.invoke(ayah, screenOffset)
                                    }
                                }
                            }
                            true
                        }
                        textView.setOnTouchListener { view, event ->
                            view.tag = Pair(event.x, event.y)
                            false
                        }
                        textView.setOnClickListener { onTap?.invoke() }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                )
            }
        }
    }
}


@Composable
private fun CompactSurahHeader(
    surahNumber: Int,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    themeColors: ReadingThemeColors,
    readingTheme: ReadingTheme = ReadingTheme.LIGHT
) {
    val context = LocalContext.current

    // Select SVG based on theme
    val svgFileName = when (readingTheme) {
        ReadingTheme.NIGHT -> "surah_header_dark.svg"
        ReadingTheme.SEPIA -> "surah_header_brown.svg"
        ReadingTheme.OCEAN -> "surah_header_blue.svg"
        ReadingTheme.CUSTOM -> "surah_header_green.svg" // Use green for custom theme
        ReadingTheme.TAJWEED -> "surah_header_green.svg" // Use green for tajweed theme
        else -> "surah_header_green.svg" // LIGHT, PAPER
    }

    val surahName = surahNamesArabic[surahNumber] ?: "$surahNumber"
    // Use ayahMarker color for surah name to match custom header color
    val textColor = themeColors.ayahMarker

    val surahNameFontSize = (fontSize.value * 0.9f).coerceIn(14f, 24f).sp
    val bismillahFontSize = (fontSize.value * 0.85f).coerceIn(16f, 26f).sp

    Column(
        modifier = Modifier
            .fillMaxWidth()
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

            // Load SVG from assets using file URI
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/$svgFileName")
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

private fun convertToArabicNumerals(number: Int): String {
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return number.toString().map { arabicDigits[it - '0'] }.joinToString("")
}

private fun String.stripKashida(): String = this.replace("\u0640", "")

class CircleBackgroundSpan(
    private val backgroundColor: Int,
    private val textColor: Int,
    private val sizeMultiplier: Float
) : ReplacementSpan() {
    // ... Existing implementation ...
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

@Composable
private fun EmptyPageContent(themeColors: ReadingThemeColors) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("جاري تحميل الصفحة...", fontFamily = scheherazadeFont, fontSize = 18.sp, color = themeColors.textSecondary)
    }
}