package com.quranmedia.player.presentation.screens.prayertimes

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.quranmedia.player.presentation.util.layoutDirection
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Theme colors - Islamic green
private val islamicGreen = Color(0xFF2E7D32)
private val lightGreen = Color(0xFFE8F5E9)
private val darkGreen = Color(0xFF1B5E20)
private val creamBackground = Color(0xFFFAF8F3)
private val goldAccent = Color(0xFFD4AF37)

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
 */
private fun calculateAdjustedHijriDate(
    originalDay: Int,
    originalMonthEnglish: String,
    originalMonthArabic: String,
    originalYear: Int,
    adjustment: Int
): AdjustedHijriDate {
    if (adjustment == 0) {
        return AdjustedHijriDate(originalDay, originalMonthEnglish, originalMonthArabic, originalYear)
    }

    // Find current month index
    val currentMonthIndex = hijriMonthsEnglish.indexOfFirst {
        it.equals(originalMonthEnglish, ignoreCase = true)
    }.takeIf { it >= 0 } ?: hijriMonthsArabic.indexOfFirst {
        it == originalMonthArabic
    }.takeIf { it >= 0 } ?: 0

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
    viewModel: PrayerTimesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val language = settings.appLanguage

    var showLocationDialog by remember { mutableStateOf(false) }
    var cityInput by remember { mutableStateOf("") }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
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
                        IconButton(onClick = onNavigateToAthanSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Prayer Settings",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = islamicGreen,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = creamBackground
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Location card
                item {
                    LocationCard(
                        location = uiState.location,
                        hijriDate = uiState.prayerTimes?.hijriDate?.let { hijri ->
                            // Apply Hijri date adjustment with proper month/year handling
                            val adjusted = calculateAdjustedHijriDate(
                                originalDay = hijri.day,
                                originalMonthEnglish = hijri.month,
                                originalMonthArabic = hijri.monthArabic,
                                originalYear = hijri.year,
                                adjustment = settings.hijriDateAdjustment
                            )
                            if (language == AppLanguage.ARABIC) {
                                "${adjusted.day} ${adjusted.monthArabic} ${adjusted.year}"
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
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        onManualLocation = { showLocationDialog = true }
                    )
                }

                // Loading indicator
                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = islamicGreen)
                        }
                    }
                }

                // Error message
                uiState.error?.let { error ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = error,
                                color = Color.Red,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Next prayer countdown
                uiState.prayerTimes?.let { prayerTimes ->
                    uiState.nextPrayer?.let { nextPrayer ->
                        item {
                            NextPrayerCard(
                                nextPrayer = nextPrayer,
                                timeRemaining = uiState.timeToNextPrayer,
                                prayerTime = getPrayerTime(prayerTimes, nextPrayer),
                                language = language
                            )
                        }
                    }

                    // All prayer times
                    item {
                        PrayerTimesCard(
                            prayerTimes = prayerTimes,
                            nextPrayer = uiState.nextPrayer,
                            language = language
                        )
                    }
                }

                // No location message
                if (uiState.location == null && !uiState.isLoading && uiState.error == null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = lightGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
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
    onManualLocation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Hijri date - prominent at top (adjustment is in Settings)
            hijriDate?.let { date ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    goldAccent.copy(alpha = 0.15f),
                                    goldAccent.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date,
                        fontFamily = scheherazadeFont,
                        fontSize = 18.sp,
                        color = darkGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Location row - compact
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = islamicGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = location?.cityName
                            ?: if (language == AppLanguage.ARABIC) "الموقع غير محدد" else "Location not set",
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = darkGreen
                    )
                    location?.countryName?.let { country ->
                        Text(
                            text = " - $country",
                            fontSize = 13.sp,
                            color = islamicGreen
                        )
                    }
                }

                // Compact location buttons
                Row {
                    IconButton(
                        onClick = onDetectLocation,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Detect Location",
                            tint = islamicGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onManualLocation,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Manual Location",
                            tint = islamicGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NextPrayerCard(
    nextPrayer: PrayerType,
    timeRemaining: String,
    prayerTime: LocalTime,
    language: AppLanguage
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(islamicGreen, darkGreen)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - prayer name and time
                Column {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "الصلاة القادمة" else "Next Prayer",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = if (language == AppLanguage.ARABIC)
                            nextPrayer.nameArabic
                        else
                            nextPrayer.nameEnglish,
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = formatPrayerTime(prayerTime, language),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                // Right side - countdown
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "متبقي" else "in",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = timeRemaining,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
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
    language: AppLanguage
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Islamic decorative header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                goldAccent.copy(alpha = 0.2f),
                                islamicGreen.copy(alpha = 0.1f),
                                goldAccent.copy(alpha = 0.2f)
                            )
                        )
                    )
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Left ornament
                    Text(
                        text = "✦",
                        color = goldAccent,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (language == AppLanguage.ARABIC) "أوقات الصلاة" else "Prayer Times",
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkGreen
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    // Right ornament
                    Text(
                        text = "✦",
                        color = goldAccent,
                        fontSize = 14.sp
                    )
                }
            }

            // Decorative line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                goldAccent.copy(alpha = 0.5f),
                                goldAccent,
                                goldAccent.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Prayer times with Islamic frame styling
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                PrayerTimeRow(PrayerType.FAJR, prayerTimes.fajr, nextPrayer == PrayerType.FAJR, language)
                IslamicDivider()
                PrayerTimeRow(PrayerType.SUNRISE, prayerTimes.sunrise, nextPrayer == PrayerType.SUNRISE, language)
                IslamicDivider()
                PrayerTimeRow(PrayerType.DHUHR, prayerTimes.dhuhr, nextPrayer == PrayerType.DHUHR, language)
                IslamicDivider()
                PrayerTimeRow(PrayerType.ASR, prayerTimes.asr, nextPrayer == PrayerType.ASR, language)
                IslamicDivider()
                PrayerTimeRow(PrayerType.MAGHRIB, prayerTimes.maghrib, nextPrayer == PrayerType.MAGHRIB, language)
                IslamicDivider()
                PrayerTimeRow(PrayerType.ISHA, prayerTimes.isha, nextPrayer == PrayerType.ISHA, language)
            }

            // Bottom decorative line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                goldAccent.copy(alpha = 0.5f),
                                goldAccent,
                                goldAccent.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Bottom ornament
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "❀ ۩ ❀",
                    color = goldAccent.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun IslamicDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        islamicGreen.copy(alpha = 0.2f),
                        islamicGreen.copy(alpha = 0.3f),
                        islamicGreen.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
private fun PrayerTimeRow(
    prayerType: PrayerType,
    time: LocalTime,
    isNext: Boolean,
    language: AppLanguage
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isNext) lightGreen else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = getIconForPrayer(prayerType),
                contentDescription = null,
                tint = if (isNext) islamicGreen else Color.Gray.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (language == AppLanguage.ARABIC)
                    prayerType.nameArabic
                else
                    prayerType.nameEnglish,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 15.sp,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                color = if (isNext) islamicGreen else darkGreen.copy(alpha = 0.8f)
            )
        }
        Text(
            text = formatPrayerTime(time, language),
            fontSize = 15.sp,
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
            color = if (isNext) islamicGreen else darkGreen.copy(alpha = 0.8f)
        )
    }
}

private fun formatPrayerTime(time: LocalTime, language: AppLanguage): String {
    return if (language == AppLanguage.ARABIC) {
        // Arabic format: use صباحاً (morning) and مساءً (evening) instead of AM/PM
        val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
        val minute = time.minute.toString().padStart(2, '0')
        val period = if (time.hour < 12) "صباحاً" else "مساءً"
        "$hour:$minute $period"
    } else {
        time.format(DateTimeFormatter.ofPattern("hh:mm a"))
    }
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
