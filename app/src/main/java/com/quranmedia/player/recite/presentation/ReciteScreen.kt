package com.quranmedia.player.recite.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.recite.domain.model.ReciteState
import com.quranmedia.player.recite.presentation.components.*

/**
 * Main Recite screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReciteScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReciteViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val allSurahs by viewModel.allSurahs.collectAsState()
    val language by viewModel.language.collectAsState()
    val selectedAyahs by viewModel.selectedAyahs.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    // Permission launcher for RECORD_AUDIO
    var pendingRecordingSelection by remember { mutableStateOf<com.quranmedia.player.recite.domain.model.ReciteSelection?>(null) }
    var pendingIsRealTime by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingRecordingSelection?.let { selection ->
                if (pendingIsRealTime) {
                    viewModel.startRealTimeRecording(selection)
                } else {
                    viewModel.startRecording(selection)
                }
            }
        } else {
            viewModel.reset()
        }
        pendingRecordingSelection = null
        pendingIsRealTime = false
    }

    fun handleStartRecording(selection: com.quranmedia.player.recite.domain.model.ReciteSelection, isRealTime: Boolean) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            if (isRealTime) {
                viewModel.startRealTimeRecording(selection)
            } else {
                viewModel.startRecording(selection)
            }
        } else {
            pendingRecordingSelection = selection
            pendingIsRealTime = isRealTime
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "التسميع" else "Recite"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = if (language == AppLanguage.ARABIC) "رجوع" else "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val currentState = state) {
                is ReciteState.Idle -> {
                    WelcomeCard(
                        language = language,
                        onStartSelection = { viewModel.startSelection() }
                    )
                }

                is ReciteState.Selecting -> {
                    SelectionCard(
                        language = language,
                        allSurahs = allSurahs,
                        currentSelection = currentState.selection,
                        realTimeMode = settings.reciteRealTimeAssessment,
                        onSelectionChanged = { surahNumber, startAyah, endAyah ->
                            viewModel.updateSelection(
                                surahNumber = surahNumber,
                                startAyah = startAyah,
                                endAyah = endAyah,
                                allSurahs = allSurahs
                            )
                        },
                        onStartRecording = { selection, isRealTime ->
                            handleStartRecording(selection, isRealTime)
                        },
                        onCancel = {
                            viewModel.reset()
                        }
                    )
                }

                is ReciteState.Recording -> {
                    RecordingCard(
                        language = language,
                        selection = currentState.selection,
                        elapsedSeconds = currentState.elapsedSeconds,
                        onStopRecording = { viewModel.stopRecording() },
                        onCancelRecording = { viewModel.cancelRecording() }
                    )
                }

                is ReciteState.RealTimeRecording -> {
                    // Get current ayah text based on state
                    val currentAyahText = selectedAyahs.find {
                        it.ayahNumber == currentState.currentAyahNumber
                    }?.textArabic ?: ""

                    RealTimeRecordingCard(
                        language = language,
                        state = currentState,
                        currentAyahText = currentAyahText,
                        onContinueFromMistake = { viewModel.continueFromMistake() },
                        onCancelRecording = { viewModel.cancelRealTimeRecording() }
                    )
                }

                is ReciteState.Processing -> {
                    ProcessingCard(
                        language = language,
                        selection = currentState.selection
                    )
                }

                is ReciteState.ShowingResults -> {
                    ResultsCard(
                        language = language,
                        result = currentState.result,
                        ayahsData = selectedAyahs,
                        onRetry = { viewModel.reset() }
                    )
                }

                is ReciteState.Error -> {
                    ErrorCard(
                        language = language,
                        message = currentState.message,
                        onRetry = { viewModel.reset() }
                    )
                }
            }
        }
    }
}
