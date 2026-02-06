package com.quranmedia.player.recite.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.quranmedia.player.R
import com.quranmedia.player.data.database.entity.SurahEntity
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.domain.model.Ayah
import com.quranmedia.player.recite.domain.model.MismatchType
import com.quranmedia.player.recite.domain.model.ReciteResult
import com.quranmedia.player.recite.domain.model.ReciteSelection
import com.quranmedia.player.recite.domain.model.WordMismatch
import kotlin.math.roundToInt

// KFGQPC Hafs Uthmanic Script - Official King Fahd Complex font for accurate Quran rendering
val kfgqpcHafsFont = FontFamily(
    Font(R.font.kfgqpc_hafs_uthmanic, FontWeight.Normal),
    Font(R.font.kfgqpc_hafs_uthmanic, FontWeight.Bold)
)

/**
 * Welcome card shown when starting the Recite feature
 */
@Composable
fun WelcomeCard(
    language: AppLanguage,
    onStartSelection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RecordVoiceOver,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (language == AppLanguage.ARABIC) "التسميع" else "Recite",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (language == AppLanguage.ARABIC)
                    "راجع حفظك من القرآن الكريم"
                else
                    "Review your Quran memorization",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onStartSelection,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (language == AppLanguage.ARABIC) "ابدأ" else "Start")
            }
        }
    }
}

/**
 * Selection view showing Quran text with floating action buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionCard(
    language: AppLanguage,
    allSurahs: List<SurahEntity>,
    currentSelection: ReciteSelection?,
    realTimeMode: Boolean,
    onSelectionChanged: (surahNumber: Int, startAyah: Int, endAyah: Int) -> Unit,
    onStartRecording: (ReciteSelection, Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var selectedSurah by remember { mutableStateOf(allSurahs.firstOrNull()?.number ?: 1) }
    var startAyah by remember { mutableStateOf(1) }
    var endAyah by remember { mutableStateOf(1) }
    var expanded by remember { mutableStateOf(false) }
    var showQuranView by remember { mutableStateOf(false) }

    // Get selected surah details
    val surah = remember(selectedSurah, allSurahs) {
        allSurahs.find { it.number == selectedSurah }
    }

    // Update end ayah when surah changes
    LaunchedEffect(surah) {
        if (surah != null) {
            endAyah = minOf(endAyah, surah.ayahCount)
            startAyah = minOf(startAyah, surah.ayahCount)
            onSelectionChanged(selectedSurah, startAyah, endAyah)
        }
    }

    // If showQuranView is false, show selection interface
    if (!showQuranView) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (language == AppLanguage.ARABIC) "اختر السورة والآيات" else "Select Surah and Ayahs",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Surah dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = surah?.let {
                            if (language == AppLanguage.ARABIC) it.nameArabic else it.nameEnglish
                        } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (language == AppLanguage.ARABIC) "السورة" else "Surah") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        allSurahs.forEach { surahItem ->
                            DropdownMenuItem(
                                text = {
                                    Text(if (language == AppLanguage.ARABIC) surahItem.nameArabic else surahItem.nameEnglish)
                                },
                                onClick = {
                                    selectedSurah = surahItem.number
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (surah != null) {
                    // Start Ayah slider
                    Text(
                        text = if (language == AppLanguage.ARABIC)
                            "من الآية: $startAyah"
                        else
                            "From Ayah: $startAyah"
                    )
                    Slider(
                        value = startAyah.toFloat(),
                        onValueChange = {
                            startAyah = it.roundToInt()
                            if (endAyah < startAyah) {
                                endAyah = startAyah
                            }
                            onSelectionChanged(selectedSurah, startAyah, endAyah)
                        },
                        valueRange = 1f..surah.ayahCount.toFloat(),
                        steps = surah.ayahCount - 2
                    )

                    // End Ayah slider
                    Text(
                        text = if (language == AppLanguage.ARABIC)
                            "إلى الآية: $endAyah"
                        else
                            "To Ayah: $endAyah"
                    )
                    Slider(
                        value = endAyah.toFloat(),
                        onValueChange = {
                            endAyah = it.roundToInt()
                            if (startAyah > endAyah) {
                                startAyah = endAyah
                            }
                            onSelectionChanged(selectedSurah, startAyah, endAyah)
                        },
                        valueRange = 1f..surah.ayahCount.toFloat(),
                        steps = surah.ayahCount - 2
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (language == AppLanguage.ARABIC) "إلغاء" else "Cancel")
                    }

                    Button(
                        onClick = { showQuranView = true },
                        enabled = currentSelection != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (language == AppLanguage.ARABIC) "عرض" else "View")
                    }
                }
            }
        }
    } else {
        // Show Quran view with selection - will be implemented with actual ayah data
        Box(modifier = Modifier.fillMaxSize()) {
            // Placeholder for Quran text view
            Text(
                text = if (language == AppLanguage.ARABIC)
                    "سيتم عرض الآيات هنا"
                else
                    "Quran text will be displayed here",
                modifier = Modifier.align(Alignment.Center)
            )

            // Floating action buttons
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = { showQuranView = false },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = if (language == AppLanguage.ARABIC) "رجوع" else "Back")
                }

                FloatingActionButton(
                    onClick = {
                        currentSelection?.let { onStartRecording(it, realTimeMode) }
                    },
                    containerColor = Color.Red
                ) {
                    Icon(Icons.Default.Mic, contentDescription = if (language == AppLanguage.ARABIC) "بدء التسجيل" else "Start Recording", tint = Color.White)
                }
            }

            // Show real-time mode indicator if enabled
            if (realTimeMode) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF4CAF50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Text(
                            text = if (language == AppLanguage.ARABIC) "وضع الوقت الحقيقي" else "Real-Time Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Real-time continuous recording card with automatic chunk assessment
 * Shows recording status, current progress, and mistake details when detected
 */
