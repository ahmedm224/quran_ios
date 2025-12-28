package com.quranmedia.player.data.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object SettingsSerializer : Serializer<UserSettings> {
    override val defaultValue: UserSettings = UserSettings.getDefaultInstance()
        .toBuilder()
        .setPlaybackSpeed(1.0f)
        .setPitchLockEnabled(true)
        .setSmallSeekIncrementMs(250)
        .setLargeSeekIncrementMs(1000)
        .setSnapToAyahEnabled(true)
        .setGaplessPlayback(true)
        .setVolumeLevel(100)
        .setNormalizeAudio(true)
        .setAutoLoopEnabled(false)
        .setLoopCount(1)
        .setWaveformEnabled(true)
        .setShowTranslation(false)
        .setTranslationLanguage("en")
        .setDarkMode(false)
        .setDynamicColors(true)
        .setWifiOnlyDownloads(true)
        .setAutoDeleteAfterPlayback(false)
        .setPreferredBitrate(128)
        .setPreferredAudioFormat("MP3")
        .setShowNotification(true)
        .setShowPlaybackPosition(true)
        .setAndroidAutoEnabled(true)
        .setRecentBookmarksCount(5)
        .setLastReciterId("")
        .setLastSurahNumber(1)
        .setLastPositionMs(0)
        .setResumeOnStartup(true)
        .setLargeText(false)
        .setHighContrast(false)
        .setHapticFeedbackIntensity(50)
        .build()

    override suspend fun readFrom(input: InputStream): UserSettings {
        try {
            return UserSettings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: UserSettings, output: OutputStream) {
        t.writeTo(output)
    }
}
