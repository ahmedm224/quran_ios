package com.quranmedia.player.presentation.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.data.repository.ReadingTheme
import com.quranmedia.player.data.repository.ReminderInterval
import com.quranmedia.player.data.source.FontDownloadProgress
import com.quranmedia.player.data.source.FontDownloadState
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.theme.ReadingThemes
import com.quranmedia.player.presentation.util.Strings
import com.quranmedia.player.presentation.util.layoutDirection

private val islamicGreen = Color(0xFF2E7D32)
private val lightGreen = Color(0xFF66BB6A)
private val darkGreen = Color(0xFF1B5E20)
private val creamBackground = Color(0xFFFAF8F3)

enum class ColorPickerType {
    BACKGROUND, TEXT, HEADER
}

// Helper function to safely convert Long to Color
private fun Long.toColor(): Color {
    val argb = (this and 0xFFFFFFFF).toInt()
    return Color(argb)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val language = settings.appLanguage
    val context = LocalContext.current

    // Font download progress
    val v4Progress by viewModel.v4DownloadProgress.collectAsState()

    var showIntervalDialog by remember { mutableStateOf(false) }
    var showQuietHoursDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf<ColorPickerType?>(null) }

    // Permission launcher for notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setReminderEnabled(true)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Scaffold(
            topBar = {
                // 3D polished header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                            ambientColor = darkGreen.copy(alpha = 0.4f),
                            spotColor = darkGreen.copy(alpha = 0.4f)
                        )
                        .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1B5E20),
                                    Color(0xFF2E7D32),
                                    Color(0xFF388E3C)
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = Strings.settings.get(language),
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.size(48.dp)) // Balance the back button
                    }
                }
            },
            containerColor = creamBackground
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Reading Theme Section
                SettingsSection(
                    title = if (language == AppLanguage.ARABIC) "مظهر القراءة" else "Reading Theme",
                    language = language
                ) {
                    // Theme selection chips in a wrap layout (no horizontal scroll)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReadingTheme.entries.toList().forEach { theme: ReadingTheme ->
                            ThemeChipCompact(
                                theme = theme,
                                isSelected = settings.readingTheme == theme,
                                language = language,
                                onClick = { viewModel.setReadingTheme(theme) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Custom theme color pickers (only show if CUSTOM theme selected)
                    if (settings.readingTheme == ReadingTheme.CUSTOM) {
                        CustomThemeColorPickers(
                            backgroundColor = settings.customBackgroundColor.toColor(),
                            textColor = settings.customTextColor.toColor(),
                            headerColor = settings.customHeaderColor.toColor(),
                            language = language,
                            onBackgroundColorClick = { showColorPicker = ColorPickerType.BACKGROUND },
                            onTextColorClick = { showColorPicker = ColorPickerType.TEXT },
                            onHeaderColorClick = { showColorPicker = ColorPickerType.HEADER }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Theme preview - more compact
                    ThemePreviewCompact(
                        theme = settings.readingTheme,
                        customBackgroundColor = settings.customBackgroundColor.toColor(),
                        customTextColor = settings.customTextColor.toColor(),
                        customHeaderColor = settings.customHeaderColor.toColor(),
                        language = language
                    )
                }

                // Reading Reminder Section
                SettingsSection(
                    title = if (language == AppLanguage.ARABIC) "تذكير القراءة" else "Reading Reminder",
                    language = language
                ) {
                    // Enable/Disable reminder
                    SettingsSwitchItem(
                        title = if (language == AppLanguage.ARABIC) "تفعيل التذكير" else "Enable Reminder",
                        subtitle = if (language == AppLanguage.ARABIC)
                            "تذكيرك بمواصلة قراءة القرآن"
                        else
                            "Remind you to continue reading Quran",
                        icon = Icons.Default.Notifications,
                        checked = settings.readingReminderEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                // Check notification permission on Android 13+
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        return@SettingsSwitchItem
                                    }
                                }
                            }
                            viewModel.setReminderEnabled(enabled)
                        },
                        language = language
                    )

                    // Reminder interval (only show if enabled)
                    if (settings.readingReminderEnabled) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color.Gray.copy(alpha = 0.2f)
                        )

                        SettingsClickableItem(
                            title = if (language == AppLanguage.ARABIC) "فترة التذكير" else "Reminder Interval",
                            subtitle = settings.readingReminderInterval.getLabel(language),
                            icon = Icons.Default.Schedule,
                            onClick = { showIntervalDialog = true },
                            language = language
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color.Gray.copy(alpha = 0.2f)
                        )

                        // Quiet hours
                        SettingsClickableItem(
                            title = if (language == AppLanguage.ARABIC) "ساعات الهدوء" else "Quiet Hours",
                            subtitle = if (language == AppLanguage.ARABIC)
                                "من ${formatHour(settings.quietHoursStart)} إلى ${formatHour(settings.quietHoursEnd)}"
                            else
                                "From ${formatHour(settings.quietHoursStart)} to ${formatHour(settings.quietHoursEnd)}",
                            icon = Icons.Default.Bedtime,
                            onClick = { showQuietHoursDialog = true },
                            language = language
                        )

                        // Quiet hours explanation
                        Text(
                            text = if (language == AppLanguage.ARABIC)
                                "لن يتم إرسال تذكيرات خلال ساعات الهدوء"
                            else
                                "No reminders will be sent during quiet hours",
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 52.dp, top = 4.dp)
                        )
                    }
                }

                // Display Settings Section
                SettingsSection(
                    title = if (language == AppLanguage.ARABIC) "إعدادات العرض" else "Display Settings",
                    language = language
                ) {
                    // Tajweed font download
                    Text(
                        text = if (language == AppLanguage.ARABIC) "خطوط التجويد" else "Tajweed Font",
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = islamicGreen,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    FontDownloadItem(
                        title = if (language == AppLanguage.ARABIC) "خط المصحف بالتجويد" else "Mushaf Tajweed Font",
                        subtitle = if (language == AppLanguage.ARABIC) "~159 ميجابايت" else "~159 MB",
                        progress = v4Progress,
                        language = language,
                        formatSize = { viewModel.formatSize(it) },
                        downloadedSize = viewModel.getV4FontsSize(),
                        onDownload = { viewModel.downloadV4Fonts() },
                        onDelete = { viewModel.deleteV4Fonts() }
                    )

                    Text(
                        text = if (language == AppLanguage.ARABIC)
                            "حمّل خطوط التجويد لعرض ألوان التجويد. اختر مظهر التجويد من مظهر القراءة."
                        else
                            "Download Tajweed font to display Tajweed colors. Select Tajweed theme from Reading Theme.",
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color.Gray.copy(alpha = 0.2f)
                    )

                    SettingsSwitchItem(
                        title = if (language == AppLanguage.ARABIC) "خط عريض" else "Bold Font",
                        subtitle = if (language == AppLanguage.ARABIC)
                            "استخدام خط عريض للقرآن في وضع القراءة"
                        else
                            "Use bold font for Quran text in reading mode",
                        icon = Icons.Default.FormatBold,
                        checked = settings.useBoldFont,
                        onCheckedChange = { viewModel.setUseBoldFont(it) },
                        language = language
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color.Gray.copy(alpha = 0.2f)
                    )

                    // Keep screen on
                    SettingsSwitchItem(
                        title = if (language == AppLanguage.ARABIC) "إبقاء الشاشة مضاءة" else "Keep Screen On",
                        subtitle = if (language == AppLanguage.ARABIC)
                            "منع الشاشة من النوم أثناء القراءة والتلاوة"
                        else
                            "Prevent screen from sleeping during reading and recitation",
                        icon = Icons.Default.ScreenLockPortrait,
                        checked = settings.keepScreenOn,
                        onCheckedChange = { viewModel.setKeepScreenOn(it) },
                        language = language
                    )
                }

                // TODO: Recite Settings Section - Hidden until feature is ready
                /*
                SettingsSection(
                    title = if (language == AppLanguage.ARABIC) "إعدادات التسميع" else "Recite Settings",
                    language = language
                ) {
                    SettingsSwitchItem(
                        title = if (language == AppLanguage.ARABIC) "التقييم الفوري" else "Real-Time Assessment",
                        subtitle = if (language == AppLanguage.ARABIC)
                            "تقييم كل آية على حدة مع تنبيه اهتزازي عند الخطأ"
                        else
                            "Assess each ayah separately with haptic feedback on mistakes",
                        icon = Icons.Default.Speed,
                        checked = settings.reciteRealTimeAssessment,
                        onCheckedChange = { viewModel.setReciteRealTimeAssessment(it) },
                        language = language
                    )

                    if (settings.reciteRealTimeAssessment) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color.Gray.copy(alpha = 0.2f)
                        )

                        SettingsSwitchItem(
                            title = if (language == AppLanguage.ARABIC) "الاهتزاز عند الخطأ" else "Vibrate on Mistake",
                            subtitle = if (language == AppLanguage.ARABIC)
                                "اهتزاز ثلاثي عند اكتشاف خطأ في التلاوة"
                            else
                                "Triple vibration when a mistake is detected",
                            icon = Icons.Default.Vibration,
                            checked = settings.reciteHapticOnMistake,
                            onCheckedChange = { viewModel.setReciteHapticOnMistake(it) },
                            language = language
                        )
                    }

                    Text(
                        text = if (language == AppLanguage.ARABIC)
                            "الوضع العادي: تسجيل كامل ثم عرض النتائج\nالوضع الفوري: تقييم كل آية على حدة"
                        else
                            "Normal mode: Full recording then show results\nReal-time mode: Assess each ayah separately",
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 52.dp, top = 8.dp)
                    )
                }
                */

                // Language Section
                SettingsSection(
                    title = if (language == AppLanguage.ARABIC) "اللغة" else "Language",
                    language = language
                ) {
                    // Language selection
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.entries.toList().forEach { lang ->
                            LanguageOption(
                                language = lang,
                                isSelected = settings.appLanguage == lang,
                                onClick = { viewModel.setAppLanguage(lang) },
                                currentLanguage = language
                            )
                        }
                    }

                    // Info text
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (language == AppLanguage.ARABIC)
                            "سيتم تطبيق اللغة فوراً على كامل التطبيق"
                        else
                            "Language will be applied immediately to the entire app",
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }
        }
    }

    // Interval selection dialog
    if (showIntervalDialog) {
        IntervalSelectionDialog(
            currentInterval = settings.readingReminderInterval,
            language = language,
            onDismiss = { showIntervalDialog = false },
            onIntervalSelected = { interval ->
                viewModel.setReminderInterval(interval)
                showIntervalDialog = false
            }
        )
    }

    // Quiet hours dialog
    if (showQuietHoursDialog) {
        QuietHoursDialog(
            startHour = settings.quietHoursStart,
            endHour = settings.quietHoursEnd,
            language = language,
            onDismiss = { showQuietHoursDialog = false },
            onHoursSelected = { start, end ->
                viewModel.setQuietHours(start, end)
                showQuietHoursDialog = false
            }
        )
    }

    // Color picker dialog
    showColorPicker?.let { type ->
        val currentColor = when (type) {
            ColorPickerType.BACKGROUND -> settings.customBackgroundColor.toColor()
            ColorPickerType.TEXT -> settings.customTextColor.toColor()
            ColorPickerType.HEADER -> settings.customHeaderColor.toColor()
        }

        val title = when (type) {
            ColorPickerType.BACKGROUND -> if (language == AppLanguage.ARABIC) "لون الخلفية" else "Background Color"
            ColorPickerType.TEXT -> if (language == AppLanguage.ARABIC) "لون النص" else "Text Color"
            ColorPickerType.HEADER -> if (language == AppLanguage.ARABIC) "لون العنوان" else "Header Color"
        }

        ColorPickerDialog(
            title = title,
            currentColor = currentColor,
            language = language,
            onDismiss = { showColorPicker = null },
            onColorSelected = { color ->
                // Convert Color to ARGB Int, then to Long for storage
                val colorValue = color.toArgb().toLong()
                when (type) {
                    ColorPickerType.BACKGROUND -> viewModel.setCustomBackgroundColor(colorValue)
                    ColorPickerType.TEXT -> viewModel.setCustomTextColor(colorValue)
                    ColorPickerType.HEADER -> viewModel.setCustomHeaderColor(colorValue)
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    language: AppLanguage,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = islamicGreen.copy(alpha = 0.2f),
                spotColor = islamicGreen.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            islamicGreen.copy(alpha = 0.05f),
                            Color.White
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Text(
                text = title,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = islamicGreen,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            content()
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    language: AppLanguage
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .shadow(2.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            lightGreen.copy(alpha = 0.2f),
                            islamicGreen.copy(alpha = 0.15f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = islamicGreen,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                text = subtitle,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 11.sp,
                color = Color.Gray,
                maxLines = 1
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = islamicGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    language: AppLanguage
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .shadow(2.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            lightGreen.copy(alpha = 0.2f),
                            islamicGreen.copy(alpha = 0.15f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = islamicGreen,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                text = subtitle,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 11.sp,
                color = islamicGreen,
                maxLines = 1
            )
        }

        Icon(
            if (language == AppLanguage.ARABIC) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun IntervalSelectionDialog(
    currentInterval: ReminderInterval,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onIntervalSelected: (ReminderInterval) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = Color.Black,
        textContentColor = Color.Black,
        title = {
            Text(
                text = if (language == AppLanguage.ARABIC) "فترة التذكير" else "Reminder Interval",
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontWeight = FontWeight.Bold,
                color = islamicGreen
            )
        },
        text = {
            Column {
                ReminderInterval.entries.filter { it != ReminderInterval.OFF }.forEach { interval ->
                    val isSelected = interval == currentInterval
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) islamicGreen.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { onIntervalSelected(interval) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onIntervalSelected(interval) },
                            colors = RadioButtonDefaults.colors(selectedColor = islamicGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = interval.getLabel(language),
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) darkGreen else Color.Black
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    if (language == AppLanguage.ARABIC) "إغلاق" else "Close",
                    color = islamicGreen
                )
            }
        }
    )
}

@Composable
private fun QuietHoursDialog(
    startHour: Int,
    endHour: Int,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onHoursSelected: (Int, Int) -> Unit
) {
    var selectedStart by remember { mutableStateOf(startHour) }
    var selectedEnd by remember { mutableStateOf(endHour) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = Color.Black,
        textContentColor = Color.Black,
        title = {
            Text(
                text = if (language == AppLanguage.ARABIC) "ساعات الهدوء" else "Quiet Hours",
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontWeight = FontWeight.Bold,
                color = islamicGreen
            )
        },
        text = {
            Column {
                Text(
                    text = if (language == AppLanguage.ARABIC)
                        "لن يتم إرسال تذكيرات خلال هذه الفترة"
                    else
                        "No reminders will be sent during this period",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Start hour selector
                Text(
                    text = if (language == AppLanguage.ARABIC) "من:" else "From:",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                HourSelector(
                    selectedHour = selectedStart,
                    onHourSelected = { selectedStart = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // End hour selector
                Text(
                    text = if (language == AppLanguage.ARABIC) "إلى:" else "To:",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                HourSelector(
                    selectedHour = selectedEnd,
                    onHourSelected = { selectedEnd = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onHoursSelected(selectedStart, selectedEnd) }) {
                Text(
                    if (language == AppLanguage.ARABIC) "حفظ" else "Save",
                    color = islamicGreen
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    if (language == AppLanguage.ARABIC) "إلغاء" else "Cancel",
                    color = Color.Gray
                )
            }
        }
    )
}

@Composable
private fun HourSelector(
    selectedHour: Int,
    onHourSelected: (Int) -> Unit
) {
    // Common quiet hours options
    val hourOptions = listOf(
        6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 0, 1, 2, 3, 4, 5
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        hourOptions.forEach { hour ->
            val isSelected = hour == selectedHour
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onHourSelected(hour) },
                color = if (isSelected) islamicGreen else Color.Gray.copy(alpha = 0.1f)
            ) {
                Text(
                    text = formatHour(hour),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color.Black
                )
            }
        }
    }
}

@Composable
private fun rememberScrollState() = androidx.compose.foundation.rememberScrollState()

private fun formatHour(hour: Int): String {
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val amPm = if (hour < 12) "AM" else "PM"
    return "$displayHour $amPm"
}

@Composable
private fun ThemeChipCompact(
    theme: ReadingTheme,
    isSelected: Boolean,
    language: AppLanguage,
    onClick: () -> Unit
) {
    val themeColors = ReadingThemes.getTheme(theme)

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) themeColors.accent else themeColors.background,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) themeColors.accent else Color.Gray.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Small color indicator
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) Color.White.copy(alpha = 0.3f) else themeColors.textPrimary.copy(alpha = 0.2f))
            )

            Text(
                text = theme.getLabel(language),
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else themeColors.textPrimary
            )
        }
    }
}

@Composable
private fun CustomThemeColorPickers(
    backgroundColor: Color,
    textColor: Color,
    headerColor: Color,
    language: AppLanguage,
    onBackgroundColorClick: () -> Unit,
    onTextColorClick: () -> Unit,
    onHeaderColorClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (language == AppLanguage.ARABIC) "تخصيص الألوان" else "Customize Colors",
            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = islamicGreen
        )

        // Background color
        ColorPickerRow(
            label = if (language == AppLanguage.ARABIC) "الخلفية" else "Background",
            color = backgroundColor,
            language = language,
            onClick = onBackgroundColorClick
        )

        // Text color
        ColorPickerRow(
            label = if (language == AppLanguage.ARABIC) "النص" else "Text",
            color = textColor,
            language = language,
            onClick = onTextColorClick
        )

        // Header color
        ColorPickerRow(
            label = if (language == AppLanguage.ARABIC) "العنوان" else "Header",
            color = headerColor,
            language = language,
            onClick = onHeaderColorClick
        )
    }
}

@Composable
private fun ColorPickerRow(
    label: String,
    color: Color,
    language: AppLanguage,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
            fontSize = 13.sp,
            color = Color.Black
        )

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
        )
    }
}