@Composable
fun RealTimeRecordingCard(
    language: AppLanguage,
    state: com.quranmedia.player.recite.domain.model.ReciteState.RealTimeRecording,
    currentAyahText: String,
    onContinueFromMistake: () -> Unit,
    onCancelRecording: () -> Unit
) {
    val progress = state.getProgress()

    // Pulsing animation for recording indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (state.mistakeDetected) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.background
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (state.mistakeDetected) Color.Red else MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress text
            Text(
                text = if (language == AppLanguage.ARABIC)
                    "الآية ${state.currentAyahNumber} من ${state.selection.endAyah}"
                else
                    "Ayah ${state.currentAyahNumber} of ${state.selection.endAyah}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Surah name header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.mistakeDetected)
                        Color(0xFFFFCDD2)
                    else
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.selection.surahNameArabic,
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = kfgqpcHafsFont,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (language == AppLanguage.ARABIC)
                            "الآيات ${state.selection.startAyah}-${state.selection.endAyah}"
                        else
                            "Ayahs ${state.selection.startAyah}-${state.selection.endAyah}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main content area - either recording or mistake display
            if (state.mistakeDetected && state.currentMistake != null) {
                // Mistake detected view
                MistakeDetailCard(
                    language = language,
                    mistake = state.currentMistake,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Normal recording view - show current ayah text
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        item {
                            Text(
                                text = currentAyahText,
                                style = MaterialTheme.typography.headlineMedium,
                                fontFamily = kfgqpcHafsFont,
                                textAlign = TextAlign.Center,
                                lineHeight = 48.sp,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recording status and timer
            if (state.mistakeDetected) {
                // Mistake paused state
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.Red
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (language == AppLanguage.ARABIC) "تم اكتشاف خطأ" else "Mistake Detected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
            } else if (state.isAssessing) {
                // Assessing state
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (language == AppLanguage.ARABIC) "جاري التقييم..." else "Assessing...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                // Recording state - pulsing mic indicator
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .scale(pulseScale)
                        .background(Color.Red, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (language == AppLanguage.ARABIC) "جاري التسجيل..." else "Recording...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Red
                )
            }

            // Timer
            val minutes = state.elapsedSeconds / 60
            val seconds = state.elapsedSeconds % 60
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (state.mistakeDetected) Color.Red else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Instruction text
            Text(
                text = if (state.mistakeDetected) {
                    if (language == AppLanguage.ARABIC)
                        "راجع الخطأ ثم اضغط \"استمر\" للمتابعة من الآية ${state.currentMistake?.ayahNumber ?: state.currentAyahNumber}"
                    else
                        "Review the mistake and press \"Continue\" to resume from Ayah ${state.currentMistake?.ayahNumber ?: state.currentAyahNumber}"
                } else {
                    if (language == AppLanguage.ARABIC)
                        "تابع التلاوة - سيتم التقييم تلقائياً"
                    else
                        "Continue reciting - assessment is automatic"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelRecording,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == AppLanguage.ARABIC) "إلغاء" else "Cancel")
                }

                if (state.mistakeDetected) {
                    // Continue button (only shown when mistake detected)
                    Button(
                        onClick = onContinueFromMistake,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (language == AppLanguage.ARABIC) "استمر" else "Continue")
                    }
                }
            }
        }
    }
}

