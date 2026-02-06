package com.quranmedia.player.presentation.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.data.database.entity.DownloadTaskEntity
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.download.DownloadManager
import com.quranmedia.player.domain.model.AudioFormat
import com.quranmedia.player.domain.model.AudioVariant
import com.quranmedia.player.domain.model.Reciter
import com.quranmedia.player.domain.model.Surah
import com.quranmedia.player.domain.repository.QuranRepository
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

data class DownloadWithReciter(
    val download: DownloadTaskEntity,
    val reciterName: String,
    val reciterNameArabic: String?,
    val surahNameArabic: String?,
    val surahNameEnglish: String?
)

data class DownloadsState(
    val downloads: List<DownloadWithReciter> = emptyList(),
    val reciters: List<Reciter> = emptyList(),
    val surahs: List<Surah> = emptyList(),
    val selectedReciter: Reciter? = null,
    val selectedSurah: Surah? = null,
    val isLoading: Boolean = false,
    val showDownloadDialog: Boolean = false,
    val downloadFullQuran: Boolean = false,  // Toggle for full Quran download mode
    val fullQuranDownloading: Boolean = false  // True while full Quran download is in progress
)

data class DownloadsSettings(
    val appLanguage: AppLanguage = AppLanguage.ARABIC,
    val useIndoArabicNumerals: Boolean = false
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadManager: DownloadManager,
    private val settingsRepository: SettingsRepository,
    private val quranRepository: QuranRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DownloadsState())
    val state: StateFlow<DownloadsState> = _state.asStateFlow()

    private val _settings = MutableStateFlow(DownloadsSettings())
    val settings: StateFlow<DownloadsSettings> = _settings.asStateFlow()

    // Cache for reciter names to avoid repeated lookups
    private val reciterCache = mutableMapOf<String, Reciter?>()
    // Cache for surah names
    private val surahCache = mutableMapOf<Int, Surah?>()

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { userSettings ->
                _settings.value = DownloadsSettings(
                    appLanguage = userSettings.appLanguage,
                    useIndoArabicNumerals = userSettings.useIndoArabicNumerals
                )
            }
        }
    }

    init {
        loadDownloads()
        loadSettings()
        loadRecitersAndSurahs()
    }

    private fun loadDownloads() {
        viewModelScope.launch {
            downloadManager.getAllDownloads().collect { downloads ->
                val downloadsWithReciters = downloads.map { download ->
                    val reciter = getReciterCached(download.reciterId)
                    val surah = getSurahCached(download.surahNumber)
                    DownloadWithReciter(
                        download = download,
                        reciterName = reciter?.name ?: formatReciterId(download.reciterId),
                        reciterNameArabic = reciter?.nameArabic,
                        surahNameArabic = surah?.nameArabic,
                        surahNameEnglish = surah?.nameEnglish
                    )
                }
                _state.value = _state.value.copy(
                    downloads = downloadsWithReciters,
                    isLoading = false
                )
            }
        }
    }

    private suspend fun getSurahCached(surahNumber: Int): Surah? {
        return surahCache.getOrPut(surahNumber) {
            quranRepository.getSurahByNumber(surahNumber)
        }
    }

    private fun loadRecitersAndSurahs() {
        viewModelScope.launch {
            try {
                val reciters = quranRepository.getAllReciters().first()
                val surahs = quranRepository.getAllSurahs().first()
                _state.value = _state.value.copy(
                    reciters = reciters,
                    surahs = surahs,
                    selectedReciter = reciters.firstOrNull()
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading reciters and surahs")
            }
        }
    }

    fun showDownloadDialog() {
        _state.value = _state.value.copy(showDownloadDialog = true)
    }

    fun hideDownloadDialog() {
        _state.value = _state.value.copy(
            showDownloadDialog = false,
            selectedSurah = null,
            downloadFullQuran = false
        )
    }

    fun selectReciter(reciter: Reciter) {
        _state.value = _state.value.copy(selectedReciter = reciter)
    }

    fun selectSurah(surah: Surah) {
        _state.value = _state.value.copy(selectedSurah = surah)
    }

    fun toggleFullQuranDownload(enabled: Boolean) {
        _state.value = _state.value.copy(downloadFullQuran = enabled)
    }

    fun startDownload() {
        val reciter = _state.value.selectedReciter ?: return

        viewModelScope.launch {
            try {
                if (_state.value.downloadFullQuran) {
                    // Download full Quran
                    _state.value = _state.value.copy(fullQuranDownloading = true)
                    downloadManager.downloadFullQuran(reciter.id)
                    Timber.d("Full Quran download started for ${reciter.name}")
                    _state.value = _state.value.copy(fullQuranDownloading = false)
                } else {
                    // Download single surah
                    val surah = _state.value.selectedSurah ?: return@launch
                    val audioVariant = AudioVariant(
                        id = "${reciter.id}_${surah.number}",
                        reciterId = reciter.id,
                        surahNumber = surah.number,
                        bitrate = 128,
                        format = AudioFormat.MP3,
                        url = "", // Will be resolved by download manager
                        localPath = null,
                        durationMs = 0L,
                        fileSizeBytes = null,
                        hash = null
                    )
                    downloadManager.downloadAudio(reciter.id, surah.number, audioVariant)
                    Timber.d("Download started for ${surah.nameEnglish} by ${reciter.name}")
                }
                hideDownloadDialog()
            } catch (e: Exception) {
                Timber.e(e, "Error starting download")
                _state.value = _state.value.copy(fullQuranDownloading = false)
            }
        }
    }

    fun cancelAllDownloadsForReciter(reciterId: String) {
        viewModelScope.launch {
            try {
                downloadManager.cancelAllDownloadsForReciter(reciterId)
            } catch (e: Exception) {
                Timber.e(e, "Error cancelling all downloads for reciter")
            }
        }
    }

    fun deleteAllDownloadsForReciter(reciterId: String) {
        viewModelScope.launch {
            try {
                downloadManager.deleteAllDownloadsForReciter(reciterId)
            } catch (e: Exception) {
                Timber.e(e, "Error deleting all downloads for reciter")
            }
        }
    }

    private suspend fun getReciterCached(reciterId: String): Reciter? {
        return reciterCache.getOrPut(reciterId) {
            quranRepository.getReciterById(reciterId)
        }
    }

    // Convert API ID like "ar.abdulbasitmurattal" to readable name
    private fun formatReciterId(reciterId: String): String {
        return reciterId
            .substringAfter(".")
            .replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replaceFirstChar { it.uppercase() }
    }

    fun downloadSurah(reciterId: String, surahNumber: Int, audioVariant: AudioVariant) {
        viewModelScope.launch {
            try {
                downloadManager.downloadAudio(reciterId, surahNumber, audioVariant)
                Timber.d("Download started for surah $surahNumber")
            } catch (e: Exception) {
                Timber.e(e, "Error starting download")
            }
        }
    }

    fun pauseDownload(taskId: String) {
        viewModelScope.launch {
            try {
                downloadManager.pauseDownload(taskId)
            } catch (e: Exception) {
                Timber.e(e, "Error pausing download")
            }
        }
    }

    fun resumeDownload(taskId: String) {
        viewModelScope.launch {
            try {
                downloadManager.resumeDownload(taskId)
            } catch (e: Exception) {
                Timber.e(e, "Error resuming download")
            }
        }
    }

    fun cancelDownload(taskId: String) {
        viewModelScope.launch {
            try {
                downloadManager.cancelDownload(taskId)
            } catch (e: Exception) {
                Timber.e(e, "Error cancelling download")
            }
        }
    }

    fun deleteDownload(reciterId: String, surahNumber: Int) {
        viewModelScope.launch {
            try {
                downloadManager.deleteDownloadedAudio(reciterId, surahNumber)
            } catch (e: Exception) {
                Timber.e(e, "Error deleting download")
            }
        }
    }
}
