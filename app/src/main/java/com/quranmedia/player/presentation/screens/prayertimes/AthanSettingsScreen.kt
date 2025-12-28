package com.quranmedia.player.presentation.screens.prayertimes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.data.repository.PrayerNotificationMode
import com.quranmedia.player.domain.model.AsrJuristicMethod
import com.quranmedia.player.domain.model.Athan
import com.quranmedia.player.domain.model.CalculationMethod
import com.quranmedia.player.domain.model.PrayerType
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.util.layoutDirection

// Theme colors - Islamic green
private val islamicGreen = Color(0xFF2E7D32)
private val lightGreen = Color(0xFFE8F5E9)
private val darkGreen = Color(0xFF1B5E20)
private val creamBackground = Color(0xFFFAF8F3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthanSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AthanSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val language = uiState.language
    val context = LocalContext.current

    var selectedPrayerForAthan by remember { mutableStateOf<PrayerType?>(null) }
    var showGlobalAthanSelector by remember { mutableStateOf(false) }
    var showPerPrayerCustomization by remember { mutableStateOf(false) }

    // Track pending mode change for after permission is granted
    var pendingModeChange by remember { mutableStateOf<Pair<PrayerType, PrayerNotificationMode>?>(null) }

    // Permission launcher for notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Apply the pending mode change after permission is granted
            pendingModeChange?.let { (prayerType, mode) ->
                viewModel.setPrayerMode(prayerType, mode)
            }
        }
        pendingModeChange = null
    }

    // Helper function to check permission and set prayer mode
    fun setPrayerModeWithPermissionCheck(prayerType: PrayerType, mode: PrayerNotificationMode) {
        // Only check permission for modes that require notifications
        if (mode == PrayerNotificationMode.SILENT) {
            viewModel.setPrayerMode(prayerType, mode)
            return
        }

        // Check notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Store pending change and request permission
                pendingModeChange = Pair(prayerType, mode)
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        // Permission granted or not required, apply the change
        viewModel.setPrayerMode(prayerType, mode)
    }

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "إعدادات التنبيه" else "Prayer Settings",
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Calculation Method Selection
                item {
                    var expanded by remember { mutableStateOf(false) }
                    CompactCard {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (language == AppLanguage.ARABIC) "طريقة الحساب" else "Calculation Method",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = darkGreen,
                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Gray.copy(alpha = 0.1f))
                                        .clickable { expanded = true }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.ARABIC)
                                            uiState.calculationMethod.nameArabic
                                        else
                                            uiState.calculationMethod.nameEnglish,
                                        fontSize = 14.sp,
                                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                        color = islamicGreen
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = islamicGreen
                                    )
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    CalculationMethod.entries.forEach { method ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (language == AppLanguage.ARABIC)
                                                        method.nameArabic else method.nameEnglish,
                                                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                                    fontWeight = if (method == uiState.calculationMethod)
                                                        FontWeight.Bold else FontWeight.Normal,
                                                    color = if (method == uiState.calculationMethod)
                                                        islamicGreen else Color.Black
                                                )
                                            },
                                            onClick = {
                                                viewModel.setCalculationMethod(method)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Asr Juristic Method
                item {
                    CompactCard {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (language == AppLanguage.ARABIC) "حساب العصر" else "Asr Calculation",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = darkGreen,
                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AsrJuristicMethod.entries.forEach { method ->
                                    val isSelected = method == uiState.asrJuristicMethod
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setAsrJuristicMethod(method) },
                                        label = {
                                            Text(
                                                text = if (language == AppLanguage.ARABIC)
                                                    method.nameArabic else method.nameEnglish,
                                                fontSize = 13.sp,
                                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                                            )
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
                        }
                    }
                }

                // Hijri Date Adjustment
                item {
                    CompactCard {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (language == AppLanguage.ARABIC) "تعديل التاريخ الهجري" else "Hijri Date Adjustment",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = darkGreen,
                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(-2, -1, 0, 1, 2).forEach { adjustment ->
                                    val isSelected = adjustment == uiState.hijriDateAdjustment
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setHijriDateAdjustment(adjustment) },
                                        label = {
                                            Text(
                                                text = when {
                                                    adjustment > 0 -> "+$adjustment"
                                                    adjustment == 0 -> "0"
                                                    else -> "$adjustment"
                                                },
                                                fontSize = 13.sp
                                            )
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
                            if (uiState.hijriDateAdjustment != 0) {
                                Text(
                                    text = if (language == AppLanguage.ARABIC)
                                        "تعديل ${if (uiState.hijriDateAdjustment > 0) "+" else ""}${uiState.hijriDateAdjustment} ${if (kotlin.math.abs(uiState.hijriDateAdjustment) == 1) "يوم" else "أيام"}"
                                    else
                                        "${if (uiState.hijriDateAdjustment > 0) "+" else ""}${uiState.hijriDateAdjustment} ${if (kotlin.math.abs(uiState.hijriDateAdjustment) == 1) "day" else "days"}",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Check if any prayer uses Athan mode
                val hasAnyAthanModeEnabled = uiState.fajrMode == PrayerNotificationMode.ATHAN ||
                        uiState.dhuhrMode == PrayerNotificationMode.ATHAN ||
                        uiState.asrMode == PrayerNotificationMode.ATHAN ||
                        uiState.maghribMode == PrayerNotificationMode.ATHAN ||
                        uiState.ishaMode == PrayerNotificationMode.ATHAN

                // Global Muezzin Selector - only show if any prayer uses Athan mode
                if (hasAnyAthanModeEnabled) {
                    item {
                        CompactCard {
                            Column {
                                // Header
                                Text(
                                    text = if (language == AppLanguage.ARABIC) "المؤذن" else "Muezzin",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = darkGreen,
                                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                    modifier = Modifier.padding(12.dp, 10.dp, 12.dp, 6.dp)
                                )

                                // Global Muezzin selector
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Gray.copy(alpha = 0.05f))
                                        .clickable { showGlobalAthanSelector = true }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = uiState.getSelectedAthanName(PrayerType.FAJR) ?: (if (language == AppLanguage.ARABIC) "اختر المؤذن" else "Select Muezzin"),
                                        fontSize = 13.sp,
                                        color = if (uiState.getSelectedAthanName(PrayerType.FAJR) != null) Color.Black else Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (uiState.isAthanDownloaded(PrayerType.FAJR)) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = islamicGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Expand/collapse button for per-prayer customization
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { showPerPrayerCustomization = !showPerPrayerCustomization }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.ARABIC) "تخصيص لكل صلاة" else "Customize per prayer",
                                        fontSize = 12.sp,
                                        color = islamicGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        if (showPerPrayerCustomization) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = islamicGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Per-prayer customization (expandable)
                                if (showPerPrayerCustomization) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = Color.Gray.copy(alpha = 0.15f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    listOf(
                                        PrayerType.FAJR,
                                        PrayerType.DHUHR,
                                        PrayerType.ASR,
                                        PrayerType.MAGHRIB,
                                        PrayerType.ISHA
                                    ).forEachIndexed { index, prayerType ->
                                        if (index > 0) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                        PerPrayerAthanSelector(
                                            prayerType = prayerType,
                                            selectedAthanName = uiState.getSelectedAthanName(prayerType),
                                            isAthanDownloaded = uiState.isAthanDownloaded(prayerType),
                                            language = language,
                                            onSelectAthan = { selectedPrayerForAthan = prayerType }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }

                // All prayers in one compact card
                item {
                    CompactCard {
                        Column {
                            // Header
                            Text(
                                text = if (language == AppLanguage.ARABIC) "إعدادات كل صلاة" else "Prayer Notifications",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = darkGreen,
                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                modifier = Modifier.padding(12.dp, 10.dp, 12.dp, 6.dp)
                            )

                            listOf(
                                PrayerType.FAJR,
                                PrayerType.DHUHR,
                                PrayerType.ASR,
                                PrayerType.MAGHRIB,
                                PrayerType.ISHA
                            ).forEachIndexed { index, prayerType ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = Color.Gray.copy(alpha = 0.15f)
                                    )
                                }
                                CompactPrayerRow(
                                    prayerType = prayerType,
                                    mode = uiState.getPrayerMode(prayerType),
                                    selectedAthanName = uiState.getSelectedAthanName(prayerType),
                                    isAthanDownloaded = uiState.isAthanDownloaded(prayerType),
                                    language = language,
                                    onModeChange = { mode -> setPrayerModeWithPermissionCheck(prayerType, mode) },
                                    onSelectAthan = { selectedPrayerForAthan = prayerType }
                                )
                            }
                        }
                    }
                }

                // Check if any prayer has notifications enabled (non-SILENT)
                val hasAnyNotificationEnabled = uiState.fajrMode != PrayerNotificationMode.SILENT ||
                        uiState.dhuhrMode != PrayerNotificationMode.SILENT ||
                        uiState.asrMode != PrayerNotificationMode.SILENT ||
                        uiState.maghribMode != PrayerNotificationMode.SILENT ||
                        uiState.ishaMode != PrayerNotificationMode.SILENT

                // Check if any prayer uses Athan mode
                val hasAnyAthanEnabled = uiState.fajrMode == PrayerNotificationMode.ATHAN ||
                        uiState.dhuhrMode == PrayerNotificationMode.ATHAN ||
                        uiState.asrMode == PrayerNotificationMode.ATHAN ||
                        uiState.maghribMode == PrayerNotificationMode.ATHAN ||
                        uiState.ishaMode == PrayerNotificationMode.ATHAN

                // Check if any prayer uses Notification (alert) mode
                val hasAnyAlertEnabled = uiState.fajrMode == PrayerNotificationMode.NOTIFICATION ||
                        uiState.dhuhrMode == PrayerNotificationMode.NOTIFICATION ||
                        uiState.asrMode == PrayerNotificationMode.NOTIFICATION ||
                        uiState.maghribMode == PrayerNotificationMode.NOTIFICATION ||
                        uiState.ishaMode == PrayerNotificationMode.NOTIFICATION

                // Notification timing - only show if any notification is enabled
                if (hasAnyNotificationEnabled) {
                    item {
                        CompactCard {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (language == AppLanguage.ARABIC) "توقيت التنبيه" else "Notify Before",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = darkGreen,
                                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(0, 5, 10, 15, 30).forEach { minutes ->
                                        val isSelected = uiState.notifyMinutesBefore == minutes
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.setNotifyMinutesBefore(minutes) },
                                            label = {
                                                Text(
                                                    text = if (minutes == 0) {
                                                        if (language == AppLanguage.ARABIC) "الآن" else "Now"
                                                    } else "${minutes}m",
                                                    fontSize = 11.sp
                                                )
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
                            }
                        }
                    }
                }

                // Athan options - only show if any prayer uses Athan mode
                if (hasAnyAthanEnabled) {
                    item {
                        CompactCard {
                            Column {
                                Text(
                                    text = if (language == AppLanguage.ARABIC) "خيارات الأذان" else "Athan Options",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = darkGreen,
                                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                    modifier = Modifier.padding(12.dp, 10.dp, 12.dp, 4.dp)
                                )

                                CompactToggleRow(
                                    title = if (language == AppLanguage.ARABIC) "الصوت الأعلى" else "Max Volume",
                                    checked = uiState.athanMaxVolume,
                                    onCheckedChange = { viewModel.setAthanMaxVolume(it) },
                                    language = language
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = Color.Gray.copy(alpha = 0.15f)
                                )

                                CompactToggleRow(
                                    title = if (language == AppLanguage.ARABIC) "قلب للإيقاف" else "Flip to Silence",
                                    checked = uiState.flipToSilence,
                                    onCheckedChange = { viewModel.setFlipToSilence(it) },
                                    language = language
                                )
                            }
                        }
                    }
                }

                // Notification options - only show if any prayer uses Alert mode
                if (hasAnyAlertEnabled) {
                    item {
                        CompactCard {
                            Column {
                                Text(
                                    text = if (language == AppLanguage.ARABIC) "خيارات الإشعار" else "Notification Options",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = darkGreen,
                                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                    modifier = Modifier.padding(12.dp, 10.dp, 12.dp, 4.dp)
                                )

                                CompactToggleRow(
                                    title = if (language == AppLanguage.ARABIC) "الصوت" else "Sound",
                                    checked = uiState.notificationSound,
                                    onCheckedChange = { viewModel.setNotificationSound(it) },
                                    language = language
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = Color.Gray.copy(alpha = 0.15f)
                                )

                                CompactToggleRow(
                                    title = if (language == AppLanguage.ARABIC) "الاهتزاز" else "Vibration",
                                    checked = uiState.notificationVibrate,
                                    onCheckedChange = { viewModel.setNotificationVibrate(it) },
                                    language = language
                                )
                            }
                        }
                    }
                }

                // Downloaded athans - only show if any prayer uses Athan mode and has downloads
                if (hasAnyAthanEnabled && uiState.downloadedAthans.isNotEmpty()) {
                    item {
                        CompactCard {
                            Column {
                                Text(
                                    text = if (language == AppLanguage.ARABIC) "الأذانات المحملة" else "Downloaded",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = darkGreen,
                                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                    modifier = Modifier.padding(12.dp, 10.dp, 12.dp, 4.dp)
                                )

                                uiState.downloadedAthans.forEachIndexed { index, athan ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            color = Color.Gray.copy(alpha = 0.15f)
                                        )
                                    }
                                    CompactDownloadedAthanRow(
                                        athan = athan,
                                        isPlaying = uiState.previewingAthanId == athan.id,
                                        onPlay = { viewModel.previewAthan(athan) },
                                        onStop = { viewModel.stopPreview() },
                                        onDelete = { viewModel.deleteAthan(athan.id) }
                                    )
                                }
                            }
                        }
                    }
                }

            }
        }

        // Global Athan selection dialog
        if (showGlobalAthanSelector) {
            CompactAthanSelectionDialog(
                prayerType = PrayerType.FAJR,  // Just for display purposes
                availableAthans = uiState.availableAthans,
                downloadedAthanIds = uiState.downloadedAthans.map { it.id }.toSet(),
                currentAthanId = uiState.fajrAthanId,
                isLoading = uiState.isLoadingAthans,
                downloadingAthanId = uiState.downloadingAthanId,
                previewingAthanId = uiState.previewingAthanId,
                language = language,
                onDismiss = { showGlobalAthanSelector = false },
                onSelectAthan = { athanId ->
                    viewModel.setGlobalAthan(athanId)
                    showGlobalAthanSelector = false
                },
                onDownloadAthan = { athan -> viewModel.downloadAthan(athan) },
                onPreviewAthan = { athan -> viewModel.previewAthan(athan) },
                onStopPreview = { viewModel.stopPreview() }
            )
        }

        // Athan selection dialog (per-prayer)
        if (selectedPrayerForAthan != null) {
            CompactAthanSelectionDialog(
                prayerType = selectedPrayerForAthan!!,
                availableAthans = uiState.availableAthans,
                downloadedAthanIds = uiState.downloadedAthans.map { it.id }.toSet(),
                currentAthanId = uiState.getSelectedAthanId(selectedPrayerForAthan!!),
                isLoading = uiState.isLoadingAthans,
                downloadingAthanId = uiState.downloadingAthanId,
                previewingAthanId = uiState.previewingAthanId,
                language = language,
                onDismiss = { selectedPrayerForAthan = null },
                onSelectAthan = { athanId ->
                    viewModel.selectAthanForPrayer(selectedPrayerForAthan!!, athanId)
                    selectedPrayerForAthan = null
                },
                onDownloadAthan = { athan -> viewModel.downloadAthan(athan) },
                onPreviewAthan = { athan -> viewModel.previewAthan(athan) },
                onStopPreview = { viewModel.stopPreview() }
            )
        }
    }
}

@Composable
private fun CompactCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        content()
    }
}

