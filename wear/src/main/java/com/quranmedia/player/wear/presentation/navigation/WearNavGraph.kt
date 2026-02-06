package com.quranmedia.player.wear.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.quranmedia.player.wear.presentation.screens.athkar.AthkarListScreen
import com.quranmedia.player.wear.presentation.screens.home.WearHomeScreen
import com.quranmedia.player.wear.presentation.screens.prayertimes.PrayerSettingsScreen
import com.quranmedia.player.wear.presentation.screens.prayertimes.PrayerTimesScreen
import com.quranmedia.player.wear.presentation.screens.quran.QuranIndexScreen
import com.quranmedia.player.wear.presentation.screens.quran.QuranReaderScreen

/**
 * Navigation routes for the Wear app.
 */
object WearRoutes {
    const val HOME = "home"
    const val ATHKAR_LIST = "athkar/{categoryId}"
    const val PRAYER_TIMES = "prayer_times"
    const val PRAYER_SETTINGS = "prayer_settings"
    const val QURAN_INDEX = "quran_index"
    const val QURAN_READER = "quran_reader/{surahNumber}"

    fun athkarList(categoryId: String) = "athkar/$categoryId"
    fun quranReader(surahNumber: Int) = "quran_reader/$surahNumber"
}

/**
 * Main navigation graph for the Wear app.
 */
@Composable
fun WearNavGraph() {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = WearRoutes.HOME
    ) {
        composable(WearRoutes.HOME) {
            WearHomeScreen(
                onCategoryClick = { categoryId ->
                    navController.navigate(WearRoutes.athkarList(categoryId))
                },
                onPrayerTimesClick = {
                    navController.navigate(WearRoutes.PRAYER_TIMES)
                },
                onQuranClick = {
                    navController.navigate(WearRoutes.QURAN_INDEX)
                }
            )
        }

        composable(WearRoutes.ATHKAR_LIST) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            AthkarListScreen(categoryId = categoryId)
        }

        composable(WearRoutes.PRAYER_TIMES) {
            PrayerTimesScreen(
                onSettingsClick = {
                    navController.navigate(WearRoutes.PRAYER_SETTINGS)
                }
            )
        }

        composable(WearRoutes.PRAYER_SETTINGS) {
            PrayerSettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(WearRoutes.QURAN_INDEX) {
            QuranIndexScreen(
                onSurahClick = { surahNumber ->
                    navController.navigate(WearRoutes.quranReader(surahNumber))
                }
            )
        }

        composable(WearRoutes.QURAN_READER) { backStackEntry ->
            val surahNumber = backStackEntry.arguments?.getString("surahNumber")?.toIntOrNull() ?: 1
            QuranReaderScreen(
                surahNumber = surahNumber,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
