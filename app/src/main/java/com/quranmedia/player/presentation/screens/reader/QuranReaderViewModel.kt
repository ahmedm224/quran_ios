package com.quranmedia.player.presentation.screens.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.data.database.dao.AyahDao
import com.quranmedia.player.data.database.dao.ReadingBookmarkDao
import com.quranmedia.player.data.database.entity.AyahEntity
import com.quranmedia.player.data.database.entity.ReadingBookmarkEntity
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.data.repository.UserSettings
import com.quranmedia.player.domain.model.Ayah
import com.quranmedia.player.domain.model.Reciter
import com.quranmedia.player.domain.model.Surah
import com.quranmedia.player.domain.repository.QuranRepository
import com.quranmedia.player.data.repository.TafseerRepository
import com.quranmedia.player.domain.model.AvailableTafseers
import com.quranmedia.player.presentation.screens.reader.components.TafseerModalState
import com.quranmedia.player.presentation.screens.reader.components.surahNamesArabic
import com.quranmedia.player.media.controller.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class ReadingBookmark(
    val id: String,
    val pageNumber: Int,
    val surahName: String?,
    val label: String?,
    val createdAt: Long
)

data class QuranReaderState(
    val currentPage: Int = 1,
    val totalPages: Int = 604,
    val ayahsOnPage: List<Ayah> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val highlightedAyah: HighlightedAyah? = null,
    val isMetadataReady: Boolean = false,
    val reciters: List<Reciter> = emptyList(),
    val selectedReciter: Reciter? = null,
    val readingBookmarks: List<ReadingBookmark> = emptyList(),
    val isCurrentPageBookmarked: Boolean = false,
    val dailyTargetPages: Float? = null  // Pages needed per day to finish by month end
)

data class HighlightedAyah(
    val surahNumber: Int,
    val ayahNumber: Int
)

sealed class DownloadState {
    object NotDownloaded : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    object Downloaded : DownloadState()
}

