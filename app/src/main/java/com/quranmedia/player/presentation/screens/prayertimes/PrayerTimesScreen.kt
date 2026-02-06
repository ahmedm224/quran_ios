package com.quranmedia.player.presentation.screens.prayertimes

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.domain.model.PrayerTimes
import com.quranmedia.player.domain.model.PrayerType
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.screens.reader.components.islamicGreen
import com.quranmedia.player.presentation.screens.reader.components.darkGreen
import com.quranmedia.player.presentation.screens.reader.components.lightGreen
import com.quranmedia.player.presentation.screens.reader.components.goldAccent
import com.quranmedia.player.presentation.screens.reader.components.creamBackground
import com.quranmedia.player.presentation.screens.reader.components.coffeeBrown
import com.quranmedia.player.presentation.screens.reader.components.softWoodBrown
import com.quranmedia.player.presentation.util.layoutDirection
import com.quranmedia.player.presentation.util.Strings
import com.quranmedia.player.presentation.components.CommonOverflowMenu
import com.quranmedia.player.domain.util.ArabicNumeralUtils
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Ramadan accent colors (subtle hints)
private val RamadanNight = Color(0xFF1A1A2E)
private val RamadanPurple = Color(0xFF2D2B55)
private val RamadanGold = Color(0xFFFFD700)

// Hijri months in order (for date adjustment calculations)
private val hijriMonthsEnglish = listOf(
    "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
    "Jumada al-Ula", "Jumada al-Thani", "Rajab", "Sha'ban",
    "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
)

private val hijriMonthsArabic = listOf(
    "محرم", "صفر", "ربيع الأول", "ربيع الثاني",
    "جمادى الأولى", "جمادى الثانية", "رجب", "شعبان",
    "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
)

// Data class for adjusted Hijri date
private data class AdjustedHijriDate(
    val day: Int,
    val monthEnglish: String,
    val monthArabic: String,
    val year: Int
)

/**
 * Calculate adjusted Hijri date with proper month/year boundary handling.
 * Hijri months alternate between 29 and 30 days (roughly), but we use 30 as default.
 * Uses monthNumber (1-12) directly to avoid string matching issues with transliteration.
 */