/**
 * Card showing mistake details in real-time mode
 */
@Composable
private fun MistakeDetailCard(
    language: AppLanguage,
    mistake: com.quranmedia.player.recite.domain.model.RealTimeMistake,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (language == AppLanguage.ARABIC)
                        "خطأ في الآية ${mistake.ayahNumber}"
                    else
                        "Mistake in Ayah ${mistake.ayahNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
            }

            Divider(color = Color.Red.copy(alpha = 0.3f))

            // Expected word
            if (mistake.expectedWord.isNotBlank()) {
                Column {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "الكلمة الصحيحة:" else "Expected Word:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = mistake.expectedWord,
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = kfgqpcHafsFont,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            // Detected word
            if (mistake.detectedWord.isNotBlank()) {
                Column {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "ما قلته:" else "What You Said:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = mistake.detectedWord,
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = kfgqpcHafsFont,
                        color = Color.Red
                    )
                }
            }

            // Full ayah text for reference
            Divider(color = Color.Red.copy(alpha = 0.3f))

            Text(
                text = if (language == AppLanguage.ARABIC) "نص الآية:" else "Ayah Text:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                item {
                    Text(
                        text = mistake.expectedText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = kfgqpcHafsFont,
                        textAlign = TextAlign.Right,
                        lineHeight = 36.sp,
                        color = Color(0xFF1B5E20),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Recording card with pulsing animation (full screen overlay)
 */
@Composable
fun RecordingCard(
    language: AppLanguage,
    selection: ReciteSelection,
    elapsedSeconds: Long,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit
) {
    // Pulsing animation for the microphone icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFEBEE)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pulsing microphone icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .background(Color.Red, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
            }

            Text(
                text = if (language == AppLanguage.ARABIC) "جاري التسجيل..." else "Recording...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )

            Text(
                text = selection.surahNameArabic,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = kfgqpcHafsFont
            )

            Text(
                text = if (language == AppLanguage.ARABIC)
                    "الآيات ${selection.startAyah}-${selection.endAyah}"
                else
                    "Ayahs ${selection.startAyah}-${selection.endAyah}",
                style = MaterialTheme.typography.bodyLarge
            )

            // Timer
            val minutes = elapsedSeconds / 60
            val seconds = elapsedSeconds % 60
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelRecording,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == AppLanguage.ARABIC) "إلغاء" else "Cancel")
                }

                Button(
                    onClick = onStopRecording,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == AppLanguage.ARABIC) "إيقاف" else "Stop")
                }
            }
        }
    }
}

/**
 * Processing card with loading indicator
 */
@Composable
fun ProcessingCard(
    language: AppLanguage,
    selection: ReciteSelection
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = if (language == AppLanguage.ARABIC) "جاري المعالجة..." else "Processing...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = selection.surahNameArabic,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = kfgqpcHafsFont
            )

            Text(
                text = if (language == AppLanguage.ARABIC)
                    "يتم الآن تحليل التلاوة"
                else
                    "Analyzing your recitation",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Results view showing Quran text with highlighted mistakes
 */
@Composable
fun ResultsCard(
    language: AppLanguage,
    result: ReciteResult,
    ayahsData: List<Ayah>,  // Will be passed from ViewModel
    onRetry: () -> Unit
) {
    var selectedMismatch by remember { mutableStateOf<WordMismatch?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with accuracy
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "النتائج" else "Results",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // Accuracy badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            result.accuracyPercentage >= 90f -> Color(0xFF4CAF50)
                            result.accuracyPercentage >= 70f -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    ) {
                        Text(
                            text = "${result.accuracyPercentage.roundToInt()}%",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Quran text with highlighted words
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Group ayahs and display with highlighting
                items(ayahsData) { ayah ->
                    QuranAyahWithHighlighting(
                        ayah = ayah,
                        mismatches = result.mismatches,
                        language = language,
                        onWordClick = { mismatch ->
                            selectedMismatch = mismatch
                        }
                    )
                }
            }
        }

        // Floating action button for retry
        FloatingActionButton(
            onClick = onRetry,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = if (language == AppLanguage.ARABIC) "حاول مرة أخرى" else "Try Again")
        }
    }

    // Show mismatch detail dialog when a word is clicked
    selectedMismatch?.let { mismatch ->
        MismatchDetailDialog(
            language = language,
            mismatch = mismatch,
            onDismiss = { selectedMismatch = null }
        )
    }
}

