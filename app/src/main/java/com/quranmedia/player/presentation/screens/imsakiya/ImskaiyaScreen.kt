package com.quranmedia.player.presentation.screens.imsakiya

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.domain.util.ArabicNumeralUtils
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ramadan Imsakiya Screen - Prayer times calendar for the month of Ramadan.
 * Features an elegant Islamic-themed table design with Mihrab-inspired border.
 *
 * TODO: After Ramadan, this file can be removed along with the entire imsakiya package.
 */

// Ramadan theme colors
private val RamadanDarkPurple = Color(0xFF1A1A2E)
private val RamadanPurple = Color(0xFF16213E)
private val RamadanPurpleLight = Color(0xFF252B48)
private val RamadanGold = Color(0xFFFFD700)
private val RamadanGoldLight = Color(0xFFFFE55C)
private val RamadanGoldDark = Color(0xFFB8860B)
private val RamadanCream = Color(0xFFF5F5DC)
private val RamadanCreamDim = Color(0xFFE8E4C9)

// Prayer highlights
private val FajrHighlight = Color(0xFF2E4A62)
private val MaghribHighlight = Color(0xFF5A3D3D)
private val CurrentDayHighlight = Color(0xFF3D5A3D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImskaiyaScreen(
    viewModel: ImskaiyaViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val language = settings.appLanguage
    val isArabic = language == AppLanguage.ARABIC
    val useIndoArabic = isArabic && settings.useIndoArabicNumerals

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.offlineWarning) {
        uiState.offlineWarning?.let { warning ->
            snackbarHostState.showSnackbar(
                message = warning,
                duration = SnackbarDuration.Short
            )
            viewModel.clearOfflineWarning()
        }
    }

    LaunchedEffect(uiState.currentDayIndex) {
        if (uiState.currentDayIndex >= 0) {
            listState.animateScrollToItem(
                index = maxOf(0, uiState.currentDayIndex),
                scrollOffset = 0
            )
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.NightsStay,
                                contentDescription = null,
                                tint = RamadanGold
                            )
                            Column {
                                Text(
                                    text = if (isArabic) "إمساكية رمضان" else "Ramadan Imsakiya",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = RamadanGold
                                )
                                if (uiState.daysUntilRamadan > 0) {
                                    val daysText = if (isArabic) {
                                        val daysNum = ArabicNumeralUtils.formatNumber(uiState.daysUntilRamadan, useIndoArabic)
                                        if (uiState.daysUntilRamadan == 1) "باقي يوم واحد" else "باقي $daysNum يوم"
                                    } else {
                                        "${uiState.daysUntilRamadan} day${if (uiState.daysUntilRamadan > 1) "s" else ""} until Ramadan"
                                    }
                                    Text(
                                        text = daysText,
                                        fontSize = 11.sp,
                                        color = RamadanGoldLight
                                    )
                                } else {
                                    val hijriDate = if (isArabic) uiState.currentHijriDateArabic else uiState.currentHijriDate
                                    hijriDate?.let {
                                        Text(
                                            text = if (useIndoArabic) ArabicNumeralUtils.toIndoArabic(it) else it,
                                            fontSize = 11.sp,
                                            color = RamadanGoldLight
                                        )
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isArabic) "رجوع" else "Back",
                                tint = RamadanCream
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.loadImsakiya() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = if (isArabic) "تحديث" else "Refresh",
                                tint = RamadanCream
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = RamadanDarkPurple,
                        titleContentColor = RamadanGold
                    )
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = RamadanDarkPurple,
                        contentColor = RamadanCream
                    )
                }
            },
            containerColor = RamadanPurple
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    uiState.isLoading -> LoadingState(isArabic)
                    uiState.error != null -> ErrorState(uiState.error!!, isArabic) { viewModel.loadImsakiya() }
                    uiState.imsakiya != null -> ImskaiyaContent(
                        imsakiya = uiState.imsakiya!!,
                        currentDayIndex = uiState.currentDayIndex,
                        isArabic = isArabic,
                        useIndoArabic = useIndoArabic,
                        listState = listState
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState(isArabic: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = RamadanGold)
            Text(
                text = if (isArabic) "جاري تحميل الإمساكية..." else "Loading Imsakiya...",
                color = RamadanCream
            )
        }
    }
}