private fun calculateAdjustedHijriDate(
    originalDay: Int,
    originalMonthNumber: Int,
    originalYear: Int,
    adjustment: Int
): AdjustedHijriDate {
    // Convert 1-based month number to 0-based index
    val currentMonthIndex = (originalMonthNumber - 1).coerceIn(0, 11)

    if (adjustment == 0) {
        return AdjustedHijriDate(
            originalDay,
            hijriMonthsEnglish[currentMonthIndex],
            hijriMonthsArabic[currentMonthIndex],
            originalYear
        )
    }

    var newDay = originalDay + adjustment
    var newMonthIndex = currentMonthIndex
    var newYear = originalYear

    // Handle day overflow (going forward past end of month)
    while (newDay > 30) {
        newDay -= 30
        newMonthIndex++
        if (newMonthIndex > 11) {
            newMonthIndex = 0
            newYear++
        }
    }

    // Handle day underflow (going backward past start of month)
    while (newDay < 1) {
        newMonthIndex--
        if (newMonthIndex < 0) {
            newMonthIndex = 11
            newYear--
        }
        newDay += 30  // Previous month's days (simplified to 30)
    }

    return AdjustedHijriDate(
        day = newDay,
        monthEnglish = hijriMonthsEnglish[newMonthIndex],
        monthArabic = hijriMonthsArabic[newMonthIndex],
        year = newYear
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAthanSettings: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAthkar: () -> Unit = {},
    onNavigateToTracker: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToReading: () -> Unit = {},
    onNavigateToImsakiya: () -> Unit = {},
    viewModel: PrayerTimesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val language = settings.appLanguage

    var showLocationDialog by remember { mutableStateOf(false) }
    var cityInput by remember { mutableStateOf("") }

    // Snackbar for offline warnings
    val snackbarHostState = remember { SnackbarHostState() }

    // Show offline warning as snackbar
    LaunchedEffect(uiState.offlineWarning) {
        uiState.offlineWarning?.let { warning ->
            snackbarHostState.showSnackbar(
                message = warning,
                duration = SnackbarDuration.Short
            )
            viewModel.clearOfflineWarning()
        }
    }

    // Location permission launcher (approximate location is sufficient for prayer times)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onLocationPermissionGranted()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "مواقيت الصلاة" else "Prayer Times",
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        CommonOverflowMenu(
                            language = language,
                            onNavigateToSettings = onNavigateToSettings,
                            onNavigateToReading = onNavigateToReading,
                            onNavigateToImsakiya = onNavigateToImsakiya,
                            onNavigateToAthkar = onNavigateToAthkar,
                            onNavigateToTracker = onNavigateToTracker,
                            onNavigateToDownloads = onNavigateToDownloads,
                            onNavigateToAbout = onNavigateToAbout,
                            hidePrayerTimes = true  // Hide Prayer Times since we're on this screen
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = islamicGreen,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = darkGreen,
                        contentColor = Color.White
                    )
                }
            },
            containerColor = creamBackground
        ) { paddingValues ->
            val useIndoArabic = language == AppLanguage.ARABIC && settings.useIndoArabicNumerals
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Location card - compact
                LocationCard(
                    location = uiState.location,
                    hijriDate = uiState.prayerTimes?.hijriDate?.let { hijri ->
                        // Apply Hijri date adjustment with proper month/year handling
                        val adjusted = calculateAdjustedHijriDate(
                            originalDay = hijri.day,
                            originalMonthNumber = hijri.monthNumber,
                            originalYear = hijri.year,
                            adjustment = settings.hijriDateAdjustment
                        )
                        if (language == AppLanguage.ARABIC) {
                            val dayStr = ArabicNumeralUtils.formatNumber(adjusted.day, useIndoArabic)
                            val yearStr = ArabicNumeralUtils.formatNumber(adjusted.year, useIndoArabic)
                            "$dayStr ${adjusted.monthArabic} $yearStr"
                        } else {
                            "${adjusted.day} ${adjusted.monthEnglish} ${adjusted.year} AH"
                        }
                    },
                    language = language,
                    hasPermission = uiState.hasLocationPermission,
                    onDetectLocation = {
                        if (uiState.hasLocationPermission) {
                            viewModel.detectLocation()
                        } else {
                            locationPermissionLauncher.launch(
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        }
                    },
                    onManualLocation = { showLocationDialog = true },
                    useIndoArabic = useIndoArabic
                )

                // Loading indicator
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = islamicGreen)
                    }
                }

                // Error message
                uiState.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = error,
                            color = Color.Red,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }

                // Next prayer countdown
                uiState.prayerTimes?.let { prayerTimes ->
                    uiState.nextPrayer?.let { nextPrayer ->
                        NextPrayerCard(
                            nextPrayer = nextPrayer,
                            hoursRemaining = uiState.hoursRemaining,
                            minutesRemaining = uiState.minutesRemaining,
                            prayerTime = getPrayerTime(prayerTimes, nextPrayer),
                            language = language,
                            useIndoArabic = useIndoArabic
                        )
                    }

                    // All prayer times - takes remaining space
                    PrayerTimesCard(
                        prayerTimes = prayerTimes,
                        nextPrayer = uiState.nextPrayer,
                        language = language,
                        useIndoArabic = useIndoArabic,
                        modifier = Modifier.weight(1f)
                    )
                }

                // No location message
                if (uiState.location == null && !uiState.isLoading && uiState.error == null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = lightGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = islamicGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (language == AppLanguage.ARABIC)
                                    "يرجى تحديد موقعك للحصول على مواقيت الصلاة"
                                else
                                    "Please set your location to view prayer times",
                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                textAlign = TextAlign.Center,
                                color = islamicGreen
                            )
                        }
                    }
                }
            }
        }
    }

    // Location input dialog
    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = {
                Text(
                    text = if (language == AppLanguage.ARABIC) "أدخل المدينة" else "Enter City",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                )
            },
            text = {
                OutlinedTextField(
                    value = cityInput,
                    onValueChange = { cityInput = it },
                    label = {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "اسم المدينة" else "City Name"
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (cityInput.isNotBlank()) {
                            viewModel.setManualLocation(cityInput)
                            showLocationDialog = false
                            cityInput = ""
                        }
                    }
                ) {
                    Text(if (language == AppLanguage.ARABIC) "موافق" else "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text(if (language == AppLanguage.ARABIC) "إلغاء" else "Cancel")
                }
            }
        )
    }

}

