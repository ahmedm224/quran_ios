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
import com.quranmedia.player.data.database.dao.TafseerDao
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
     * Migration from version 8 to 9
     * Updates prayer_times_cache table with asrMethod and hijriMonthNumber columns
     * for proper offline caching
     */
    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Drop and recreate prayer_times_cache with new schema
            // This is a cache table so data loss is acceptable
            database.execSQL("DROP TABLE IF EXISTS prayer_times_cache")
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS prayer_times_cache (
                    date TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    fajr TEXT NOT NULL,
                    sunrise TEXT NOT NULL,
                    dhuhr TEXT NOT NULL,
                    asr TEXT NOT NULL,
                    maghrib TEXT NOT NULL,
                    isha TEXT NOT NULL,
                    locationName TEXT NOT NULL,
                    calculationMethod INTEGER NOT NULL,
                    asrMethod INTEGER NOT NULL DEFAULT 0,
                    hijriDay INTEGER NOT NULL,
                    hijriMonth TEXT NOT NULL,
                    hijriMonthArabic TEXT NOT NULL,
                    hijriYear INTEGER NOT NULL,
                    hijriMonthNumber INTEGER NOT NULL DEFAULT 1,
                    cachedAt INTEGER NOT NULL,
                    PRIMARY KEY(date, latitude, longitude, calculationMethod, asrMethod)
                )
            """)
        }
    }

    /**
     * Migration from version 7 to 8
     * Adds tafseer feature tables
     */
    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create tafseer_downloads table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS tafseer_downloads (
                    tafseerId TEXT NOT NULL PRIMARY KEY,
                    nameArabic TEXT NOT NULL,
                    nameEnglish TEXT NOT NULL,
                    language TEXT NOT NULL,
                    type TEXT NOT NULL,
                    downloadedAt INTEGER NOT NULL,
                    totalSizeBytes INTEGER NOT NULL,
                    surahsDownloaded INTEGER NOT NULL DEFAULT 114
                )
            """)

            // Create tafseer_content table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS tafseer_content (
                    tafseerId TEXT NOT NULL,
                    surah INTEGER NOT NULL,
                    ayah INTEGER NOT NULL,
                    text TEXT NOT NULL,
                    PRIMARY KEY(tafseerId, surah, ayah)
                )
            """)

            // Create indices for tafseer_content
            database.execSQL("CREATE INDEX IF NOT EXISTS index_tafseer_content_tafseerId ON tafseer_content(tafseerId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_tafseer_content_surah_ayah ON tafseer_content(surah, ayah)")
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
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
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

    @Provides
    fun provideTafseerDao(database: QuranDatabase): TafseerDao {
        return database.tafseerDao()
    }
}
