package com.quranmedia.player.wear.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.quranmedia.player.wear.data.database.WearDatabase
import com.quranmedia.player.wear.data.database.dao.PrayerTimesDao
import com.quranmedia.player.wear.data.location.LocationHelper
import com.quranmedia.player.wear.data.notification.PrayerNotificationScheduler
import com.quranmedia.player.wear.data.repository.WearAthkarRepository
import com.quranmedia.player.wear.data.repository.WearPrayerTimesRepository
import com.quranmedia.player.wear.data.repository.WearQuranRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WearAppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideWearAthkarRepository(
        @ApplicationContext context: Context,
        gson: Gson
    ): WearAthkarRepository {
        return WearAthkarRepository(context, gson)
    }

    @Provides
    @Singleton
    fun provideWearDatabase(
        @ApplicationContext context: Context
    ): WearDatabase {
        return Room.databaseBuilder(
            context,
            WearDatabase::class.java,
            "wear_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun providePrayerTimesDao(database: WearDatabase): PrayerTimesDao {
        return database.prayerTimesDao()
    }

    @Provides
    @Singleton
    fun provideLocationHelper(
        @ApplicationContext context: Context
    ): LocationHelper {
        return LocationHelper(context)
    }

    @Provides
    @Singleton
    fun provideWearPrayerTimesRepository(
        prayerTimesDao: PrayerTimesDao,
        locationHelper: LocationHelper
    ): WearPrayerTimesRepository {
        return WearPrayerTimesRepository(prayerTimesDao, locationHelper)
    }

    @Provides
    @Singleton
    fun providePrayerNotificationScheduler(
        @ApplicationContext context: Context,
        repository: WearPrayerTimesRepository
    ): PrayerNotificationScheduler {
        return PrayerNotificationScheduler(context, repository)
    }

    @Provides
    @Singleton
    fun provideWearQuranRepository(
        @ApplicationContext context: Context,
        gson: Gson
    ): WearQuranRepository {
        return WearQuranRepository(context, gson)
    }
}
