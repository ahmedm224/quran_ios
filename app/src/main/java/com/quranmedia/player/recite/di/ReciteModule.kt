package com.quranmedia.player.recite.di

import android.content.Context
import com.google.gson.Gson
import com.quranmedia.player.BuildConfig
import com.quranmedia.player.recite.audio.AudioRecorder
import com.quranmedia.player.recite.data.api.WhisperApi
import com.quranmedia.player.recite.data.repository.ReciteRepositoryImpl
import com.quranmedia.player.recite.domain.repository.ReciteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier annotation for Whisper API
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ReciteWhisper

/**
 * Dependency injection module for Recite feature network components
 */
@Module
@InstallIn(SingletonComponent::class)
object ReciteNetworkModule {

    /**
     * Provide OkHttpClient with OpenAI authentication
     */
    @Provides
    @Singleton
    @ReciteWhisper
    fun provideWhisperOkHttpClient(): OkHttpClient {
        // Authentication interceptor for OpenAI API
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
                .build()
            chain.proceed(request)
        }

        // Logging interceptor for debugging
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS) // Longer timeout for transcription
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Provide Retrofit instance for Whisper API
     */
    @Provides
    @Singleton
    @ReciteWhisper
    fun provideWhisperRetrofit(
        @ReciteWhisper okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(WhisperApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * Provide WhisperApi instance
     */
    @Provides
    @Singleton
    fun provideWhisperApi(@ReciteWhisper retrofit: Retrofit): WhisperApi {
        return retrofit.create(WhisperApi::class.java)
    }

    /**
     * Provide AudioRecorder instance
     */
    @Provides
    fun provideAudioRecorder(@ApplicationContext context: Context): AudioRecorder {
        return AudioRecorder(context)
    }
}

/**
 * Dependency injection module for Recite feature repository
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ReciteRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindReciteRepository(
        reciteRepositoryImpl: ReciteRepositoryImpl
    ): ReciteRepository
}
