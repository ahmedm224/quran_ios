package com.quranmedia.player.presentation.screens.athkar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.data.repository.UserSettings
import com.quranmedia.player.domain.model.AthkarCategory
import com.quranmedia.player.domain.model.Thikr
import com.quranmedia.player.domain.repository.AthkarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AthkarListViewModel @Inject constructor(
    private val athkarRepository: AthkarRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val categoryId: String = savedStateHandle.get<String>("categoryId") ?: ""

    val athkar: StateFlow<List<Thikr>> = athkarRepository.getAthkarByCategory(categoryId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    private val _category = MutableStateFlow<AthkarCategory?>(null)
    val category: StateFlow<AthkarCategory?> = _category.asStateFlow()

    // Track remaining counts for each thikr
    private val _remainingCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val remainingCounts: StateFlow<Map<String, Int>> = _remainingCounts.asStateFlow()

    init {
        loadCategory()
    }

    private fun loadCategory() {
        viewModelScope.launch {
            athkarRepository.getAllCategories().collect { categories ->
                _category.value = categories.find { it.id == categoryId }
            }
        }
    }

    fun initializeRemainingCounts(athkarList: List<Thikr>) {
        if (_remainingCounts.value.isEmpty()) {
            _remainingCounts.value = athkarList.associate { it.id to it.repeatCount }
        }
    }

    fun decrementCount(thikrId: String) {
        val currentCounts = _remainingCounts.value.toMutableMap()
        val currentCount = currentCounts[thikrId] ?: 1
        if (currentCount > 0) {
            currentCounts[thikrId] = currentCount - 1
            _remainingCounts.value = currentCounts
        }
    }

    fun resetCount(thikrId: String, originalCount: Int) {
        val currentCounts = _remainingCounts.value.toMutableMap()
        currentCounts[thikrId] = originalCount
        _remainingCounts.value = currentCounts
    }

    fun resetAllCounts() {
        viewModelScope.launch {
            athkar.value.let { list ->
                _remainingCounts.value = list.associate { it.id to it.repeatCount }
            }
        }
    }
}