@Composable
private fun CompactPrayerRow(
    prayerType: PrayerType,
    mode: PrayerNotificationMode,
    selectedAthanName: String?,
    isAthanDownloaded: Boolean,
    language: AppLanguage,
    onModeChange: (PrayerNotificationMode) -> Unit,
    onSelectAthan: () -> Unit
) {
    val prayerName = if (language == AppLanguage.ARABIC) prayerType.nameArabic else prayerType.nameEnglish

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prayer name
            Text(
                text = prayerName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = darkGreen,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                modifier = Modifier.width(70.dp)
            )

            // Mode chips - very compact
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PrayerNotificationMode.entries.forEach { notificationMode ->
                    val isSelected = mode == notificationMode
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onModeChange(notificationMode) },
                        color = if (isSelected) islamicGreen else Color.Gray.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = when (notificationMode) {
                                PrayerNotificationMode.ATHAN -> if (language == AppLanguage.ARABIC) "أذان" else "Athan"
                                PrayerNotificationMode.NOTIFICATION -> if (language == AppLanguage.ARABIC) "إشعار" else "Alert"
                                PrayerNotificationMode.SILENT -> if (language == AppLanguage.ARABIC) "صامت" else "Off"
                            },
                            fontSize = 10.sp,
                            color = if (isSelected) Color.White else Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Muezzin selector (only when ATHAN mode)
        if (mode == PrayerNotificationMode.ATHAN) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Gray.copy(alpha = 0.05f))
                    .clickable { onSelectAthan() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedAthanName ?: (if (language == AppLanguage.ARABIC) "اختر المؤذن" else "Select Muezzin"),
                    fontSize = 12.sp,
                    color = if (selectedAthanName != null) Color.Black else Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAthanDownloaded) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = islamicGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PerPrayerAthanSelector(
    prayerType: PrayerType,
    selectedAthanName: String?,
    isAthanDownloaded: Boolean,
    language: AppLanguage,
    onSelectAthan: () -> Unit
) {
    val prayerName = if (language == AppLanguage.ARABIC) prayerType.nameArabic else prayerType.nameEnglish

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Prayer name
        Text(
            text = prayerName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = darkGreen,
            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
            modifier = Modifier.width(60.dp)
        )

        // Muezzin selector
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Gray.copy(alpha = 0.05f))
                .clickable { onSelectAthan() }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedAthanName ?: (if (language == AppLanguage.ARABIC) "اختر المؤذن" else "Select"),
                fontSize = 11.sp,
                color = if (selectedAthanName != null) Color.Black else Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAthanDownloaded) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = islamicGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    language: AppLanguage
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.height(24.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = islamicGreen
            )
        )
    }
}

