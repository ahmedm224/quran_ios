package com.quranmedia.player.presentation.screens.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.data.database.dao.AyahDao
import com.quranmedia.player.data.database.dao.HizbQuarterInfo
import com.quranmedia.player.data.database.dao.JuzStartInfo
import com.quranmedia.player.data.database.dao.ReadingBookmarkDao
import com.quranmedia.player.data.database.entity.AyahEntity
import com.quranmedia.player.data.database.entity.ReadingBookmarkEntity
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.data.repository.UserSettings
import com.quranmedia.player.domain.model.Ayah
import com.quranmedia.player.domain.model.Surah
import com.quranmedia.player.domain.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SurahWithPage(
    val surah: Surah,
    val startPage: Int
)

data class IndexReadingBookmark(
    val id: String,
    val pageNumber: Int,
    val surahName: String?,
    val label: String?,
    val createdAt: Long
)

data class SearchResultWithPage(
    val ayah: Ayah,
    val surahName: String
)

data class QuranIndexState(
    val totalPages: Int = 604,
    val surahs: List<Surah> = emptyList(),
    val surahsWithPages: List<SurahWithPage> = emptyList(),
    val juzNumbers: List<Int> = emptyList(),
    val juzStartInfo: List<JuzStartInfo> = emptyList(),
    val hizbQuartersInfo: List<HizbQuarterInfo> = emptyList(),
    val readingBookmarks: List<IndexReadingBookmark> = emptyList(),
    val isLoading: Boolean = true,
    // Search state
    val searchResults: List<SearchResultWithPage> = emptyList(),
    val isSearching: Boolean = false,
    // Daily target for bookmarks
    val dailyTargetPages: Float? = null
)

