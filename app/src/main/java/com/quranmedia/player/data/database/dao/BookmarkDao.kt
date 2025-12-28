package com.quranmedia.player.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranmedia.player.data.database.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks ORDER BY updatedAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("""
        SELECT * FROM bookmarks
        WHERE reciterId = :reciterId
        AND surahNumber = :surahNumber
        AND ayahNumber = :ayahNumber
    """)
    suspend fun getBookmarksForAyah(
        reciterId: String,
        surahNumber: Int,
        ayahNumber: Int
    ): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE id = :bookmarkId")
    suspend fun getBookmarkById(bookmarkId: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmark(bookmarkId: String)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAllBookmarks()
}
