package com.quranmedia.player.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.quranmedia.player.data.database.dao.AthkarDao
import com.quranmedia.player.data.database.dao.AyahDao
import com.quranmedia.player.data.database.dao.AyahIndexDao
import com.quranmedia.player.data.database.dao.AudioVariantDao
import com.quranmedia.player.data.database.dao.BookmarkDao
import com.quranmedia.player.data.database.dao.DownloadedAthanDao
import com.quranmedia.player.data.database.dao.DownloadTaskDao
import com.quranmedia.player.data.database.dao.PrayerTimesDao
import com.quranmedia.player.data.database.dao.ReadingBookmarkDao
import com.quranmedia.player.data.database.dao.ReciterDao
import com.quranmedia.player.data.database.dao.SurahDao
import com.quranmedia.player.data.database.dao.DailyActivityDao
import com.quranmedia.player.data.database.dao.QuranProgressDao
import com.quranmedia.player.data.database.dao.KhatmahGoalDao
import com.quranmedia.player.data.database.dao.TafseerDao
import com.quranmedia.player.data.database.entity.AthkarCategoryEntity
import com.quranmedia.player.data.database.entity.AyahEntity
import com.quranmedia.player.data.database.entity.AyahIndexEntity
import com.quranmedia.player.data.database.entity.AudioVariantEntity
import com.quranmedia.player.data.database.entity.BookmarkEntity
import com.quranmedia.player.data.database.entity.DownloadedAthanEntity
import com.quranmedia.player.data.database.entity.DownloadTaskEntity
import com.quranmedia.player.data.database.entity.PrayerTimesEntity
import com.quranmedia.player.data.database.entity.ReadingBookmarkEntity
import com.quranmedia.player.data.database.entity.ReciterEntity
import com.quranmedia.player.data.database.entity.SurahEntity
import com.quranmedia.player.data.database.entity.ThikrEntity
import com.quranmedia.player.data.database.entity.UserLocationEntity
import com.quranmedia.player.data.database.entity.DailyActivityEntity
import com.quranmedia.player.data.database.entity.QuranProgressEntity
import com.quranmedia.player.data.database.entity.KhatmahGoalEntity
import com.quranmedia.player.data.database.entity.TafseerDownloadEntity
import com.quranmedia.player.data.database.entity.TafseerContentEntity

@Database(
    entities = [
        ReciterEntity::class,
        SurahEntity::class,
        AudioVariantEntity::class,
        AyahEntity::class,  // Quran Ayah data
        AyahIndexEntity::class,
        BookmarkEntity::class,
        ReadingBookmarkEntity::class,  // Reading mode page bookmarks
        DownloadTaskEntity::class,
        // Athkar entities
        AthkarCategoryEntity::class,
        ThikrEntity::class,
        // Prayer Times entities
        PrayerTimesEntity::class,
        UserLocationEntity::class,
        // Downloaded Athans
        DownloadedAthanEntity::class,
        // Daily Tracker entities
        DailyActivityEntity::class,
        QuranProgressEntity::class,
        KhatmahGoalEntity::class,
        // Tafseer entities
        TafseerDownloadEntity::class,
        TafseerContentEntity::class
    ],
    version = 9,  // Incremented for Prayer Times offline caching improvements
    exportSchema = true
)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun reciterDao(): ReciterDao
    abstract fun surahDao(): SurahDao
    abstract fun audioVariantDao(): AudioVariantDao
    abstract fun ayahDao(): AyahDao
    abstract fun ayahIndexDao(): AyahIndexDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun readingBookmarkDao(): ReadingBookmarkDao
    abstract fun downloadTaskDao(): DownloadTaskDao
    abstract fun athkarDao(): AthkarDao
    abstract fun prayerTimesDao(): PrayerTimesDao
    abstract fun downloadedAthanDao(): DownloadedAthanDao
    abstract fun dailyActivityDao(): DailyActivityDao
    abstract fun quranProgressDao(): QuranProgressDao
    abstract fun khatmahGoalDao(): KhatmahGoalDao
    abstract fun tafseerDao(): TafseerDao
}
