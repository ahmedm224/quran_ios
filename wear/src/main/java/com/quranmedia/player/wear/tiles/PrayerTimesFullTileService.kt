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
import com.quranmedia.player.wear.domain.model.PrayerTimes
import com.quranmedia.player.wear.domain.model.PrayerType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import timber.log.Timber
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Wear OS Tile showing all prayer times with the next prayer highlighted.
 */
@AndroidEntryPoint
class PrayerTimesFullTileService : TileService() {

    @Inject
    lateinit var repository: WearPrayerTimesRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val RESOURCES_VERSION = "1"

        // Colors - Islamic inspired palette
        private const val COLOR_BACKGROUND = 0xFF000000.toInt()
        private const val COLOR_GOLD = 0xFFD4AF37.toInt()      // Islamic gold accent
        private const val COLOR_GREEN = 0xFF4CAF50.toInt()     // Next prayer highlight
        private const val COLOR_TEXT = 0xFFE0E0E0.toInt()      // Light gray for readability
        private const val COLOR_TEXT_DIM = 0xFF707070.toInt()  // Dimmed for passed prayers
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return serviceScope.future {
            try {
                val prayerTimes = repository.getPrayerTimesForToday()
                val nextPrayer = repository.getNextPrayer()

                Timber.d("Building tile: nextPrayer=${nextPrayer?.first}")

                TileBuilders.Tile.Builder()
                    .setResourcesVersion(RESOURCES_VERSION)
                    .setFreshnessIntervalMillis(60000) // Refresh every minute
                    .setTileTimeline(
                        TimelineBuilders.Timeline.Builder()
                            .addTimelineEntry(
                                TimelineEntry.Builder()
                                    .setLayout(
                                        LayoutElementBuilders.Layout.Builder()
                                            .setRoot(buildLayout(prayerTimes, nextPrayer?.first))
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            } catch (e: Exception) {
                Timber.e(e, "Error building prayer times tile")
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

    private fun buildLayout(prayerTimes: PrayerTimes, nextPrayer: PrayerType?): LayoutElementBuilders.LayoutElement {
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm")

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
            .addContent(
                Column.Builder()
                    .setWidth(wrap())
                    .setHeight(wrap())
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .setModifiers(
                        Modifiers.Builder()
                            .setPadding(
                                Padding.Builder()
                                    .setTop(dp(18f))
                                    .build()
                            )
                            .build()
                    )
                    // Decorative header
                    .addContent(
                        Text.Builder()
                            .setText("☪ مواقيت الصلاة")
                            .setFontStyle(
                                FontStyle.Builder()
                                    .setSize(sp(12f))
                                    .setColor(argb(COLOR_GOLD))
                                    .build()
                            )
                            .build()
                    )
                    .addContent(Spacer.Builder().setHeight(dp(6f)).build())
                    // Prayer times - clean list
                    .addContent(buildPrayerRow("الفجر", prayerTimes.fajr, timeFormatter, nextPrayer == PrayerType.FAJR))
                    .addContent(buildPrayerRow("الظهر", prayerTimes.dhuhr, timeFormatter, nextPrayer == PrayerType.DHUHR))
                    .addContent(buildPrayerRow("العصر", prayerTimes.asr, timeFormatter, nextPrayer == PrayerType.ASR))
                    .addContent(buildPrayerRow("المغرب", prayerTimes.maghrib, timeFormatter, nextPrayer == PrayerType.MAGHRIB))
                    .addContent(buildPrayerRow("العشاء", prayerTimes.isha, timeFormatter, nextPrayer == PrayerType.ISHA))
                    .build()
            )
            .build()
    }

    private fun buildPrayerRow(
        nameAr: String,
        time: LocalTime,
        formatter: DateTimeFormatter,
        isNext: Boolean
    ): LayoutElementBuilders.LayoutElement {
        // Simple format: "الفجر   ٤:٥٢" with dot separator
        val timeStr = time.format(formatter)
        val color = if (isNext) COLOR_GREEN else COLOR_TEXT
        val prefix = if (isNext) "▸ " else "   "

        return Text.Builder()
            .setText("$prefix$nameAr  ·  $timeStr")
            .setFontStyle(
                FontStyle.Builder()
                    .setSize(sp(14f))
                    .setColor(argb(color))
                    .build()
            )
            .setModifiers(
                Modifiers.Builder()
                    .setPadding(
                        Padding.Builder()
                            .setTop(dp(2f))
                            .setBottom(dp(2f))
                            .build()
                    )
                    .build()
            )
            .build()
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
                                                    .setText("☪ مواقيت الصلاة")
                                                    .setFontStyle(
                                                        FontStyle.Builder()
                                                            .setSize(sp(14f))
                                                            .setColor(argb(COLOR_GOLD))
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
