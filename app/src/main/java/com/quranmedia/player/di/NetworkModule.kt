package com.quranmedia.player.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.quranmedia.player.data.api.AladhanApi
import com.quranmedia.player.data.api.AlQuranCloudApi
import com.quranmedia.player.data.api.AthanApi
import com.quranmedia.player.data.api.AthkarApi
import com.quranmedia.player.data.api.QuranApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier annotations for different API services
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AlQuranCloud

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CloudLinqed

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Aladhan

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class HisnMuslim

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AlFurqan

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        // Create HTTP cache (50 MB) - will respect server Cache-Control headers
        val cacheSize = 50L * 1024L * 1024L // 50 MB
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, cacheSize)

        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                }
            )
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                // Log cache usage for debugging
                val cacheResponse = response.cacheResponse
                val networkResponse = response.networkResponse
                when {
                    cacheResponse != null && networkResponse != null -> {
                        Timber.d("HTTP Cache: CONDITIONAL (validated with server)")
                    }
                    cacheResponse != null -> {
                        Timber.d("HTTP Cache: HIT (served from cache)")
                    }
                    networkResponse != null -> {
                        Timber.d("HTTP Cache: MISS (fetched from network)")
                    }
                }
                response
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Retrofit instance for Al-Quran Cloud API (text/metadata)
     */
    @Provides
    @Singleton
    @AlQuranCloud
    fun provideAlQuranCloudRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AlQuranCloudApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * Retrofit instance for CloudLinqed Quran API (reciters/audio)
     */
    @Provides
    @Singleton
    @CloudLinqed
    fun provideQuranApiRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(QuranApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideAlQuranCloudApi(@AlQuranCloud retrofit: Retrofit): AlQuranCloudApi {
        return retrofit.create(AlQuranCloudApi::class.java)
    }

    @Provides
    @Singleton
    fun provideQuranApi(@CloudLinqed retrofit: Retrofit): QuranApi {
        return retrofit.create(QuranApi::class.java)
    }

    /**
     * Retrofit instance for Aladhan Prayer Times API
     */
    @Provides
    @Singleton
    @Aladhan
    fun provideAladhanRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AladhanApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * Retrofit instance for HisnMuslim Athkar API
     */
    @Provides
    @Singleton
    @HisnMuslim
    fun provideHisnMuslimRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AthkarApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideAladhanApi(@Aladhan retrofit: Retrofit): AladhanApi {
        return retrofit.create(AladhanApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAthkarApi(@HisnMuslim retrofit: Retrofit): AthkarApi {
        return retrofit.create(AthkarApi::class.java)
    }

    /**
     * Retrofit instance for Al Furqan Athan API
     */
    @Provides
    @Singleton
    @AlFurqan
    fun provideAlFurqanRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AthanApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideAthanApi(@AlFurqan retrofit: Retrofit): AthanApi {
        return retrofit.create(AthanApi::class.java)
    }
}
