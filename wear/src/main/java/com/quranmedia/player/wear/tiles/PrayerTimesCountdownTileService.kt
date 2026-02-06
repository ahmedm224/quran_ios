package com.quranmedia.player.wear.tiles

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.quranmedia.player.wear.data.repository.WearPrayerTimesRepository
import com.quranmedia.player.wear.domain.model.PrayerType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import timber.log.Timber
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Wear OS Tile showing countdown to next prayer with prayer name and time.
 */
@AndroidEntryPoint
class PrayerTimesCountdownTileService : TileService() {

    @Inject
    lateinit var repository: WearPrayerTimesRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val RESOURCES_VERSION = "1"

        // Colors
        private const val COLOR_BACKGROUND = 0xFF000000.toInt()
        private const val COLOR_PRIMARY = 0xFF4CAF50.toInt()
        private const val COLOR_TEXT = 0xFFFFFFFF.toInt()
        private const val COLOR_TEXT_DIM = 0xFFB0B0B0.toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return serviceScope.future {
            try {
                val nextPrayer = repository.getNextPrayer()

                TileBuilders.Tile.Builder()
                    .setResourcesVersion(RESOURCES_VERSION)
                    .setFreshnessIntervalMillis(30000) // Refresh every 30 seconds for countdown
                    .setTileTimeline(
                        TimelineBuilders.Timeline.Builder()
                            .addTimelineEntry(
                                TimelineEntry.Builder()
                                    .setLayout(
                                        LayoutElementBuilders.Layout.Builder()
                                            .setRoot(buildLayout(nextPrayer))
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            } catch (e: Exception) {
                Timber.e(e, "Error building countdown tile")
                buildErrorTile()
            }
        }
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build()
        )
    }

    private fun buildLayout(nextPrayer: Pair<PrayerType, LocalTime>?): LayoutElementBuilders.LayoutElement {
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

        if (nextPrayer == null) {
            // Fallback - should not happen since getNextPrayer now always returns a value
            return Text.Builder()
                .setText("--")
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(sp(14f))
                        .setColor(argb(COLOR_TEXT_DIM))
                        .build()
                )
                .build()
        }

        val (prayerType, prayerTime) = nextPrayer
        val now = LocalDateTime.now()

        // Calculate prayer datetime - if prayer time is before current time, it's tomorrow
        val prayerDateTime = if (prayerTime.isBefore(now.toLocalTime())) {
            LocalDateTime.of(now.toLocalDate().plusDays(1), prayerTime)
        } else {
            LocalDateTime.of(now.toLocalDate(), prayerTime)
        }

        val duration = Duration.between(now, prayerDateTime)

        val countdown = formatCountdown(duration)
        val prayerName = prayerType.nameArabic
        val prayerTimeStr = prayerTime.format(timeFormatter)

        return Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(
                Modifiers.Builder()
                    .setBackground(
                        Background.Builder()
                            .setColor(argb(COLOR_BACKGROUND))
                            .build()
                    )
                    .setClickable(
                        Clickable.Builder()
                            .setOnClick(
                                ActionBuilders.LaunchAction.Builder()
                                    .setAndroidActivity(
                                        ActionBuilders.AndroidActivity.Builder()
                                            .setPackageName(packageName)
                                            .setClassName("com.quranmedia.player.wear.presentation.MainActivity")
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            // Center content
            .addContent(
                Column.Builder()
                    .setWidth(wrap())
                    .setHeight(wrap())
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .setModifiers(
                        Modifiers.Builder()
                            .setPadding(
                                Padding.Builder()
                                    .setAll(dp(16f))
                                    .build()
                            )
                            .build()
                    )
                    // "Next prayer" label
                    .addContent(
                        Text.Builder()
                            .setText("الصلاة القادمة")
                            .setFontStyle(
                                FontStyle.Builder()
                                    .setSize(sp(12f))
                                    .setColor(argb(COLOR_TEXT_DIM))
                                    .build()
                            )
                            .build()
                    )
                    .addContent(Spacer.Builder().setHeight(dp(4f)).build())
                    // Prayer name
                    .addContent(
                        Text.Builder()
                            .setText(prayerName)
                            .setFontStyle(
                                FontStyle.Builder()
                                    .setSize(sp(24f))
                                    .setColor(argb(COLOR_PRIMARY))
                                    .build()
                            )
                            .build()
                    )
                    .addContent(Spacer.Builder().setHeight(dp(8f)).build())
                    // Countdown
                    .addContent(
                        Text.Builder()
                            .setText(countdown)
                            .setFontStyle(
                                FontStyle.Builder()
                                    .setSize(sp(36f))
                                    .setColor(argb(COLOR_TEXT))
                                    .build()
                            )
                            .build()
                    )
                    .addContent(Spacer.Builder().setHeight(dp(4f)).build())
                    // Prayer time
                    .addContent(
                        Text.Builder()
                            .setText(prayerTimeStr)
                            .setFontStyle(
                                FontStyle.Builder()
                                    .setSize(sp(14f))
                                    .setColor(argb(COLOR_TEXT_DIM))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun formatCountdown(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutesPart()

        return String.format("%02dh:%02dm", hours, minutes)
    }

    private fun buildErrorTile(): TileBuilders.Tile {
        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(300000) // Retry in 5 minutes
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(
                                        Box.Builder()
                                            .setWidth(expand())
                                            .setHeight(expand())
                                            .setModifiers(
                                                Modifiers.Builder()
                                                    .setBackground(
                                                        Background.Builder()
                                                            .setColor(argb(COLOR_BACKGROUND))
                                                            .build()
                                                    )
                                                    .build()
                                            )
                                            .addContent(
                                                Text.Builder()
                                                    .setText("الصلاة القادمة")
                                                    .setFontStyle(
                                                        FontStyle.Builder()
                                                            .setSize(sp(14f))
                                                            .setColor(argb(COLOR_PRIMARY))
                                                            .build()
                                                    )
                                                    .build()
                                            )
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }
}
