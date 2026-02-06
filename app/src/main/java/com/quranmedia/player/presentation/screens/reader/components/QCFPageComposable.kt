package com.quranmedia.player.presentation.screens.reader.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.quranmedia.player.data.model.QCFFontMode
import com.quranmedia.player.data.model.QCFLineData
import com.quranmedia.player.data.model.QCFLineType
import com.quranmedia.player.data.model.QCFPageData
import com.quranmedia.player.data.repository.ReadingTheme
import com.quranmedia.player.domain.model.Ayah
import com.quranmedia.player.presentation.screens.reader.HighlightedAyah
import com.quranmedia.player.presentation.theme.ReadingThemeColors
import com.quranmedia.player.presentation.theme.ReadingThemes

private const val STANDARD_LINE_COUNT = 15
private const val BASE_WIDTH_DP = 400f
private val SURAH_HEADER_HEIGHT = 40.dp

/**
 * QCF (Quran Complex Fonts) page composable.
 * Renders Mushaf pages using per-page fonts with pixel-perfect rendering.
 *
 * IMPORTANT RENDERING NOTES:
 * - Both V4 (Tajweed) and V2 (Plain) fonts use qpcV2 glyph codes
 * - Words MUST be joined with SPACE (" ") - not empty string
 * - Basmala lines use fallback Arabic Unicode text (JSON has incorrect codes)
 * - Auto-sizing handles overflow on pages with Kashida/Tatweel
 */