@HiltViewModel
class QuranReaderViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val ayahDao: AyahDao,
    private val readingBookmarkDao: ReadingBookmarkDao,
    private val playbackController: PlaybackController,
    private val settingsRepository: SettingsRepository,
    private val downloadManager: com.quranmedia.player.download.DownloadManager,
    private val trackerRepository: com.quranmedia.player.data.repository.TrackerRepository,
    private val prayerTimesRepository: com.quranmedia.player.domain.repository.PrayerTimesRepository,
    val qcfAssetLoader: com.quranmedia.player.data.source.QCFAssetLoader,
    private val fontDownloadManager: com.quranmedia.player.data.source.QCFFontDownloadManager,
    private val tafseerRepository: TafseerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(QuranReaderState())
    val state: StateFlow<QuranReaderState> = _state.asStateFlow()

    // Search state
    private val _searchResults = MutableStateFlow<List<Ayah>>(emptyList())
    val searchResults: StateFlow<List<Ayah>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    // Tafseer state
    private val _tafseerState = MutableStateFlow(TafseerModalState())
    val tafseerState: StateFlow<TafseerModalState> = _tafseerState.asStateFlow()

    // Page tracking for daily progress
    private val pageViewStartTime = mutableMapOf<Int, Long>()
    private val MIN_READ_TIME_MS = 10_000L  // 10 seconds to count as "read"

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

    /**
     * Check if QCF fonts are available for the requested mode.
     * Returns true if at least one font pack is downloaded.
     */
    fun areQCFFontsAvailable(): Boolean {
        return fontDownloadManager.isSVGDownloaded() || fontDownloadManager.isV4Downloaded()
    }

    /**
     * Check if SVG Mushaf pages are downloaded
     */
    fun isSVGDownloaded(): Boolean = fontDownloadManager.isSVGDownloaded()

    /**
     * Check if V4 (tajweed) fonts are downloaded
     */
    fun isV4FontsDownloaded(): Boolean = fontDownloadManager.isV4Downloaded()

    private var _totalPages: Int = 604

    init {
        initializeTotalPages()
        observePlaybackForHighlighting()
        loadReciters()
        observeReadingBookmarks()
        loadDailyTarget()
    }

    private fun initializeTotalPages() {
        viewModelScope.launch {
            try {
                // Wait for metadata to be ready (retry a few times if needed)
                var retries = 0
                var maxPage: Int? = null

                while (retries < 10 && (maxPage == null || maxPage == 0)) {
                    maxPage = ayahDao.getMaxPageNumber()
                    if (maxPage == null || maxPage == 0) {
                        Timber.d("Waiting for metadata to be populated (attempt ${retries + 1})")
                        kotlinx.coroutines.delay(1000) // Wait 1 second
                        retries++
                    }
                }

                if (maxPage != null && maxPage > 0) {
                    _totalPages = maxPage
                    _state.value = _state.value.copy(
                        totalPages = maxPage,
                        isMetadataReady = true,
                        isLoading = false
                    )
                    Timber.d("Total Quran pages: $maxPage")
                } else {
                    Timber.w("Metadata not yet populated, using default 604 pages")
                    _state.value = _state.value.copy(
                        isMetadataReady = false,
                        isLoading = false,
                        error = "Loading Quran data... Please wait and try again."
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error getting total pages")
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    private fun observePlaybackForHighlighting() {
        viewModelScope.launch {
            playbackController.playbackState.collect { playbackState ->
                val currentSurah = playbackState.currentSurah
                val currentAyah = playbackState.currentAyah

                Timber.d("Playback state update - surah: $currentSurah, ayah: $currentAyah, playing: ${playbackState.isPlaying}")

                // Highlight ayah when there's active playback (even when paused)
                if (currentSurah != null && currentAyah != null) {
                    _state.value = _state.value.copy(
                        highlightedAyah = HighlightedAyah(currentSurah, currentAyah)
                    )
                    Timber.d("Highlighting ayah: surah $currentSurah, ayah $currentAyah")
                } else {
                    _state.value = _state.value.copy(highlightedAyah = null)
                }
            }
        }
    }

    fun loadPage(pageNumber: Int) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, currentPage = pageNumber)

                val ayahs = quranRepository.getAyahsByPage(pageNumber).first()

                _state.value = _state.value.copy(
                    ayahsOnPage = ayahs,
                    isLoading = false,
                    error = if (ayahs.isEmpty()) "No ayahs found for page $pageNumber" else null
                )

                // Update last reading timestamp for reminder system
                settingsRepository.updateLastReadingTimestamp()

                // Track page view start time for progress tracking
                pageViewStartTime[pageNumber] = System.currentTimeMillis()
                trackerRepository.updateLastReadingPosition(pageNumber)

                // Update daily target (recalculates fallback based on current page)
                loadDailyTarget()

                Timber.d("Loaded page $pageNumber with ${ayahs.size} ayahs (tracking started)")
            } catch (e: Exception) {
                Timber.e(e, "Error loading page $pageNumber")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load page"
                )
            }
        }
    }

    /**
     * Called when user leaves a page (navigates to another page)
     * Tracks reading progress if page was viewed for minimum duration
     */
    fun onPageLeft(pageNumber: Int) {
        viewModelScope.launch {
            val startTime = pageViewStartTime[pageNumber]
            if (startTime != null) {
                val duration = System.currentTimeMillis() - startTime

                // Consider page "read" if viewed for >10 seconds
                if (duration >= MIN_READ_TIME_MS) {
                    trackerRepository.incrementPagesRead(1)
                    trackerRepository.incrementReadingDuration(duration)
                    Timber.d("Page $pageNumber tracked as read (duration: ${duration}ms)")
                } else {
                    Timber.d("Page $pageNumber not counted (viewed for only ${duration}ms)")
                }

                pageViewStartTime.remove(pageNumber)
            }
        }
    }

    fun goToPage(pageNumber: Int) {
        if (pageNumber in 1.._totalPages) {
            loadPage(pageNumber)
        }
    }

    fun nextPage() {
        val current = _state.value.currentPage
        if (current < _totalPages) {
            loadPage(current + 1)
        }
    }

    fun previousPage() {
        val current = _state.value.currentPage
        if (current > 1) {
            loadPage(current - 1)
        }
    }

    suspend fun getPageForSurah(surahNumber: Int): Int? {
        return quranRepository.getFirstPageOfSurah(surahNumber)
    }

    suspend fun getPageForJuz(juzNumber: Int): Int? {
        return quranRepository.getFirstPageOfJuz(juzNumber)
    }

    suspend fun getPageForAyah(surahNumber: Int, ayahNumber: Int): Int? {
        return quranRepository.getPageForAyah(surahNumber, ayahNumber)
    }

    fun navigateToCurrentPlaybackPage() {
        viewModelScope.launch {
            val playback = playbackState.value
            val surah = playback.currentSurah
            val ayah = playback.currentAyah

            if (surah != null && ayah != null) {
                val page = getPageForAyah(surah, ayah)
                if (page != null) {
                    goToPage(page)
                }
            }
        }
    }

    fun getAyahsForPage(pageNumber: Int): kotlinx.coroutines.flow.Flow<List<com.quranmedia.player.domain.model.Ayah>> {
        return quranRepository.getAyahsByPage(pageNumber)
    }

    private fun loadReciters() {
        viewModelScope.launch {
            try {
                val reciters = quranRepository.getAllReciters().first()

                // Check if there's already an active reciter from current playback
                val currentPlaybackReciterId = playbackController.playbackState.value.currentReciter
                val currentReciter = if (currentPlaybackReciterId != null) {
                    reciters.find { it.id == currentPlaybackReciterId }
                } else {
                    null
                }

                // Load saved reciter preference, or use default (Minshawy Murattal)
                val savedReciterId = settingsRepository.getSelectedReciterId()
                val savedReciter = reciters.find { it.id == savedReciterId }

                // Priority: current playback > saved preference > default (Minshawy Murattal)
                val selectedReciter = currentReciter
                    ?: savedReciter
                    ?: reciters.find { it.id == "minshawy-murattal" }
                    ?: reciters.firstOrNull()

                _state.value = _state.value.copy(
                    reciters = reciters,
                    selectedReciter = selectedReciter
                )
                Timber.d("Loaded ${reciters.size} reciters, selected: ${selectedReciter?.name ?: "none"} (saved: $savedReciterId, fromPlayback: ${currentReciter != null})")
            } catch (e: Exception) {
                Timber.e(e, "Error loading reciters")
                // Don't crash - just leave empty list
            }
        }
    }

    fun selectReciter(reciter: Reciter) {
        val previousReciter = _state.value.selectedReciter
        _state.value = _state.value.copy(selectedReciter = reciter)
        // Save reciter preference so it persists across app restarts
        viewModelScope.launch {
            settingsRepository.setSelectedReciterId(reciter.id)

            // If reciter changed during active playback, restart current ayah with new reciter
            if (previousReciter != null && previousReciter.id != reciter.id) {
                val currentPlayback = playbackState.value
                if (currentPlayback.isPlaying && currentPlayback.currentSurah != null && currentPlayback.currentAyah != null) {
                    val surahNumber = currentPlayback.currentSurah
                    val currentAyahNumber = currentPlayback.currentAyah

                    // Replay current ayah with new reciter
                    Timber.d("Reciter changed during playback, switching to ${reciter.name} at current ayah $currentAyahNumber")
                    playFromAyah(surahNumber, currentAyahNumber)
                }
            }
        }
        Timber.d("Selected reciter: ${reciter.name} (saved)")
    }

    /**
     * Refresh the selected reciter from settings.
     * Called when returning to the screen to sync with any changes made elsewhere.
     */
    fun refreshSelectedReciter() {
        viewModelScope.launch {
            val reciters = _state.value.reciters
            if (reciters.isNotEmpty()) {
                val selectedReciterId = settingsRepository.getSelectedReciterId()
                val newReciter = reciters.find { it.id == selectedReciterId }
                if (newReciter != null && newReciter.id != _state.value.selectedReciter?.id) {
                    _state.value = _state.value.copy(selectedReciter = newReciter)
                    Timber.d("Refreshed selected reciter to: ${newReciter.name}")
                }
            }
        }
    }

    fun playFromAyah(surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch {
            val reciter = _state.value.selectedReciter
            if (reciter == null) {
                Timber.e("No reciter selected")
                return@launch
            }

            try {
                // Get surah info
                val surah = quranRepository.getSurahByNumber(surahNumber)
                if (surah == null) {
                    Timber.e("Surah $surahNumber not found")
                    return@launch
                }

                // Get audio variant for this surah/reciter
                val audioVariants = quranRepository.getAudioVariants(reciter.id, surahNumber).first()
                if (audioVariants.isEmpty()) {
                    Timber.e("No audio variant found for reciter ${reciter.id}, surah $surahNumber")
                    return@launch
                }

                val audioVariant = audioVariants.first()
                val audioUrl = audioVariant.url

                if (audioUrl.isBlank()) {
                    Timber.e("Audio URL is blank")
                    return@launch
                }

                Timber.d("Playing surah $surahNumber from ayah $ayahNumber with reciter ${reciter.name}")

                playbackController.playAudio(
                    reciterId = reciter.id,
                    surahNumber = surahNumber,
                    audioUrl = audioUrl,
                    surahNameArabic = surah.nameArabic,
                    surahNameEnglish = surah.nameEnglish,
                    reciterName = reciter.name,
                    startFromAyah = ayahNumber,
                    startFromPositionMs = null
                )
            } catch (e: Exception) {
                Timber.e(e, "Error starting playback")
            }
        }
    }

    fun togglePlayPause() {
        val currentPlayback = playbackState.value
        if (currentPlayback.isPlaying) {
            playbackController.pause()
        } else if (currentPlayback.currentSurah != null) {
            playbackController.play()
        }
    }

    /**
     * Play downloaded audio for a specific surah with a specific reciter.
     * Called when navigating from Downloads screen.
     */
    fun playFromDownload(reciterId: String, surahNumber: Int) {
        viewModelScope.launch {
            try {
                // Find and select the reciter
                val reciter = quranRepository.getReciterById(reciterId)
                if (reciter != null) {
                    _state.value = _state.value.copy(selectedReciter = reciter)
                }

                // Get surah info
                val surah = quranRepository.getSurahByNumber(surahNumber)
                if (surah == null) {
                    Timber.e("Surah $surahNumber not found")
                    return@launch
                }

                // Get audio variant for this surah/reciter
                val audioVariants = quranRepository.getAudioVariants(reciterId, surahNumber).first()
                if (audioVariants.isEmpty()) {
                    Timber.e("No audio variant found for reciter $reciterId, surah $surahNumber")
                    return@launch
                }

                val audioVariant = audioVariants.first()
                val audioUrl = audioVariant.localPath ?: audioVariant.url

                if (audioUrl.isBlank()) {
                    Timber.e("Audio URL is blank")
                    return@launch
                }

                val reciterName = reciter?.name ?: reciterId.substringAfter(".")

                Timber.d("Playing downloaded surah $surahNumber with reciter $reciterId")

                playbackController.playAudio(
                    reciterId = reciterId,
                    surahNumber = surahNumber,
                    audioUrl = audioUrl,
                    surahNameArabic = surah.nameArabic,
                    surahNameEnglish = surah.nameEnglish,
                    reciterName = reciterName,
                    startFromAyah = 1,
                    startFromPositionMs = null
                )
            } catch (e: Exception) {
                Timber.e(e, "Error starting playback from download")
            }
        }
    }

    fun stopPlayback() {
        playbackController.stop()
    }

    // Advanced playback controls
    fun seekTo(positionMs: Long) {
        playbackController.seekTo(positionMs)
    }

    fun seekToAyah(ayahNumber: Int) {
        playbackController.seekToAyah(ayahNumber)
    }

    fun nextAyah() {
        playbackController.nextAyah()
    }

    fun previousAyah() {
        playbackController.previousAyah()
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            playbackController.setPlaybackSpeed(speed)
        }
    }

    fun setAyahRepeatCount(count: Int) {
        viewModelScope.launch {
            playbackController.setAyahRepeatCount(count)
        }
    }

    // Reading Bookmark Functions
    private fun observeReadingBookmarks() {
        viewModelScope.launch {
            readingBookmarkDao.getAllReadingBookmarks().collect { bookmarks ->
                val mappedBookmarks = bookmarks.map { entity ->
                    ReadingBookmark(
                        id = entity.id,
                        pageNumber = entity.pageNumber,
                        surahName = entity.surahName,
                        label = entity.label,
                        createdAt = entity.createdAt
                    )
                }
                val currentPage = _state.value.currentPage
                val isBookmarked = bookmarks.any { it.pageNumber == currentPage }
                _state.value = _state.value.copy(
                    readingBookmarks = mappedBookmarks,
                    isCurrentPageBookmarked = isBookmarked
                )
            }
        }
    }

    /**
     * Load daily target pages needed to finish Quran by end of month
     * Uses active goal if available, otherwise calculates based on current page
     */
    private fun loadDailyTarget() {
        viewModelScope.launch {
            try {
                // First, try to get from active goal
                val activeGoal = trackerRepository.getActiveGoal().first()

                val dailyTarget = if (activeGoal != null) {
                    // Use goal's daily target
                    val goalProgress = trackerRepository.calculateKhatmahProgress(activeGoal.id)
                    goalProgress?.dailyTargetPages
                } else {
                    // Fallback: Calculate based on current page and days remaining in Hijri month
                    calculateFallbackDailyTarget(_state.value.currentPage)
                }

                _state.value = _state.value.copy(dailyTargetPages = dailyTarget)
                Timber.d("Daily target: ${dailyTarget ?: "N/A"} pages/day")
            } catch (e: Exception) {
                Timber.e(e, "Error loading daily target")
            }
        }
    }

    /**
     * Calculate daily target when no goal is set
     * Based on current page and days remaining in Hijri month
     */
    private suspend fun calculateFallbackDailyTarget(currentPage: Int): Float? {
        return try {
            // Get current Hijri date
            val prayerTimes = prayerTimesRepository.getCachedPrayerTimes(java.time.LocalDate.now()).first()

            val hijriDate = prayerTimes?.hijriDate
                ?: com.quranmedia.player.domain.util.HijriCalendarUtils.gregorianToHijri(java.time.LocalDate.now())

            // Calculate days remaining in month
            val daysRemaining = com.quranmedia.player.domain.util.HijriCalendarUtils
                .getDaysRemainingInMonth(hijriDate)

            if (daysRemaining > 0) {
                // Pages remaining to finish Quran
                val pagesRemaining = 604 - currentPage
                pagesRemaining.toFloat() / daysRemaining
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error calculating fallback daily target")
            null
        }
    }

    fun toggleBookmarkCurrentPage() {
        viewModelScope.launch {
            val currentPage = _state.value.currentPage
            val existingBookmark = readingBookmarkDao.getBookmarkForPage(currentPage)

            if (existingBookmark != null) {
                // Remove bookmark
                readingBookmarkDao.deleteBookmark(existingBookmark.id)
                Timber.d("Removed bookmark for page $currentPage")
            } else {
                // Add bookmark - get surah name from current ayahs
                val surahName = _state.value.ayahsOnPage.firstOrNull()?.let { ayah ->
                    getSurahNameForNumber(ayah.surahNumber)
                }

                val bookmark = ReadingBookmarkEntity(
                    pageNumber = currentPage,
                    surahNumber = _state.value.ayahsOnPage.firstOrNull()?.surahNumber,
                    surahName = surahName,
                    label = "Page $currentPage"
                )
                readingBookmarkDao.insertBookmark(bookmark)
                Timber.d("Added bookmark for page $currentPage")
            }
        }
    }

    fun deleteReadingBookmark(bookmarkId: String) {
        viewModelScope.launch {
            readingBookmarkDao.deleteBookmark(bookmarkId)
            Timber.d("Deleted bookmark $bookmarkId")
        }
    }

    private suspend fun getSurahNameForNumber(surahNumber: Int): String? {
        return quranRepository.getSurahByNumber(surahNumber)?.nameArabic
    }

    fun updateCurrentPageBookmarkStatus(pageNumber: Int) {
        viewModelScope.launch {
            val existingBookmark = readingBookmarkDao.getBookmarkForPage(pageNumber)
            _state.value = _state.value.copy(
                isCurrentPageBookmarked = existingBookmark != null
            )
        }
    }

    // Download state for current surah
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.NotDownloaded)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun checkDownloadStatus() {
        viewModelScope.launch {
            val playback = playbackState.value
            val reciterId = playback.currentReciter ?: return@launch
            val surahNumber = playback.currentSurah ?: return@launch

            downloadManager.getAllDownloads().collect { downloads ->
                val downloadTask = downloads.firstOrNull {
                    it.reciterId == reciterId && it.surahNumber == surahNumber
                }

                _downloadState.value = when {
                    downloadTask == null -> DownloadState.NotDownloaded
                    downloadTask.status == com.quranmedia.player.data.database.entity.DownloadStatus.COMPLETED.name -> {
                        DownloadState.Downloaded
                    }
                    downloadTask.status == com.quranmedia.player.data.database.entity.DownloadStatus.IN_PROGRESS.name -> {
                        DownloadState.Downloading(downloadTask.progress)
                    }
                    else -> DownloadState.NotDownloaded
                }
            }
        }
    }

    fun downloadCurrentSurah() {
        viewModelScope.launch {
            try {
                val playback = playbackState.value
                val reciterId = playback.currentReciter
                val surahNumber = playback.currentSurah

                if (reciterId == null || surahNumber == null) {
                    Timber.e("Cannot download: no active playback")
                    return@launch
                }

                _downloadState.value = DownloadState.Downloading(0f)

                val audioVariantEntities = quranRepository.getAudioVariants(reciterId, surahNumber).first()
                if (audioVariantEntities.isNotEmpty()) {
                    val entity = audioVariantEntities.first()
                    val formatString = entity.format.name.uppercase()
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
                    Timber.d("Download started for surah $surahNumber")

                    // Start observing download status
                    checkDownloadStatus()
                } else {
                    _downloadState.value = DownloadState.NotDownloaded
                    Timber.e("No audio variant found for download")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error downloading surah")
                _downloadState.value = DownloadState.NotDownloaded
            }
        }
    }

    // Search functions
    fun searchQuran(query: String) {
        searchJob?.cancel()

        if (query.trim().length < 2) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(300) // Debounce

            try {
                // Create search variants to handle Quranic orthography (silent alef, etc.)
                val searchVariants = createSearchVariants(query.trim())
                Timber.d("Searching with variants: $searchVariants")

                // Search across all ayahs by fetching from database and filtering
                val allResults = mutableListOf<AyahEntity>()
                for (surahNum in 1..114) {
                    val surahAyahs = ayahDao.getAyahsBySurahSync(surahNum)
                    val matchingAyahs = surahAyahs.filter { entity ->
                        val normalizedText = stripArabicDiacritics(entity.textArabic)
                        // Check if any search variant matches
                        searchVariants.any { variant -> normalizedText.contains(variant) }
                    }
                    allResults.addAll(matchingAyahs)
                }

                _searchResults.value = allResults.map { entity ->
                    Ayah(
                        surahNumber = entity.surahNumber,
                        ayahNumber = entity.ayahNumber,
                        textArabic = entity.textArabic,
                        globalAyahNumber = entity.globalAyahNumber,
                        juz = entity.juz,
                        page = entity.page,
                        manzil = entity.manzil,
                        ruku = entity.ruku,
                        hizbQuarter = entity.hizbQuarter,
                        sajda = entity.sajda
                    )
                }
                Timber.d("Search found ${allResults.size} results for '$query'")
            } catch (e: Exception) {
                Timber.e(e, "Error searching ayahs")
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    /**
     * Create search variants to handle Quranic orthography differences
     * In Quranic text, some alefs are omitted (written as dagger alef or superscript)
     * e.g., الراسخون -> الرسخون, السماوات -> السموت, الرحمان -> الرحمن
     */
    private fun createSearchVariants(query: String): List<String> {
        val normalized = stripArabicDiacritics(query)
        val variants = mutableSetOf(normalized)

        // Create variant without silent alef (dagger alef / alef khanjariyya)
        // In Quranic Uthmani script, regular alef is often replaced with a small
        // superscript alef that's not typed on standard keyboards.
        // This can occur after almost any Arabic consonant.
        // All Arabic consonants: ب ت ث ج ح خ د ذ ر ز س ش ص ض ط ظ ع غ ف ق ك ل م ن ه و ي
        val allConsonants = "بتثجحخدذرزسشصضطظعغفقكلمنهوي"
        val silentAlefPattern = Regex("([$allConsonants])ا")
        val withoutSilentAlef = silentAlefPattern.replace(normalized) { it.groupValues[1] }

        if (withoutSilentAlef != normalized) {
            variants.add(withoutSilentAlef)
        }

        return variants.toList()
    }

    /**
     * Normalize Arabic text for search comparison:
     * - Remove diacritics (tashkeel)
     * - Normalize alef variations (أ إ آ ٱ) to plain alef (ا)
     * - Normalize teh marbuta (ة) to heh (ه)
     * - Normalize alef maksura (ى) to yeh (ي)
     */
    private fun stripArabicDiacritics(text: String): String {
        var normalized = text

        // Remove Arabic diacritics (tashkeel)
        // Unicode range: 0x064B - 0x065F (Fathah, Dammah, Kasrah, Shadda, Sukun, etc.)
        // Also includes: 0x0670 (superscript alef), 0x06D6-0x06DC (Quranic marks)
        val diacriticsPattern = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06DC\\u06DF-\\u06E4\\u06E7\\u06E8\\u06EA-\\u06ED]")
        normalized = normalized.replace(diacriticsPattern, "")

        // Normalize alef variations to plain alef (ا)
        // أ (alef with hamza above) -> ا
        // إ (alef with hamza below) -> ا
        // آ (alef with madda) -> ا
        // ٱ (alef wasla) -> ا
        normalized = normalized.replace('أ', 'ا')
        normalized = normalized.replace('إ', 'ا')
        normalized = normalized.replace('آ', 'ا')
        normalized = normalized.replace('ٱ', 'ا')

        // Normalize teh marbuta to heh (optional, helps with some searches)
        normalized = normalized.replace('ة', 'ه')

        // Normalize alef maksura to yeh
        normalized = normalized.replace('ى', 'ي')

        return normalized
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    /**
     * Set a highlighted ayah (from search result navigation).
     * The highlight will be cleared when playback starts.
     */
    fun setHighlightedAyah(surahNumber: Int, ayahNumber: Int) {
        _state.value = _state.value.copy(
            highlightedAyah = HighlightedAyah(surahNumber, ayahNumber)
        )
    }

    /**
     * Get all surahs for the custom recitation dialog.
     */
    fun getAllSurahs(): kotlinx.coroutines.flow.Flow<List<Surah>> {
        return quranRepository.getAllSurahs()
    }

    /**
     * Get all reciters for the custom recitation dialog.
     */
    fun getAllReciters(): kotlinx.coroutines.flow.Flow<List<com.quranmedia.player.domain.model.Reciter>> {
        return quranRepository.getAllReciters()
    }

    /**
     * Get the selected reciter ID from settings.
     */
    fun getSelectedReciterId(): kotlinx.coroutines.flow.Flow<String> {
        return kotlinx.coroutines.flow.flow {
            emit(settingsRepository.getSelectedReciterId())
        }
    }

    /**
     * Get the app language from settings.
     */
    fun getAppLanguage(): kotlinx.coroutines.flow.Flow<com.quranmedia.player.data.repository.AppLanguage> {
        return settingsRepository.settings.map { it.appLanguage }
    }

    /**
     * Start custom recitation with the provided settings.
     */
    fun startCustomRecitation(reciterId: String, settings: com.quranmedia.player.domain.model.CustomRecitationSettings) {
        viewModelScope.launch {
            playbackController.startCustomRecitation(reciterId, settings)
        }
    }

    /**
     * Save a custom recitation preset.
     */
    fun saveRecitationPreset(name: String, settings: com.quranmedia.player.domain.model.CustomRecitationSettings) {
        settingsRepository.saveRecitationPreset(name, settings)
    }

    /**
     * Get saved custom recitation presets.
     */
    fun getRecitationPresets(): kotlinx.coroutines.flow.Flow<List<com.quranmedia.player.domain.model.RecitationPreset>> {
        return settingsRepository.getRecitationPresets()
    }

    // ==================== Tafseer Functions ====================

    /**
     * Show tafseer modal for the selected ayah.
     * Loads available tafseers and their content.
     */
    fun showTafseer(ayah: Ayah) {
        viewModelScope.launch {
            val appLanguage = settings.value.appLanguage.name.lowercase()
            val allSorted = AvailableTafseers.getSortedByLanguage(appLanguage)
            val downloadedTafseers = tafseerRepository.getDownloadedTafseerIds()

            // Show modal immediately with loading state
            _tafseerState.value = TafseerModalState(
                isVisible = true,
                surah = ayah.surahNumber,
                ayah = ayah.ayahNumber,
                surahName = surahNamesArabic[ayah.surahNumber] ?: "",
                ayahText = ayah.textArabic,
                allTafseers = allSorted,
                downloadedIds = downloadedTafseers,
                isLoading = true
            )

            try {
                // Get available tafseers for this ayah
                val tafseers = tafseerRepository.getAllTafseersForAyah(ayah.surahNumber, ayah.ayahNumber)

                val sortedIds = allSorted.map { it.id }
                val sortedTafseers = tafseers.sortedBy { (tafseerInfo, _) ->
                    sortedIds.indexOf(tafseerInfo.id).takeIf { it >= 0 } ?: Int.MAX_VALUE
                }

                _tafseerState.value = _tafseerState.value.copy(
                    isLoading = false,
                    availableTafseers = sortedTafseers,
                    selectedTafseerId = sortedTafseers.firstOrNull()?.first?.id
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading tafseer")
                _tafseerState.value = _tafseerState.value.copy(
                    isLoading = false,
                    error = "Error loading tafseer"
                )
            }
        }
    }

    /**
     * Download a tafseer from within the tafseer modal.
     * After download completes, reload content for the current ayah.
     */
    fun downloadTafseerFromModal(tafseerId: String) {
        viewModelScope.launch {
            _tafseerState.value = _tafseerState.value.copy(
                downloadingTafseerId = tafseerId,
                downloadProgress = 0f
            )

            val success = tafseerRepository.downloadTafseer(tafseerId) { progress ->
                _tafseerState.value = _tafseerState.value.copy(
                    downloadProgress = progress
                )
            }

            if (success) {
                // Refresh downloaded IDs
                val downloadedIds = tafseerRepository.getDownloadedTafseerIds()

                // Reload tafseer content for current ayah
                val currentState = _tafseerState.value
                val tafseers = tafseerRepository.getAllTafseersForAyah(currentState.surah, currentState.ayah)
                val appLanguage = settings.value.appLanguage.name.lowercase()
                val sortedIds = AvailableTafseers.getSortedByLanguage(appLanguage).map { it.id }
                val sortedTafseers = tafseers.sortedBy { (info, _) ->
                    sortedIds.indexOf(info.id).takeIf { it >= 0 } ?: Int.MAX_VALUE
                }

                _tafseerState.value = currentState.copy(
                    downloadingTafseerId = null,
                    downloadProgress = 0f,
                    downloadedIds = downloadedIds,
                    availableTafseers = sortedTafseers,
                    selectedTafseerId = tafseerId  // Select the newly downloaded tafseer
                )
            } else {
                _tafseerState.value = _tafseerState.value.copy(
                    downloadingTafseerId = null,
                    downloadProgress = 0f
                )
            }
        }
    }

    /**
     * Dismiss the tafseer modal.
     */
    fun dismissTafseer() {
        _tafseerState.value = TafseerModalState()
    }

    /**
     * Select a different tafseer from the available options.
     */
    fun selectTafseer(tafseerId: String) {
        _tafseerState.value = _tafseerState.value.copy(
            selectedTafseerId = tafseerId
        )
    }
}
