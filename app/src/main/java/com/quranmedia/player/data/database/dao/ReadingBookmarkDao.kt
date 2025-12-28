package com.quranmedia.player.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranmedia.player.data.database.entity.ReadingBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingBookmarkDao {

    @Query("SELECT * FROM reading_bookmarks ORDER BY createdAt DESC")
    fun getAllReadingBookmarks(): Flow<List<ReadingBookmarkEntity>>

    @Query("SELECT * FROM reading_bookmarks WHERE pageNumber = :pageNumber LIMIT 1")
    suspend fun getBookmarkForPage(pageNumber: Int): ReadingBookmarkEntity?

    @Query("SELECT * FROM reading_bookmarks WHERE id = :bookmarkId")
    suspend fun getBookmarkById(bookmarkId: String): ReadingBookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: ReadingBookmarkEntity)

    @Query("DELETE FROM reading_bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmark(bookmarkId: String)

    @Query("DELETE FROM reading_bookmarks WHERE pageNumber = :pageNumber")
    suspend fun deleteBookmarkForPage(pageNumber: Int)

    @Query("DELETE FROM reading_bookmarks")
    suspend fun deleteAllBookmarks()

    @Query("SELECT COUNT(*) FROM reading_bookmarks")
    suspend fun getBookmarkCount(): Int

    @Query("SELECT * FROM reading_bookmarks ORDER BY createdAt DESC LIMIT 1")
    suspend fun getMostRecentBookmark(): ReadingBookmarkEntity?
}
