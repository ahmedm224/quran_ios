package com.quranmedia.player.presentation.screens.reader.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG
import com.quranmedia.player.data.model.QCFLineType
import com.quranmedia.player.data.model.QCFPageData
import com.quranmedia.player.data.repository.ReadingTheme
import com.quranmedia.player.domain.model.Ayah
import com.quranmedia.player.presentation.screens.reader.HighlightedAyah
import com.quranmedia.player.presentation.theme.CustomThemeColors
import com.quranmedia.player.presentation.theme.ReadingThemeColors
import com.quranmedia.player.presentation.theme.ReadingThemes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.ceil
import kotlin.math.floor

private const val FOREGROUND_ALPHA_THRESHOLD = 16
private const val BG_COLOR_TOLERANCE = 10
private const val CONTENT_COLUMN_COVERAGE_RATIO = 0.08f
private const val CONTENT_COLUMN_COVERAGE_RELAXED_RATIO = 0.02f
private const val MIN_CONTENT_WIDTH_RATIO = 0.6f
private const val CONTENT_SIDE_PADDING_PX = 2
private const val TOTAL_LINE_SLOTS = 15

private data class ContentBounds(
    val left: Int,
    val right: Int,
    val top: Int,
    val bottom: Int
)

private data class RenderedSvgPage(
    val bitmap: Bitmap,
    val outputHeightPx: Int,
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val picH: Int,
    val contentLeft: Int,
    val contentRight: Int,
    val contentTop: Int,
    val contentBottom: Int
)

@Composable
fun SVGPageComposable(
    pageNumber: Int,
    svgContent: String?,
    pageData: QCFPageData? = null,
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
            CustomThemeColors(
                backgroundColor = customBackgroundColor,
                textColor = customTextColor,
                headerColor = customHeaderColor
            )
        )
    } else {
        ReadingThemes.getTheme(readingTheme)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap?.invoke() })
            },
        contentAlignment = Alignment.Center
    ) {
        if (svgContent == null) {
            CircularProgressIndicator(color = themeColors.accent)
        } else {
            val safeInsetsModifier = if (isLandscape) {
                Modifier.windowInsetsPadding(WindowInsets.displayCutout)
            } else {
                Modifier
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize().then(safeInsetsModifier)) {
                val density = LocalDensity.current
                val widthPx = with(density) { maxWidth.toPx() }.toInt()
                val heightPx = with(density) { maxHeight.toPx() }.toInt()

                var renderedPage by remember { mutableStateOf<RenderedSvgPage?>(null) }

                LaunchedEffect(svgContent, readingTheme, widthPx, heightPx,
                    customBackgroundColor, customTextColor, customHeaderColor) {
                    if (widthPx <= 0 || heightPx <= 0) return@LaunchedEffect

                    renderedPage = withContext(Dispatchers.Default) {
                        renderSvgBitmap(
                            svgContent = svgContent,
                            containerW = widthPx,
                            containerH = heightPx,
                            theme = themeColors,
                            pageNumber = pageNumber,
                            isLandscape = isLandscape
                        )
                    }
                }

                val page = renderedPage
                if (page != null) {
                    if (isLandscape && page.outputHeightPx > heightPx) {
                        val scrollState = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(with(density) { page.outputHeightPx.toDp() })
                            ) {
                                Image(
                                    bitmap = page.bitmap.asImageBitmap(),
                                    contentDescription = "Quran Page $pageNumber",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.FillBounds
                                )
                                SvgAyahOverlay(
                                    page = page,
                                    pageData = pageData,
                                    ayahs = ayahs,
                                    highlightedAyah = highlightedAyah,
                                    highlightColor = themeColors.highlight,
                                    onTap = onTap,
                                    onAyahLongPress = onAyahLongPress,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                bitmap = page.bitmap.asImageBitmap(),
                                contentDescription = "Quran Page $pageNumber",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                            SvgAyahOverlay(
                                page = page,
                                pageData = pageData,
                                ayahs = ayahs,
                                highlightedAyah = highlightedAyah,
                                highlightColor = themeColors.highlight,
                                onTap = onTap,
                                onAyahLongPress = onAyahLongPress,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = themeColors.accent)
                    }
                }
            }
        }
    }
}

