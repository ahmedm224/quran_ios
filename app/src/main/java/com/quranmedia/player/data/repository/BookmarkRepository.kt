package com.quranmedia.player.data.repository

import com.quranmedia.player.data.database.dao.BookmarkDao
import com.quranmedia.player.data.database.entity.toEntity
import com.quranmedia.player.data.database.entity.toDomainModel
import com.quranmedia.player.domain.model.Bookmark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao
) {

    fun getAllBookmarks(): Flow<List<Bookmark>> {
        return bookmarkDao.getAllBookmarks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun getBookmarkById(id: String): Bookmark? {
        return bookmarkDao.getBookmarkById(id)?.toDomainModel()
    }

    suspend fun getBookmarksForAyah(
        reciterId: String,
        surahNumber: Int,
        ayahNumber: Int
    ): List<Bookmark> {
        return bookmarkDao.getBookmarksForAyah(reciterId, surahNumber, ayahNumber)
            .map { it.toDomainModel() }
    }

    suspend fun insertBookmark(
        reciterId: String,
        surahNumber: Int,
        ayahNumber: Int,
        positionMs: Long,
        label: String? = null,
        loopEndMs: Long? = null
    ): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()

        val bookmark = Bookmark(
            id = id,
            reciterId = reciterId,
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            positionMs = positionMs,
            label = label,
            loopEndMs = loopEndMs,
            createdAt = Date(now),
            updatedAt = Date(now)
        )

        bookmarkDao.insertBookmark(bookmark.toEntity())
        return id
    }

    suspend fun updateBookmark(bookmark: Bookmark) {
        val updated = bookmark.copy(updatedAt = Date())
        bookmarkDao.insertBookmark(updated.toEntity())
    }

    suspend fun deleteBookmark(id: String) {
        bookmarkDao.deleteBookmark(id)
    }

    suspend fun deleteAllBookmarks() {
        bookmarkDao.deleteAllBookmarks()
    }
}
