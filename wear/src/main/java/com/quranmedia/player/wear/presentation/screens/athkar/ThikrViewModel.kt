package com.quranmedia.player.wear.presentation.screens.athkar

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.wear.data.repository.WearAthkarRepository
import com.quranmedia.player.wear.domain.model.Thikr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ThikrViewModel @Inject constructor(
    private val repository: WearAthkarRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val categoryId: String = savedStateHandle["categoryId"] ?: ""

    private val _athkar = mutableStateOf<List<Thikr>>(emptyList())
    val athkar: State<List<Thikr>> = _athkar

    private val _remainingCounts = mutableStateMapOf<String, Int>()
    val remainingCounts: Map<String, Int> = _remainingCounts

    private val _categoryName = mutableStateOf("")
    val categoryName: State<String> = _categoryName

    init {
        loadAthkar()
    }

    private fun loadAthkar() {
        viewModelScope.launch {
            try {
                val athkarList = repository.getAthkarByCategory(categoryId)
                _athkar.value = athkarList

                // Initialize remaining counts
                athkarList.forEach { thikr ->
                    _remainingCounts[thikr.id] = thikr.repeatCount
                }

                // Get category name
                val categories = repository.getAllCategories()
                _categoryName.value = categories.find { it.id == categoryId }?.nameArabic ?: ""

                Timber.d("Loaded ${athkarList.size} athkar for category: $categoryId")
            } catch (e: Exception) {
                Timber.e(e, "Error loading athkar")
            }
        }
    }

    /**
     * Decrement the count for a thikr.
     * Returns true if the thikr is now complete.
     */
    fun decrementCount(thikrId: String): Boolean {
        val current = _remainingCounts[thikrId] ?: return false
        if (current > 0) {
            _remainingCounts[thikrId] = current - 1
            return (current - 1) == 0
        }
        return true
    }

    /**
     * Reset the count for a thikr.
     */
    fun resetCount(thikrId: String) {
        val thikr = _athkar.value.find { it.id == thikrId }
        if (thikr != null) {
            _remainingCounts[thikrId] = thikr.repeatCount
        }
    }

    /**
     * Check if a thikr is complete.
     */
    fun isComplete(thikrId: String): Boolean {
        return (_remainingCounts[thikrId] ?: 0) == 0
    }

    /**
     * Get the progress (0.0 to 1.0) for a thikr.
     */
    fun getProgress(thikrId: String): Float {
        val thikr = _athkar.value.find { it.id == thikrId } ?: return 0f
        val remaining = _remainingCounts[thikrId] ?: thikr.repeatCount
        return 1f - (remaining.toFloat() / thikr.repeatCount.toFloat())
    }
}
