package com.quranmedia.player.presentation.screens.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.data.repository.TrackerRepository
import com.quranmedia.player.domain.model.ActivityType
import com.quranmedia.player.domain.model.DailyActivity
import com.quranmedia.player.domain.model.GoalType
import com.quranmedia.player.domain.model.HijriDate
import com.quranmedia.player.domain.model.KhatmahGoal
import com.quranmedia.player.domain.model.KhatmahProgress
import com.quranmedia.player.domain.model.QuranProgress
import com.quranmedia.player.domain.repository.PrayerTimesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

data class TrackerUiState(
    val todayActivities: List<DailyActivity> = emptyList(),
    val todayProgress: QuranProgress? = null,
    val activeGoalProgress: KhatmahProgress? = null,
    val allGoals: List<KhatmahGoal> = emptyList(),
    val hijriDate: HijriDate? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateGoalDialog: Boolean = false
)

@HiltViewModel
class TrackerViewModel @Inject constructor(
    private val trackerRepository: TrackerRepository,
    private val settingsRepository: SettingsRepository,
    private val prayerTimesRepository: PrayerTimesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    init {
        loadTodayData()
        observeActiveGoal()
        observeAllGoals()
        loadHijriDate()
    }

    private fun loadTodayData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Load today's activities
                trackerRepository.getTodayActivities().collect { activities ->
                    _uiState.value = _uiState.value.copy(
                        todayActivities = activities,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading today's data")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load today's data"
                )
            }
        }

        viewModelScope.launch {
            try {
                // Load today's Quran progress
                trackerRepository.getTodayProgress().collect { progress ->
                    _uiState.value = _uiState.value.copy(todayProgress = progress)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading today's progress")
            }
        }
    }

    private fun observeActiveGoal() {
        viewModelScope.launch {
            try {
                trackerRepository.getActiveGoal().collect { goal ->
                    if (goal != null) {
                        // Calculate progress for active goal
                        val progress = trackerRepository.calculateKhatmahProgress(goal.id)
                        _uiState.value = _uiState.value.copy(activeGoalProgress = progress)
                    } else {
                        _uiState.value = _uiState.value.copy(activeGoalProgress = null)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error observing active goal")
            }
        }
    }

    private fun observeAllGoals() {
        viewModelScope.launch {
            try {
                trackerRepository.getAllGoals().collect { goals ->
                    _uiState.value = _uiState.value.copy(allGoals = goals)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error observing all goals")
            }
        }
    }

    private fun loadHijriDate() {
        viewModelScope.launch {
            try {
                // Try to get Hijri date from prayer times first
                val prayerTimes = prayerTimesRepository.getCachedPrayerTimes(LocalDate.now()).first()
                val hijriDateFromPrayerTimes = prayerTimes?.hijriDate

                val hijriDate = if (hijriDateFromPrayerTimes != null) {
                    // Apply Hijri adjustment from settings
                    val settings = settingsRepository.getCurrentSettings()
                    val adjustment = settings.hijriDateAdjustment

                    // Adjust the day
                    val adjustedDay = (hijriDateFromPrayerTimes.day + adjustment).coerceIn(1, 30)
                    hijriDateFromPrayerTimes.copy(day = adjustedDay)
                } else {
                    // Fallback: Calculate Hijri date from current Gregorian date
                    val settings = settingsRepository.getCurrentSettings()
                    val adjustment = settings.hijriDateAdjustment

                    val calculatedHijri = com.quranmedia.player.domain.util.HijriCalendarUtils
                        .gregorianToHijri(LocalDate.now())

                    // Apply adjustment
                    val adjustedDay = (calculatedHijri.day + adjustment).coerceIn(1, 30)
                    calculatedHijri.copy(day = adjustedDay)
                }

                _uiState.value = _uiState.value.copy(hijriDate = hijriDate)
            } catch (e: Exception) {
                Timber.e(e, "Error loading Hijri date")
                // Last resort fallback
                _uiState.value = _uiState.value.copy(
                    hijriDate = com.quranmedia.player.domain.util.HijriCalendarUtils
                        .gregorianToHijri(LocalDate.now())
                )
            }
        }
    }

    // ============ Actions ============

    fun toggleActivity(activityType: ActivityType) {
        viewModelScope.launch {
            try {
                val currentActivity = _uiState.value.todayActivities.find {
                    it.activityType == activityType
                }
                val newCompleted = !(currentActivity?.completed ?: false)

                trackerRepository.toggleActivity(activityType, newCompleted)
                Timber.d("Toggled $activityType to $newCompleted")
            } catch (e: Exception) {
                Timber.e(e, "Error toggling activity")
                _uiState.value = _uiState.value.copy(error = "Failed to update activity")
            }
        }
    }

    fun setPagesListened(pages: Int) {
        viewModelScope.launch {
            try {
                trackerRepository.setPagesListened(pages.coerceIn(0, 604))
                Timber.d("Set pages listened to $pages")
            } catch (e: Exception) {
                Timber.e(e, "Error setting pages listened")
                _uiState.value = _uiState.value.copy(error = "Failed to update listening progress")
            }
        }
    }

    fun incrementPagesListened(increment: Int) {
        viewModelScope.launch {
            try {
                val current = _uiState.value.todayProgress?.pagesListened ?: 0
                val newValue = (current + increment).coerceIn(0, 604)
                trackerRepository.setPagesListened(newValue)
                Timber.d("Incremented pages listened by $increment to $newValue")
            } catch (e: Exception) {
                Timber.e(e, "Error incrementing pages listened")
            }
        }
    }

    fun showCreateGoalDialog() {
        _uiState.value = _uiState.value.copy(showCreateGoalDialog = true)
    }

    fun hideCreateGoalDialog() {
        _uiState.value = _uiState.value.copy(showCreateGoalDialog = false)
    }

    fun createKhatmahGoal(
        name: String,
        startDate: LocalDate,
        endDate: LocalDate,
        goalType: GoalType,
        startPage: Int = 1,
        targetPages: Int = 604
    ) {
        viewModelScope.launch {
            try {
                trackerRepository.createGoal(
                    name = name,
                    startDate = startDate,
                    endDate = endDate,
                    startPage = startPage,
                    targetPages = targetPages,
                    goalType = goalType,
                    setAsActive = true
                )
                hideCreateGoalDialog()
                Timber.d("Created Khatmah goal: $name")
            } catch (e: Exception) {
                Timber.e(e, "Error creating goal")
                _uiState.value = _uiState.value.copy(error = "Failed to create goal")
            }
        }
    }

    fun setActiveGoal(goalId: String) {
        viewModelScope.launch {
            try {
                trackerRepository.setActiveGoal(goalId)
                Timber.d("Set active goal: $goalId")
            } catch (e: Exception) {
                Timber.e(e, "Error setting active goal")
                _uiState.value = _uiState.value.copy(error = "Failed to set active goal")
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            try {
                trackerRepository.deleteGoal(goalId)
                Timber.d("Deleted goal: $goalId")
            } catch (e: Exception) {
                Timber.e(e, "Error deleting goal")
                _uiState.value = _uiState.value.copy(error = "Failed to delete goal")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refresh() {
        loadTodayData()
        loadHijriDate()
    }
}