private fun renderSvgBitmap(
    svgContent: String,
    containerW: Int,
    containerH: Int,
    theme: ReadingThemeColors,
    pageNumber: Int,
    isLandscape: Boolean
): RenderedSvgPage? {
    return try {
        val themedSvg = recolorSvg(svgContent, theme, muteOrnaments = pageNumber <= 2)
        val svg = SVG.getFromString(themedSvg)

        // Use SVG dimensions as-is — no viewBox manipulation
        var svgW = svg.documentWidth
        var svgH = svg.documentHeight
        if (svgW <= 0f || svgH <= 0f) {
            svg.documentViewBox?.let { vb ->
                svgW = vb.width()
                svgH = vb.height()
            }
        }
        if (svgW <= 0f || svgH <= 0f) return null

        // Render using the document viewBox as the authoritative visible window.
        val picW = ceil(svgW).toInt()
        val picH = ceil(svgH).toInt()
        val renderOptions = RenderOptions.create()
        svg.documentViewBox?.let { vb ->
            if (vb.width() > 0f && vb.height() > 0f) {
                renderOptions.viewBox(vb.left, vb.top, vb.width(), vb.height())
            }
        }
        val picture = svg.renderToPicture(picW, picH, renderOptions)

        val bgColor = toAndroidColor(theme.background)
        val contentBounds = detectContentBounds(
            picture = picture,
            picW = picW,
            picH = picH,
            bgColor = bgColor
        )

        // Fit by detected core page width (keeps aspect ratio, improves vertical fill).
        val contentW = (contentBounds.right - contentBounds.left + 1).coerceAtLeast(1)
        val scale = containerW.toFloat() / contentW.toFloat()
        val scaledH = picH * scale

        val targetScaledH = ceil(scaledH).toInt()
        val bitmapH = if (isLandscape) {
            maxOf(containerH, targetScaledH)
        } else {
            containerH
        }

        // Create output bitmap (can be taller in landscape to enable vertical scrolling)
        val bmp = Bitmap.createBitmap(containerW, bitmapH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(bgColor)

        val offsetX = -contentBounds.left * scale
        val offsetY = if (isLandscape && scaledH > containerH) {
            0f
        } else {
            (bitmapH - scaledH) / 2f
        }

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        canvas.clipRect(0f, 0f, picW.toFloat(), picH.toFloat())
        canvas.drawPicture(picture)
        canvas.restore()

        RenderedSvgPage(
            bitmap = bmp,
            outputHeightPx = bitmapH,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            picH = picH,
            contentLeft = contentBounds.left,
            contentRight = contentBounds.right,
            contentTop = contentBounds.top,
            contentBottom = contentBounds.bottom
        )
    } catch (e: Exception) {
        Timber.e(e, "Failed to render SVG page $pageNumber")
        null
    }
}

private fun detectContentBounds(
    picture: android.graphics.Picture,
    picW: Int,
    picH: Int,
    bgColor: Int
): ContentBounds {
    if (picW <= 0 || picH <= 0) return ContentBounds(0, 0, 0, 0)

    val probe = Bitmap.createBitmap(picW, picH, Bitmap.Config.ARGB_8888)
    return try {
        val probeCanvas = Canvas(probe)
        probeCanvas.drawColor(bgColor)
        probeCanvas.drawPicture(picture)

        val pixels = IntArray(picW * picH)
        probe.getPixels(pixels, 0, picW, 0, 0, picW, picH)
        val foregroundByColumn = IntArray(picW)
        val foregroundByRow = IntArray(picH)

        var idx = 0
        for (y in 0 until picH) {
            for (x in 0 until picW) {
                val color = pixels[idx]
                val alpha = color ushr 24
                if (alpha > FOREGROUND_ALPHA_THRESHOLD && !isNearColor(color, bgColor, BG_COLOR_TOLERANCE)) {
                    foregroundByColumn[x]++
                    foregroundByRow[y]++
                }
                idx++
            }
        }

        // Horizontal bounds
        val strictThreshold = maxOf(2, (picH * CONTENT_COLUMN_COVERAGE_RATIO).toInt())
        val strictBounds = findColumnBounds(foregroundByColumn, strictThreshold)
        val hBounds = if (strictBounds != null &&
            (strictBounds.second - strictBounds.first + 1) >= (picW * MIN_CONTENT_WIDTH_RATIO).toInt()
        ) {
            strictBounds
        } else {
            val relaxedThreshold = maxOf(1, (picH * CONTENT_COLUMN_COVERAGE_RELAXED_RATIO).toInt())
            findColumnBounds(foregroundByColumn, relaxedThreshold) ?: (0 to (picW - 1))
        }

        val left = (hBounds.first - CONTENT_SIDE_PADDING_PX).coerceAtLeast(0)
        val right = (hBounds.second + CONTENT_SIDE_PADDING_PX).coerceAtMost(picW - 1)

        // Vertical bounds
        val rowThreshold = maxOf(2, (picW * CONTENT_COLUMN_COVERAGE_RELAXED_RATIO).toInt())
        val top = foregroundByRow.indexOfFirst { it >= rowThreshold }.coerceAtLeast(0)
        val bottom = foregroundByRow.indexOfLast { it >= rowThreshold }.let {
            if (it < top) picH - 1 else it
        }

        ContentBounds(left, right, top, bottom)
    } finally {
        probe.recycle()
    }
}

private fun findColumnBounds(columns: IntArray, threshold: Int): Pair<Int, Int>? {
    val left = columns.indexOfFirst { it >= threshold }
    if (left == -1) return null
    val right = columns.indexOfLast { it >= threshold }
    if (right == -1 || right < left) return null
    return left to right
}

private fun isNearColor(color: Int, target: Int, tolerance: Int): Boolean {
    val r1 = (color shr 16) and 0xFF
    val g1 = (color shr 8) and 0xFF
    val b1 = color and 0xFF
    val r2 = (target shr 16) and 0xFF
    val g2 = (target shr 8) and 0xFF
    val b2 = target and 0xFF
    return kotlin.math.abs(r1 - r2) <= tolerance &&
        kotlin.math.abs(g1 - g2) <= tolerance &&
        kotlin.math.abs(b1 - b2) <= tolerance
}

private fun toAndroidColor(color: Color): Int {
    return android.graphics.Color.argb(
        (color.alpha * 255).toInt(),
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt()
    )
}

private fun recolorSvg(svgText: String, theme: ReadingThemeColors, muteOrnaments: Boolean = false): String {
    val textColor = colorToHex(theme.textPrimary)
    val bgColor = colorToHex(theme.background)

    // Pages 1-2: muted accent (30% accent blended with 70% background)
    val ornamentColor = if (muteOrnaments) {
        val r = theme.accent.red * 0.3f + theme.background.red * 0.7f
        val g = theme.accent.green * 0.3f + theme.background.green * 0.7f
        val b = theme.accent.blue * 0.3f + theme.background.blue * 0.7f
        colorToHex(Color(r, g, b))
    } else {
        colorToHex(theme.accent)
    }

    var out = svgText
    out = out.replace("fill:#231f20", "fill:$textColor")
    out = out.replace("fill: #231f20", "fill:$textColor")
    out = out.replace("fill:#231F20", "fill:$textColor")
    out = out.replace("fill:#bfe8c1", "fill:$ornamentColor")
    out = out.replace("fill: #bfe8c1", "fill:$ornamentColor")
    out = out.replace("fill:#BFE8C1", "fill:$ornamentColor")
    out = out.replace("fill:#ffffff", "fill:$bgColor")
    out = out.replace("fill: #ffffff", "fill:$bgColor")
    out = out.replace("fill:#fff;", "fill:$bgColor;")
    out = out.replace("fill:#FFFFFF", "fill:$bgColor")
    return out
}

private fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue * 255).toInt()
    return "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}"
}