@Composable
private fun LocationCard(
    location: com.quranmedia.player.domain.model.UserLocation?,
    hijriDate: String?,
    language: AppLanguage,
    hasPermission: Boolean,
    onDetectLocation: () -> Unit,
    onManualLocation: () -> Unit,
    useIndoArabic: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Hijri date and location
            Column(modifier = Modifier.weight(1f)) {
                // Hijri date - compact
                hijriDate?.let { date ->
                    Text(
                        text = date,
                        fontFamily = scheherazadeFont,
                        fontSize = 15.sp,
                        color = darkGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Location row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = islamicGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = location?.cityName
                            ?: if (language == AppLanguage.ARABIC) "الموقع غير محدد" else "Location not set",
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = coffeeBrown
                    )
                    location?.countryName?.let { country ->
                        Text(
                            text = ", $country",
                            fontSize = 12.sp,
                            color = islamicGreen
                        )
                    }
                }
            }

            // Right side: compact location buttons
            Row {
                IconButton(
                    onClick = onDetectLocation,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = "Detect Location",
                        tint = islamicGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onManualLocation,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Manual Location",
                        tint = islamicGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NextPrayerCard(
    nextPrayer: PrayerType,
    hoursRemaining: Int,
    minutesRemaining: Int,
    prayerTime: LocalTime,
    language: AppLanguage,
    useIndoArabic: Boolean = false
) {
    // Format time remaining based on language
    val timeRemaining = if (language == AppLanguage.ARABIC) {
        val hoursStr = ArabicNumeralUtils.formatNumber(hoursRemaining, useIndoArabic)
        val minutesStr = ArabicNumeralUtils.formatNumber(minutesRemaining, useIndoArabic)
        if (hoursRemaining > 0) {
            "$hoursStr س $minutesStr د"
        } else {
            "$minutesStr د"
        }
    } else {
        if (hoursRemaining > 0) {
            "${hoursRemaining}h ${minutesRemaining}m"
        } else {
            "${minutesRemaining}m"
        }
    }

    // Check if it's Fajr or Maghrib (important for Ramadan fasting)
    val isRamadanPrayer = nextPrayer == PrayerType.FAJR || nextPrayer == PrayerType.MAGHRIB

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = if (isRamadanPrayer) {
                            listOf(RamadanNight, RamadanPurple)
                        } else {
                            listOf(islamicGreen, darkGreen)
                        }
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - prayer icon and info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Compact prayer icon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (isRamadanPrayer) RamadanGold.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(
                            imageVector = getIconForPrayer(nextPrayer),
                            contentDescription = null,
                            tint = if (isRamadanPrayer) RamadanGold else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "الصلاة القادمة" else "Next Prayer",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (language == AppLanguage.ARABIC)
                                    nextPrayer.nameArabic
                                else
                                    nextPrayer.nameEnglish,
                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formatPrayerTime(prayerTime, language, useIndoArabic),
                                fontSize = 13.sp,
                                color = if (isRamadanPrayer) RamadanGold.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Right side - countdown compact
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isRamadanPrayer) RamadanGold.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "متبقي" else "in",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = timeRemaining,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRamadanPrayer) RamadanGold else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerTimesCard(
    prayerTimes: PrayerTimes,
    nextPrayer: PrayerType?,
    language: AppLanguage,
    useIndoArabic: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Islamic decorative header - compact
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                RamadanPurple.copy(alpha = 0.08f),
                                goldAccent.copy(alpha = 0.15f),
                                islamicGreen.copy(alpha = 0.08f),
                                goldAccent.copy(alpha = 0.15f),
                                RamadanPurple.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = null,
                        tint = goldAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.ARABIC) "أوقات الصلاة" else "Prayer Times",
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "✦",
                        color = goldAccent,
                        fontSize = 12.sp
                    )
                }
            }

            // Decorative gold line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                goldAccent.copy(alpha = 0.4f),
                                goldAccent.copy(alpha = 0.8f),
                                goldAccent.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Prayer times list - fill remaining space evenly
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                PrayerTimeRow(PrayerType.FAJR, prayerTimes.fajr, nextPrayer == PrayerType.FAJR, language, useIndoArabic, isRamadanPrayer = true)
                PrayerTimeRow(PrayerType.SUNRISE, prayerTimes.sunrise, nextPrayer == PrayerType.SUNRISE, language, useIndoArabic)
                PrayerTimeRow(PrayerType.DHUHR, prayerTimes.dhuhr, nextPrayer == PrayerType.DHUHR, language, useIndoArabic)
                PrayerTimeRow(PrayerType.ASR, prayerTimes.asr, nextPrayer == PrayerType.ASR, language, useIndoArabic)
                PrayerTimeRow(PrayerType.MAGHRIB, prayerTimes.maghrib, nextPrayer == PrayerType.MAGHRIB, language, useIndoArabic, isRamadanPrayer = true)
                PrayerTimeRow(PrayerType.ISHA, prayerTimes.isha, nextPrayer == PrayerType.ISHA, language, useIndoArabic)
            }

            // Bottom decorative element - compact
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                goldAccent.copy(alpha = 0.1f),
                                goldAccent.copy(alpha = 0.2f),
                                goldAccent.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "☪ ۩ ☪",
                    color = goldAccent.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun PrayerTimeRow(
    prayerType: PrayerType,
    time: LocalTime,
    isNext: Boolean,
    language: AppLanguage,
    useIndoArabic: Boolean = false,
    isRamadanPrayer: Boolean = false
) {
    val backgroundColor = when {
        isNext -> goldAccent.copy(alpha = 0.2f)
        isRamadanPrayer -> RamadanPurple.copy(alpha = 0.06f)
        else -> Color.Transparent
    }

    val iconTint = when {
        isNext -> islamicGreen
        isRamadanPrayer -> RamadanPurple.copy(alpha = 0.7f)
        else -> softWoodBrown
    }

    val textColor = when {
        isNext -> islamicGreen
        isRamadanPrayer -> RamadanNight
        else -> coffeeBrown
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Compact icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = if (isNext) islamicGreen.copy(alpha = 0.1f)
                               else if (isRamadanPrayer) RamadanPurple.copy(alpha = 0.08f)
                               else softWoodBrown.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForPrayer(prayerType),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            // Prayer name and optional Ramadan label
            Text(
                text = if (language == AppLanguage.ARABIC)
                    prayerType.nameArabic
                else
                    prayerType.nameEnglish,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 14.sp,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
            // Show Ramadan label inline for Fajr/Maghrib
            if (isRamadanPrayer) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (prayerType == PrayerType.FAJR) {
                        if (language == AppLanguage.ARABIC) "(إمساك)" else "(Suhoor)"
                    } else {
                        if (language == AppLanguage.ARABIC) "(إفطار)" else "(Iftar)"
                    },
                    fontSize = 10.sp,
                    color = RamadanPurple.copy(alpha = 0.6f)
                )
            }
        }

        // Time display - compact
        Box(
            modifier = Modifier
                .background(
                    color = if (isNext) islamicGreen.copy(alpha = 0.1f)
                           else if (isRamadanPrayer) RamadanGold.copy(alpha = 0.1f)
                           else Color.Transparent,
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = formatPrayerTime(time, language, useIndoArabic),
                fontSize = 14.sp,
                fontWeight = if (isNext || isRamadanPrayer) FontWeight.Bold else FontWeight.Medium,
                color = if (isNext) islamicGreen else if (isRamadanPrayer) RamadanNight else coffeeBrown
            )
        }
    }
}

