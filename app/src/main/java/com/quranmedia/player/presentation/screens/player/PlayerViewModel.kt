package com.quranmedia.player.presentation.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.data.repository.BookmarkRepository
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.domain.repository.QuranRepository
import com.quranmedia.player.media.controller.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val quranRepository: QuranRepository,
    private val settingsRepository: SettingsRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val downloadManager: com.quranmedia.player.download.DownloadManager
) : ViewModel() {

    val playbackState = playbackController.playbackState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = playbackController.playbackState.value
        )

    private val _downloadState = MutableStateFlow<DownloadButtonState>(DownloadButtonState.NotDownloaded)
    val downloadState = _downloadState.asStateFlow()

    suspend fun getSavedPosition(reciterId: String, surahNumber: Int): Long? {
        return try {
            val settings = settingsRepository.settings.first()
            if (settings.lastReciterId == reciterId && settings.lastSurahNumber == surahNumber) {
                Timber.d("Found saved position: ${settings.lastPositionMs}ms")
                settings.lastPositionMs
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting saved position")
            null
        }
    }

    fun loadAudio(reciterId: String, surahNumber: Int, resumePosition: Long? = null, startFromAyah: Int? = null) {
        viewModelScope.launch {
            try {
                val surah = quranRepository.getSurahByNumber(surahNumber)
                val reciter = quranRepository.getReciterById(reciterId)
                val audioVariants = quranRepository.getAudioVariants(reciterId, surahNumber).first()

                if (surah == null || reciter == null) {
                    Timber.e("Surah or reciter not found")
                    return@launch
                }

                if (audioVariants.isEmpty()) {
                    Timber.e("No audio variant found for reciter $reciterId, surah $surahNumber")
                    return@launch
                }

                // Get the first audio variant (there should typically be one per reciter/surah combo)
                val audioVariant = audioVariants.first()
                val audioUrl = audioVariant.url

                if (audioUrl.isBlank()) {
                    Timber.e("Audio URL is blank for reciter $reciterId, surah $surahNumber")
                    return@launch
                }

                Timber.d("Loading audio: $audioUrl, resumePosition=$resumePosition, startFromAyah=$startFromAyah")

                playbackController.playAudio(
                    reciterId = reciterId,
                    surahNumber = surahNumber,
                    audioUrl = audioUrl,
                    surahNameArabic = surah.nameArabic,
                    surahNameEnglish = surah.nameEnglish,
                    reciterName = reciter.name,
                    startFromAyah = startFromAyah,
                    startFromPositionMs = resumePosition
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading audio")
            }
        }
    }

    fun togglePlayPause() {
        if (playbackState.value.isPlaying) {
            playbackController.pause()
        } else {
            playbackController.play()
        }
    }

    fun seekTo(positionMs: Long) {
        playbackController.seekTo(positionMs)
    }

    fun nextAyah() {
        playbackController.nextAyah()
    }

    fun previousAyah() {
        playbackController.previousAyah()
    }

    fun seekToAyah(ayahNumber: Int) {
        playbackController.seekToAyah(ayahNumber)
    }

    fun nudgeForward() {
        playbackController.nudgeForward()
    }

    fun nudgeBackward() {
        playbackController.nudgeBackward()
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            playbackController.setPlaybackSpeed(speed)
        }
    }

    // Bookmark state
    private val _bookmarkSaved = MutableStateFlow(false)
    val bookmarkSaved = _bookmarkSaved.asStateFlow()

    fun createBookmark(label: String? = null) {
        viewModelScope.launch {
            try {
                val state = playbackState.value
                val reciterId = state.currentReciter
                val surahNumber = state.currentSurah
                val ayahNumber = state.currentAyah
                val positionMs = state.currentPosition

                if (reciterId != null && surahNumber != null && ayahNumber != null) {
                    val bookmarkId = bookmarkRepository.insertBookmark(
                        reciterId = reciterId,
                        surahNumber = surahNumber,
                        ayahNumber = ayahNumber,
                        positionMs = positionMs,
                        label = label
                    )
                    Timber.d("Bookmark created: $bookmarkId")
                    _bookmarkSaved.value = true
                    // Reset after a short delay
                    kotlinx.coroutines.delay(2000)
                    _bookmarkSaved.value = false
                } else {
                    Timber.e("Cannot create bookmark: missing playback state")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error creating bookmark")
            }
        }
    }

    fun downloadCurrentSurah() {
        viewModelScope.launch {
            try {
                val state = playbackState.value
                val reciterId = state.currentReciter
                val surahNumber = state.currentSurah

                if (reciterId != null && surahNumber != null) {
                    _downloadState.value = DownloadButtonState.Downloading

                    val audioVariantEntities = quranRepository.getAudioVariants(reciterId, surahNumber).first()
                    if (audioVariantEntities.isNotEmpty()) {
                        val entity = audioVariantEntities.first()
                        val formatString = entity.format.toString().uppercase()
                        val audioFormat = when (formatString) {
                            "MP3" -> com.quranmedia.player.domain.model.AudioFormat.MP3
                            "FLAC" -> com.quranmedia.player.domain.model.AudioFormat.FLAC
                            "M4A" -> com.quranmedia.player.domain.model.AudioFormat.M4A
                            else -> com.quranmedia.player.domain.model.AudioFormat.MP3
                        }

                        val audioVariant = com.quranmedia.player.domain.model.AudioVariant(
                            id = entity.id,
                            reciterId = entity.reciterId,
                            surahNumber = entity.surahNumber,
                            bitrate = entity.bitrate,
                            format = audioFormat,
                            url = entity.url,
                            localPath = entity.localPath,
                            durationMs = entity.durationMs,
                            fileSizeBytes = entity.fileSizeBytes,
                            hash = entity.hash
                        )
                        downloadManager.downloadAudio(reciterId, surahNumber, audioVariant)

                        _downloadState.value = DownloadButtonState.Downloaded
                        Timber.d("Download started for surah $surahNumber")
                    } else {
                        _downloadState.value = DownloadButtonState.NotDownloaded
                        Timber.e("No audio variant found for download")
                    }
                } else {
                    Timber.e("Cannot download: missing playback state")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error downloading surah")
                _downloadState.value = DownloadButtonState.NotDownloaded
            }
        }
    }

    fun checkDownloadStatus(reciterId: String, surahNumber: Int) {
        viewModelScope.launch {
            try {
                // Observe download status changes continuously
                downloadManager.getAllDownloads().collect { downloads ->
                    val downloadTask = downloads.firstOrNull {
                        it.reciterId == reciterId && it.surahNumber == surahNumber
                    }

                    _downloadState.value = when {
                        downloadTask == null -> DownloadButtonState.NotDownloaded
                        downloadTask.status == com.quranmedia.player.data.database.entity.DownloadStatus.COMPLETED.name -> {
                            Timber.d("Download completed for surah $surahNumber")
                            DownloadButtonState.Downloaded
                        }
                        downloadTask.status == com.quranmedia.player.data.database.entity.DownloadStatus.IN_PROGRESS.name -> {
                            Timber.d("Download in progress for surah $surahNumber: ${(downloadTask.progress * 100).toInt()}%")
                            DownloadButtonState.Downloading
                        }
                        else -> DownloadButtonState.NotDownloaded
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking download status")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("PlayerViewModel cleared")
    }
}

sealed class DownloadButtonState {
    object NotDownloaded : DownloadButtonState()
    object Downloading : DownloadButtonState()
    object Downloaded : DownloadButtonState()
}
