package com.quranmedia.player.media.service

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.quranmedia.player.media.player.QuranPlayer
import com.quranmedia.player.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class QuranMediaService : MediaSessionService() {

    @Inject
    lateinit var quranPlayer: QuranPlayer

    @Inject
    lateinit var playbackController: com.quranmedia.player.media.controller.PlaybackController

    @Inject
    lateinit var quranRepository: com.quranmedia.player.domain.repository.QuranRepository

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val CUSTOM_COMMAND_NEXT_AYAH = "NEXT_AYAH"
        const val CUSTOM_COMMAND_PREVIOUS_AYAH = "PREVIOUS_AYAH"
        const val CUSTOM_COMMAND_TOGGLE_LOOP = "TOGGLE_LOOP"
        const val CUSTOM_COMMAND_NUDGE_FORWARD = "NUDGE_FORWARD"
        const val CUSTOM_COMMAND_NUDGE_BACKWARD = "NUDGE_BACKWARD"
    }

    override fun onCreate() {
        super.onCreate()

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, quranPlayer.player)
            .setCallback(MediaSessionCallback())
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        Timber.d("QuranMediaService created")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
        Timber.d("QuranMediaService destroyed")
    }

    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(CUSTOM_COMMAND_NEXT_AYAH, Bundle.EMPTY))
                .add(SessionCommand(CUSTOM_COMMAND_PREVIOUS_AYAH, Bundle.EMPTY))
                .add(SessionCommand(CUSTOM_COMMAND_TOGGLE_LOOP, Bundle.EMPTY))
                .add(SessionCommand(CUSTOM_COMMAND_NUDGE_FORWARD, Bundle.EMPTY))
                .add(SessionCommand(CUSTOM_COMMAND_NUDGE_BACKWARD, Bundle.EMPTY))
                .build()

            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommands)
                .build()
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0)
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CUSTOM_COMMAND_NEXT_AYAH -> {
                    Timber.d("Next ayah command")
                }
                CUSTOM_COMMAND_PREVIOUS_AYAH -> {
                    Timber.d("Previous ayah command")
                }
                CUSTOM_COMMAND_TOGGLE_LOOP -> {
                    Timber.d("Toggle loop command")
                }
                CUSTOM_COMMAND_NUDGE_FORWARD -> {
                    val incrementMs = args.getLong("incrementMs", 250)
                    quranPlayer.nudgeForward(incrementMs)
                }
                CUSTOM_COMMAND_NUDGE_BACKWARD -> {
                    val incrementMs = args.getLong("incrementMs", 250)
                    quranPlayer.nudgeBackward(incrementMs)
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val result = SettableFuture.create<MutableList<MediaItem>>()

            serviceScope.launch {
                try {
                    val updatedMediaItems = mediaItems.map { mediaItem ->
                        val mediaId = mediaItem.mediaId

                        // Check if it already has a URI
                        if (mediaItem.localConfiguration?.uri != null) {
                            return@map mediaItem
                        }

                        // Check if requestMetadata has a URI
                        val requestUri = mediaItem.requestMetadata.mediaUri
                        if (requestUri != null) {
                            return@map mediaItem.buildUpon()
                                .setUri(requestUri)
                                .build()
                        }

                        // Parse mediaId to build URI (for Android Auto)
                        // Format 1: "reciter_ar.alafasy:surah_1" (from browse selection)
                        // Format 2: "surah_1" (from voice search or Browse by Surah)

                        var reciterId: String? = null
                        var surahNumber: Int? = null

                        if (mediaId.contains(":") && mediaId.contains("surah_")) {
                            // Format 1: Full format with reciter
                            val parts = mediaId.split(":")
                            reciterId = parts[0].removePrefix("reciter_")
                            surahNumber = parts[1].removePrefix("surah_").toIntOrNull()
                        } else if (mediaId.startsWith("surah_")) {
                            // Format 2: Just surah number - use default reciter (ar.abdulbasitmurattal)
                            surahNumber = mediaId.removePrefix("surah_").toIntOrNull()
                            reciterId = "ar.abdulbasitmurattal"
                            Timber.d("Using default reciter ar.abdulbasitmurattal for mediaId: $mediaId")
                        }

                        if (surahNumber != null && reciterId != null) {
                            // IMPORTANT: Initialize PlaybackController with ayah tracking
                            serviceScope.launch {
                                try {
                                    val surah = quranRepository.getSurahByNumber(surahNumber)
                                    val reciter = quranRepository.getReciterById(reciterId)
                                    val audioVariants = quranRepository.getAudioVariants(reciterId, surahNumber).first()

                                    if (surah != null && reciter != null && audioVariants.isNotEmpty()) {
                                        val audioUrl = audioVariants.first().url
                                        Timber.d("Initializing PlaybackController for Android Auto: $reciterId, surah $surahNumber")
                                        playbackController.playAudio(
                                            reciterId = reciterId,
                                            surahNumber = surahNumber,
                                            audioUrl = audioUrl,
                                            surahNameArabic = surah.nameArabic,
                                            surahNameEnglish = surah.nameEnglish,
                                            reciterName = reciter.name
                                        )
                                    }
                                } catch (e: Exception) {
                                    Timber.e(e, "Error initializing playback from Android Auto")
                                }
                            }

                            val audioUrl = "https://cdn.islamic.network/quran/audio-surah/128/$reciterId/$surahNumber.mp3"
                            Timber.d("Building MediaItem for $mediaId with URI: $audioUrl")

                            return@map mediaItem.buildUpon()
                                .setUri(audioUrl)
                                .build()
                        }

                        // Fallback: return as-is
                        Timber.w("Could not resolve URI for mediaId: $mediaId")
                        mediaItem
                    }.toMutableList()

                    result.set(updatedMediaItems)
                } catch (e: Exception) {
                    Timber.e(e, "Error in onAddMediaItems")
                    result.set(mediaItems)
                }
            }

            return result
        }
    }
}
