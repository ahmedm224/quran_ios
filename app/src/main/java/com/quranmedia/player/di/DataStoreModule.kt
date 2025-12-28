package com.quranmedia.player.di

// TEMPORARY: Commented out due to protobuf/KSP configuration issue
// TODO: Re-enable after fixing protobuf source set configuration
// See QURAN_DATA_INTEGRATION.md for fix options

/*
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.quranmedia.player.data.datastore.SettingsSerializer
import com.quranmedia.player.data.datastore.UserSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context
    ): DataStore<UserSettings> {
        return DataStoreFactory.create(
            serializer = SettingsSerializer,
            produceFile = { context.dataStoreFile("settings.pb") }
        )
    }
}
*/