/**
 * Display a single ayah with word-level highlighting for mismatches
 */
@Composable
private fun QuranAyahWithHighlighting(
    ayah: Ayah,
    mismatches: List<WordMismatch>,
    language: AppLanguage,
    onWordClick: (WordMismatch) -> Unit
) {
    // Split the ayah text into words
    val words = ayah.textArabic.split(" ")

    // Find mismatches for this ayah
    val ayahMismatches = mismatches.filter { it.ayahNumber == ayah.ayahNumber }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Ayah number header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (language == AppLanguage.ARABIC)
                        "الآية ${ayah.ayahNumber}"
                    else
                        "Ayah ${ayah.ayahNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                val mistakesInAyah = ayahMismatches.size
                if (mistakesInAyah > 0) {
                    Text(
                        text = if (language == AppLanguage.ARABIC)
                            "$mistakesInAyah أخطاء"
                        else
                            "$mistakesInAyah mistakes",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Red
                    )
                }
            }

            // Display words with highlighting
            Text(
                text = buildAnnotatedString {
                    words.forEachIndexed { index, word ->
                        val mismatch = ayahMismatches.find { it.wordIndex == index }

                        if (mismatch != null) {
                            // Highlighted word (mistake)
                            withStyle(
                                SpanStyle(
                                    color = Color.White,
                                    background = Color.Red,
                                    fontFamily = kfgqpcHafsFont,
                                    fontSize = 24.sp
                                )
                            ) {
                                append(" $word ")
                            }
                        } else {
                            // Normal word
                            withStyle(
                                SpanStyle(
                                    color = Color(0xFF1B5E20),
                                    fontFamily = kfgqpcHafsFont,
                                    fontSize = 24.sp
                                )
                            ) {
                                append("$word ")
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Find clicked word and show detail
                        // For now, just show the first mismatch if any
                        ayahMismatches.firstOrNull()?.let { onWordClick(it) }
                    },
                textAlign = TextAlign.Right,
                lineHeight = 40.sp
            )
        }
    }
}

/**
 * Dialog showing mismatch details
 */
@Composable
private fun MismatchDetailDialog(
    language: AppLanguage,
    mismatch: WordMismatch,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "تفاصيل الخطأ" else "Mistake Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = if (language == AppLanguage.ARABIC) "إغلاق" else "Close")
                    }
                }

                Divider()

                // Ayah and word info
                Text(
                    text = if (language == AppLanguage.ARABIC)
                        "الآية ${mismatch.ayahNumber} - الكلمة ${mismatch.wordIndex + 1}"
                    else
                        "Ayah ${mismatch.ayahNumber} - Word ${mismatch.wordIndex + 1}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Mismatch type
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Red.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = when (mismatch.type) {
                            MismatchType.MISSING -> if (language == AppLanguage.ARABIC) "كلمة ناقصة" else "Missing Word"
                            MismatchType.INCORRECT -> if (language == AppLanguage.ARABIC) "كلمة خاطئة" else "Incorrect Word"
                            MismatchType.EXTRA -> if (language == AppLanguage.ARABIC) "كلمة زائدة" else "Extra Word"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Expected word
                if (mismatch.type != MismatchType.EXTRA) {
                    Column {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "الكلمة الصحيحة:" else "Expected Word:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = mismatch.expectedWord,
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = kfgqpcHafsFont,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                // Detected word
                if (mismatch.detectedWord != null) {
                    Column {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "ما قلته:" else "What You Said:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = mismatch.detectedWord,
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = kfgqpcHafsFont,
                            color = Color.Red
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (language == AppLanguage.ARABIC) "حسناً" else "OK")
                }
            }
        }
    }
}

/**
 * Error card
 */
@Composable
fun ErrorCard(
    language: AppLanguage,
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Text(
                text = if (language == AppLanguage.ARABIC) "خطأ" else "Error",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (language == AppLanguage.ARABIC) "حاول مرة أخرى" else "Try Again")
            }
        }
    }
}
