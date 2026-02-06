package com.quranmedia.player.presentation.screens.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.data.database.dao.ReadingBookmarkDao
import com.quranmedia.player.data.database.entity.ReadingBookmarkEntity
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.data.repository.BookmarkRepository
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.domain.model.Bookmark
import com.quranmedia.player.domain.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class BookmarkWithDetails(
    val bookmark: Bookmark,
    val surahName: String,
    val reciterName: String
)

data class BookmarksState(
    val playbackBookmarks: List<BookmarkWithDetails> = emptyList(),
    val readingBookmarks: List<ReadingBookmarkEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val appLanguage: AppLanguage = AppLanguage.ARABIC,
    val useIndoArabicNumerals: Boolean = false
) {
    // For backward compatibility
    val bookmarks: List<BookmarkWithDetails> get() = playbackBookmarks
}

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val quranRepository: QuranRepository,
    private val readingBookmarkDao: ReadingBookmarkDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BookmarksState())
    val state: StateFlow<BookmarksState> = _state.asStateFlow()

    init {
        loadBookmarks()
        loadReadingBookmarks()
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.value = _state.value.copy(
                    appLanguage = settings.appLanguage,
                    useIndoArabicNumerals = settings.useIndoArabicNumerals
                )
            }
        }
    }

    private fun loadReadingBookmarks() {
        viewModelScope.launch {
            try {
                readingBookmarkDao.getAllReadingBookmarks().collect { bookmarks ->
                    _state.value = _state.value.copy(readingBookmarks = bookmarks)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading reading bookmarks")
            }
        }
    }

    private fun loadBookmarks() {
        viewModelScope.launch {
            try {
                bookmarkRepository.getAllBookmarks().collect { bookmarks ->
                    val detailedBookmarks = bookmarks.mapNotNull { bookmark ->
                        try {
                            val surah = quranRepository.getSurahByNumber(bookmark.surahNumber)
                            val reciter = quranRepository.getReciterById(bookmark.reciterId)

                            if (surah != null && reciter != null) {
                                BookmarkWithDetails(
                                    bookmark = bookmark,
                                    surahName = "${surah.nameEnglish} (${surah.nameArabic})",
                                    reciterName = reciter.name
                                )
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error loading bookmark details")
                            null
                        }
                    }

                    _state.value = _state.value.copy(
                        playbackBookmarks = detailedBookmarks,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading bookmarks")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load bookmarks"
                )
            }
        }
    }

    fun deleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            try {
                bookmarkRepository.deleteBookmark(bookmarkId)
                Timber.d("Deleted bookmark: $bookmarkId")
            } catch (e: Exception) {
                Timber.e(e, "Error deleting bookmark")
            }
        }
    }

    fun deleteAllBookmarks() {
        viewModelScope.launch {
            try {
                bookmarkRepository.deleteAllBookmarks()
                Timber.d("Deleted all bookmarks")
            } catch (e: Exception) {
                Timber.e(e, "Error deleting all bookmarks")
            }
        }
    }

    fun deleteReadingBookmark(bookmarkId: String) {
        viewModelScope.launch {
            try {
                readingBookmarkDao.deleteBookmark(bookmarkId)
                Timber.d("Deleted reading bookmark: $bookmarkId")
            } catch (e: Exception) {
                Timber.e(e, "Error deleting reading bookmark")
            }
        }
    }
}
