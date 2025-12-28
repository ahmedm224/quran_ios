package com.quranmedia.player.domain.repository

import com.quranmedia.player.domain.model.SearchResult
import com.quranmedia.player.domain.util.Resource

interface SearchRepository {
    suspend fun searchAyahs(query: String): Resource<List<SearchResult>>
}