private fun formatPrayerTime(time: LocalTime, language: AppLanguage, useIndoArabic: Boolean = false): String {
    val timeStr = if (language == AppLanguage.ARABIC) {
        // Arabic format: use صباحاً (morning) and مساءً (evening) instead of AM/PM
        val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
        val minute = time.minute.toString().padStart(2, '0')
        val period = if (time.hour < 12) "صباحاً" else "مساءً"
        "$hour:$minute $period"
    } else {
        time.format(DateTimeFormatter.ofPattern("hh:mm a"))
    }
    return if (useIndoArabic) ArabicNumeralUtils.toIndoArabic(timeStr) else timeStr
}

private fun getIconForPrayer(prayerType: PrayerType): ImageVector {
    return when (prayerType) {
        PrayerType.FAJR -> Icons.Default.NightsStay
        PrayerType.SUNRISE -> Icons.Default.WbSunny
        PrayerType.DHUHR -> Icons.Default.WbSunny
        PrayerType.ASR -> Icons.Default.WbTwilight
        PrayerType.MAGHRIB -> Icons.Default.WbTwilight
        PrayerType.ISHA -> Icons.Default.NightsStay
    }
}

private fun getPrayerTime(prayerTimes: PrayerTimes, prayerType: PrayerType): LocalTime {
    return when (prayerType) {
        PrayerType.FAJR -> prayerTimes.fajr
        PrayerType.SUNRISE -> prayerTimes.sunrise
        PrayerType.DHUHR -> prayerTimes.dhuhr
        PrayerType.ASR -> prayerTimes.asr
        PrayerType.MAGHRIB -> prayerTimes.maghrib
        PrayerType.ISHA -> prayerTimes.isha
    }
}
