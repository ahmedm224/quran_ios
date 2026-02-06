package com.quranmedia.player.presentation.screens.downloads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import com.quranmedia.player.presentation.components.CommonOverflowMenu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.database.entity.DownloadStatus
import com.quranmedia.player.data.database.entity.DownloadTaskEntity
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.domain.model.Reciter
import com.quranmedia.player.domain.model.Surah
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.screens.reader.components.islamicGreen
import com.quranmedia.player.presentation.screens.reader.components.darkGreen
import com.quranmedia.player.presentation.screens.reader.components.creamBackground
import com.quranmedia.player.presentation.util.layoutDirection
import com.quranmedia.player.domain.util.ArabicNumeralUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateBack: () -> Unit,
    onDownloadClick: (reciterId: String, surahNumber: Int) -> Unit = { _, _ -> },
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPrayerTimes: () -> Unit = {},
    onNavigateToAthkar: () -> Unit = {},
    onNavigateToTracker: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToReading: () -> Unit = {},
    onNavigateToImsakiya: () -> Unit = {},
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val language = settings.appLanguage
    val useIndoArabic = language == AppLanguage.ARABIC && settings.useIndoArabicNumerals

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "التنزيلات" else "Downloads",
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        CommonOverflowMenu(
                            language = language,
                            onNavigateToSettings = onNavigateToSettings,
                            onNavigateToReading = onNavigateToReading,
                            onNavigateToPrayerTimes = onNavigateToPrayerTimes,
                            onNavigateToImsakiya = onNavigateToImsakiya,
                            onNavigateToAthkar = onNavigateToAthkar,
                            onNavigateToTracker = onNavigateToTracker,
                            onNavigateToAbout = onNavigateToAbout,
                            hideDownloads = true  // Hide Downloads since we're on this screen
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = islamicGreen,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.showDownloadDialog() },
                    containerColor = islamicGreen,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = if (language == AppLanguage.ARABIC) "إضافة تنزيل" else "Add Download")
                }
            },
            containerColor = creamBackground
        ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = islamicGreen
                    )
                }
                state.downloads.isEmpty() -> {
                    EmptyDownloadsView(
                        modifier = Modifier.align(Alignment.Center),
                        language = language,
                        onAddDownload = { viewModel.showDownloadDialog() }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.downloads, key = { it.download.id }) { downloadWithReciter ->
                            DownloadCard(
                                downloadWithReciter = downloadWithReciter,
                                language = language,
                                onClick = {
                                    if (downloadWithReciter.download.status == DownloadStatus.COMPLETED.name) {
                                        onDownloadClick(downloadWithReciter.download.reciterId, downloadWithReciter.download.surahNumber)
                                    }
                                },
                                onPause = { viewModel.pauseDownload(downloadWithReciter.download.id) },
                                onResume = { viewModel.resumeDownload(downloadWithReciter.download.id) },
                                onCancel = { viewModel.cancelDownload(downloadWithReciter.download.id) },
                                onDelete = { viewModel.deleteDownload(downloadWithReciter.download.reciterId, downloadWithReciter.download.surahNumber) },
                                useIndoArabic = useIndoArabic
                            )
                        }
                    }
                }
            }
        }
        }

        // Download Dialog
        if (state.showDownloadDialog) {
            DownloadSelectionDialog(
                reciters = state.reciters,
                surahs = state.surahs,
                selectedReciter = state.selectedReciter,
                selectedSurah = state.selectedSurah,
                downloadFullQuran = state.downloadFullQuran,
                isDownloading = state.fullQuranDownloading,
                language = language,
                onReciterSelected = { viewModel.selectReciter(it) },
                onSurahSelected = { viewModel.selectSurah(it) },
                onFullQuranToggled = { viewModel.toggleFullQuranDownload(it) },
                onDismiss = { viewModel.hideDownloadDialog() },
                onConfirm = { viewModel.startDownload() },
                useIndoArabic = useIndoArabic
            )
        }
    }
}

@Composable
private fun EmptyDownloadsView(
    modifier: Modifier = Modifier,
    language: AppLanguage = AppLanguage.ENGLISH,
    onAddDownload: () -> Unit = {}
) {
    val islamicGreen = Color(0xFF2E7D32)

    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CloudDownload,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.Gray.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (language == AppLanguage.ARABIC) "لا توجد تنزيلات" else "No Downloads Yet",
            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
            style = MaterialTheme.typography.titleLarge,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (language == AppLanguage.ARABIC) "قم بتنزيل السور للاستماع بدون إنترنت" else "Download surahs for offline listening",
            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddDownload,
            colors = ButtonDefaults.buttonColors(containerColor = islamicGreen)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (language == AppLanguage.ARABIC) "إضافة تنزيل" else "Add Download",
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
            )
        }
    }
}

