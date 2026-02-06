package com.quranmedia.player.wear.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.quranmedia.player.wear.data.database.dao.PrayerTimesDao
import com.quranmedia.player.wear.data.database.entity.PrayerSettingsEntity
import com.quranmedia.player.wear.data.database.entity.PrayerTimesEntity
import com.quranmedia.player.wear.data.database.entity.UserLocationEntity

@Database(
    entities = [
        PrayerTimesEntity::class,
        UserLocationEntity::class,
        PrayerSettingsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class WearDatabase : RoomDatabase() {
    abstract fun prayerTimesDao(): PrayerTimesDao
}
