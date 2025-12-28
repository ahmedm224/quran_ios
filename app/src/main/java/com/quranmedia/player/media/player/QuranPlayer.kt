package com.quranmedia.player.media.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.quranmedia.player.media.model.LoopState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuranPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var _player: ExoPlayer? = null
    val player: ExoPlayer
        get() = _player ?: createPlayer()

    private val _loopState = MutableStateFlow(LoopState())
    val loopState: StateFlow<LoopState> = _loopState.asStateFlow()

    private val _currentAyahNumber = MutableStateFlow<Int?>(null)
    val currentAyahNumber: StateFlow<Int?> = _currentAyahNumber.asStateFlow()

    private fun createPlayer(): ExoPlayer {
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setMaxAudioBitrate(Int.MAX_VALUE)
            )
        }

        // Create HTTP data source factory for streaming
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("QuranMediaPlayer/1.0")
            .setConnectTimeoutMs(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS)
            .setReadTimeoutMs(DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS)
            .setAllowCrossProtocolRedirects(true)

        // Create data source factory that supports both HTTP streaming and local files
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        // Create media source factory with support for both HTTP and file URIs
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setSeekBackIncrementMs(250)
            .setSeekForwardIncrementMs(250)
            .build()
            .apply {
                // Configure for exact seeking (critical for ayah-level precision)
                setSeekParameters(SeekParameters.EXACT)

                // Add player listener
                addListener(playerListener)

                Timber.d("ExoPlayer created with HTTP/File streaming support and exact seek parameters")
            }
            .also { _player = it }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> Timber.d("Player buffering")
                Player.STATE_READY -> Timber.d("Player ready")
                Player.STATE_ENDED -> handlePlaybackEnded()
                Player.STATE_IDLE -> Timber.d("Player idle")
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Timber.d("Player isPlaying: $isPlaying")
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Timber.e(error, "Player error: ${error.message}")
        }
    }

    fun setMediaItem(mediaItem: MediaItem) {
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        Timber.d("Seeking to position: ${positionMs}ms")
    }

    fun seekToAyah(ayahStartMs: Long, ayahNumber: Int) {
        seekTo(ayahStartMs)
        _currentAyahNumber.value = ayahNumber
        Timber.d("Seeking to ayah $ayahNumber at ${ayahStartMs}ms")
    }

    fun nudgeForward(incrementMs: Long) {
        val newPosition = (player.currentPosition + incrementMs).coerceAtMost(player.duration)
        seekTo(newPosition)
    }

    fun nudgeBackward(incrementMs: Long) {
        val newPosition = (player.currentPosition - incrementMs).coerceAtLeast(0)
        seekTo(newPosition)
    }

    fun setPlaybackSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
        Timber.d("Playback speed set to: $speed")
    }

    fun enableLoop(startMs: Long, endMs: Long, loopCount: Int = -1) {
        _loopState.value = LoopState(
            isEnabled = true,
            startMs = startMs,
            endMs = endMs,
            currentLoop = 0,
            totalLoops = loopCount
        )
        Timber.d("Loop enabled: ${startMs}ms to ${endMs}ms, count: $loopCount")
    }

    fun disableLoop() {
        _loopState.value = LoopState()
        Timber.d("Loop disabled")
    }

    fun checkAndHandleLoop() {
        val loop = _loopState.value
        if (!loop.isEnabled) return

        val currentPos = player.currentPosition
        if (currentPos >= loop.endMs) {
            if (loop.totalLoops == -1 || loop.currentLoop < loop.totalLoops - 1) {
                // Continue looping
                seekTo(loop.startMs)
                _loopState.value = loop.copy(currentLoop = loop.currentLoop + 1)
                Timber.d("Looping: iteration ${loop.currentLoop + 1}")
            } else {
                // Loop completed
                disableLoop()
                player.pause()
                Timber.d("Loop completed")
            }
        }
    }

    private fun handlePlaybackEnded() {
        val loop = _loopState.value
        if (loop.isEnabled) {
            seekTo(loop.startMs)
        }
    }

    fun getCurrentPosition(): Long = player.currentPosition

    fun getDuration(): Long = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L

    fun isPlaying(): Boolean = player.isPlaying

    fun release() {
        _player?.release()
        _player = null
        Timber.d("Player released")
    }
}