@Composable
private fun DownloadCard(
    downloadWithReciter: DownloadWithReciter,
    language: AppLanguage,
    onClick: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    useIndoArabic: Boolean = false
) {
    val download = downloadWithReciter.download
    val islamicGreen = Color(0xFF2E7D32)
    val isArabic = language == AppLanguage.ARABIC

    // Choose the appropriate reciter name based on language
    val reciterDisplayName = if (isArabic && downloadWithReciter.reciterNameArabic != null) {
        downloadWithReciter.reciterNameArabic
    } else {
        downloadWithReciter.reciterName
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Surah info and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Display surah name based on language, fall back to number if not available
                    val surahDisplayName = if (isArabic) {
                        downloadWithReciter.surahNameArabic?.let { "سورة $it" }
                            ?: "سورة ${ArabicNumeralUtils.formatNumber(download.surahNumber, useIndoArabic)}"
                    } else {
                        downloadWithReciter.surahNameEnglish?.let { "Surah $it" }
                            ?: "Surah ${download.surahNumber}"
                    }
                    Text(
                        text = surahDisplayName,
                        fontFamily = if (isArabic) scheherazadeFont else null,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reciterDisplayName,
                        fontFamily = if (isArabic && downloadWithReciter.reciterNameArabic != null) scheherazadeFont else null,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status badge
                StatusBadge(status = download.status, language = language)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar (for in-progress downloads)
            if (download.status == DownloadStatus.IN_PROGRESS.name) {
                Column {
                    LinearProgressIndicator(
                        progress = { download.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = islamicGreen,
                        trackColor = Color.Gray.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = ArabicNumeralUtils.formatNumber((download.progress * 100).toInt(), useIndoArabic) + "%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            text = formatBytes(download.bytesDownloaded) + " / " + formatBytes(download.bytesTotal),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Error message (for failed downloads)
            if (download.status == DownloadStatus.FAILED.name && download.errorMessage != null) {
                Text(
                    text = if (isArabic) "خطأ: ${download.errorMessage}" else "Error: ${download.errorMessage}",
                    fontFamily = if (isArabic) scheherazadeFont else null,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (download.status) {
                    DownloadStatus.IN_PROGRESS.name -> {
                        TextButton(onClick = onPause) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "إيقاف" else "Pause", fontFamily = if (isArabic) scheherazadeFont else null)
                        }
                        TextButton(onClick = onCancel) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "إلغاء" else "Cancel", fontFamily = if (isArabic) scheherazadeFont else null)
                        }
                    }
                    DownloadStatus.PAUSED.name -> {
                        TextButton(onClick = onResume) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "استئناف" else "Resume", fontFamily = if (isArabic) scheherazadeFont else null)
                        }
                        TextButton(onClick = onCancel) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "إلغاء" else "Cancel", fontFamily = if (isArabic) scheherazadeFont else null)
                        }
                    }
                    DownloadStatus.COMPLETED.name -> {
                        TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "حذف" else "Delete", fontFamily = if (isArabic) scheherazadeFont else null)
                        }
                    }
                    DownloadStatus.FAILED.name -> {
                        TextButton(onClick = onResume) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "إعادة" else "Retry", fontFamily = if (isArabic) scheherazadeFont else null)
                        }
                        TextButton(onClick = onCancel) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "إزالة" else "Remove", fontFamily = if (isArabic) scheherazadeFont else null)
                        }
                    }
                    else -> {
                        // PENDING
                        TextButton(onClick = onCancel) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "إلغاء" else "Cancel", fontFamily = if (isArabic) scheherazadeFont else null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String, language: AppLanguage = AppLanguage.ENGLISH) {
    val isArabic = language == AppLanguage.ARABIC
    val (color, text) = when (status) {
        DownloadStatus.PENDING.name -> Pair(Color.Gray, if (isArabic) "قيد الانتظار" else "Pending")
        DownloadStatus.IN_PROGRESS.name -> Pair(Color(0xFF1976D2), if (isArabic) "جاري التنزيل" else "Downloading")
        DownloadStatus.PAUSED.name -> Pair(Color(0xFFF57C00), if (isArabic) "متوقف" else "Paused")
        DownloadStatus.COMPLETED.name -> Pair(Color(0xFF388E3C), if (isArabic) "مكتمل" else "Completed")
        DownloadStatus.FAILED.name -> Pair(Color(0xFFD32F2F), if (isArabic) "فشل" else "Failed")
        else -> Pair(Color.Gray, if (isArabic) "غير معروف" else "Unknown")
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontFamily = if (isArabic) scheherazadeFont else null,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    return "%.1f MB".format(mb)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadSelectionDialog(
    reciters: List<Reciter>,
    surahs: List<Surah>,
    selectedReciter: Reciter?,
    selectedSurah: Surah?,
    downloadFullQuran: Boolean,
    isDownloading: Boolean,
    language: AppLanguage,
    onReciterSelected: (Reciter) -> Unit,
    onSurahSelected: (Surah) -> Unit,
    onFullQuranToggled: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    useIndoArabic: Boolean = false
) {
    val islamicGreen = Color(0xFF2E7D32)
    val isArabic = language == AppLanguage.ARABIC
    var reciterExpanded by remember { mutableStateOf(false) }
    var surahExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isDownloading) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = if (isArabic) {
                        if (downloadFullQuran) "تنزيل القرآن كاملاً" else "تنزيل سورة"
                    } else {
                        if (downloadFullQuran) "Download Full Quran" else "Download Surah"
                    },
                    fontFamily = if (isArabic) scheherazadeFont else null,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = islamicGreen,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Reciter Dropdown
                Column {
                    Text(
                        text = if (isArabic) "اختر القارئ" else "Select Reciter",
                        fontFamily = if (isArabic) scheherazadeFont else null,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = reciterExpanded,
                        onExpandedChange = { reciterExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = if (isArabic && selectedReciter?.nameArabic != null)
                                selectedReciter.nameArabic!!
                            else
                                selectedReciter?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reciterExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = islamicGreen,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = if (isArabic && selectedReciter?.nameArabic != null) scheherazadeFont else null,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = reciterExpanded,
                            onDismissRequest = { reciterExpanded = false }
                        ) {
                            reciters.forEach { reciter ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isArabic && reciter.nameArabic != null) reciter.nameArabic!! else reciter.name,
                                            fontFamily = if (isArabic && reciter.nameArabic != null) scheherazadeFont else null
                                        )
                                    },
                                    onClick = {
                                        onReciterSelected(reciter)
                                        reciterExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Full Quran Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (downloadFullQuran) islamicGreen.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.05f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFullQuranToggled(!downloadFullQuran) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "تنزيل القرآن كاملاً" else "Download Full Quran",
                                fontFamily = if (isArabic) scheherazadeFont else null,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                            Text(
                                text = if (isArabic) "تنزيل جميع السور (١١٤ سورة)" else "Download all 114 surahs",
                                fontFamily = if (isArabic) scheherazadeFont else null,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = downloadFullQuran,
                            onCheckedChange = { onFullQuranToggled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = islamicGreen,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                // Surah Dropdown (only show if not downloading full Quran)
                if (!downloadFullQuran) {
                    Column {
                        Text(
                            text = if (isArabic) "اختر السورة" else "Select Surah",
                            fontFamily = if (isArabic) scheherazadeFont else null,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ExposedDropdownMenuBox(
                            expanded = surahExpanded,
                            onExpandedChange = { surahExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = if (selectedSurah != null) {
                                    if (isArabic) "${ArabicNumeralUtils.formatNumber(selectedSurah.number, useIndoArabic)}. ${selectedSurah.nameArabic}"
                                    else "${selectedSurah.number}. ${selectedSurah.nameEnglish}"
                                } else "",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = {
                                    Text(
                                        text = if (isArabic) "اختر سورة..." else "Choose a surah...",
                                        fontFamily = if (isArabic) scheherazadeFont else null,
                                        color = Color.Gray
                                    )
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = surahExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = islamicGreen,
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = if (isArabic) scheherazadeFont else null,
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = surahExpanded,
                                onDismissRequest = { surahExpanded = false },
                                modifier = Modifier.heightIn(max = 300.dp)
                            ) {
                                surahs.forEach { surah ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (isArabic) "${ArabicNumeralUtils.formatNumber(surah.number, useIndoArabic)}. ${surah.nameArabic}"
                                                       else "${surah.number}. ${surah.nameEnglish}",
                                                fontFamily = if (isArabic) scheherazadeFont else null
                                            )
                                        },
                                        onClick = {
                                            onSurahSelected(surah)
                                            surahExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isDownloading,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                    ) {
                        Text(
                            text = if (isArabic) "إلغاء" else "Cancel",
                            fontFamily = if (isArabic) scheherazadeFont else null
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        enabled = selectedReciter != null && (downloadFullQuran || selectedSurah != null) && !isDownloading,
                        colors = ButtonDefaults.buttonColors(containerColor = islamicGreen)
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) "جاري التنزيل..." else "Starting...",
                                fontFamily = if (isArabic) scheherazadeFont else null
                            )
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (downloadFullQuran) {
                                    if (isArabic) "تنزيل الكل" else "Download All"
                                } else {
                                    if (isArabic) "تنزيل" else "Download"
                                },
                                fontFamily = if (isArabic) scheherazadeFont else null
                            )
                        }
                    }
                }
            }
        }
    }
}
