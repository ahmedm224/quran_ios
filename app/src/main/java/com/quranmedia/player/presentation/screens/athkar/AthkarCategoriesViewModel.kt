package com.quranmedia.player.presentation.screens.athkar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.data.repository.UserSettings
import com.quranmedia.player.domain.model.AthkarCategory
import com.quranmedia.player.domain.repository.AthkarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AthkarCategoriesViewModel @Inject constructor(
    private val athkarRepository: AthkarRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val categories: StateFlow<List<AthkarCategory>> = athkarRepository.getAllCategories()
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

    init {
        // Initialize athkar from bundled assets if needed
        viewModelScope.launch {
            athkarRepository.initializeFromAssets()
        }
    }

    fun refreshFromApi() {
        viewModelScope.launch {
            athkarRepository.refreshAthkarFromApi()
        }
    }
}