@HiltViewModel
class QuranIndexViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val ayahDao: AyahDao,
    private val readingBookmarkDao: ReadingBookmarkDao,
    private val settingsRepository: SettingsRepository,
    private val trackerRepository: com.quranmedia.player.data.repository.TrackerRepository,
    private val prayerTimesRepository: com.quranmedia.player.domain.repository.PrayerTimesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(QuranIndexState())
    val state: StateFlow<QuranIndexState> = _state.asStateFlow()

    val settings = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    init {
        loadData()
        observeReadingBookmarks()
        loadDailyTarget()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                // Load total pages
                val maxPage = ayahDao.getMaxPageNumber()
                if (maxPage != null && maxPage > 0) {
                    _state.value = _state.value.copy(totalPages = maxPage)
                }

                // Load surahs
                val surahs = quranRepository.getAllSurahs().first()
                _state.value = _state.value.copy(surahs = surahs)

                // Load surah start pages
                val surahPages = ayahDao.getAllSurahStartPages()
                // Ensure page numbers are at least 1 (metadata might not be loaded yet)
                val surahPageMap = surahPages.associate { it.surahNumber to maxOf(1, it.page) }
                val surahsWithPages = surahs.map { surah ->
                    SurahWithPage(surah, surahPageMap[surah.number] ?: 1)
                }
                _state.value = _state.value.copy(surahsWithPages = surahsWithPages)

                // Load juz numbers and start info
                val juzNumbers = quranRepository.getAllJuzNumbers().first()
                val juzStartInfo = ayahDao.getAllJuzStartInfo()
                val hizbQuartersInfo = ayahDao.getAllHizbQuartersInfo()
                _state.value = _state.value.copy(
                    juzNumbers = juzNumbers.ifEmpty { (1..30).toList() },
                    juzStartInfo = juzStartInfo,
                    hizbQuartersInfo = hizbQuartersInfo,
                    isLoading = false
                )

                Timber.d("Loaded index: ${surahs.size} surahs, ${juzNumbers.size} juz, ${hizbQuartersInfo.size} hizb quarters, ${surahsWithPages.size} surah pages")
            } catch (e: Exception) {
                Timber.e(e, "Error loading index data")
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun getPageForSurah(surahNumber: Int, callback: (Int?) -> Unit) {
        viewModelScope.launch {
            val page = quranRepository.getFirstPageOfSurah(surahNumber)
            callback(page)
        }
    }

    fun getPageForJuz(juzNumber: Int, callback: (Int?) -> Unit) {
        viewModelScope.launch {
            val page = quranRepository.getFirstPageOfJuz(juzNumber)
            callback(page)
        }
    }

    private fun observeReadingBookmarks() {
        viewModelScope.launch {
            readingBookmarkDao.getAllReadingBookmarks().collect { entities: List<ReadingBookmarkEntity> ->
                val bookmarks = entities.map { entity: ReadingBookmarkEntity ->
                    IndexReadingBookmark(
                        id = entity.id,
                        pageNumber = entity.pageNumber,
                        surahName = entity.surahName,
                        label = entity.label,
                        createdAt = entity.createdAt
                    )
                }
                _state.value = _state.value.copy(readingBookmarks = bookmarks)
                Timber.d("Loaded ${bookmarks.size} reading bookmarks")
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

    // Search functionality
    private var searchJob: Job? = null

    fun searchQuran(query: String) {
        searchJob?.cancel()

        if (query.trim().length < 2) {
            _state.value = _state.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }

        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true)
            delay(300) // Debounce

            try {
                val searchVariants = createSearchVariants(query.trim())
                Timber.d("Searching with variants: $searchVariants")

                val allResults = mutableListOf<AyahEntity>()
                for (surahNum in 1..114) {
                    val surahAyahs = ayahDao.getAyahsBySurahSync(surahNum)
                    val matchingAyahs = surahAyahs.filter { entity ->
                        val normalizedAyah = stripArabicDiacritics(entity.textArabic)
                        searchVariants.any { variant ->
                            normalizedAyah.contains(variant, ignoreCase = true)
                        }
                    }
                    allResults.addAll(matchingAyahs)
                }

                // Get surah names and convert to domain model
                val results = allResults.take(100).mapNotNull { entity ->
                    val surah = _state.value.surahs.find { it.number == entity.surahNumber }
                    if (surah != null) {
                        SearchResultWithPage(
                            ayah = Ayah(
                                surahNumber = entity.surahNumber,
                                ayahNumber = entity.ayahNumber,
                                globalAyahNumber = entity.globalAyahNumber,
                                textArabic = entity.textArabic,
                                juz = entity.juz,
                                manzil = entity.manzil,
                                page = entity.page,
                                ruku = entity.ruku,
                                hizbQuarter = entity.hizbQuarter,
                                sajda = entity.sajda
                            ),
                            surahName = surah.nameArabic
                        )
                    } else null
                }

                _state.value = _state.value.copy(searchResults = results, isSearching = false)
                Timber.d("Search found ${results.size} results")
            } catch (e: Exception) {
                Timber.e(e, "Search error")
                _state.value = _state.value.copy(searchResults = emptyList(), isSearching = false)
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _state.value = _state.value.copy(searchResults = emptyList(), isSearching = false)
    }

    private fun createSearchVariants(query: String): List<String> {
        val variants = mutableSetOf<String>()
        val normalized = stripArabicDiacritics(query)
        variants.add(normalized)

        // Add variant without silent alef
        val allConsonants = "بتثجحخدذرزسشصضطظعغفقكلمنهوي"
        val silentAlefPattern = Regex("([$allConsonants])ا")
        val withoutSilentAlef = silentAlefPattern.replace(normalized) { it.groupValues[1] }
        if (withoutSilentAlef != normalized) {
            variants.add(withoutSilentAlef)
        }

        return variants.toList()
    }

    private fun stripArabicDiacritics(text: String): String {
        var normalized = text
        val diacriticsPattern = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06DC\\u06DF-\\u06E4\\u06E7\\u06E8\\u06EA-\\u06ED]")
        normalized = normalized.replace(diacriticsPattern, "")
        normalized = normalized.replace('أ', 'ا')
        normalized = normalized.replace('إ', 'ا')
        normalized = normalized.replace('آ', 'ا')
        normalized = normalized.replace('ٱ', 'ا')
        normalized = normalized.replace('ة', 'ه')
        normalized = normalized.replace('ى', 'ي')
        return normalized
    }

    /**
     * Load daily target pages needed to finish Quran by end of month
     * Uses active goal if available, otherwise calculates based on page 1
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
                    // Fallback: Calculate based on page 1 and days remaining in Hijri month
                    calculateFallbackDailyTarget(1)
                }

                _state.value = _state.value.copy(dailyTargetPages = dailyTarget)
                Timber.d("Daily target for bookmarks: ${dailyTarget ?: "N/A"} pages/day")
            } catch (e: Exception) {
                Timber.e(e, "Error loading daily target")
            }
        }
    }

    /**
     * Calculate daily target when no goal is set
     * Based on given page and days remaining in Hijri month
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
}