@Composable
private fun ThemePreviewCompact(
    theme: ReadingTheme,
    customBackgroundColor: Color = Color.White,
    customTextColor: Color = Color.Black,
    customHeaderColor: Color = Color(0xFF2E7D32),
    language: AppLanguage
) {
    val themeColors = if (theme == ReadingTheme.CUSTOM) {
        ReadingThemes.getTheme(
            theme,
            com.quranmedia.player.presentation.theme.CustomThemeColors(
                backgroundColor = customBackgroundColor,
                textColor = customTextColor,
                headerColor = customHeaderColor
            )
        )
    } else {
        ReadingThemes.getTheme(theme)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Full Bismillah ayah with proper display
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Full Bismillah text
                    Text(
                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                        fontFamily = scheherazadeFont,
                        fontSize = 18.sp,
                        color = themeColors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Highlighted ayah example
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(themeColors.highlightBackground)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
                                fontFamily = scheherazadeFont,
                                fontSize = 16.sp,
                                color = themeColors.highlight
                            )
                        }
                        Text(
                            text = " ﴿٢﴾",
                            fontFamily = scheherazadeFont,
                            fontSize = 12.sp,
                            color = themeColors.ayahMarker
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageOption(
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit,
    currentLanguage: AppLanguage
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) islamicGreen.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Language icon/flag
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        colors = if (isSelected) {
                            listOf(
                                lightGreen.copy(alpha = 0.3f),
                                islamicGreen.copy(alpha = 0.2f)
                            )
                        } else {
                            listOf(
                                lightGreen.copy(alpha = 0.1f),
                                islamicGreen.copy(alpha = 0.05f)
                            )
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Language,
                contentDescription = null,
                tint = if (isSelected) islamicGreen else Color.Gray,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when (language) {
                    AppLanguage.ARABIC -> "العربية"
                    AppLanguage.ENGLISH -> "English"
                },
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) darkGreen else Color.Black
            )
            Text(
                text = when (language) {
                    AppLanguage.ARABIC -> if (currentLanguage == AppLanguage.ARABIC) "نظام الكتابة من اليمين إلى اليسار" else "Right-to-left"
                    AppLanguage.ENGLISH -> if (currentLanguage == AppLanguage.ARABIC) "من اليسار إلى اليمين" else "Left-to-right"
                },
                fontFamily = if (currentLanguage == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        // Checkmark if selected
        if (isSelected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = islamicGreen,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun FontDownloadItem(
    title: String,
    subtitle: String,
    progress: FontDownloadProgress,
    language: AppLanguage,
    formatSize: (Long) -> String,
    downloadedSize: Long,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val isDownloaded = progress.state == FontDownloadState.DOWNLOADED
    val isDownloading = progress.state == FontDownloadState.DOWNLOADING
    val hasError = progress.state == FontDownloadState.ERROR

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDownloaded) lightGreen.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.05f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Text(
                        text = when {
                            isDownloaded -> if (language == AppLanguage.ARABIC)
                                "تم التحميل (${formatSize(downloadedSize)})"
                            else
                                "Downloaded (${formatSize(downloadedSize)})"
                            isDownloading -> if (language == AppLanguage.ARABIC)
                                "جاري التحميل... ${(progress.progress * 100).toInt()}%"
                            else
                                "Downloading... ${(progress.progress * 100).toInt()}%"
                            hasError -> progress.errorMessage ?: (if (language == AppLanguage.ARABIC) "خطأ" else "Error")
                            else -> subtitle
                        },
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 11.sp,
                        color = when {
                            isDownloaded -> islamicGreen
                            hasError -> Color.Red
                            else -> Color.Gray
                        }
                    )
                }

                // Action button
                when {
                    isDownloading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = islamicGreen,
                            strokeWidth = 2.dp
                        )
                    }
                    isDownloaded -> {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = if (language == AppLanguage.ARABIC) "حذف" else "Delete",
                                tint = Color.Red.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    else -> {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = islamicGreen),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp,
                                vertical = 0.dp
                            )
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == AppLanguage.ARABIC) "تحميل" else "Download",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Progress bar (only show when downloading)
            if (isDownloading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = islamicGreen,
                    trackColor = Color.Gray.copy(alpha = 0.2f)
                )
            }
        }
    }
}
