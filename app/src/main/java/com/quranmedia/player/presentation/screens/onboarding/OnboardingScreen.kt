package com.quranmedia.player.presentation.screens.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.presentation.util.layoutDirection

// Colors
private val PrimaryGreen = Color(0xFF1B5E20)
private val LightGreen = Color(0xFFE8F5E9)
private val WarningOrange = Color(0xFFFF9800)
private val LightOrange = Color(0xFFFFF3E0)

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val language = uiState.language
    val isArabic = language == AppLanguage.ARABIC

    // Permission launchers
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onLocationPermissionResult(granted)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    // Battery optimization launcher
    val context = LocalContext.current
    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Refresh status after returning from settings
        viewModel.refreshBatteryOptimizationStatus()
    }

    // Track when completion process finishes
    val previouslyCompleting = remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isCompleting) {
        if (previouslyCompleting.value && !uiState.isCompleting) {
            // Completion finished, navigate away
            onComplete()
        }
        previouslyCompleting.value = uiState.isCompleting
    }

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with language toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stage indicator
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StageIndicator(
                            isActive = uiState.currentStage == OnboardingStage.DOWNLOADS,
                            isCompleted = uiState.currentStage == OnboardingStage.PERMISSIONS
                        )
                        StageIndicator(
                            isActive = uiState.currentStage == OnboardingStage.PERMISSIONS,
                            isCompleted = false
                        )
                    }

                    // Language toggle
                    TextButton(onClick = { viewModel.toggleLanguage() }) {
                        Text(
                            text = if (isArabic) "English" else "العربية",
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Welcome message (only on Downloads stage)
                if (uiState.currentStage == OnboardingStage.DOWNLOADS) {
                    Text(
                        text = if (isArabic) "مرحباً بك في الفرقان" else "Welcome to Al-Furqan",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Title
                Text(
                    text = when (uiState.currentStage) {
                        OnboardingStage.DOWNLOADS -> if (isArabic) "إعداد التطبيق" else "App Setup"
                        OnboardingStage.PERMISSIONS -> if (isArabic) "الصلاحيات" else "Permissions"
                    },
                    fontSize = if (uiState.currentStage == OnboardingStage.DOWNLOADS) 16.sp else 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (uiState.currentStage == OnboardingStage.DOWNLOADS) Color.Gray else PrimaryGreen
                )

                Text(
                    text = when (uiState.currentStage) {
                        OnboardingStage.DOWNLOADS -> if (isArabic)
                            "حمّل الملفات المطلوبة واختر طريقة الحساب"
                        else
                            "Download required files and choose calculation method"
                        OnboardingStage.PERMISSIONS -> if (isArabic)
                            "اسمح بالصلاحيات للاستفادة الكاملة"
                        else
                            "Allow permissions for full features"
                    },
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Content based on stage
                AnimatedContent(
                    targetState = uiState.currentStage,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                    },
                    label = "stage_content"
                ) { stage ->
                    when (stage) {
                        OnboardingStage.DOWNLOADS -> DownloadsStage(
                            uiState = uiState,
                            isArabic = isArabic,
                            onDownloadV2 = { viewModel.downloadV2() },
                            onDownloadV4 = { viewModel.downloadV4() },
                            onDownloadTafseerArabic = { viewModel.downloadTafseerArabic() },
                            onDownloadTafseerEnglish = { viewModel.downloadTafseerEnglish() }
                        )
                        OnboardingStage.PERMISSIONS -> PermissionsStage(
                            uiState = uiState,
                            isArabic = isArabic,
                            onRequestLocation = {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            },
                            onRequestNotification = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onRequestBatteryOptimization = {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                batteryOptimizationLauncher.launch(intent)
                            },
                            onCalculationMethodSelected = { viewModel.setCalculationMethod(it) }
                        )
                    }
                }

                // Bottom buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Skip button (only on Downloads stage)
                    if (uiState.currentStage == OnboardingStage.DOWNLOADS) {
                        OutlinedButton(
                            onClick = { viewModel.goToNextStage() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.Gray
                            )
                        ) {
                            Text(if (isArabic) "تخطي" else "Skip")
                        }
                    }

                    // Continue/Finish button
                    Button(
                        onClick = {
                            if (uiState.currentStage == OnboardingStage.PERMISSIONS) {
                                viewModel.completeOnboarding()
                            } else {
                                viewModel.goToNextStage()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen
                        ),
                        enabled = !uiState.isCompleting
                    ) {
                        if (uiState.isCompleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = when (uiState.currentStage) {
                                    OnboardingStage.DOWNLOADS -> if (isArabic) "التالي" else "Next"
                                    OnboardingStage.PERMISSIONS -> if (isArabic) "ابدأ" else "Start"
                                }
                            )
                        }
                    }
                }

                // Settings hint
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isArabic)
                        "يمكنك التحميل لاحقاً من الإعدادات"
                    else
                        "You can download later from Settings",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            // Skip warning dialog
            if (uiState.showSkipWarning) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissSkipWarning() },
                    icon = { Icon(Icons.Default.Warning, null, tint = WarningOrange) },
                    title = {
                        Text(
                            if (isArabic) "تنبيه" else "Warning",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            if (isArabic)
                                "بدون خطوط المصحف، لن يُعرض القرآن بخط المصحف الأصيل. يمكنك التحميل لاحقاً من الإعدادات."
                            else
                                "Without Mushaf fonts, the Quran won't display in authentic Mushaf style. You can download later from Settings."
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.confirmSkip() },
                            colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
                        ) {
                            Text(if (isArabic) "تخطي على أي حال" else "Skip Anyway")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { viewModel.dismissSkipWarning() }) {
                            Text(if (isArabic) "العودة" else "Go Back")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StageIndicator(isActive: Boolean, isCompleted: Boolean) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(
                when {
                    isCompleted -> PrimaryGreen
                    isActive -> PrimaryGreen
                    else -> Color.LightGray
                }
            )
    )
}

