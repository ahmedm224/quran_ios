package com.quranmedia.player.recite.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.repository.ReadingTheme
import com.quranmedia.player.recite.domain.model.HighlightType
import com.quranmedia.player.recite.domain.model.WordHighlightState
import com.quranmedia.player.recite.presentation.components.ReciteQuranPage
import com.quranmedia.player.presentation.screens.reader.components.kfgqpcHafsFont

/**
 * Minimal Recite screen - shows Quran page with recite button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReciteStreamingScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReciteStreamingViewModel = hiltViewModel()
) {
    val streamingState by viewModel.streamingState.collectAsState()
    val selectedAyahs by viewModel.selectedAyahs.collectAsState()
    val wordHighlights by viewModel.wordHighlights.collectAsState()
    val errorHighlights by viewModel.errorHighlights.collectAsState()
    val selection by viewModel.selection.collectAsState()
    val allSurahs by viewModel.allSurahs.collectAsState()
    val context = LocalContext.current

    // Selection dialog state
    var showSelectionDialog by remember { mutableStateOf(false) }
    var selectedSurah by remember { mutableStateOf(1) }
    var startAyah by remember { mutableStateOf(1) }
    var endAyah by remember { mutableStateOf(7) }

    // Sync dialog state with ViewModel's current selection
    LaunchedEffect(selection) {
        selection?.let {
            selectedSurah = it.surahNumber
            startAyah = it.startAyah
            endAyah = it.endAyah
        }
    }

    // Error dialog state
    var selectedError by remember { mutableStateOf<WordHighlightState?>(null) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startStreaming()
        }
    }

    fun requestPermissionAndStart() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.startStreaming()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Show selection dialog on first launch (don't auto-load any surah)
    LaunchedEffect(allSurahs) {
        if (allSurahs.isNotEmpty() && selectedAyahs.isEmpty()) {
            // Don't auto-load - show dialog for user to select
            showSelectionDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Quran Page - always visible
        if (selectedAyahs.isNotEmpty()) {
            ReciteQuranPage(
                pageNumber = selectedAyahs.firstOrNull()?.page ?: 1,
                ayahs = selectedAyahs,
                wordHighlights = wordHighlights,
                readingTheme = ReadingTheme.LIGHT,
                modifier = Modifier.fillMaxSize(),
                onWordClick = { highlight ->
                    if (highlight.type == HighlightType.ERROR) {
                        selectedError = highlight
                    }
                }
            )
        }

        // Back button (top left)
        IconButton(
            onClick = {
                if (streamingState is ReciteStreamingState.Streaming) {
                    viewModel.stopStreaming()
                }
                onNavigateBack()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // Streaming indicator (top center)
        if (streamingState is ReciteStreamingState.Streaming) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = 0.8f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Listening...", color = Color.White, fontSize = 14.sp)
                if (errorHighlights.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("${errorHighlights.size} errors", color = Color.Yellow, fontSize = 14.sp)
                }
            }
        }

        // Connecting indicator
        if (streamingState is ReciteStreamingState.Connecting) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .clip(CircleShape)
                    .background(Color.Blue.copy(alpha = 0.8f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Connecting...", color = Color.White, fontSize = 14.sp)
            }
        }

        // FAB - Recite button (bottom right)
        FloatingActionButton(
            onClick = {
                when (streamingState) {
                    is ReciteStreamingState.Streaming -> viewModel.stopStreaming()
                    is ReciteStreamingState.Connecting -> viewModel.stopStreaming()
                    else -> showSelectionDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = if (streamingState is ReciteStreamingState.Streaming) Color.Red else MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (streamingState is ReciteStreamingState.Streaming) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = "Recite"
            )
        }
    }

    // Selection Dialog
    if (showSelectionDialog) {
        val currentSurah = allSurahs.find { it.number == selectedSurah }
        val maxAyah = currentSurah?.ayahCount ?: 7

        AlertDialog(
            onDismissRequest = { showSelectionDialog = false },
            title = { Text("Select Ayahs to Recite") },
            text = {
                Column {
                    // Surah dropdown
                    Text("Surah:", fontSize = 12.sp)
                    var surahExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = surahExpanded,
                        onExpandedChange = { surahExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = currentSurah?.let { "${it.number}. ${it.nameEnglish}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(surahExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = surahExpanded,
                            onDismissRequest = { surahExpanded = false }
                        ) {
                            allSurahs.forEach { surah ->
                                DropdownMenuItem(
                                    text = { Text("${surah.number}. ${surah.nameEnglish}") },
                                    onClick = {
                                        selectedSurah = surah.number
                                        startAyah = 1
                                        endAyah = minOf(7, surah.ayahCount)
                                        surahExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Ayah range
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("From:", fontSize = 12.sp)
                            OutlinedTextField(
                                value = startAyah.toString(),
                                onValueChange = {
                                    it.toIntOrNull()?.let { v ->
                                        startAyah = v.coerceIn(1, maxAyah)
                                        if (endAyah < startAyah) endAyah = startAyah
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("To:", fontSize = 12.sp)
                            OutlinedTextField(
                                value = endAyah.toString(),
                                onValueChange = {
                                    it.toIntOrNull()?.let { v ->
                                        endAyah = v.coerceIn(startAyah, maxAyah)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateSelection(selectedSurah, startAyah, endAyah, allSurahs)
                    showSelectionDialog = false
                    requestPermissionAndStart()
                }) {
                    Icon(Icons.Default.Mic, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Reciting")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error detail dialog - informational only, user repeats ayah automatically
    if (selectedError != null) {
        AlertDialog(
            onDismissRequest = { selectedError = null },
            title = { Text("Error at Ayah ${selectedError!!.ayahNumber}") },
            text = {
                Column {
                    Text("Expected:", fontSize = 12.sp, color = Color.Green)
                    Text(
                        selectedError!!.expectedWord,
                        fontFamily = kfgqpcHafsFont,
                        fontSize = 24.sp,
                        color = Color.Green
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Detected:", fontSize = 12.sp, color = Color.Red)
                    Text(
                        selectedError!!.transcribedWord.ifEmpty { "(skipped)" },
                        fontFamily = kfgqpcHafsFont,
                        fontSize = 24.sp,
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Please repeat this ayah correctly to continue.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedError = null }) { Text("OK") }
            }
        )
    }
}
