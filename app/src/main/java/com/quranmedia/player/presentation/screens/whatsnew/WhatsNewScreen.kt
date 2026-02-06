package com.quranmedia.player.presentation.screens.whatsnew

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.lazy.items
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
import com.quranmedia.player.BuildConfig
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.data.repository.PrayerNotificationMode
import com.quranmedia.player.data.source.FontDownloadState
import com.quranmedia.player.domain.model.CalculationMethod
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.screens.reader.components.islamicGreen
import com.quranmedia.player.presentation.screens.reader.components.darkGreen
import com.quranmedia.player.presentation.screens.reader.components.goldAccent
import com.quranmedia.player.presentation.screens.reader.components.creamBackground
import com.quranmedia.player.presentation.util.layoutDirection

// WhatsNew-specific light green for backgrounds (lighter than shared lightGreen)
private val lightGreen = Color(0xFFE8F5E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewScreen(
    onComplete: () -> Unit,
    viewModel: WhatsNewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val language = uiState.language

    // Font download progress
    val v2Progress by viewModel.v2DownloadProgress.collectAsState()
    val v4Progress by viewModel.v4DownloadProgress.collectAsState()

    // Make downloaded state reactive - updates when progress state changes
    val isV2Downloaded = v2Progress.state == FontDownloadState.DOWNLOADED || viewModel.isV2Downloaded()
    val isV4Downloaded = v4Progress.state == FontDownloadState.DOWNLOADED || viewModel.isV4Downloaded()

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

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Scaffold(
            containerColor = creamBackground
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Language Toggle
                item {
                    LanguageToggle(
                        language = language,
                        onToggle = { viewModel.toggleLanguage() }
                    )
                }

                // Header Section
                item {
                    WhatsNewHeader(
                        language = language,
                        isFirstInstall = uiState.isFirstInstall
                    )
                }

                // Feature Highlights Section
                item {
                    FeatureHighlightsSection(
                        language = language,
                        versionName = BuildConfig.VERSION_NAME
                    )
                }

                // Mushaf Font Download Section
                item {
                    MushafFontDownloadSection(
                        language = language,
                        isV2Downloaded = isV2Downloaded,
                        isV4Downloaded = isV4Downloaded,
                        v2Progress = v2Progress,
                        v4Progress = v4Progress,
                        onDownloadV2 = { viewModel.downloadV2Fonts() },
                        onDownloadV4 = { viewModel.downloadV4Fonts() }
                    )
                }

                // Permission Request Cards
                if (!uiState.locationPermissionGranted) {
                    item {
                        PermissionCard(
                            language = language,
                            icon = Icons.Default.LocationOn,
                            title = if (language == AppLanguage.ARABIC) "الموقع للصلاة" else "Location for Prayer",
                            description = if (language == AppLanguage.ARABIC)
                                "للحصول على مواقيت الصلاة الدقيقة لمنطقتك"
                                else "Get accurate prayer times for your location",
                            onRequestPermission = {
                                locationPermissionLauncher.launch(
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            }
                        )
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !uiState.notificationPermissionGranted) {
                    item {
                        PermissionCard(
                            language = language,
                            icon = Icons.Default.Notifications,
                            title = if (language == AppLanguage.ARABIC) "الإشعارات" else "Notifications",
                            description = if (language == AppLanguage.ARABIC)
                                "لتلقي تنبيهات الأذان والتذكيرات"
                                else "Receive prayer time notifications and reminders",
                            onRequestPermission = {
                                notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            }
                        )
                    }
                }

                // Prayer Calculation Method Section
                item {
                    PrayerMethodSection(
                        language = language,
                        selectedMethodId = uiState.selectedPrayerMethod,
                        onMethodSelected = viewModel::onPrayerMethodSelected
                    )
                }

                // Prayer Notification Mode Section
                item {
                    PrayerNotificationModeSection(
                        language = language,
                        selectedMode = uiState.selectedPrayerNotificationMode,
                        onModeSelected = viewModel::onPrayerNotificationModeSelected
                    )
                }

                // Action Buttons
                item {
                    WhatsNewActions(
                        language = language,
                        onSkip = {
                            viewModel.markAsComplete()
                            onComplete()
                        },
                        onContinue = {
                            viewModel.markAsComplete()
                            onComplete()
                        }
                    )
                }

                // Bottom spacing
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun WhatsNewHeader(
    language: AppLanguage,
    isFirstInstall: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Logo Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(islamicGreen, darkGreen)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = if (language == AppLanguage.ARABIC) {
                if (isFirstInstall) "مرحباً بك في الفرقان" else "ما الجديد في الفرقان"
            } else {
                if (isFirstInstall) "Welcome to Alfurqan" else "What's New in Alfurqan"
            },
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
            color = darkGreen,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Version badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = lightGreen,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = if (language == AppLanguage.ARABIC)
                    "الإصدار 2"
                    else "Version 2",
                fontSize = 14.sp,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                color = darkGreen,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun FeatureHighlightsSection(
    language: AppLanguage,
    versionName: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (language == AppLanguage.ARABIC) "المميزات الجديدة" else "New Features",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                color = islamicGreen
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Version-specific features
            getFeaturesList(versionName, language).forEach { feature ->
                FeatureItem(
                    icon = feature.icon,
                    title = feature.title,
                    description = feature.description,
                    language = language
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String,
    language: AppLanguage
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(lightGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = islamicGreen,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                color = darkGreen
            )
            Text(
                text = description,
                fontSize = 12.sp,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                color = Color.Gray,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun PermissionCard(
    language: AppLanguage,
    icon: ImageVector,
    title: String,
    description: String,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(lightGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = islamicGreen,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    color = darkGreen
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = islamicGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (language == AppLanguage.ARABIC) "السماح" else "Allow",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerMethodSection(
    language: AppLanguage,
    selectedMethodId: Int,
    onMethodSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val methods = CalculationMethod.entries
    val selectedMethod = CalculationMethod.fromId(selectedMethodId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(lightGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = islamicGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (language == AppLanguage.ARABIC)
                            "طريقة حساب الصلاة"
                            else "Prayer Calculation Method",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        color = darkGreen
                    )
                    Text(
                        text = if (language == AppLanguage.ARABIC)
                            "اختر الطريقة المناسبة لمنطقتك"
                            else "Choose the method for your region",
                        fontSize = 14.sp,
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = if (language == AppLanguage.ARABIC)
                        selectedMethod.nameArabic
                        else selectedMethod.nameEnglish,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = islamicGreen,
                        unfocusedBorderColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    methods.forEach { method ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (language == AppLanguage.ARABIC)
                                        method.nameArabic
                                        else method.nameEnglish,
                                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                                )
                            },
                            onClick = {
                                onMethodSelected(method.id)
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
private fun PrayerNotificationModeSection(
    language: AppLanguage,
    selectedMode: PrayerNotificationMode,
    onModeSelected: (PrayerNotificationMode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(lightGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = islamicGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (language == AppLanguage.ARABIC)
                            "تنبيهات الصلاة"
                            else "Prayer Alerts",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        color = darkGreen
                    )
                    Text(
                        text = if (language == AppLanguage.ARABIC)
                            "اختر كيف تريد التنبيه لكل الصلوات"
                            else "Choose how you want to be notified for all prayers",
                        fontSize = 14.sp,
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode selection chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrayerNotificationMode.entries.forEach { mode ->
                    val isSelected = mode == selectedMode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onModeSelected(mode) },
                        label = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = when (mode) {
                                        PrayerNotificationMode.ATHAN -> Icons.Default.VolumeUp
                                        PrayerNotificationMode.NOTIFICATION -> Icons.Default.NotificationsActive
                                        PrayerNotificationMode.SILENT -> Icons.Default.NotificationsOff
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSelected) Color.White else islamicGreen
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (language == AppLanguage.ARABIC) mode.arabicLabel else mode.englishLabel,
                                    fontSize = 12.sp,
                                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                    textAlign = TextAlign.Center
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = islamicGreen,
                            selectedLabelColor = Color.White,
                            containerColor = Color.Gray.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Description based on selected mode
            if (selectedMode != PrayerNotificationMode.SILENT) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (selectedMode) {
                        PrayerNotificationMode.ATHAN -> if (language == AppLanguage.ARABIC)
                            "✓ سيتم تشغيل الأذان بصوت المؤذن في أوقات الصلاة"
                        else
                            "✓ Full Athan will play at prayer times"
                        PrayerNotificationMode.NOTIFICATION -> if (language == AppLanguage.ARABIC)
                            "✓ ستظهر إشعارات بسيطة في أوقات الصلاة"
                        else
                            "✓ Simple notifications will appear at prayer times"
                        else -> ""
                    },
                    fontSize = 12.sp,
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    color = islamicGreen,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun WhatsNewActions(
    language: AppLanguage,
    onSkip: () -> Unit,
    onContinue: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = islamicGreen
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, islamicGreen)
        ) {
            Text(
                text = if (language == AppLanguage.ARABIC) "تخطي" else "Skip",
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Button(
            onClick = onContinue,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = islamicGreen
            )
        ) {
            Text(
                text = if (language == AppLanguage.ARABIC) "ابدأ" else "Continue",
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

// Feature data class
private data class Feature(
    val icon: ImageVector,
    val title: String,
    val description: String
)

// Version-specific features
private fun getFeaturesList(versionName: String, language: AppLanguage): List<Feature> {
    // Version 2.0.0 features
    return if (language == AppLanguage.ARABIC) {
        listOf(
            Feature(
                icon = Icons.Default.MenuBook,
                title = "خطوط المصحف الشريف",
                description = "604 خط مخصص لكل صفحة للحصول على عرض مثالي للمصحف"
            ),
            Feature(
                icon = Icons.Default.Palette,
                title = "مصحف التجويد",
                description = "604 صفحة بألوان أحكام التجويد"
            ),
            Feature(
                icon = Icons.Default.Repeat,
                title = "التلاوة المتكررة",
                description = "كرر الآيات لحفظ أفضل"
            ),
            Feature(
                icon = Icons.Default.Notifications,
                title = "تنبيهات الصلاة",
                description = "اختر: صامت، إشعار، أو أذان كامل مع مؤذن واحد"
            ),
            Feature(
                icon = Icons.Default.Edit,
                title = "الأذكار",
                description = "أذكار الصباح والمساء وبعد الصلاة"
            ),
            Feature(
                icon = Icons.Default.CalendarMonth,
                title = "متتبع القراءة",
                description = "تتبع تقدمك وأهداف الختمة"
            ),
            Feature(
                icon = Icons.Default.Info,
                title = "التفسير",
                description = "تفسير ابن كثير وغيره بالعربية والإنجليزية"
            ),
            Feature(
                icon = Icons.Default.Abc,
                title = "المفردات",
                description = "معاني كلمات القرآن الكريم"
            )
        )
    } else {
        listOf(
            Feature(
                icon = Icons.Default.MenuBook,
                title = "Mushaf Fonts",
                description = "604 per-page fonts for pixel-perfect Mushaf display"
            ),
            Feature(
                icon = Icons.Default.Palette,
                title = "Tajweed Mushaf",
                description = "604 pages with Tajweed color-coded rules"
            ),
            Feature(
                icon = Icons.Default.Repeat,
                title = "Repeat Mode",
                description = "Repeat ayahs for better memorization"
            ),
            Feature(
                icon = Icons.Default.Notifications,
                title = "Prayer Alerts",
                description = "Choose: Silent, Notification, or full Athan with one Muazzin"
            ),
            Feature(
                icon = Icons.Default.Edit,
                title = "Athkar",
                description = "Morning, evening, and post-prayer"
            ),
            Feature(
                icon = Icons.Default.CalendarMonth,
                title = "Reading Tracker",
                description = "Track progress and Khatmah goals"
            ),
            Feature(
                icon = Icons.Default.Info,
                title = "Tafseer",
                description = "Ibn Kathir and more in Arabic and English"
            ),
            Feature(
                icon = Icons.Default.Abc,
                title = "Mufradat",
                description = "Word-by-word meanings of the Quran"
            )
        )
    }
}

@Composable
private fun LanguageToggle(
    language: AppLanguage,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = onToggle,
            colors = ButtonDefaults.textButtonColors(contentColor = islamicGreen)
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (language == AppLanguage.ARABIC) "English" else "العربية",
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun MushafFontDownloadSection(
    language: AppLanguage,
    isV2Downloaded: Boolean,
    isV4Downloaded: Boolean,
    v2Progress: com.quranmedia.player.data.source.FontDownloadProgress,
    v4Progress: com.quranmedia.player.data.source.FontDownloadProgress,
    onDownloadV2: () -> Unit,
    onDownloadV4: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(lightGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = islamicGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (language == AppLanguage.ARABIC)
                            "تحميل خطوط المصحف"
                        else "Download Mushaf Fonts",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        color = darkGreen
                    )
                    Text(
                        text = if (language == AppLanguage.ARABIC)
                            "للحصول على أفضل عرض للقرآن"
                        else "For the best Quran reading experience",
                        fontSize = 14.sp,
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Plain Fonts (V2)
            FontDownloadItem(
                language = language,
                title = if (language == AppLanguage.ARABIC) "الخطوط الأساسية" else "Plain Fonts",
                size = "~198 MB",
                isDownloaded = isV2Downloaded,
                progress = v2Progress,
                onDownload = onDownloadV2
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tajweed Fonts (V4)
            FontDownloadItem(
                language = language,
                title = if (language == AppLanguage.ARABIC) "خطوط التجويد" else "Tajweed Fonts",
                size = "~159 MB",
                isDownloaded = isV4Downloaded,
                progress = v4Progress,
                onDownload = onDownloadV4
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (language == AppLanguage.ARABIC)
                    "يمكنك تخطي هذه الخطوة وتحميل الخطوط لاحقاً من الإعدادات"
                else "You can skip this and download fonts later from Settings",
                fontSize = 12.sp,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FontDownloadItem(
    language: AppLanguage,
    title: String,
    size: String,
    isDownloaded: Boolean,
    progress: com.quranmedia.player.data.source.FontDownloadProgress,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDownloaded) lightGreen.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.1f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    color = darkGreen
                )
                // Show "Completed" badge when downloaded
                if (isDownloaded) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = islamicGreen.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "مكتمل" else "Completed",
                            fontSize = 10.sp,
                            color = islamicGreen,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = if (isDownloaded) {
                    if (language == AppLanguage.ARABIC) "تم التحميل بنجاح ✓" else "Downloaded successfully ✓"
                } else size,
                fontSize = 12.sp,
                color = if (isDownloaded) islamicGreen else Color.Gray
            )

            // Show progress if downloading
            if (progress.state == FontDownloadState.DOWNLOADING) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = progress.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = islamicGreen,
                    trackColor = Color.Gray.copy(alpha = 0.2f)
                )
                Text(
                    text = "${(progress.progress * 100).toInt()}%",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }

        when {
            isDownloaded -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Downloaded",
                    tint = islamicGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
            progress.state == FontDownloadState.DOWNLOADING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = islamicGreen,
                    strokeWidth = 2.dp
                )
            }
            progress.state == FontDownloadState.ERROR -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onDownload) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = Color.Red
                        )
                    }
                    Text(
                        text = if (language == AppLanguage.ARABIC) "فشل" else "Failed",
                        fontSize = 10.sp,
                        color = Color.Red
                    )
                }
            }
            else -> {
                Button(
                    onClick = onDownload,
                    colors = ButtonDefaults.buttonColors(containerColor = islamicGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == AppLanguage.ARABIC) "تحميل" else "Download",
                        fontSize = 12.sp,
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                    )
                }
            }
        }
    }
}