@Composable
private fun DownloadsStage(
    uiState: OnboardingUiState,
    isArabic: Boolean,
    onDownloadV2: () -> Unit,
    onDownloadV4: () -> Unit,
    onDownloadTafseerArabic: () -> Unit,
    onDownloadTafseerEnglish: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // V2 - Required
        DownloadItem(
            state = uiState.v2State,
            isArabic = isArabic,
            onDownload = onDownloadV2,
            showRequiredBadge = true
        )

        // V4 - Optional
        DownloadItem(
            state = uiState.v4State,
            isArabic = isArabic,
            onDownload = onDownloadV4
        )

        // Arabic Tafseer - Optional
        DownloadItem(
            state = uiState.tafseerArabicState,
            isArabic = isArabic,
            onDownload = onDownloadTafseerArabic
        )

        // English Tafseer - Optional
        DownloadItem(
            state = uiState.tafseerEnglishState,
            isArabic = isArabic,
            onDownload = onDownloadTafseerEnglish
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerMethodSelector(
    selectedMethod: Int,
    isArabic: Boolean,
    onMethodSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Prayer calculation methods - IDs must match CalculationMethod enum in PrayerTimes.kt
    val methods = listOf(
        4 to ("أم القرى - مكة" to "Umm Al-Qura, Makkah"),
        3 to ("رابطة العالم الإسلامي" to "Muslim World League"),
        5 to ("الهيئة المصرية" to "Egyptian General Authority"),
        1 to ("جامعة كراتشي" to "Univ. of Karachi"),
        2 to ("أمريكا الشمالية" to "ISNA, North America"),
        7 to ("طهران" to "Tehran"),
        8 to ("الخليج" to "Gulf Region"),
        9 to ("الكويت" to "Kuwait"),
        10 to ("قطر" to "Qatar"),
        16 to ("دبي" to "Dubai"),
        11 to ("سنغافورة" to "Singapore"),
        12 to ("فرنسا" to "France"),
        13 to ("تركيا" to "Turkey"),
        14 to ("روسيا" to "Russia")
    )

    val selectedMethodName = methods.find { it.first == selectedMethod }?.let {
        if (isArabic) it.second.first else it.second.second
    } ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (isArabic) "طريقة حساب مواقيت الصلاة" else "Prayer Calculation Method",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryGreen
            )

            Spacer(modifier = Modifier.height(6.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedMethodName,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    singleLine = true
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    methods.forEach { (id, names) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isArabic) names.first else names.second,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                onMethodSelected(id)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadItem(
    state: DownloadItemState,
    isArabic: Boolean,
    onDownload: () -> Unit,
    showRequiredBadge: Boolean = false
) {
    val borderColor = when {
        state.isDownloaded -> PrimaryGreen
        state.isRequired -> WarningOrange
        else -> Color.LightGray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                state.isDownloaded -> LightGreen
                state.isRequired -> LightOrange.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Icon(
                imageVector = when {
                    state.isDownloaded -> Icons.Default.CheckCircle
                    state.id == "v2" || state.id == "v4" -> Icons.Default.FontDownload
                    else -> Icons.Default.MenuBook
                },
                contentDescription = null,
                tint = when {
                    state.isDownloaded -> PrimaryGreen
                    state.isRequired -> WarningOrange
                    else -> Color.Gray
                },
                modifier = Modifier.size(28.dp)
            )

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isArabic) state.nameArabic else state.nameEnglish,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (showRequiredBadge && !state.isDownloaded) {
                        Text(
                            text = if (isArabic) "مطلوب" else "Required",
                            fontSize = 9.sp,
                            color = Color.White,
                            modifier = Modifier
                                .background(WarningOrange, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = "${if (isArabic) state.descriptionArabic else state.description} (${state.sizeInfo})",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Progress bar
                if (state.isDownloading) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = PrimaryGreen
                    )
                }
            }

            // Download button
            if (!state.isDownloaded && !state.isDownloading) {
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = if (isArabic) "تحميل" else "Download",
                        tint = PrimaryGreen
                    )
                }
            } else if (state.isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = PrimaryGreen
                )
            } else {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionsStage(
    uiState: OnboardingUiState,
    isArabic: Boolean,
    onRequestLocation: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onCalculationMethodSelected: (Int) -> Unit
) {
    // Battery optimization - self-contained state and launcher
    val context = LocalContext.current
    val powerManager = remember { context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager }
    var isBatteryOptimizationDisabled by remember {
        mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName))
    }
    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isBatteryOptimizationDisabled = powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Location permission
        PermissionItem(
            icon = Icons.Default.LocationOn,
            title = if (isArabic) "الموقع" else "Location",
            description = if (isArabic)
                "لمعرفة مواقيت الصلاة في منطقتك"
            else
                "For prayer times in your area",
            isGranted = uiState.locationPermissionGranted,
            isLoading = uiState.isDetectingLocation,
            extraInfo = uiState.detectedLocationName,
            isArabic = isArabic,
            onRequest = onRequestLocation
        )

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionItem(
                icon = Icons.Default.Notifications,
                title = if (isArabic) "الإشعارات" else "Notifications",
                description = if (isArabic)
                    "للأذان وتذكير القراءة"
                else
                    "For Athan and reading reminders",
                isGranted = uiState.notificationPermissionGranted,
                isArabic = isArabic,
                onRequest = onRequestNotification
            )
        }

        // Battery optimization (for reliable Athan) - using local state
        PermissionItem(
            icon = Icons.Default.BatteryChargingFull,
            title = if (isArabic) "البطارية" else "Battery",
            description = if (isArabic)
                "لضمان عمل الأذان في الوقت المحدد"
            else
                "To ensure Athan plays on time",
            isGranted = isBatteryOptimizationDisabled,
            isArabic = isArabic,
            onRequest = {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}")
                )
                batteryLauncher.launch(intent)
            }
        )

        // Prayer Calculation Method
        PrayerMethodSelector(
            selectedMethod = uiState.selectedCalculationMethod,
            isArabic = isArabic,
            onMethodSelected = onCalculationMethodSelected
        )

        Spacer(modifier = Modifier.weight(1f))

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LightGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isArabic)
                        "سيتم تفعيل الأذان والصوت العالي وإسكات الهاتف بالقلب تلقائياً"
                    else
                        "Athan, max volume, and flip-to-silence will be enabled automatically",
                    fontSize = 12.sp,
                    color = PrimaryGreen
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    isLoading: Boolean = false,
    extraInfo: String? = null,
    isArabic: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isGranted && !isLoading) { onRequest() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) LightGreen else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) PrimaryGreen else Color.LightGray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) Color.White else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                if (extraInfo != null) {
                    Text(
                        text = extraInfo,
                        fontSize = 12.sp,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Status
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = PrimaryGreen
                )
            } else if (isGranted) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        if (isArabic) "السماح" else "Allow",
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
