package com.quranmedia.player.wear.presentation.screens.prayertimes

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.quranmedia.player.wear.domain.model.AppLanguage
import com.quranmedia.player.wear.domain.model.AsrJuristicMethod
import com.quranmedia.player.wear.domain.model.CalculationMethod
import com.quranmedia.player.wear.presentation.theme.ScheherazadeFont
import com.quranmedia.player.wear.presentation.theme.WearAthkarColors
import kotlinx.coroutines.launch

@Composable
fun PrayerSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: PrayerSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberScalingLazyListState()
    val scope = rememberCoroutineScope()
    val isArabic = uiState.appLanguage == AppLanguage.ARABIC

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.onLocationPermissionGranted()
        }
    }

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(WearAthkarColors.Background)
        ) {
            // Header
            item {
                ListHeader {
                    Text(
                        text = if (isArabic) "الإعدادات" else "Settings",
                        fontFamily = if (isArabic) ScheherazadeFont else null,
                        fontSize = 16.sp
                    )
                }
            }

            // Language Section Header
            item {
                Text(
                    text = if (isArabic) "اللغة" else "Language",
                    fontFamily = if (isArabic) ScheherazadeFont else null,
                    fontSize = 12.sp,
                    color = WearAthkarColors.OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Language Options
            items(AppLanguage.entries) { language ->
                val isSelected = uiState.appLanguage == language
                Chip(
                    onClick = {
                        scope.launch {
                            viewModel.setAppLanguage(language)
                        }
                    },
                    label = {
                        Text(
                            text = language.nativeName,
                            fontFamily = if (language == AppLanguage.ARABIC) ScheherazadeFont else null,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = if (isSelected) {
                        ChipDefaults.chipColors(
                            backgroundColor = WearAthkarColors.Primary.copy(alpha = 0.5f)
                        )
                    } else {
                        ChipDefaults.secondaryChipColors()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            // Location Section Header (moved after Language)
            item {
                Text(
                    text = if (isArabic) "الموقع" else "Location",
                    fontFamily = if (isArabic) ScheherazadeFont else null,
                    fontSize = 12.sp,
                    color = WearAthkarColors.OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Location Display
            item {
                Text(
                    text = uiState.locationName,
                    fontSize = 14.sp,
                    color = WearAthkarColors.Primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Detect Location Button
            item {
                Chip(
                    onClick = {
                        if (uiState.hasLocationPermission) {
                            scope.launch {
                                viewModel.detectLocation()
                            }
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    enabled = !uiState.isDetectingLocation,
                    label = {
                        if (uiState.isDetectingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isArabic) "تحديث الموقع" else "Detect Location",
                                fontFamily = if (isArabic) ScheherazadeFont else null,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            // Calculation Method Section Header
            item {
                Text(
                    text = if (isArabic) "طريقة الحساب" else "Calculation Method",
                    fontFamily = if (isArabic) ScheherazadeFont else null,
                    fontSize = 12.sp,
                    color = WearAthkarColors.OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Calculation Methods
            items(CalculationMethod.entries) { method ->
                val isSelected = uiState.calculationMethod == method
                Chip(
                    onClick = {
                        scope.launch {
                            viewModel.setCalculationMethod(method)
                        }
                    },
                    label = {
                        Text(
                            text = if (isArabic) method.nameArabic else method.nameEnglish,
                            fontFamily = if (isArabic) ScheherazadeFont else null,
                            fontSize = 14.sp,
                            textAlign = if (isArabic) TextAlign.End else TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = if (isSelected) {
                        ChipDefaults.chipColors(
                            backgroundColor = WearAthkarColors.Primary.copy(alpha = 0.5f)
                        )
                    } else {
                        ChipDefaults.secondaryChipColors()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            // Asr Method Header
            item {
                Text(
                    text = if (isArabic) "حساب العصر" else "Asr Calculation",
                    fontFamily = if (isArabic) ScheherazadeFont else null,
                    fontSize = 12.sp,
                    color = WearAthkarColors.OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Asr Methods
            items(AsrJuristicMethod.entries) { method ->
                val isSelected = uiState.asrMethod == method
                val displayName = when (method) {
                    AsrJuristicMethod.SHAFI -> if (isArabic) "الشافعي / المالكي / الحنبلي" else "Shafi'i / Maliki / Hanbali"
                    AsrJuristicMethod.HANAFI -> if (isArabic) "الحنفي" else "Hanafi"
                }
                Chip(
                    onClick = {
                        scope.launch {
                            viewModel.setAsrMethod(method)
                        }
                    },
                    label = {
                        Text(
                            text = displayName,
                            fontFamily = if (isArabic) ScheherazadeFont else null,
                            fontSize = 14.sp,
                            textAlign = if (isArabic) TextAlign.End else TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = if (isSelected) {
                        ChipDefaults.chipColors(
                            backgroundColor = WearAthkarColors.Primary.copy(alpha = 0.5f)
                        )
                    } else {
                        ChipDefaults.secondaryChipColors()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
