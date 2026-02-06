package com.quranmedia.player.wear.presentation.screens.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.wear.data.repository.WearQuranRepository
import com.quranmedia.player.wear.domain.model.QuranPageMapping
import com.quranmedia.player.wear.domain.model.QuranReadingPosition
import com.quranmedia.player.wear.domain.model.Surah
import com.quranmedia.player.wear.domain.model.SurahInfo
import com.quranmedia.player.wear.domain.model.SurahMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class QuranIndexUiState(
    val surahs: List<SurahInfo> = emptyList(),
    val isLoading: Boolean = false,  // No loading needed - metadata is instant
    val lastPosition: QuranReadingPosition = QuranReadingPosition()
)

data class QuranReaderUiState(
    val currentSurah: Surah? = null,
    val currentSurahIndex: Int = 0,
    val totalSurahs: Int = 114,
    val isDarkMode: Boolean = true,
    val isLoading: Boolean = true
)

@HiltViewModel
class QuranViewModel @Inject constructor(
    private val repository: WearQuranRepository
) : ViewModel() {

    private val _indexState = MutableStateFlow(QuranIndexUiState())
    val indexState: StateFlow<QuranIndexUiState> = _indexState.asStateFlow()

    private val _readerState = MutableStateFlow(QuranReaderUiState())
    val readerState: StateFlow<QuranReaderUiState> = _readerState.asStateFlow()

    init {
        // Load index instantly using pre-defined metadata
        loadIndex()
    }

    private fun loadIndex() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get last reading position
                val lastPosition = repository.getReadingPosition()

                // Use pre-defined metadata - instant, no file loading
                _indexState.value = QuranIndexUiState(
                    surahs = SurahMetadata.all,
                    isLoading = false,
                    lastPosition = lastPosition
                )

                Timber.d("Index ready with ${SurahMetadata.all.size} surahs, last position: surah ${lastPosition.surahNumber}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load index")
                // Even on error, show the surah list
                _indexState.value = QuranIndexUiState(
                    surahs = SurahMetadata.all,
                    isLoading = false
                )
            }
        }
    }

    fun openSurah(surahNumber: Int) {
        // Set loading state immediately
        _readerState.value = _readerState.value.copy(isLoading = true)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Load only the specific surah we need (lazy loading)
                val surah = repository.getSurah(surahNumber)

                if (surah != null) {
                    _readerState.value = QuranReaderUiState(
                        currentSurah = surah,
                        currentSurahIndex = surahNumber - 1,
                        totalSurahs = 114,
                        isDarkMode = _readerState.value.isDarkMode,
                        isLoading = false
                    )
                    savePosition(surahNumber, 1)
                } else {
                    Timber.e("Surah $surahNumber not found")
                    _readerState.value = _readerState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to open surah $surahNumber")
                _readerState.value = _readerState.value.copy(isLoading = false)
            }
        }
    }

    fun goToNextSurah() {
        val nextSurahNumber = _readerState.value.currentSurahIndex + 2  // +1 for next, +1 for 1-based
        if (nextSurahNumber <= 114) {
            openSurah(nextSurahNumber)
        }
    }

    fun goToPreviousSurah() {
        val prevSurahNumber = _readerState.value.currentSurahIndex  // currentIndex is 0-based, so this gives prev surah number
        if (prevSurahNumber >= 1) {
            openSurah(prevSurahNumber)
        }
    }

    fun toggleTheme() {
        _readerState.value = _readerState.value.copy(
            isDarkMode = !_readerState.value.isDarkMode
        )
    }

    /**
     * Navigate to a specific page number (1-604).
     * Returns the surah number for that page.
     */
    fun goToPage(pageNumber: Int): Int {
        val surahNumber = QuranPageMapping.getSurahForPage(pageNumber)
        openSurah(surahNumber)
        return surahNumber
    }

    /**
     * Get the page number for a given surah.
     */
    fun getPageForSurah(surahNumber: Int): Int {
        return QuranPageMapping.getPageForSurah(surahNumber)
    }

    private fun savePosition(surahNumber: Int, verseNumber: Int) {
        repository.saveReadingPosition(QuranReadingPosition(surahNumber, verseNumber))
    }
}
