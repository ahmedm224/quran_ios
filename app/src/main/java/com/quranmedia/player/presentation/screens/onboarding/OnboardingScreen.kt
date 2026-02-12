package com.quranmedia.player.presentation.screens.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    val context = LocalContext.current

    // Multiple permissions launcher (location + notifications)
    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.onMultiplePermissionsResult(permissions)
    }

    // Battery optimization launcher
    val powerManager = remember { context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager }
    var isBatteryOptimizationDisabled by remember {
        mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName))
    }
    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isBatteryOptimizationDisabled = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        viewModel.refreshBatteryOptimizationStatus()
    }

    // Track when completion process finishes
    val previouslyCompleting = remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isCompleting) {
        if (previouslyCompleting.value && !uiState.isCompleting) {
            onComplete()
        }
        previouslyCompleting.value = uiState.isCompleting
    }

    // Permission warning conditions
    val showPermissionWarning = !isBatteryOptimizationDisabled || !uiState.notificationPermissionGranted
    val allPermissionsGranted = uiState.locationPermissionGranted &&
            uiState.notificationPermissionGranted &&
            isBatteryOptimizationDisabled

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
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { viewModel.toggleLanguage() }) {
                        Text(
                            text = if (isArabic) "English" else "العربية",
                            fontSize = 14.sp
                        )
                    }
                }

                // Welcome message
                Text(
                    text = if (isArabic) "مرحباً بك في الفرقان" else "Welcome to Al-Furqan",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isArabic) "إعداد التطبيق" else "App Setup",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )

                Text(
                    text = if (isArabic)
                        "اسمح بالصلاحيات واختر إعداداتك"
                    else
                        "Allow permissions and choose your settings",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Permissions card - compact summary with single button
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (allPermissionsGranted) LightGreen else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Permission status rows
                            PermissionStatusRow(
                                icon = Icons.Default.LocationOn,
                                label = if (isArabic) "الموقع" else "Location",
                                isGranted = uiState.locationPermissionGranted,
                                extraInfo = if (uiState.isDetectingLocation) {
                                    if (isArabic) "جارٍ الكشف..." else "Detecting..."
                                } else {
                                    uiState.detectedLocationName
                                }
                            )

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                PermissionStatusRow(
                                    icon = Icons.Default.Notifications,
                                    label = if (isArabic) "الإشعارات" else "Notifications",
                                    isGranted = uiState.notificationPermissionGranted
                                )
                            }

                            PermissionStatusRow(
                                icon = Icons.Default.BatteryChargingFull,
                                label = if (isArabic) "البطارية" else "Battery",
                                isGranted = isBatteryOptimizationDisabled
                            )

                            // Single "Allow Permissions" button
                            if (!allPermissionsGranted) {
                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = {
                                        // Request location + notification permissions together
                                        val permissions = mutableListOf(Manifest.permission.ACCESS_COARSE_LOCATION)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        multiplePermissionsLauncher.launch(permissions.toTypedArray())

                                        // Battery optimization requires separate intent
                                        if (!isBatteryOptimizationDisabled) {
                                            val intent = Intent(
                                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            batteryOptimizationLauncher.launch(intent)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isArabic) "السماح بالصلاحيات" else "Allow Permissions",
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // Warning card for battery and notification
                    if (showPermissionWarning) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = LightOrange),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = WarningOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    if (!uiState.notificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        Text(
                                            text = if (isArabic)
                                                "بدون إذن الإشعارات، لن تتلقى تنبيهات الأذان"
                                            else
                                                "Without notification permission, you won't receive Athan alerts",
                                            fontSize = 12.sp,
                                            color = Color(0xFF795548)
                                        )
                                    }
                                    if (!isBatteryOptimizationDisabled) {
                                        if (!uiState.notificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                        Text(
                                            text = if (isArabic)
                                                "بدون استثناء البطارية، قد لا يعمل الأذان في الوقت المحدد"
                                            else
                                                "Without battery exemption, Athan may not play on time",
                                            fontSize = 12.sp,
                                            color = Color(0xFF795548)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Prayer Calculation Method
                    PrayerMethodSelector(
                        selectedMethod = uiState.selectedCalculationMethod,
                        isArabic = isArabic,
                        onMethodSelected = { viewModel.setCalculationMethod(it) }
                    )

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

                // Start button
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.completeOnboarding() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    enabled = !uiState.isCompleting
                ) {
                    if (uiState.isCompleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(if (isArabic) "ابدأ" else "Start")
                    }
                }

                // Settings hint
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isArabic)
                        "يمكنك تغيير الإعدادات لاحقاً من القائمة"
                    else
                        "You can change settings later from the menu",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * Compact permission status row - icon + label + check/x
 */
@Composable
private fun PermissionStatusRow(
    icon: ImageVector,
    label: String,
    isGranted: Boolean,
    extraInfo: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGranted) PrimaryGreen else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        if (extraInfo != null) {
            Text(
                text = extraInfo,
                fontSize = 12.sp,
                color = PrimaryGreen,
                fontWeight = FontWeight.Medium
            )
        }
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isGranted) PrimaryGreen else Color.LightGray,
            modifier = Modifier.size(20.dp)
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
