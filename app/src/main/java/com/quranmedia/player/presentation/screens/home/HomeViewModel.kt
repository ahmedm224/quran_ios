package com.quranmedia.player.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.data.repository.UserSettings
import com.quranmedia.player.domain.model.Reciter
import com.quranmedia.player.domain.model.Surah
import com.quranmedia.player.domain.repository.QuranRepository
import com.quranmedia.player.media.controller.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class LastPlaybackInfo(
    val reciter: Reciter? = null,
    val surah: Surah? = null,
    val positionMs: Long = 0L
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val settingsRepository: SettingsRepository,
    private val playbackController: PlaybackController
) : ViewModel() {

    val settings = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    val playbackState = playbackController.playbackState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = playbackController.playbackState.value
        )

    private val _reciters = MutableStateFlow<List<Reciter>>(emptyList())
    val reciters: StateFlow<List<Reciter>> = _reciters.asStateFlow()

    private val _selectedReciter = MutableStateFlow<Reciter?>(null)
    val selectedReciter: StateFlow<Reciter?> = _selectedReciter.asStateFlow()

    private val _surahs = MutableStateFlow<List<Surah>>(emptyList())
    val surahs: StateFlow<List<Surah>> = _surahs.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setAppLanguage(language)
        }
    }

    private val _lastPlaybackInfo = MutableStateFlow<LastPlaybackInfo?>(null)
    val lastPlaybackInfo: StateFlow<LastPlaybackInfo?> = _lastPlaybackInfo.asStateFlow()

    init {
        loadLastPlaybackInfo()
        loadReciters()
        loadSurahs()
    }

    private fun loadSurahs() {
        viewModelScope.launch {
            try {
                quranRepository.getAllSurahs().collect { surahList ->
                    _surahs.value = surahList
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading surahs")
            }
        }
    }

    private fun loadReciters() {
        viewModelScope.launch {
            try {
                quranRepository.getAllReciters().collect { reciterList ->
                    _reciters.value = reciterList
                    // Set default selected reciter from saved preference or first in list
                    if (_selectedReciter.value == null && reciterList.isNotEmpty()) {
                        val selectedReciterId = settingsRepository.getSelectedReciterId()
                        _selectedReciter.value = reciterList.find { it.id == selectedReciterId }
                            ?: reciterList.find { it.id == "minshawy-murattal" }
                            ?: reciterList.firstOrNull()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading reciters")
            }
        }
    }

    fun selectReciter(reciter: Reciter) {
        _selectedReciter.value = reciter
        viewModelScope.launch {
            // Save selected reciter to settings - PlaybackController will pick this up
            // on next ayah or when user resumes playback
            settingsRepository.setSelectedReciterId(reciter.id)
            Timber.d("Reciter changed to ${reciter.name}, will apply on next ayah or resume")
        }
    }

    fun togglePlayPause() {
        val state = playbackController.playbackState.value
        if (state.isPlaying) {
            playbackController.pause()
        } else if (state.currentSurah != null) {
            // Resume paused playback - PlaybackController will check for reciter change
            playbackController.play()
        } else {
            // No active playback - try to start from last played surah
            startFromLastPlayback()
        }
    }

    /**
     * Start playback from the beginning of the last played surah.
     * Called when user presses play but there's no active playback.
     * Uses the currently selected reciter (which may differ from last played reciter).
     */
    private fun startFromLastPlayback() {
        viewModelScope.launch {
            try {
                val lastInfo = _lastPlaybackInfo.value
                val surah = lastInfo?.surah
                // Use currently selected reciter, or fall back to last played reciter
                val reciter = _selectedReciter.value ?: lastInfo?.reciter

                if (surah != null && reciter != null) {
                    Timber.d("Starting playback from last surah: ${surah.nameEnglish} with ${reciter.name}")

                    // Get audio variant for this surah/reciter
                    val audioVariants = quranRepository.getAudioVariants(reciter.id, surah.number).first()
                    if (audioVariants.isNotEmpty()) {
                        val audioVariant = audioVariants.first()
                        playbackController.playAudio(
                            reciterId = reciter.id,
                            surahNumber = surah.number,
                            audioUrl = audioVariant.url,
                            surahNameArabic = surah.nameArabic,
                            surahNameEnglish = surah.nameEnglish,
                            reciterName = reciter.name,
                            startFromAyah = 1  // Start from beginning
                        )
                    } else {
                        Timber.e("No audio variants found for surah ${surah.number}")
                    }
                } else {
                    Timber.d("No last playback info available")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error starting from last playback")
            }
        }
    }

    fun stopPlayback() {
        playbackController.stop()
        // Refresh last playback info so UI shows the stopped surah for restart
        viewModelScope.launch {
            // Small delay to ensure PlaybackController has saved the state
            kotlinx.coroutines.delay(100)
            loadLastPlaybackInfo()
        }
    }

    /**
     * Select a surah and start playback from the beginning.
     */
    fun selectSurah(surah: Surah) {
        viewModelScope.launch {
            try {
                val reciter = _selectedReciter.value
                if (reciter == null) {
                    Timber.e("No reciter selected")
                    return@launch
                }

                Timber.d("Starting playback of ${surah.nameEnglish} with ${reciter.name}")

                val audioVariants = quranRepository.getAudioVariants(reciter.id, surah.number).first()
                if (audioVariants.isNotEmpty()) {
                    val audioVariant = audioVariants.first()
                    playbackController.playAudio(
                        reciterId = reciter.id,
                        surahNumber = surah.number,
                        audioUrl = audioVariant.url,
                        surahNameArabic = surah.nameArabic,
                        surahNameEnglish = surah.nameEnglish,
                        reciterName = reciter.name,
                        startFromAyah = 1
                    )
                } else {
                    Timber.e("No audio variants found for surah ${surah.number}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error selecting surah")
            }
        }
    }

    fun previousAyah() {
        playbackController.previousAyah()
    }

    fun nextAyah() {
        playbackController.nextAyah()
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            playbackController.setPlaybackSpeed(speed)
        }
    }

    fun cyclePlaybackSpeed() {
        viewModelScope.launch {
            val currentSpeed = playbackController.playbackState.value.playbackSpeed
            val nextSpeed = when (currentSpeed) {
                1.0f -> 1.25f
                1.25f -> 1.5f
                1.5f -> 2.0f
                else -> 1.0f
            }
            playbackController.setPlaybackSpeed(nextSpeed)
        }
    }

    fun setAyahRepeatCount(count: Int) {
        viewModelScope.launch {
            playbackController.setAyahRepeatCount(count)
        }
    }

    fun cycleAyahRepeatCount() {
        viewModelScope.launch {
            val currentCount = settingsRepository.settings.first().ayahRepeatCount
            val nextCount = when (currentCount) {
                1 -> 2
                2 -> 3
                else -> 1
            }
            playbackController.setAyahRepeatCount(nextCount)
        }
    }

    /**
     * Get the page number for the currently playing ayah.
     * Returns null if no playback is active.
     */
    suspend fun getCurrentPlaybackPage(): Int? {
        val state = playbackController.playbackState.value
        val surah = state.currentSurah
        val ayah = state.currentAyah

        if (surah != null && ayah != null) {
            return quranRepository.getPageForAyah(surah, ayah)
        }
        return null
    }

    /**
     * Check if there's active playback (playing or paused with position)
     */
    fun hasActivePlayback(): Boolean {
        val state = playbackController.playbackState.value
        return state.currentSurah != null && state.currentAyah != null
    }

    fun refreshLastPlaybackInfo() {
        loadLastPlaybackInfo()
    }

    fun refreshSelectedReciter() {
        viewModelScope.launch {
            val reciters = _reciters.value
            if (reciters.isNotEmpty()) {
                val selectedReciterId = settingsRepository.getSelectedReciterId()
                val newReciter = reciters.find { it.id == selectedReciterId }
                if (newReciter != null && newReciter.id != _selectedReciter.value?.id) {
                    _selectedReciter.value = newReciter
                    Timber.d("Refreshed selected reciter to: ${newReciter.name}")
                }
            }
        }
    }

    private fun loadLastPlaybackInfo() {
        viewModelScope.launch {
            try {
                val userSettings = settingsRepository.settings.first()
                val lastReciterId = userSettings.lastReciterId
                val lastSurahNumber = userSettings.lastSurahNumber
                val lastPositionMs = userSettings.lastPositionMs

                Timber.d("Loading last playback: reciter=$lastReciterId, surah=$lastSurahNumber, position=$lastPositionMs")

                if (lastReciterId.isNotBlank() && lastSurahNumber > 0) {
                    val reciter = quranRepository.getReciterById(lastReciterId)
                    val surah = quranRepository.getSurahByNumber(lastSurahNumber)

                    Timber.d("Found reciter: ${reciter?.name}, surah: ${surah?.nameEnglish}")

                    _lastPlaybackInfo.value = LastPlaybackInfo(
                        reciter = reciter,
                        surah = surah,
                        positionMs = lastPositionMs
                    )

                    Timber.d("Last playback info set: ${_lastPlaybackInfo.value}")
                } else {
                    Timber.d("No valid last playback (reciter blank or surah <= 0)")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading last playback info")
            }
        }
    }
}
