package com.quranmedia.player.media.auto

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.quranmedia.player.domain.repository.QuranRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class QuranMediaBrowserService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var quranRepository: QuranRepository

    @Inject
    lateinit var bookmarkRepository: com.quranmedia.player.data.repository.BookmarkRepository

    @Inject
    lateinit var coroutineScope: CoroutineScope

    private var lastMetadataMediaId: String? = null

    companion object {
        const val MEDIA_ROOT_ID = "root"
        const val MEDIA_RECITERS_ID = "reciters"
        const val MEDIA_SURAHS_ID = "surahs"
        const val MEDIA_RECITER_PREFIX = "reciter_"
        const val MEDIA_SURAH_PREFIX = "surah_"
        const val MEDIA_BOOKMARKS_ID = "bookmarks"
    }

    // App icon URI for Android Auto browse items - use high-res Alfurqan logo
    private val appIconUri: Uri by lazy {
        // Use the dedicated high-res logo for crisp rendering in Android Auto
        Uri.parse("android.resource://${packageName}/${com.quranmedia.player.R.drawable.quran_logo}")
    }

    /**
     * Sanitizes Arabic text for Bluetooth display by removing diacritics (harakat/tashkeel).
     * Bluetooth devices often can't render Arabic combining characters, showing squares instead.
     */
    private fun sanitizeForBluetooth(text: String): String {
        return text
            .replace(Regex("[\u064B-\u0652\u0670\u0640\u06D6-\u06ED]"), "")
            .trim()
    }

    override fun onCreate() {
        super.onCreate()

        // Create a simple MediaSessionCompat that will proxy to the main service
        val sessionCompat = android.support.v4.media.session.MediaSessionCompat(this, "QuranMediaBrowserService")

        // Set up a connection to the main Media3 service
        val mediaController = androidx.media3.session.MediaController.Builder(
            this,
            androidx.media3.session.SessionToken(
                this,
                android.content.ComponentName(this, com.quranmedia.player.media.service.QuranMediaService::class.java)
            )
        ).buildAsync()

        mediaController.addListener(
            {
                // Once connected, proxy playback commands to the Media3 controller
                val controller = mediaController.get()

                // Listen to player state changes and update compat session
                controller.addListener(object : androidx.media3.common.Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        updatePlaybackState(sessionCompat, controller)
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updatePlaybackState(sessionCompat, controller)
                    }

                    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                        updateMetadata(sessionCompat, controller)
                    }
                })

                sessionCompat.setCallback(object : android.support.v4.media.session.MediaSessionCompat.Callback() {
                    override fun onPlayFromMediaId(mediaId: String?, extras: android.os.Bundle?) {
                        Timber.d("onPlayFromMediaId: $mediaId")
                        // The main service will handle this via onAddMediaItems
                        controller.setMediaItem(
                            androidx.media3.common.MediaItem.Builder()
                                .setMediaId(mediaId ?: "")
                                .build()
                        )
                        controller.prepare()
                        controller.play()
                    }

                    override fun onPlay() {
                        controller.play()
                    }

                    override fun onPause() {
                        controller.pause()
                    }

                    override fun onStop() {
                        controller.stop()
                    }

                    override fun onSkipToNext() {
                        controller.seekToNext()
                    }

                    override fun onSkipToPrevious() {
                        controller.seekToPrevious()
                    }
                })

                sessionCompat.isActive = true
                updatePlaybackState(sessionCompat, controller)
                updateMetadata(sessionCompat, controller)
                Timber.d("QuranMediaBrowserService connected to main MediaSession")
            },
            com.google.common.util.concurrent.MoreExecutors.directExecutor()
        )

        sessionToken = sessionCompat.sessionToken
        Timber.d("QuranMediaBrowserService created")
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        Timber.d("onGetRoot: $clientPackageName")
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        Timber.d("onLoadChildren: $parentId")

        result.detach()

        coroutineScope.launch {
            try {
                val mediaItems = when {
                    parentId == MEDIA_ROOT_ID -> getRootItems()
                    parentId == MEDIA_RECITERS_ID -> getRecitersItems()
                    parentId == MEDIA_SURAHS_ID -> getSurahsItems()
                    parentId == MEDIA_BOOKMARKS_ID -> getBookmarksItems()
                    parentId.startsWith(MEDIA_RECITER_PREFIX) -> {
                        val reciterId = parentId.removePrefix(MEDIA_RECITER_PREFIX)
                        getReciterSurahsItems(reciterId)
                    }
                    else -> emptyList()
                }
                result.sendResult(mediaItems.toMutableList())
            } catch (e: Exception) {
                Timber.e(e, "Error loading children for $parentId")
                result.sendResult(mutableListOf())
            }
        }
    }

    private fun getRootItems(): List<MediaBrowserCompat.MediaItem> {
        return listOf(
            MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(MEDIA_RECITERS_ID)
                    .setTitle("Browse by Reciter")
                    .setSubtitle("Select a reciter")
                    .setIconUri(appIconUri)
                    .build(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            ),
            MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(MEDIA_SURAHS_ID)
                    .setTitle("Browse by Surah")
                    .setSubtitle("Select a surah")
                    .setIconUri(appIconUri)
                    .build(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            ),
            MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(MEDIA_BOOKMARKS_ID)
                    .setTitle("Bookmarks")
                    .setSubtitle("Your saved positions")
                    .setIconUri(appIconUri)
                    .build(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            )
        )
    }

    private suspend fun getRecitersItems(): List<MediaBrowserCompat.MediaItem> {
        val reciters = quranRepository.getAllReciters().first()
        return reciters.map { reciter ->
            MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId("$MEDIA_RECITER_PREFIX${reciter.id}")
                    .setTitle(reciter.name)
                    .setSubtitle(sanitizeForBluetooth(reciter.nameArabic ?: ""))
                    .setIconUri(appIconUri)
                    .build(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            )
        }
    }

    private suspend fun getSurahsItems(): List<MediaBrowserCompat.MediaItem> {
        val surahs = quranRepository.getAllSurahs().first()
        return surahs.map { surah ->
            MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId("$MEDIA_SURAH_PREFIX${surah.number}")
                    .setTitle("${surah.number}. ${surah.nameEnglish}")
                    .setSubtitle("${sanitizeForBluetooth(surah.nameArabic)} - ${surah.ayahCount} ayahs")
                    .setIconUri(appIconUri)
                    .build(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        }
    }

    private suspend fun getReciterSurahsItems(reciterId: String): List<MediaBrowserCompat.MediaItem> {
        val surahs = quranRepository.getAllSurahs().first()
        val reciter = quranRepository.getReciterById(reciterId)

        Timber.d("Loading surahs for reciter: $reciterId (${reciter?.name})")

        return surahs.map { surah ->
            // Get audio variant URL from database or build fallback URL
            val audioVariant = quranRepository.getAudioVariant(reciterId, surah.number)
            val audioUrl = audioVariant?.url ?: buildAudioUrl(reciterId, surah.number)

            Timber.d("Surah ${surah.number}: $audioUrl")

            MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId("$MEDIA_RECITER_PREFIX$reciterId:$MEDIA_SURAH_PREFIX${surah.number}")
                    .setTitle(surah.nameEnglish)
                    .setSubtitle("${sanitizeForBluetooth(surah.nameArabic)} - ${reciter?.name ?: ""}")
                    .setMediaUri(Uri.parse(audioUrl))  // CRITICAL: Android Auto needs this
                    .setIconUri(appIconUri)
                    .build(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        }
    }

    private suspend fun getBookmarksItems(): List<MediaBrowserCompat.MediaItem> {
        val bookmarks = bookmarkRepository.getAllBookmarks().first()

        Timber.d("Loading ${bookmarks.size} bookmarks for Android Auto")

        return bookmarks.mapNotNull { bookmark ->
            try {
                val surah = quranRepository.getSurahByNumber(bookmark.surahNumber)
                val reciter = quranRepository.getReciterById(bookmark.reciterId)
                val audioVariant = quranRepository.getAudioVariant(bookmark.reciterId, bookmark.surahNumber)
                val audioUrl = audioVariant?.url ?: buildAudioUrl(bookmark.reciterId, bookmark.surahNumber)

                if (surah != null && reciter != null) {
                    val label = bookmark.label ?: "Ayah ${bookmark.ayahNumber}"
                    val title = "${surah.nameEnglish} - $label"
                    val subtitle = "${sanitizeForBluetooth(surah.nameArabic)} - ${reciter.name}"

                    MediaBrowserCompat.MediaItem(
                        MediaDescriptionCompat.Builder()
                            .setMediaId("$MEDIA_RECITER_PREFIX${bookmark.reciterId}:$MEDIA_SURAH_PREFIX${bookmark.surahNumber}")
                            .setTitle(title)
                            .setSubtitle(subtitle)
                            .setMediaUri(Uri.parse(audioUrl))
                            .setIconUri(appIconUri)
                            .build(),
                        MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                    )
                } else {
                    Timber.w("Could not load bookmark: surah or reciter not found")
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading bookmark")
                null
            }
        }
    }

    private fun buildAudioUrl(reciterId: String, surahNumber: Int): String {
        // Default format: https://cdn.islamic.network/quran/audio-surah/128/{reciter}/{surah}.mp3
        val url = "https://cdn.islamic.network/quran/audio-surah/128/$reciterId/$surahNumber.mp3"
        Timber.d("Built audio URL: $url")
        return url
    }

    private fun updatePlaybackState(
        sessionCompat: android.support.v4.media.session.MediaSessionCompat,
        controller: androidx.media3.session.MediaController
    ) {
        val stateBuilder = android.support.v4.media.session.PlaybackStateCompat.Builder()

        // Map Media3 playback state to compat state
        val state = when (controller.playbackState) {
            androidx.media3.common.Player.STATE_IDLE -> android.support.v4.media.session.PlaybackStateCompat.STATE_NONE
            androidx.media3.common.Player.STATE_BUFFERING -> android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING
            androidx.media3.common.Player.STATE_READY -> {
                if (controller.isPlaying) {
                    android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
                } else {
                    android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED
                }
            }
            androidx.media3.common.Player.STATE_ENDED -> android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED
            else -> android.support.v4.media.session.PlaybackStateCompat.STATE_NONE
        }

        stateBuilder.setState(state, controller.currentPosition, 1.0f)
        stateBuilder.setActions(
            android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY or
            android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE or
            android.support.v4.media.session.PlaybackStateCompat.ACTION_STOP or
            android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            android.support.v4.media.session.PlaybackStateCompat.ACTION_SEEK_TO or
            android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
        )

        sessionCompat.setPlaybackState(stateBuilder.build())
        Timber.d("Updated compat playback state: $state, isPlaying: ${controller.isPlaying}")
    }

    private fun updateMetadata(
        sessionCompat: android.support.v4.media.session.MediaSessionCompat,
        controller: androidx.media3.session.MediaController
    ) {
        val currentMediaItem = controller.currentMediaItem
        if (currentMediaItem != null) {
            // Skip if this is the same media item we just updated
            if (currentMediaItem.mediaId == lastMetadataMediaId) {
                Timber.d("Skipping duplicate metadata update for mediaId: ${currentMediaItem.mediaId}")
                return
            }

            val metadata = currentMediaItem.mediaMetadata
            val title = metadata.title?.toString() ?: ""  // Already sanitized in PlaybackController
            val displayTitle = metadata.displayTitle?.toString() ?: title  // Full Arabic text for Android Auto
            val artist = metadata.artist?.toString() ?: ""
            val subtitle = metadata.subtitle?.toString() ?: ""

            // For Bluetooth: Use full sanitized text in TITLE - some head units will scroll long text
            // For Android Auto: Use DISPLAY_TITLE which has proper marquee scrolling
            val metadataBuilder = android.support.v4.media.MediaMetadataCompat.Builder()
                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID, currentMediaItem.mediaId)
                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE, title)  // Full sanitized text for Bluetooth scrolling
                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, displayTitle)  // Full text for Android Auto
                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, subtitle)
                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM, metadata.albumTitle?.toString() ?: "")
                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, displayTitle)
                .putLong(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION, controller.duration)
                // Additional metadata for AVRCP 1.6+ devices that support extended text
                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_COMPILATION, displayTitle)  // Some devices read this for scrolling
                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_WRITER, artist)  // Redundant artist info

            // Add artwork - use provided URI or fallback to high-res Alfurqan logo
            val artworkUri = metadata.artworkUri
                ?: Uri.parse("android.resource://${packageName}/${com.quranmedia.player.R.drawable.quran_logo}")
            metadataBuilder.putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ART_URI, artworkUri.toString())
            metadataBuilder.putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artworkUri.toString())
            metadataBuilder.putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, artworkUri.toString())

            sessionCompat.setMetadata(metadataBuilder.build())
            lastMetadataMediaId = currentMediaItem.mediaId
            Timber.d("Updated compat metadata - title: ${displayTitle.take(50)}..., artist: $artist, length: ${displayTitle.length} chars")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("QuranMediaBrowserService destroyed")
    }
}