@Composable
private fun SvgAyahOverlay(
    page: RenderedSvgPage,
    pageData: QCFPageData?,
    ayahs: List<Ayah>,
    highlightedAyah: HighlightedAyah?,
    highlightColor: Color,
    onTap: (() -> Unit)?,
    onAyahLongPress: ((Ayah, Offset) -> Unit)?,
    modifier: Modifier = Modifier
) {
    var overlayPosition by remember { mutableStateOf(Offset.Zero) }
    val haptic = LocalHapticFeedback.current

    // Content region in picture coordinates for accurate line mapping
    val contentTopPic = page.contentTop.toFloat()
    val contentHeightPic = (page.contentBottom - page.contentTop).toFloat().coerceAtLeast(1f)
    val lineSlotH = contentHeightPic / TOTAL_LINE_SLOTS

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                overlayPosition = coordinates.positionInRoot()
            }
            .pointerInput(pageData, ayahs, onTap, onAyahLongPress) {
                detectTapGestures(
                    onTap = { onTap?.invoke() },
                    onLongPress = { localOffset ->
                        if (onAyahLongPress == null || pageData == null) return@detectTapGestures

                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                        val pictureY = (localOffset.y - page.offsetY) / page.scale
                        val lineSlot = floor((pictureY - contentTopPic) / lineSlotH).toInt() + 1

                        val textLine = pageData.lines.find {
                            it.line == lineSlot && it.type == QCFLineType.TEXT
                        }
                        val words = textLine?.words
                        if (words.isNullOrEmpty()) return@detectTapGestures

                        val pictureX = (localOffset.x - page.offsetX) / page.scale
                        val contentWidth = (page.contentRight - page.contentLeft + 1).toFloat()
                        val relativeX = ((pictureX - page.contentLeft) / contentWidth).coerceIn(0f, 1f)
                        // RTL: right side (relativeX=1) = first word, left side (relativeX=0) = last word
                        val wordIndex = ((1f - relativeX) * words.size).toInt().coerceIn(0, words.lastIndex)

                        val tappedWord = words[wordIndex]
                        val surah = tappedWord.getSurahNumber()
                        val verse = tappedWord.getVerseNumber()

                        if (surah != null && verse != null) {
                            val ayah = ayahs.find { it.surahNumber == surah && it.ayahNumber == verse }
                            if (ayah != null) {
                                val globalOffset = Offset(
                                    x = overlayPosition.x + localOffset.x,
                                    y = overlayPosition.y + localOffset.y
                                )
                                onAyahLongPress(ayah, globalOffset)
                            }
                        }
                    }
                )
            }
    ) {
        if (highlightedAyah != null && pageData != null) {
            val contentLeftBm = page.contentLeft * page.scale + page.offsetX
            val contentRightBm = (page.contentRight + 1) * page.scale + page.offsetX
            val bitmapContentW = contentRightBm - contentLeftBm
            val cornerR = CornerRadius(4.dp.toPx())

            pageData.lines.forEach { line ->
                if (line.type != QCFLineType.TEXT) return@forEach
                val words = line.words ?: return@forEach
                val lineNum = line.line ?: return@forEach

                // Find highlighted word range within this line
                val firstIdx = words.indexOfFirst { word ->
                    word.getSurahNumber() == highlightedAyah.surahNumber &&
                        word.getVerseNumber() == highlightedAyah.ayahNumber
                }
                if (firstIdx < 0) return@forEach

                val lastIdx = words.indexOfLast { word ->
                    word.getSurahNumber() == highlightedAyah.surahNumber &&
                        word.getVerseNumber() == highlightedAyah.ayahNumber
                }

                // Line vertical position using detected content bounds
                val lineTopPic = contentTopPic + (lineNum - 1) * lineSlotH
                val lineBottomPic = contentTopPic + lineNum * lineSlotH
                val lineTopBm = lineTopPic * page.scale + page.offsetY
                val lineBottomBm = lineBottomPic * page.scale + page.offsetY

                // Ayah horizontal span (RTL: word 0 at right, word N-1 at left)
                val totalWords = words.size.toFloat()
                val highlightLeft = contentRightBm - ((lastIdx + 1) / totalWords) * bitmapContentW
                val highlightRight = contentRightBm - (firstIdx / totalWords) * bitmapContentW

                drawRoundRect(
                    color = highlightColor.copy(alpha = 0.3f),
                    topLeft = Offset(highlightLeft, lineTopBm),
                    size = Size(highlightRight - highlightLeft, lineBottomBm - lineTopBm),
                    cornerRadius = cornerR
                )
            }
        }
    }
}
