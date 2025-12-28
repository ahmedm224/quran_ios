package com.quranmedia.player.media.controller

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import com.google.common.util.concurrent.ListenableFuture
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.domain.model.AyahIndex
import com.quranmedia.player.domain.repository.QuranRepository
import com.quranmedia.player.media.model.PlaybackState
import com.quranmedia.player.media.service.QuranMediaService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaControllerFuture: ListenableFuture<MediaController>,
    private val quranRepository: QuranRepository,
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope
) {
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var mediaController: MediaController? = null
    private var positionUpdateJob: Job? = null
    private var ayahIndices: List<AyahIndex> = emptyList()

    // Custom recitation state
    private var customRecitationSettings: com.quranmedia.player.domain.model.CustomRecitationSettings? = null
    private var customRecitationCurrentGroupIteration: Int = 0

    // App icon URI for artwork - use high-res Alfurqan logo for Android Auto
    private val appIconUri: Uri by lazy {
        // Use the dedicated high-res logo for crisp rendering in Android Auto
        val uri = Uri.parse("android.resource://${context.packageName}/${com.quranmedia.player.R.drawable.quran_logo}")
        Timber.d("App icon URI for artwork: $uri")
        uri
    }

    /**
     * Sanitizes Arabic text for Bluetooth display by removing diacritics (harakat/tashkeel).
     * Bluetooth devices often can't render Arabic combining characters, showing squares instead.
     * This keeps the base Arabic letters readable on Bluetooth while the full text with
     * diacritics is preserved in the app and Android Auto (which handles them properly).
     */
    private fun sanitizeForBluetooth(text: String): String {
        // Remove Arabic diacritics (harakat) that cause display issues on Bluetooth
        // U+064B-U+0652: Fathatan, Dammatan, Kasratan, Fatha, Damma, Kasra, Shadda, Sukun
        // U+0670: Superscript Alef
        // U+0640: Tatweel (kashida - elongation character)
        // U+06D6-U+06ED: Additional Quranic annotation marks
        return text
            .replace(Regex("[\u064B-\u0652\u0670\u0640\u06D6-\u06ED]"), "")
            .trim()
    }

    init {
        mediaControllerFuture.addListener({
            mediaController = mediaControllerFuture.get()
            setupPlayerListener()
            startPositionUpdates()
        }, { it.run() })
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)

                // Save position when pausing
                if (!isPlaying) {
                    coroutineScope.launch {
                        val currentReciter = _playbackState.value.currentReciter
                        val currentSurah = _playbackState.value.currentSurah
                        val currentAyah = _playbackState.value.currentAyah
                        if (currentReciter != null && currentSurah != null && currentAyah != null) {
                            settingsRepository.updateLastPlaybackState(currentReciter, currentSurah, currentAyah.toLong())
                            Timber.d("Saved on pause: reciter=$currentReciter, surah=$currentSurah, ayah=$currentAyah")
                        }
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateString = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN"
                }
                Timber.d("Playback state changed to: $stateString")

                _playbackState.value = _playbackState.value.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING
                )

                // Check if playback ended and auto-play next surah
                if (playbackState == Player.STATE_ENDED) {
                    Timber.d("Playback ended, checking continuous playback...")
                    handlePlaybackEnded()
                }
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                // Update current ayah text when track changes
                mediaItem?.let { updateCurrentMediaItem(it) }
            }
        })
    }

    private fun handlePlaybackEnded() {
        coroutineScope.launch {
            try {
                // If in custom recitation mode, stop playback instead of continuing
                if (customRecitationSettings != null) {
                    Timber.d("Custom recitation ended. Stopping playback.")
                    customRecitationSettings = null
                    customRecitationCurrentGroupIteration = 0
                    return@launch
                }

                // Check if continuous playback is enabled
                val isContinuousPlaybackEnabled = settingsRepository.settings.first().continuousPlaybackEnabled

                if (!isContinuousPlaybackEnabled) {
                    Timber.d("Playback ended. Continuous playback is disabled")
                    return@launch
                }

                val currentSurah = _playbackState.value.currentSurah ?: return@launch

                // Use the user's selected reciter from settings (may have changed during playback)
                val selectedReciterId = settingsRepository.getSelectedReciterId()
                val reciterId = if (selectedReciterId.isNotEmpty()) {
                    selectedReciterId
                } else {
                    _playbackState.value.currentReciter ?: return@launch
                }

                // Move to next surah (1-114)
                val nextSurahNumber = currentSurah + 1

                if (nextSurahNumber > 114) {
                    Timber.d("Reached end of Quran. No more surahs to play")
                    return@launch
                }

                Timber.d("Auto-playing next surah: $nextSurahNumber with reciter: $reciterId")

                // Load next surah data
                val nextSurah = quranRepository.getSurahByNumber(nextSurahNumber)
                val reciter = quranRepository.getReciterById(reciterId)
                val audioVariants = quranRepository.getAudioVariants(reciterId, nextSurahNumber).first()

                if (nextSurah == null || reciter == null || audioVariants.isEmpty()) {
                    Timber.e("Failed to load next surah data")
                    return@launch
                }

                val audioVariant = audioVariants.first()
                playAudio(
                    reciterId = reciterId,
                    surahNumber = nextSurahNumber,
                    audioUrl = audioVariant.url,
                    surahNameArabic = nextSurah.nameArabic,
                    surahNameEnglish = nextSurah.nameEnglish,
                    reciterName = reciter.name
                )
            } catch (e: Exception) {
                Timber.e(e, "Error auto-playing next surah")
            }
        }
    }

    private fun updateCurrentMediaItem(mediaItem: androidx.media3.common.MediaItem) {
        val metadata = mediaItem.mediaMetadata
        val extras = metadata.extras

        _playbackState.value = _playbackState.value.copy(
            currentAyahText = metadata.title?.toString(),
            currentAyah = extras?.getInt("ayahNumber"),
            totalAyahs = extras?.getInt("totalAyahs"),
            currentSurahNameArabic = extras?.getString("surahNameArabic"),
            currentSurahNameEnglish = extras?.getString("surahNameEnglish")
        )
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = coroutineScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            while (isActive) {
                mediaController?.let { controller ->
                    val position = controller.currentPosition
                    val duration = controller.duration

                    _playbackState.value = _playbackState.value.copy(
                        currentPosition = position,
                        duration = duration
                    )

                    // Update current ayah based on position
                    updateCurrentAyah(position)
                }
                delay(100) // Update every 100ms for smooth UI
            }
        }
    }

    private fun updateCurrentAyah(positionMs: Long) {
        val currentAyah = ayahIndices.find { ayah ->
            positionMs >= ayah.startMs && positionMs < ayah.endMs
        }

        if (currentAyah != null && currentAyah.ayahNumber != _playbackState.value.currentAyah) {
            _playbackState.value = _playbackState.value.copy(
                currentAyah = currentAyah.ayahNumber
            )
        }
    }

    suspend fun playAudio(
        reciterId: String,
        surahNumber: Int,
        audioUrl: String,
        surahNameArabic: String,
        surahNameEnglish: String,
        reciterName: String,
        startFromAyah: Int? = null,
        startFromPositionMs: Long? = null
    ) {
        try {
            // Ensure MediaController is ready
            if (mediaController == null) {
                Timber.e("MediaController not initialized yet")
                return
            }

            // Check if we have downloaded ayah files for offline playback
            val audioVariants = quranRepository.getAudioVariants(reciterId, surahNumber).first()
            val downloadedVariant = audioVariants.firstOrNull { !it.localPath.isNullOrBlank() }

            if (downloadedVariant != null && downloadedVariant.localPath != null) {
                val localPath = java.io.File(downloadedVariant.localPath)

                // Check if it's a directory (ayah-by-ayah download) or single file
                if (localPath.isDirectory && localPath.exists()) {
                    Timber.d("Playing from downloaded ayah directory: ${downloadedVariant.localPath}")
                    playFromDownloadedAyahs(
                        localPath = localPath,
                        surahNumber = surahNumber,
                        surahNameArabic = surahNameArabic,
                        surahNameEnglish = surahNameEnglish,
                        reciterName = reciterName,
                        reciterId = reciterId,
                        startFromAyah = startFromAyah
                    )
                    return
                } else if (localPath.isFile && localPath.exists()) {
                    // Legacy single file support
                    Timber.d("Playing downloaded single file: ${downloadedVariant.localPath}")
                    val fileUri = Uri.fromFile(localPath)
                    playSingleAudio(
                        audioUrl = fileUri.toString(),
                        surahNameArabic = surahNameArabic,
                        surahNameEnglish = surahNameEnglish,
                        reciterName = reciterName,
                        reciterId = reciterId,
                        surahNumber = surahNumber
                    )
                    return
                } else {
                    Timber.w("Downloaded path not found: ${downloadedVariant.localPath}")
                }
            }

            // Fetch surah data with ayah audio URLs from API (on background thread)
            Timber.d("Fetching ayah audio URLs from API for reciter: $reciterId, surah: $surahNumber")
            val surahResponse = quranRepository.getSurahWithAudio(surahNumber, reciterId)

            if (surahResponse == null) {
                Timber.e("Failed to fetch surah data from API")
                // Fallback to single URL approach
                playSingleAudio(audioUrl, surahNameArabic, surahNameEnglish, reciterName, reciterId, surahNumber)
                return
            }

            val ayahs = surahResponse.ayahs
            if (ayahs.isEmpty()) {
                Timber.e("No ayahs found in surah data")
                return
            }

            // Get ayah repeat count from settings
            val ayahRepeatCount = settingsRepository.settings.first().ayahRepeatCount

            // Build playlist of MediaItems (one per ayah, repeated based on setting)
            val mediaItems = ayahs.flatMap { ayah ->
                val ayahAudioUrl = ayah.audio
                if (ayahAudioUrl.isNullOrBlank()) {
                    Timber.w("Ayah ${ayah.numberInSurah} has no audio URL")
                    return@flatMap emptyList()
                }

                // Create repeated entries for this ayah
                (1..ayahRepeatCount).map { repeatIndex ->
                    val repeatSuffix = if (ayahRepeatCount > 1) " (${repeatIndex}/${ayahRepeatCount})" else ""
                    MediaItem.Builder()
                        .setMediaId("${reciterId}_${surahNumber}_${ayah.numberInSurah}_$repeatIndex")
                        .setUri(Uri.parse(ayahAudioUrl))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(sanitizeForBluetooth(ayah.text))  // Sanitized for Bluetooth (no diacritics)
                                .setArtist(reciterName)
                                .setAlbumTitle("$surahNameEnglish - $surahNameArabic")
                                .setDisplayTitle(ayah.text)  // Full text for Android Auto (handles Arabic properly)
                                .setSubtitle("$surahNameEnglish (${ayah.numberInSurah}/${ayahs.size})$repeatSuffix")
                                .setArtworkUri(appIconUri)  // App icon as artwork
                                .setExtras(Bundle().apply {
                                    putString("surahNameArabic", surahNameArabic)
                                    putString("surahNameEnglish", surahNameEnglish)
                                    putInt("ayahNumber", ayah.numberInSurah)
                                    putInt("totalAyahs", ayahs.size)
                                    putString("ayahText", ayah.text)  // Store full ayah text for reference
                                    putInt("repeatIndex", repeatIndex)
                                    putInt("repeatCount", ayahRepeatCount)
                                })
                                .build()
                        )
                        .build()
                }
            }

            if (mediaItems.isEmpty()) {
                Timber.e("No valid media items created from ayah data")
                return
            }

            Timber.d("Created playlist with ${mediaItems.size} ayahs for $surahNameEnglish")

            // All MediaController operations must run on main thread
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                mediaController?.apply {
                    // Set repeat mode to OFF so continuous playback can work
                    repeatMode = Player.REPEAT_MODE_OFF

                    setMediaItems(mediaItems)
                    prepare()

                    // Seek to specific ayah (either from parameter or resume position)
                    val targetAyah = if (startFromPositionMs != null && startFromPositionMs > 0) {
                        startFromPositionMs.toInt().also {
                            Timber.d("Resuming from ayah: $it")
                        }
                    } else {
                        startFromAyah
                    }

                    targetAyah?.let { ayahNum ->
                        // Use direct calculation (ayahNum - 1) for consistency with downloaded playback
                        // This assumes ayahs are sorted and numberInSurah starts from 1
                        val ayahIndex = (ayahNum - 1).coerceIn(0, ayahs.size - 1)
                        val playlistIndex = ayahIndex * ayahRepeatCount
                        if (playlistIndex in 0 until mediaItems.size) {
                            seekToDefaultPosition(playlistIndex)
                            Timber.d("Starting from ayah index: $playlistIndex (ayah #$ayahNum, repeatCount: $ayahRepeatCount, totalAyahs: ${ayahs.size})")
                        } else {
                            Timber.e("Invalid playlist index: $playlistIndex for ayah #$ayahNum (mediaItems: ${mediaItems.size})")
                        }
                    }

                    play()

                    _playbackState.value = _playbackState.value.copy(
                        currentReciter = reciterId,
                        currentReciterName = reciterName,
                        currentSurah = surahNumber,
                        currentSurahNameArabic = surahNameArabic,
                        currentSurahNameEnglish = surahNameEnglish,
                        currentAyah = startFromAyah ?: 1,
                        totalAyahs = ayahs.size
                    )

                    // Get first ayah to set initial text (account for repeat count)
                    if (mediaItems.isNotEmpty()) {
                        val initialIndex = ((startFromAyah ?: 1) - 1) * ayahRepeatCount
                        updateCurrentMediaItem(mediaItems[initialIndex.coerceIn(0, mediaItems.size - 1)])
                    }

                    // Save last playback state
                    settingsRepository.updateLastPlaybackState(reciterId, surahNumber, currentPosition)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error playing audio")
        }
    }

    private suspend fun playSingleAudio(
        audioUrl: String,
        surahNameArabic: String,
        surahNameEnglish: String,
        reciterName: String,
        reciterId: String,
        surahNumber: Int
    ) {
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(audioUrl))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(sanitizeForBluetooth(surahNameArabic))  // Sanitized for Bluetooth
                    .setArtist(reciterName)
                    .setAlbumTitle("Quran")
                    .setDisplayTitle(surahNameArabic)  // Full text for Android Auto
                    .setArtworkUri(appIconUri)  // App icon as artwork
                    .build()
            )
            .build()

        Timber.d("Playing single audio: $surahNameEnglish by $reciterName")

        // All MediaController operations must run on main thread
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            mediaController?.apply {
                setMediaItem(mediaItem)
                prepare()
                play()

                _playbackState.value = _playbackState.value.copy(
                    currentReciter = reciterId,
                    currentSurah = surahNumber
                )
            }
        }
    }

    /**
     * Play from downloaded ayah files stored in a directory.
     * Each ayah is stored as {ayahNumber}.mp3 in the directory.
     */
    private suspend fun playFromDownloadedAyahs(
        localPath: java.io.File,
        surahNumber: Int,
        surahNameArabic: String,
        surahNameEnglish: String,
        reciterName: String,
        reciterId: String,
        startFromAyah: Int?
    ) {
        try {
            // Get ayah text from database for metadata
            val ayahEntities = quranRepository.getAyahsBySurah(surahNumber).first()

            if (ayahEntities.isEmpty()) {
                Timber.e("No ayah data found in database for surah $surahNumber")
                return
            }

            // Get ayah repeat count from settings
            val ayahRepeatCount = settingsRepository.settings.first().ayahRepeatCount

            // Build playlist from local files
            val mediaItems = ayahEntities.flatMap { ayah ->
                val ayahFile = java.io.File(localPath, "${ayah.ayahNumber}.mp3")
                if (!ayahFile.exists()) {
                    Timber.w("Ayah file not found: ${ayahFile.absolutePath}")
                    return@flatMap emptyList()
                }

                val fileUri = Uri.fromFile(ayahFile)

                // Create repeated entries for this ayah
                (1..ayahRepeatCount).map { repeatIndex ->
                    val repeatSuffix = if (ayahRepeatCount > 1) " (${repeatIndex}/${ayahRepeatCount})" else ""
                    MediaItem.Builder()
                        .setMediaId("${reciterId}_${surahNumber}_${ayah.ayahNumber}_$repeatIndex")
                        .setUri(fileUri)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(sanitizeForBluetooth(ayah.textArabic))  // Sanitized for Bluetooth
                                .setArtist(reciterName)
                                .setAlbumTitle("$surahNameEnglish - $surahNameArabic")
                                .setDisplayTitle(ayah.textArabic)  // Full text for Android Auto
                                .setSubtitle("$surahNameEnglish (${ayah.ayahNumber}/${ayahEntities.size})$repeatSuffix")
                                .setArtworkUri(appIconUri)
                                .setExtras(Bundle().apply {
                                    putString("surahNameArabic", surahNameArabic)
                                    putString("surahNameEnglish", surahNameEnglish)
                                    putInt("ayahNumber", ayah.ayahNumber)
                                    putInt("totalAyahs", ayahEntities.size)
                                    putString("ayahText", ayah.textArabic)
                                    putInt("repeatIndex", repeatIndex)
                                    putInt("repeatCount", ayahRepeatCount)
                                })
                                .build()
                        )
                        .build()
                }
            }

            if (mediaItems.isEmpty()) {
                Timber.e("No valid media items created from downloaded ayah files")
                return
            }

            Timber.d("Created offline playlist with ${mediaItems.size} items for $surahNameEnglish")

            // All MediaController operations must run on main thread
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                mediaController?.apply {
                    repeatMode = Player.REPEAT_MODE_OFF
                    setMediaItems(mediaItems)
                    prepare()

                    // Seek to specific ayah if requested
                    startFromAyah?.let { ayahNum ->
                        val ayahIndex = (ayahNum - 1) * ayahRepeatCount
                        if (ayahIndex >= 0 && ayahIndex < mediaItems.size) {
                            seekToDefaultPosition(ayahIndex)
                            Timber.d("Starting offline playback from ayah index: $ayahIndex (ayah #$ayahNum)")
                        }
                    }

                    play()

                    _playbackState.value = _playbackState.value.copy(
                        currentReciter = reciterId,
                        currentReciterName = reciterName,
                        currentSurah = surahNumber,
                        currentSurahNameArabic = surahNameArabic,
                        currentSurahNameEnglish = surahNameEnglish,
                        currentAyah = startFromAyah ?: 1,
                        totalAyahs = ayahEntities.size
                    )

                    // Set initial ayah text
                    if (mediaItems.isNotEmpty()) {
                        val initialIndex = ((startFromAyah ?: 1) - 1) * ayahRepeatCount
                        updateCurrentMediaItem(mediaItems[initialIndex.coerceIn(0, mediaItems.size - 1)])
                    }

                    // Save last playback state
                    settingsRepository.updateLastPlaybackState(reciterId, surahNumber, currentPosition)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error playing from downloaded ayahs")
        }
    }

    fun play() {
        coroutineScope.launch {
            // Check if reciter has changed in settings
            val selectedReciterId = settingsRepository.getSelectedReciterId()
            val currentReciterId = _playbackState.value.currentReciter

            if (selectedReciterId.isNotEmpty() && selectedReciterId != currentReciterId && _playbackState.value.currentSurah != null) {
                // Reciter changed while paused, reload with new reciter at current ayah
                Timber.d("Reciter changed from $currentReciterId to $selectedReciterId, reloading at current ayah")
                reloadCurrentWithReciter(selectedReciterId, _playbackState.value.currentAyah ?: 1)
            } else {
                // MediaController must be accessed from main thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    mediaController?.play()
                }
            }
        }
    }

    fun pause() {
        coroutineScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            mediaController?.pause()
        }
    }

    fun stop() {
        coroutineScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            // Reset custom recitation state
            customRecitationSettings = null
            customRecitationCurrentGroupIteration = 0

            // Save current playback info before stopping so user can restart
            val currentState = _playbackState.value
            if (currentState.currentReciter != null && currentState.currentSurah != null) {
                settingsRepository.updateLastPlaybackState(
                    reciterId = currentState.currentReciter,
                    surahNumber = currentState.currentSurah,
                    positionMs = 0L  // Reset position to beginning
                )
                Timber.d("Saved last playback before stop: reciter=${currentState.currentReciter}, surah=${currentState.currentSurah}")
            }

            mediaController?.stop()
            _playbackState.value = PlaybackState()  // Reset playback state
        }
    }

    fun seekTo(positionMs: Long) {
        coroutineScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            mediaController?.seekTo(positionMs)
        }
    }

    fun seekToAyah(ayahNumber: Int) {
        coroutineScope.launch {
            try {
                // Account for ayah repeat count
                val repeatCount = settingsRepository.settings.first().ayahRepeatCount

                // MediaController must be accessed from main thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    mediaController?.let { controller ->
                        // Each ayah is repeated `repeatCount` times in the playlist
                        // Ayah numbers are 1-based, but media item indices are 0-based
                        val ayahIndex = (ayahNumber - 1) * repeatCount

                        if (ayahIndex >= 0 && ayahIndex < controller.mediaItemCount) {
                            controller.seekToDefaultPosition(ayahIndex)
                            Timber.d("Seeking to ayah $ayahNumber (media item index: $ayahIndex, repeatCount: $repeatCount)")

                            _playbackState.value = _playbackState.value.copy(currentAyah = ayahNumber)
                        } else {
                            Timber.e("Invalid ayah number: $ayahNumber (valid range: 1-${controller.mediaItemCount / repeatCount})")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error seeking to ayah $ayahNumber")
            }
        }
    }

    fun nextAyah() {
        coroutineScope.launch {
            // Check if reciter has changed in settings
            val selectedReciterId = settingsRepository.getSelectedReciterId()
            val currentReciterId = _playbackState.value.currentReciter

            if (selectedReciterId.isNotEmpty() && selectedReciterId != currentReciterId) {
                // Reciter changed, reload with new reciter starting from next ayah
                val nextAyah = (_playbackState.value.currentAyah ?: 0) + 1
                Timber.d("Reciter changed from $currentReciterId to $selectedReciterId, reloading at ayah $nextAyah")
                reloadCurrentWithReciter(selectedReciterId, nextAyah)
            } else {
                // MediaController must be accessed from main thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    mediaController?.let { controller ->
                        if (controller.hasNextMediaItem()) {
                            controller.seekToNextMediaItem()
                        }
                    }
                }
            }
        }
    }

    fun previousAyah() {
        coroutineScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            mediaController?.let { controller ->
                if (controller.hasPreviousMediaItem()) {
                    controller.seekToPreviousMediaItem()
                }
            }
        }
    }

    fun nudgeForward() {
        coroutineScope.launch {
            try {
                val incrementMs = settingsRepository.settings.first().smallSeekIncrementMs
                // MediaController must be accessed from main thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    mediaController?.let { controller ->
                        val currentPos = controller.currentPosition
                        val duration = controller.duration
                        if (duration > 0) {
                            val newPosition = (currentPos + incrementMs).coerceAtMost(duration)
                            controller.seekTo(newPosition)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error nudging forward")
            }
        }
    }

    fun nudgeBackward() {
        coroutineScope.launch {
            try {
                val incrementMs = settingsRepository.settings.first().smallSeekIncrementMs
                // MediaController must be accessed from main thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    mediaController?.let { controller ->
                        val currentPos = controller.currentPosition
                        val newPosition = (currentPos - incrementMs).coerceAtLeast(0)
                        controller.seekTo(newPosition)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error nudging backward")
            }
        }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        // MediaController must be accessed from main thread
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            mediaController?.setPlaybackSpeed(speed)
        }
        settingsRepository.setPlaybackSpeed(speed)
        _playbackState.value = _playbackState.value.copy(playbackSpeed = speed)
    }

    suspend fun setAyahRepeatCount(count: Int) {
        try {
            val oldCount = settingsRepository.settings.first().ayahRepeatCount
            settingsRepository.setAyahRepeatCount(count)
            Timber.d("Ayah repeat count changed from $oldCount to $count")

            // If there's active playback, reload the playlist with new repeat count
            val state = _playbackState.value
            val reciterId = state.currentReciter
            val surahNumber = state.currentSurah
            val currentAyah = state.currentAyah

            if (reciterId != null && surahNumber != null) {
                // Get surah and reciter info to reload
                val surah = quranRepository.getSurahByNumber(surahNumber)
                val reciter = quranRepository.getReciterById(reciterId)
                val audioVariants = quranRepository.getAudioVariants(reciterId, surahNumber).first()

                if (surah != null && reciter != null && audioVariants.isNotEmpty()) {
                    val audioVariant = audioVariants.first()
                    Timber.d("Reloading playlist with new repeat count: $count, starting from ayah: $currentAyah")

                    // Reload the surah with the new repeat count, starting from current ayah
                    playAudio(
                        reciterId = reciterId,
                        surahNumber = surahNumber,
                        audioUrl = audioVariant.url,
                        surahNameArabic = surah.nameArabic,
                        surahNameEnglish = surah.nameEnglish,
                        reciterName = reciter.name,
                        startFromAyah = currentAyah
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error setting ayah repeat count")
        }
    }

    /**
     * Reload the current surah with a different reciter, starting from the specified ayah.
     * Used when the user changes reciter during playback.
     */
    private suspend fun reloadCurrentWithReciter(newReciterId: String, startFromAyah: Int) {
        try {
            val state = _playbackState.value
            val surahNumber = state.currentSurah ?: return

            val surah = quranRepository.getSurahByNumber(surahNumber) ?: return
            val reciter = quranRepository.getReciterById(newReciterId) ?: return
            val audioVariants = quranRepository.getAudioVariants(newReciterId, surahNumber).first()

            if (audioVariants.isEmpty()) {
                Timber.e("No audio variants found for reciter $newReciterId surah $surahNumber")
                return
            }

            val audioVariant = audioVariants.first()
            Timber.d("Reloading surah $surahNumber with reciter ${reciter.name} starting from ayah $startFromAyah")

            playAudio(
                reciterId = newReciterId,
                surahNumber = surahNumber,
                audioUrl = audioVariant.url,
                surahNameArabic = surah.nameArabic,
                surahNameEnglish = surah.nameEnglish,
                reciterName = reciter.name,
                startFromAyah = startFromAyah
            )
        } catch (e: Exception) {
            Timber.e(e, "Error reloading with new reciter")
        }
    }

    /**
     * Start custom recitation with user-defined settings.
     * Limited to a single surah only.
     */
    suspend fun startCustomRecitation(reciterId: String, settings: com.quranmedia.player.domain.model.CustomRecitationSettings) {
        if (!settings.isValid()) {
            Timber.e("Invalid custom recitation settings")
            return
        }

        try {
            // Save custom recitation settings
            customRecitationSettings = settings
            customRecitationCurrentGroupIteration = 1

            if (reciterId.isEmpty()) {
                Timber.e("No reciter selected")
                return
            }

            val surah = quranRepository.getSurahByNumber(settings.startSurahNumber) ?: return
            val reciter = quranRepository.getReciterById(reciterId) ?: return
            val audioVariants = quranRepository.getAudioVariants(reciterId, settings.startSurahNumber).first()

            if (audioVariants.isEmpty()) {
                Timber.e("No audio for reciter")
                return
            }

            // Fetch surah data with ayah audio URLs from API
            val surahData = quranRepository.getSurahWithAudio(settings.startSurahNumber, reciterId)
            if (surahData == null) {
                Timber.e("Failed to fetch surah audio data")
                return
            }

            // Load ayah indices for position tracking
            ayahIndices = quranRepository.getAyahIndices(reciterId, settings.startSurahNumber).first()

            // Filter ayahs based on selected range
            val selectedAyahs = surahData.ayahs.filter { ayahData ->
                ayahData.numberInSurah >= settings.startAyahNumber &&
                ayahData.numberInSurah <= settings.endAyahNumber
            }

            if (selectedAyahs.isEmpty()) {
                Timber.e("No ayahs found in selected range")
                return
            }

            // Build custom playlist with nested repeats
            val mediaItems = mutableListOf<MediaItem>()

            val ayahRepeat = if (settings.ayahRepeatCount == -1) 1 else settings.ayahRepeatCount
            val groupRepeat = if (settings.groupRepeatCount == -1) 1 else settings.groupRepeatCount

            repeat(groupRepeat) { groupIdx ->
                selectedAyahs.forEach { ayahData ->
                    repeat(ayahRepeat) { ayahIdx ->
                        mediaItems.add(
                            MediaItem.Builder()
                                .setMediaId("${reciterId}_${settings.startSurahNumber}_${ayahData.numberInSurah}_${groupIdx}_${ayahIdx}")
                                .setUri(Uri.parse(ayahData.audio))
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle("${ayahData.text} - ${ayahData.numberInSurah}")
                                        .setArtist(surah.nameArabic)
                                        .setArtworkUri(appIconUri)
                                        .setExtras(Bundle().apply {
                                            putString("reciterId", reciterId)
                                            putInt("surahNumber", settings.startSurahNumber)
                                            putInt("ayahNumber", ayahData.numberInSurah)
                                            putString("ayahText", sanitizeForBluetooth(ayahData.text))
                                        })
                                        .build()
                                )
                                .build()
                        )
                    }
                }
            }

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                mediaController?.let { controller ->
                    controller.setMediaItems(mediaItems)
                    controller.prepare()
                    controller.setPlaybackSpeed(settings.speed)
                    controller.playWhenReady = true

                    // Set repeat mode for unlimited
                    controller.repeatMode = if (settings.ayahRepeatCount == -1 || settings.groupRepeatCount == -1) {
                        Player.REPEAT_MODE_ALL
                    } else {
                        Player.REPEAT_MODE_OFF
                    }

                    _playbackState.value = _playbackState.value.copy(
                        currentReciter = reciterId,
                        currentReciterName = reciter.name,
                        currentSurah = settings.startSurahNumber,
                        currentSurahNameArabic = surah.nameArabic,
                        currentSurahNameEnglish = surah.nameEnglish,
                        currentAyah = settings.startAyahNumber,
                        playbackSpeed = settings.speed,
                        isPlaying = true
                    )

                    Timber.d("Started custom recitation: ${reciter.name} - ${surah.nameArabic}, ayahs ${settings.startAyahNumber}-${settings.endAyahNumber}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in custom recitation")
        }
    }

    fun release() {
        positionUpdateJob?.cancel()
        MediaController.releaseFuture(mediaControllerFuture)
    }
}
