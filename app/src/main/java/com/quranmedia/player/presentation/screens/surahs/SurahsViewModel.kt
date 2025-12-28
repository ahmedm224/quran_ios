package com.quranmedia.player.presentation.screens.surahs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.domain.model.Reciter
import com.quranmedia.player.domain.model.Surah
import com.quranmedia.player.domain.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SurahsViewModel @Inject constructor(
    private val quranRepository: QuranRepository
) : ViewModel() {

    val surahs: StateFlow<List<Surah>> = quranRepository.getAllSurahs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,  // Start on first subscriber and keep active
            initialValue = emptyList()
        )

    val reciters: StateFlow<List<Reciter>> = quranRepository.getAllReciters()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,  // Start on first subscriber and keep active
            initialValue = emptyList()
        )

    private val _selectedReciter = MutableStateFlow<Reciter?>(null)
    val selectedReciter: StateFlow<Reciter?> = _selectedReciter.asStateFlow()

    fun selectReciter(reciter: Reciter) {
        _selectedReciter.value = reciter
    }
}