@Composable
private fun CompactDownloadedAthanRow(
    athan: Athan,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = athan.name,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Row {
            IconButton(
                onClick = { if (isPlaying) onStop() else onPlay() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = islamicGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.Red.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactAthanSelectionDialog(
    prayerType: PrayerType,
    availableAthans: List<Athan>,
    downloadedAthanIds: Set<String>,
    currentAthanId: String?,
    isLoading: Boolean,
    downloadingAthanId: String?,
    previewingAthanId: String?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSelectAthan: (String) -> Unit,
    onDownloadAthan: (Athan) -> Unit,
    onPreviewAthan: (Athan) -> Unit,
    onStopPreview: () -> Unit
) {
    val prayerName = if (language == AppLanguage.ARABIC) prayerType.nameArabic else prayerType.nameEnglish

    AlertDialog(
        onDismissRequest = {
            onStopPreview()
            onDismiss()
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                text = if (language == AppLanguage.ARABIC) "اختر أذان $prayerName" else "$prayerName Athan",
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = darkGreen
            )
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = islamicGreen, modifier = Modifier.size(32.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(availableAthans) { athan ->
                        val isDownloaded = athan.id in downloadedAthanIds
                        val isSelected = athan.id == currentAthanId
                        val isDownloading = athan.id == downloadingAthanId
                        val isPreviewing = athan.id == previewingAthanId

                        CompactAthanListItem(
                            athan = athan,
                            isSelected = isSelected,
                            isDownloaded = isDownloaded,
                            isDownloading = isDownloading,
                            isPreviewing = isPreviewing,
                            onSelect = {
                                if (isDownloaded) {
                                    onSelectAthan(athan.id)
                                } else {
                                    onDownloadAthan(athan)
                                }
                            },
                            onPreview = {
                                if (isPreviewing) onStopPreview() else onPreviewAthan(athan)
                            },
                            onDownload = { onDownloadAthan(athan) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onStopPreview()
                onDismiss()
            }) {
                Text(
                    text = if (language == AppLanguage.ARABIC) "إغلاق" else "Close",
                    color = islamicGreen,
                    fontSize = 13.sp
                )
            }
        }
    )
}

@Composable
private fun CompactAthanListItem(
    athan: Athan,
    isSelected: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    isPreviewing: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
    onDownload: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = !isDownloading) { onSelect() },
        color = if (isSelected) lightGreen else Color.Gray.copy(alpha = 0.05f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = athan.name,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) darkGreen else Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = athan.location,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Preview button
                IconButton(
                    onClick = onPreview,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = islamicGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Status indicator
                when {
                    isDownloading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = islamicGreen,
                            strokeWidth = 2.dp
                        )
                    }
                    isDownloaded -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = islamicGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    else -> {
                        IconButton(
                            onClick = onDownload,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = darkGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
