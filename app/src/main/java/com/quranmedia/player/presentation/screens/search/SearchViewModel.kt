package com.quranmedia.player.presentation.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.domain.model.SearchResult
import com.quranmedia.player.domain.repository.SearchRepository
import com.quranmedia.player.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SearchState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
    val searchCount: Int = 0
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query)

        // Cancel previous search
        searchJob?.cancel()

        // Don't search if query is too short
        if (query.trim().length < 2) {
            _state.value = _state.value.copy(
                results = emptyList(),
                isSearching = false,
                error = null,
                searchCount = 0
            )
            return
        }

        // Debounce search - wait 500ms after user stops typing
        searchJob = viewModelScope.launch {
            delay(500)
            performSearch(query.trim())
        }
    }

    private suspend fun performSearch(query: String) {
        _state.value = _state.value.copy(isSearching = true, error = null)

        Timber.d("Searching for: $query")
        when (val result = searchRepository.searchAyahs(query)) {
            is Resource.Success -> {
                _state.value = _state.value.copy(
                    results = result.data ?: emptyList(),
                    isSearching = false,
                    searchCount = result.data?.size ?: 0
                )
                Timber.d("Found ${result.data?.size ?: 0} results")
            }
            is Resource.Error -> {
                _state.value = _state.value.copy(
                    isSearching = false,
                    error = result.message ?: "Search failed"
                )
                Timber.e("Search error: ${result.message}")
            }
            is Resource.Loading -> {
                // Already set above
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _state.value = SearchState()
    }
}