@Composable
fun QCFPageComposable(
    pageNumber: Int,
    pageData: QCFPageData?,
    fontFamily: FontFamily?,
    fontMode: QCFFontMode = QCFFontMode.PLAIN,
    useBoldFont: Boolean = false,
    ayahs: List<Ayah> = emptyList(),
    highlightedAyah: HighlightedAyah? = null,
    readingTheme: ReadingTheme = ReadingTheme.LIGHT,
    customBackgroundColor: Color? = null,
    customTextColor: Color? = null,
    customHeaderColor: Color? = null,
    isLandscape: Boolean = false,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
    onAyahLongPress: ((Ayah, Offset) -> Unit)? = null
) {
    val themeColors = if (readingTheme == ReadingTheme.CUSTOM &&
        customBackgroundColor != null && customTextColor != null && customHeaderColor != null
    ) {
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

    // System bar padding - combine statusBars with displayCutout for punch-hole cameras
    val topInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout).asPaddingValues()
    val bottomInsets = WindowInsets.navigationBars.asPaddingValues()

    // Process lines to fix JSON data issues and inject missing elements
    val processedLines = remember(pageData) {
        if (pageData == null) emptyList()
        else {
            val lines = pageData.lines.toMutableList()
            val lastIndex = lines.lastIndex

            // Remove surah header ONLY if it's the very last line (end-of-surah marker)
            val filteredLines = lines.filterIndexed { index, line ->
                if (line.type == QCFLineType.SURAH_HEADER && index == lastIndex) {
                    // This is an "end of surah" marker at the last line - remove it
                    false
                } else {
                    true
                }
            }.toMutableList()

            // Auto-inject surah header if page starts with basmala without header
            val firstLine = filteredLines.firstOrNull()
            val hasHeaderAtStart = firstLine?.type == QCFLineType.SURAH_HEADER

            if (!hasHeaderAtStart && firstLine?.type == QCFLineType.BASMALA) {
                val firstTextLine = filteredLines.find { it.type == QCFLineType.TEXT }
                val surahNumber = firstTextLine?.words?.firstOrNull()?.getSurahNumber()

                if (surahNumber != null && surahNumber > 1) {
                    val headerLine = QCFLineData(
                        line = 0,
                        type = QCFLineType.SURAH_HEADER,
                        text = "سورة ${surahNamesArabic[surahNumber] ?: surahNumber}",
                        surah = surahNumber.toString().padStart(3, '0')
                    )
                    filteredLines.add(0, headerLine)
                }
            }

            // Inject basmala after surah headers (except for Surah 1 and 9)
            val result = mutableListOf<QCFLineData>()
            var i = 0
            while (i < filteredLines.size) {
                val line = filteredLines[i]
                result.add(line)

                // If this is a surah header, check if next line is basmala
                if (line.type == QCFLineType.SURAH_HEADER) {
                    val surahNumber = line.surah?.toIntOrNull() ?: 0
                    val nextLine = filteredLines.getOrNull(i + 1)

                    // Add basmala if:
                    // 1. Surah is not 1 (Al-Fatiha has basmala as verse 1) or 9 (At-Tawbah has no basmala)
                    // 2. Next line is not already a basmala
                    if (surahNumber !in listOf(1, 9) && nextLine?.type != QCFLineType.BASMALA) {
                        result.add(QCFLineData(
                            line = line.line,
                            type = QCFLineType.BASMALA,
                            text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"
                        ))
                    }
                }
                i++
            }

            result
        }
    }

    // Count headers and basmalas to determine page type
    val headerCount = processedLines.count { it.type == QCFLineType.SURAH_HEADER }
    val textLineCount = processedLines.count { it.type == QCFLineType.TEXT }

    // Special pages: pages 1-2, or pages with fewer text lines than standard
    val isSpecialPage = pageNumber <= 2 || textLineCount < 10

    // For PLAIN mode (V2 fonts), use theme text color
    // For TAJWEED mode (V4 COLRv1 fonts), colors are embedded in the font
    val plainTextColor = if (fontMode == QCFFontMode.PLAIN) {
        themeColors.textPrimary
    } else {
        null // Let embedded colors show through
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .pointerInput(onTap) {
                detectTapGestures(onTap = { onTap?.invoke() })
            }
    ) {
        val density = LocalDensity.current
        val widthDp = with(density) { maxWidth.value }

        // Font scale based on screen width
        val widthScale = (widthDp / BASE_WIDTH_DP).coerceIn(0.8f, 1.2f)

        // Count text lines to determine font size
        // Fewer lines = larger font, more lines = smaller font
        val textLineCount = processedLines.count { it.type == QCFLineType.TEXT }

        // V4 (Tajweed) fonts have larger metrics, use consistent size without line scaling
        // V2 (Plain) can use line scaling for better fill
        val fontScale = if (fontMode == QCFFontMode.TAJWEED) {
            // V4: Fixed scale, no variation based on line count
            widthScale * 0.92f
        } else {
            // V2: Scale based on line count for better page fill
            val lineScale = when {
                textLineCount <= 5 -> 1.25f  // Very short pages (last surahs)
                textLineCount <= 8 -> 1.15f  // Short pages
                textLineCount <= 11 -> 1.08f // Medium pages
                else -> 1.0f                  // Standard 15-line pages
            }
            widthScale * lineScale
        }

        // Page frame border - HIDDEN (uncomment to restore)
        // QCFPageFrame(
        //     themeColors = themeColors,
        //     modifier = Modifier
        //         .fillMaxSize()
        //         .padding(
        //             top = topInsets.calculateTopPadding(),
        //             bottom = bottomInsets.calculateBottomPadding()
        //         )
        //         .padding(horizontal = 10.dp, vertical = 8.dp)
        // )

        // Landscape mode: scrollable content, minimal margins, larger font to fill width
        // Use 1.6x scale in landscape to fill the wider screen
        val landscapeFontScale = if (isLandscape) fontScale * 1.6f else fontScale
        // Minimal horizontal padding in landscape to maximize text width
        val horizontalPadding = if (isLandscape) 4.dp else 16.dp
        val scrollState = rememberScrollState()

        // Page content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = topInsets.calculateTopPadding(),
                    bottom = bottomInsets.calculateBottomPadding()
                )
                .padding(horizontal = horizontalPadding, vertical = if (isLandscape) 8.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (pageData != null && fontFamily != null && processedLines.isNotEmpty()) {
                // Content area with lines
                // In landscape: scrollable with top alignment
                // In portrait: evenly spaced
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(
                            if (isLandscape) Modifier.verticalScroll(scrollState)
                            else Modifier
                        ),
                    verticalArrangement = if (isLandscape) {
                        Arrangement.Top
                    } else if (isSpecialPage) {
                        Arrangement.Center
                    } else {
                        Arrangement.SpaceEvenly
                    },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    processedLines.forEach { line ->
                        QCFLine(
                            line = line,
                            fontFamily = fontFamily,
                            fontMode = fontMode,
                            useBoldFont = useBoldFont,
                            themeColors = themeColors,
                            readingTheme = readingTheme,
                            isSpecialPage = isSpecialPage,
                            fontScale = if (isLandscape) landscapeFontScale else fontScale,
                            plainTextColor = plainTextColor,
                            ayahs = ayahs,
                            highlightedAyah = highlightedAyah,
                            onTap = onTap,
                            onAyahLongPress = onAyahLongPress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // In landscape, add page number at the end of scrollable content
                    if (isLandscape) {
                        Text(
                            text = pageNumber.toString(),
                            color = themeColors.textSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                }

                // Page number at bottom (portrait only)
                if (!isLandscape) {
                    Text(
                        text = pageNumber.toString(),
                        color = themeColors.textSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            } else {
                // Loading state
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = themeColors.accent
                    )
                }
            }
        }
    }
}

/**
 * Page frame drawn with Canvas for proper rendering within system insets.
 */
@Composable
private fun QCFPageFrame(
    themeColors: ReadingThemeColors,
    modifier: Modifier = Modifier
) {
    val frameColor = themeColors.textSecondary.copy(alpha = 0.3f)
    val accentColor = themeColors.accent.copy(alpha = 0.4f)

    Canvas(modifier = modifier) {
        val borderWidth = 1.5.dp.toPx()
        val innerBorderWidth = 0.8.dp.toPx()
        val cornerRadius = 6.dp.toPx()
        val margin = 4.dp.toPx()  // Increased margin for safety
        val innerMargin = 8.dp.toPx()

        // Outer frame
        drawRoundRect(
            color = frameColor,
            topLeft = Offset(margin, margin),
            size = Size(size.width - 2 * margin, size.height - 2 * margin),
            cornerRadius = CornerRadius(cornerRadius),
            style = Stroke(width = borderWidth)
        )

        // Inner frame
        drawRoundRect(
            color = accentColor,
            topLeft = Offset(innerMargin, innerMargin),
            size = Size(size.width - 2 * innerMargin, size.height - 2 * innerMargin),
            cornerRadius = CornerRadius(cornerRadius),
            style = Stroke(width = innerBorderWidth)
        )

        // Corner decorations
        val cornerDecorationRadius = 2.dp.toPx()
        val corners = listOf(
            Offset(margin + cornerRadius + 2.dp.toPx(), margin + cornerRadius + 2.dp.toPx()),
            Offset(size.width - margin - cornerRadius - 2.dp.toPx(), margin + cornerRadius + 2.dp.toPx()),
            Offset(margin + cornerRadius + 2.dp.toPx(), size.height - margin - cornerRadius - 2.dp.toPx()),
            Offset(size.width - margin - cornerRadius - 2.dp.toPx(), size.height - margin - cornerRadius - 2.dp.toPx())
        )

        corners.forEach { corner ->
            drawCircle(
                color = themeColors.accent,
                radius = cornerDecorationRadius,
                center = corner
            )
        }
    }
}

/**
 * Renders a single QCF line based on its type.
 */
@Composable
private fun QCFLine(
    line: QCFLineData,
    fontFamily: FontFamily,
    fontMode: QCFFontMode,
    useBoldFont: Boolean = false,
    themeColors: ReadingThemeColors,
    readingTheme: ReadingTheme,
    isSpecialPage: Boolean = false,
    fontScale: Float = 1f,
    plainTextColor: Color? = null,
    ayahs: List<Ayah> = emptyList(),
    highlightedAyah: HighlightedAyah? = null,
    onTap: (() -> Unit)? = null,
    onAyahLongPress: ((Ayah, Offset) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when (line.type) {
        QCFLineType.SURAH_HEADER -> {
            val surahNumber = line.surah?.toIntOrNull() ?: 1
            QCFSurahHeaderWithSVG(
                surahNumber = surahNumber,
                themeColors = themeColors,
                readingTheme = readingTheme,
                isSpecialPage = isSpecialPage,
                onTap = onTap,
                modifier = modifier
            )
        }
        QCFLineType.BASMALA -> {
            QCFBasmalaLine(
                line = line,
                fontFamily = fontFamily,
                useBoldFont = useBoldFont,
                themeColors = themeColors,
                isSpecialPage = isSpecialPage,
                fontScale = fontScale,
                plainTextColor = plainTextColor,
                onTap = onTap,
                modifier = modifier
            )
        }
        QCFLineType.TEXT -> {
            QCFTextLine(
                line = line,
                fontFamily = fontFamily,
                fontMode = fontMode,
                useBoldFont = useBoldFont,
                themeColors = themeColors,
                isSpecialPage = isSpecialPage,
                fontScale = fontScale,
                plainTextColor = plainTextColor,
                ayahs = ayahs,
                highlightedAyah = highlightedAyah,
                onTap = onTap,
                onAyahLongPress = onAyahLongPress,
                modifier = modifier
            )
        }
    }
}

/**
 * Surah header using existing SVG assets (matching the current app design).
 */
@Composable
private fun QCFSurahHeaderWithSVG(
    surahNumber: Int,
    themeColors: ReadingThemeColors,
    readingTheme: ReadingTheme,
    isSpecialPage: Boolean = false,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Select SVG based on theme (same logic as existing CompactSurahHeader)
    val svgFileName = when (readingTheme) {
        ReadingTheme.NIGHT -> "surah_header_dark.svg"
        ReadingTheme.SEPIA -> "surah_header_brown.svg"
        ReadingTheme.OCEAN -> "surah_header_blue.svg"
        ReadingTheme.CUSTOM -> "surah_header_green.svg"
        ReadingTheme.TAJWEED -> "surah_header_green.svg"
        else -> "surah_header_green.svg"
    }

    val surahName = surahNamesArabic[surahNumber] ?: "$surahNumber"
    val textColor = themeColors.ayahMarker

    val headerHeight = if (isSpecialPage) 48.dp else SURAH_HEADER_HEIGHT
    val fontSize = if (isSpecialPage) 18.sp else 15.sp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight)
            .padding(vertical = 2.dp)
            .pointerInput(onTap) {
                detectTapGestures(onTap = { onTap?.invoke() })
            },
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
                .height(headerHeight)
        )

        // Surah name text centered over the SVG frame
        Text(
            text = "سُورَةُ $surahName",
            fontFamily = kfgqpcHafsFont,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Basmala line - renders QCF glyphs when words are available in JSON,
 * otherwise falls back to Arabic Unicode text.
 * Uses FIXED font size (no auto-sizing) for consistent rendering.
 */
@Composable
private fun QCFBasmalaLine(
    line: QCFLineData,
    fontFamily: FontFamily,
    useBoldFont: Boolean = false,
    themeColors: ReadingThemeColors,
    isSpecialPage: Boolean = false,
    fontScale: Float = 1f,
    plainTextColor: Color? = null,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val words = line.words
    val textColor = plainTextColor ?: themeColors.textPrimary
    val fontWeight = if (useBoldFont) FontWeight.Bold else FontWeight.Normal

    // Fixed font size - scaled by fontScale which accounts for line count
    val baseFontSize = if (isSpecialPage) 22f else 20f
    val fontSize = (baseFontSize * fontScale).sp

    if (words != null && words.isNotEmpty()) {
        // Render using QCF glyphs from JSON
        val glyphText = words.joinToString(" ") { it.qpcV2 }

        Text(
            text = glyphText,
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = modifier
                .pointerInput(onTap) {
                    detectTapGestures(onTap = { onTap?.invoke() })
                }
        )
    } else {
        // Fallback to Arabic Unicode text
        val displayText = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"

        Text(
            text = displayText,
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = kfgqpcHafsFont,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = modifier
                .pointerInput(onTap) {
                    detectTapGestures(onTap = { onTap?.invoke() })
                }
        )
    }
}

/**
 * Regular text line using QCF glyphs.
 * Uses UNIFORM font size across all lines (no per-line auto-sizing).
 * Both V2 (Plain) and V4 (Tajweed) use identical rendering with same font sizing.
 */
@Composable
private fun QCFTextLine(
    line: QCFLineData,
    fontFamily: FontFamily,
    fontMode: QCFFontMode,
    useBoldFont: Boolean = false,
    themeColors: ReadingThemeColors,
    isSpecialPage: Boolean = false,
    fontScale: Float = 1f,
    plainTextColor: Color? = null,
    ayahs: List<Ayah> = emptyList(),
    highlightedAyah: HighlightedAyah? = null,
    onTap: (() -> Unit)? = null,
    onAyahLongPress: ((Ayah, Offset) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Base font size - scaled by fontScale which accounts for line count
    val baseFontSize = if (isSpecialPage) 24f else 22f
    val maxFontSize = (baseFontSize * fontScale).sp
    val minFontSize = (baseFontSize * fontScale * 0.7f).sp  // Allow shrinking to 70% to fit width
    val fontWeight = if (useBoldFont) FontWeight.Bold else FontWeight.Normal

    // Build word positions for tap detection
    val words = line.words ?: emptyList()

    // Text color: use provided color for PLAIN mode
    // For TAJWEED mode (V4 COLRv1 fonts), use Unspecified to let embedded colors show
    val textColor = plainTextColor ?: Color.Unspecified

    // Build AnnotatedString with word-level highlighting
    val annotatedText = remember(words, highlightedAyah, textColor, themeColors.highlight) {
        buildAnnotatedString {
            words.forEachIndexed { index, word ->
                val isHighlighted = highlightedAyah != null &&
                    word.getSurahNumber() == highlightedAyah.surahNumber &&
                    word.getVerseNumber() == highlightedAyah.ayahNumber

                // Add space between words (not before the first word)
                if (index > 0) {
                    append(" ")
                }

                // Apply color based on highlight status
                withStyle(SpanStyle(color = if (isHighlighted) themeColors.highlight else textColor)) {
                    append(word.qpcV2)
                }
            }
        }
    }

    // Auto-size to fit width - shrink only if overflow
    val glyphText = words.joinToString(" ") { it.qpcV2 }
    var fontSize by remember(glyphText, maxFontSize) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(glyphText, maxFontSize) { mutableStateOf(false) }

    // For long-press, we need to estimate which word was tapped based on position
    var textPosition by remember { mutableStateOf(Offset.Zero) }
    var textWidth by remember { mutableStateOf(0f) }

    Text(
        text = annotatedText,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowWidth && fontSize > minFontSize) {
                // Shrink font to fit width
                fontSize = (fontSize.value - 0.5f).sp
            } else {
                readyToDraw = true
                textWidth = textLayoutResult.size.width.toFloat()
            }
        },
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                textPosition = coordinates.positionInRoot()
            }
            .pointerInput(words, ayahs, onTap, onAyahLongPress) {
                detectTapGestures(
                    onTap = { onTap?.invoke() },
                    onLongPress = { localOffset ->
                        if (words.isNotEmpty() && onAyahLongPress != null && textWidth > 0) {
                            val containerWidth = size.width.toFloat()
                            val textStartX = (containerWidth - textWidth) / 2
                            val relativeX = localOffset.x - textStartX

                            val positionRatio = 1f - (relativeX / textWidth).coerceIn(0f, 1f)
                            val estimatedWordIndex = (positionRatio * words.size).toInt().coerceIn(0, words.lastIndex)

                            val tappedWord = words[estimatedWordIndex]
                            val surah = tappedWord.getSurahNumber()
                            val verse = tappedWord.getVerseNumber()

                            if (surah != null && verse != null) {
                                val ayah = ayahs.find { it.surahNumber == surah && it.ayahNumber == verse }
                                if (ayah != null) {
                                    val globalOffset = Offset(
                                        x = textPosition.x + localOffset.x,
                                        y = textPosition.y + localOffset.y
                                    )
                                    onAyahLongPress(ayah, globalOffset)
                                }
                            }
                        }
                    }
                )
            }
            .drawWithContent {
                if (readyToDraw) {
                    drawContent()
                }
            }
    )
}
