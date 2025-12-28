package com.quranmedia.player.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.quranmedia.player.data.database.QuranDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Migration from version 6 to 7
     * Adds textTajweed column to ayahs table
     */
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add textTajweed column (nullable)
            database.execSQL("ALTER TABLE ayahs ADD COLUMN textTajweed TEXT")
        }
    }

    /**
     * Migration from version 5 to 6
     * Adds daily tracker feature tables
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create daily_activities table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS daily_activities (
                    date TEXT NOT NULL,
                    activityType TEXT NOT NULL,
                    completed INTEGER NOT NULL,
                    completedAt INTEGER,
                    createdAt INTEGER NOT NULL,
                    PRIMARY KEY(date, activityType)
                )
            """)

            // Create quran_progress table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS quran_progress (
                    date TEXT NOT NULL PRIMARY KEY,
                    pagesRead INTEGER NOT NULL DEFAULT 0,
                    pagesListened INTEGER NOT NULL DEFAULT 0,
                    readingDurationMs INTEGER NOT NULL DEFAULT 0,
                    listeningDurationMs INTEGER NOT NULL DEFAULT 0,
                    lastPage INTEGER NOT NULL DEFAULT 1,
                    lastSurah INTEGER NOT NULL DEFAULT 1,
                    lastAyah INTEGER NOT NULL DEFAULT 1,
                    updatedAt INTEGER NOT NULL
                )
            """)

            // Create khatmah_goals table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS khatmah_goals (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    startDate TEXT NOT NULL,
                    endDate TEXT NOT NULL,
                    startPage INTEGER NOT NULL DEFAULT 1,
                    targetPages INTEGER NOT NULL DEFAULT 604,
                    isActive INTEGER NOT NULL DEFAULT 1,
                    goalType TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    completedAt INTEGER
                )
            """)
        }
    }

    @Provides
    @Singleton
    fun provideQuranDatabase(
        @ApplicationContext context: Context
    ): QuranDatabase {
        return Room.databaseBuilder(
            context,
            QuranDatabase::class.java,
            "quran_media_db"
        )
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
            .fallbackToDestructiveMigration() // Fallback for other migrations
            .build()
    }

    @Provides
    fun provideReciterDao(database: QuranDatabase): ReciterDao {
        return database.reciterDao()
    }

    @Provides
    fun provideSurahDao(database: QuranDatabase): SurahDao {
        return database.surahDao()
    }

    @Provides
    fun provideAudioVariantDao(database: QuranDatabase): AudioVariantDao {
        return database.audioVariantDao()
    }

    @Provides
    fun provideAyahIndexDao(database: QuranDatabase): AyahIndexDao {
        return database.ayahIndexDao()
    }

    @Provides
    fun provideBookmarkDao(database: QuranDatabase): BookmarkDao {
        return database.bookmarkDao()
    }

    @Provides
    fun provideDownloadTaskDao(database: QuranDatabase): DownloadTaskDao {
        return database.downloadTaskDao()
    }

    @Provides
    fun provideAyahDao(database: QuranDatabase): AyahDao {
        return database.ayahDao()
    }

    @Provides
    fun provideReadingBookmarkDao(database: QuranDatabase): ReadingBookmarkDao {
        return database.readingBookmarkDao()
    }

    @Provides
    fun provideAthkarDao(database: QuranDatabase): AthkarDao {
        return database.athkarDao()
    }

    @Provides
    fun providePrayerTimesDao(database: QuranDatabase): PrayerTimesDao {
        return database.prayerTimesDao()
    }

    @Provides
    fun provideDownloadedAthanDao(database: QuranDatabase): DownloadedAthanDao {
        return database.downloadedAthanDao()
    }

    @Provides
    fun provideDailyActivityDao(database: QuranDatabase): DailyActivityDao {
        return database.dailyActivityDao()
    }

    @Provides
    fun provideQuranProgressDao(database: QuranDatabase): QuranProgressDao {
        return database.quranProgressDao()
    }

    @Provides
    fun provideKhatmahGoalDao(database: QuranDatabase): KhatmahGoalDao {
        return database.khatmahGoalDao()
    }
}