@Composable
private fun ErrorState(error: String, isArabic: Boolean, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = if (isArabic) "حدث خطأ" else "Error",
                color = RamadanGold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = error,
                color = RamadanCream,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RamadanGold,
                    contentColor = RamadanDarkPurple
                )
            ) {
                Text(text = if (isArabic) "إعادة المحاولة" else "Retry")
            }
        }
    }
}

@Composable
private fun ImskaiyaContent(
    imsakiya: ImskaiyaMonth,
    currentDayIndex: Int,
    isArabic: Boolean,
    useIndoArabic: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        CompactHeader(imsakiya, isArabic, useIndoArabic)

        // Table with Mihrab-inspired border
        MihrabTable(
            imsakiya = imsakiya,
            currentDayIndex = currentDayIndex,
            isArabic = isArabic,
            useIndoArabic = useIndoArabic,
            listState = listState
        )
    }
}

@Composable
private fun CompactHeader(imsakiya: ImskaiyaMonth, isArabic: Boolean, useIndoArabic: Boolean) {
    val hijriYearStr = ArabicNumeralUtils.formatNumber(imsakiya.hijriYear, useIndoArabic)
    val gregorianYearStr = ArabicNumeralUtils.formatNumber(imsakiya.gregorianYear, useIndoArabic)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RamadanDarkPurple)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (isArabic) "رمضان $hijriYearStr" else "Ramadan $hijriYearStr",
                color = RamadanGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = gregorianYearStr,
                color = RamadanCream.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = imsakiya.locationName,
                color = RamadanCream,
                fontSize = 14.sp
            )
            Text(
                text = imsakiya.calculationMethod,
                color = RamadanCream.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RamadanPurple)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = FajrHighlight, label = if (isArabic) "الإمساك" else "Suhoor")
        Spacer(modifier = Modifier.width(16.dp))
        LegendItem(color = MaghribHighlight, label = if (isArabic) "الإفطار" else "Iftar")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = RoundedCornerShape(2.dp))
                .border(1.dp, RamadanGold.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
        )
        Text(
            text = label,
            color = RamadanCream,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun MihrabTable(
    imsakiya: ImskaiyaMonth,
    currentDayIndex: Int,
    isArabic: Boolean,
    useIndoArabic: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            // Mihrab-inspired border draw
            .drawBehind {
                drawMihrabBorder()
            }
            .padding(4.dp) // Inner padding for border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
        ) {
            // Table header
            IslamicTableHeader(isArabic)

            // Gold divider
            GoldDivider()

            // Table rows
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(imsakiya.days) { index, day ->
                    IslamicDayRow(
                        day = day,
                        isCurrentDay = index == currentDayIndex,
                        isArabic = isArabic,
                        useIndoArabic = useIndoArabic,
                        isEvenRow = index % 2 == 0
                    )

                    if (index < imsakiya.days.size - 1) {
                        RowDivider()
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

/**
 * Draw a Mihrab-inspired Islamic border around the table with soft edges
 */
private fun DrawScope.drawMihrabBorder() {
    val goldColor = RamadanGold
    val darkGold = RamadanGoldDark
    val softGold = RamadanGold.copy(alpha = 0.4f)

    val width = size.width
    val height = size.height

    // Top arch (Mihrab shape) - semi-circular arch
    val archHeight = 50f
    val archWidth = width * 0.65f
    val archCenterX = width / 2
    val cornerRadius = 12f

    // Create the main Mihrab path with rounded corners
    val path = Path().apply {
        // Start from bottom left corner
        moveTo(cornerRadius, height)

        // Bottom left rounded corner
        quadraticBezierTo(0f, height, 0f, height - cornerRadius)

        // Left side up to arch level
        lineTo(0f, archHeight + cornerRadius)

        // Top left corner curve
        quadraticBezierTo(0f, archHeight, cornerRadius, archHeight)

        // Line to arch start
        lineTo(archCenterX - archWidth / 2, archHeight)

        // Arch curve (semi-circle) - smoother with bezier
        val archControlY = -archHeight * 0.3f
        cubicTo(
            archCenterX - archWidth / 4, archControlY,
            archCenterX + archWidth / 4, archControlY,
            archCenterX + archWidth / 2, archHeight
        )

        // Line to top right corner
        lineTo(width - cornerRadius, archHeight)

        // Top right corner curve
        quadraticBezierTo(width, archHeight, width, archHeight + cornerRadius)

        // Right side down
        lineTo(width, height - cornerRadius)

        // Bottom right rounded corner
        quadraticBezierTo(width, height, width - cornerRadius, height)

        // Bottom line
        close()
    }

    // Draw soft outer glow
    drawPath(
        path = path,
        color = softGold,
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 8f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Draw main border with rounded caps
    drawPath(
        path = path,
        color = goldColor,
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2.5f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Draw inner decorative border
    val inset = 6f
    val innerPath = Path().apply {
        moveTo(cornerRadius + inset, height - inset)
        quadraticBezierTo(inset, height - inset, inset, height - cornerRadius - inset)
        lineTo(inset, archHeight + cornerRadius + inset)
        quadraticBezierTo(inset, archHeight + inset, cornerRadius + inset, archHeight + inset)
        lineTo(archCenterX - archWidth / 2 + inset, archHeight + inset)

        val innerArchControlY = -archHeight * 0.3f + inset * 2
        cubicTo(
            archCenterX - archWidth / 4, innerArchControlY,
            archCenterX + archWidth / 4, innerArchControlY,
            archCenterX + archWidth / 2 - inset, archHeight + inset
        )

        lineTo(width - cornerRadius - inset, archHeight + inset)
        quadraticBezierTo(width - inset, archHeight + inset, width - inset, archHeight + cornerRadius + inset)
        lineTo(width - inset, height - cornerRadius - inset)
        quadraticBezierTo(width - inset, height - inset, width - cornerRadius - inset, height - inset)
        close()
    }

    drawPath(
        path = innerPath,
        color = darkGold.copy(alpha = 0.5f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
        )
    )

    // Draw decorative Islamic patterns
    drawSoftCornerDecorations(width, height, archHeight, cornerRadius, goldColor)
    drawArchDecoration(archCenterX, goldColor)
}

private fun DrawScope.drawSoftCornerDecorations(
    width: Float,
    height: Float,
    archHeight: Float,
    cornerRadius: Float,
    color: Color
) {
    val dotRadius = 3f
    val softColor = color.copy(alpha = 0.7f)

    // Small decorative dots at key points
    // Bottom corners
    drawCircle(softColor, dotRadius, Offset(cornerRadius + 8f, height - 8f))
    drawCircle(softColor, dotRadius, Offset(width - cornerRadius - 8f, height - 8f))

    // Side decorations - small diamonds
    val diamondSize = 4f
    val sideY = height / 2

    // Left side diamond
    val leftDiamond = Path().apply {
        moveTo(4f, sideY)
        lineTo(4f + diamondSize, sideY - diamondSize)
        lineTo(4f + diamondSize * 2, sideY)
        lineTo(4f + diamondSize, sideY + diamondSize)
        close()
    }
    drawPath(leftDiamond, softColor)

    // Right side diamond
    val rightDiamond = Path().apply {
        moveTo(width - 4f, sideY)
        lineTo(width - 4f - diamondSize, sideY - diamondSize)
        lineTo(width - 4f - diamondSize * 2, sideY)
        lineTo(width - 4f - diamondSize, sideY + diamondSize)
        close()
    }
    drawPath(rightDiamond, softColor)
}

private fun DrawScope.drawArchDecoration(centerX: Float, color: Color) {
    // Star/crescent at top of arch
    val topY = 12f
    val softColor = color.copy(alpha = 0.9f)

    // Draw crescent moon
    drawCircle(softColor, 7f, Offset(centerX, topY))
    drawCircle(RamadanDarkPurple, 5.5f, Offset(centerX + 3f, topY - 1f))

    // Small star next to crescent
    val starX = centerX - 14f
    val starY = topY + 2f
    drawCircle(softColor, 2f, Offset(starX, starY))

    // Decorative dots on arch sides
    val dotColor = color.copy(alpha = 0.5f)
    drawCircle(dotColor, 2f, Offset(centerX - 40f, 25f))
    drawCircle(dotColor, 2f, Offset(centerX + 40f, 25f))
}

@Composable
private fun IslamicTableHeader(isArabic: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(RamadanDarkPurple, RamadanPurpleLight)
                ),
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
            )
            .padding(vertical = 12.dp, horizontal = 6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IslamicHeaderCell(
                text = if (isArabic) "اليوم" else "Day",
                weight = 1.3f
            )
            IslamicHeaderCell(
                text = if (isArabic) "الفجر" else "Fajr",
                weight = 1f,
                isHighlighted = true,
                highlightColor = FajrHighlight
            )
            IslamicHeaderCell(
                text = if (isArabic) "الشروق" else "Sunrise",
                weight = 1f
            )
            IslamicHeaderCell(
                text = if (isArabic) "الظهر" else "Dhuhr",
                weight = 1f
            )
            IslamicHeaderCell(
                text = if (isArabic) "العصر" else "Asr",
                weight = 1f
            )
            IslamicHeaderCell(
                text = if (isArabic) "المغرب" else "Maghrib",
                weight = 1f,
                isHighlighted = true,
                highlightColor = MaghribHighlight
            )
            IslamicHeaderCell(
                text = if (isArabic) "العشاء" else "Isha",
                weight = 1f
            )
        }
    }
}

@Composable
private fun RowScope.IslamicHeaderCell(
    text: String,
    weight: Float,
    isHighlighted: Boolean = false,
    highlightColor: Color = FajrHighlight
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .then(
                if (isHighlighted) {
                    Modifier
                        .padding(horizontal = 2.dp)
                        .background(
                            color = highlightColor.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = RamadanGold.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(vertical = 4.dp)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isHighlighted) RamadanGoldLight else RamadanCream,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GoldDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        RamadanGold.copy(alpha = 0.6f),
                        RamadanGold,
                        RamadanGold.copy(alpha = 0.6f),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 8.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        RamadanGold.copy(alpha = 0.2f),
                        RamadanGold.copy(alpha = 0.3f),
                        RamadanGold.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
private fun IslamicDayRow(
    day: ImskaiyaDay,
    isCurrentDay: Boolean,
    isArabic: Boolean,
    useIndoArabic: Boolean,
    isEvenRow: Boolean
) {
    val dateFormatter = DateTimeFormatter.ofPattern("d/M")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm")

    val dayNumberStr = ArabicNumeralUtils.formatNumber(day.dayNumber, useIndoArabic)
    val dateStr = ArabicNumeralUtils.formatDate(day.gregorianDate.format(dateFormatter), useIndoArabic)

    val backgroundColor = when {
        isCurrentDay -> CurrentDayHighlight.copy(alpha = 0.5f)
        isEvenRow -> RamadanPurple.copy(alpha = 0.4f)
        else -> RamadanDarkPurple.copy(alpha = 0.2f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .then(
                if (isCurrentDay) {
                    Modifier
                        .border(
                            width = 1.dp,
                            color = RamadanGold.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp)
                        )
                } else Modifier
            )
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DateCell(
                dayNumber = dayNumberStr,
                date = dateStr,
                isCurrentDay = isCurrentDay,
                weight = 1.3f
            )

            IslamicTimeCell(
                time = ArabicNumeralUtils.formatTime(day.fajr.format(timeFormatter), useIndoArabic),
                weight = 1f,
                isHighlighted = true,
                highlightColor = FajrHighlight
            )

            IslamicTimeCell(
                time = ArabicNumeralUtils.formatTime(day.sunrise.format(timeFormatter), useIndoArabic),
                weight = 1f
            )

            IslamicTimeCell(
                time = ArabicNumeralUtils.formatTime(day.dhuhr.format(timeFormatter), useIndoArabic),
                weight = 1f
            )

            IslamicTimeCell(
                time = ArabicNumeralUtils.formatTime(day.asr.format(timeFormatter), useIndoArabic),
                weight = 1f
            )

            IslamicTimeCell(
                time = ArabicNumeralUtils.formatTime(day.maghrib.format(timeFormatter), useIndoArabic),
                weight = 1f,
                isHighlighted = true,
                highlightColor = MaghribHighlight
            )

            IslamicTimeCell(
                time = ArabicNumeralUtils.formatTime(day.isha.format(timeFormatter), useIndoArabic),
                weight = 1f
            )
        }
    }
}

@Composable
private fun RowScope.DateCell(
    dayNumber: String,
    date: String,
    isCurrentDay: Boolean,
    weight: Float
) {
    Box(
        modifier = Modifier.weight(weight),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dayNumber,
                color = if (isCurrentDay) RamadanGoldLight else RamadanGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = date,
                color = RamadanCreamDim,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun RowScope.IslamicTimeCell(
    time: String,
    weight: Float,
    isHighlighted: Boolean = false,
    highlightColor: Color = FajrHighlight
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isHighlighted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = highlightColor.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = highlightColor.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = time,
                    color = RamadanCream,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                text = time,
                color = RamadanCream,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
