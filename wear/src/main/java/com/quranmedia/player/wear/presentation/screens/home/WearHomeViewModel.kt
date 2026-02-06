package com.quranmedia.player.wear.presentation.screens.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.wear.data.repository.WearAthkarRepository
import com.quranmedia.player.wear.domain.model.AthkarCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WearHomeViewModel @Inject constructor(
    private val repository: WearAthkarRepository
) : ViewModel() {

    private val _categories = mutableStateOf<List<AthkarCategory>>(emptyList())
    val categories: State<List<AthkarCategory>> = _categories

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                _categories.value = repository.getAllCategories()
                Timber.d("Loaded ${_categories.value.size} categories")
            } catch (e: Exception) {
                Timber.e(e, "Error loading categories")
            }
        }
    }
}
