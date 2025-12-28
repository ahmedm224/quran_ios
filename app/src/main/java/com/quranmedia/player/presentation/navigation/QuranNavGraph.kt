package com.quranmedia.player.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.quranmedia.player.data.QuranMetadata
import com.quranmedia.player.presentation.screens.about.AboutScreen
import com.quranmedia.player.presentation.screens.athkar.AthkarCategoriesScreen
import com.quranmedia.player.presentation.screens.athkar.AthkarListScreen
import com.quranmedia.player.presentation.screens.bookmarks.BookmarksScreen
import com.quranmedia.player.presentation.screens.downloads.DownloadsScreen
import com.quranmedia.player.presentation.screens.home.HomeScreenNew
import com.quranmedia.player.presentation.screens.player.PlayerScreenNew
import com.quranmedia.player.presentation.screens.prayertimes.AthanSettingsScreen
import com.quranmedia.player.presentation.screens.prayertimes.PrayerTimesScreen
import com.quranmedia.player.presentation.screens.reader.QuranIndexScreen
import com.quranmedia.player.presentation.screens.reader.QuranReaderScreen
import com.quranmedia.player.presentation.screens.reciters.RecitersScreenNew
import com.quranmedia.player.presentation.screens.search.SearchScreen
import com.quranmedia.player.presentation.screens.settings.SettingsScreen
import com.quranmedia.player.presentation.screens.surahs.SurahsScreenNew
import com.quranmedia.player.presentation.screens.whatsnew.WhatsNewScreen
import com.quranmedia.player.presentation.screens.tracker.TrackerScreen

@Composable
fun QuranNavGraph(
    navController: NavHostController,
    shouldShowWhatsNew: Boolean = false
) {
    NavHost(
        navController = navController,
        startDestination = if (shouldShowWhatsNew) Screen.WhatsNew.route else Screen.Home.route
    ) {
        // What's New Screen (first-run and version upgrades)
        composable(Screen.WhatsNew.route) {
            WhatsNewScreen(
                onComplete = {
                    // Navigate to home and clear back stack
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.WhatsNew.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreenNew(
                onNavigateToReciters = { navController.navigate(Screen.Reciters.route) },
                onNavigateToSurahs = {
                    // Navigate directly to Surahs screen with reciter dropdown
                    navController.navigate(Screen.Surahs.route)
                },
                onNavigateToPlayer = { reciterId, surahNumber, resume ->
                    navController.navigate(Screen.Player.createRoute(reciterId, surahNumber, resume))
                },
                onNavigateToBookmarks = { navController.navigate(Screen.Bookmarks.route) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) },
                onNavigateToQuranReader = { page -> navController.navigate(Screen.QuranReader.createRoute(page ?: 1)) },
                onNavigateToQuranIndex = { navController.navigate(Screen.QuranIndex.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAthkar = { navController.navigate(Screen.AthkarCategories.route) },
                onNavigateToPrayerTimes = { navController.navigate(Screen.PrayerTimes.route) },
                onNavigateToTracker = { navController.navigate(Screen.Tracker.route) }
            )
        }

        composable(Screen.Reciters.route) {
            RecitersScreenNew(
                onReciterClick = { reciter ->
                    // Navigate to unified Surahs screen (reciter selection handled in that screen)
                    navController.navigate(Screen.Surahs.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Surahs.route) {
            SurahsScreenNew(
                onSurahClick = { reciterId, surah ->
                    navController.navigate(Screen.Player.createRoute(reciterId, surah.number))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("reciterId") { type = NavType.StringType },
                navArgument("surahNumber") { type = NavType.IntType },
                navArgument("resume") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("startAyah") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val reciterId = backStackEntry.arguments?.getString("reciterId") ?: return@composable
            val surahNumber = backStackEntry.arguments?.getInt("surahNumber") ?: return@composable
            val resume = backStackEntry.arguments?.getBoolean("resume") ?: false
            val startAyah = backStackEntry.arguments?.getInt("startAyah")?.takeIf { it > 0 }

            PlayerScreenNew(
                reciterId = reciterId,
                surahNumber = surahNumber,
                resumeFromSaved = resume,
                startFromAyah = startAyah,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Bookmarks.route) {
            BookmarksScreen(
                onNavigateBack = { navController.popBackStack() },
                onBookmarkClick = { reciterId, surahNumber, ayahNumber, positionMs ->
                    // Navigate to player with the bookmarked ayah
                    navController.navigate(Screen.Player.createRoute(reciterId, surahNumber, resume = false, startAyah = ayahNumber))
                },
                onReadingBookmarkClick = { pageNumber ->
                    // Navigate to reader at the bookmarked page
                    navController.navigate(Screen.QuranReader.createRoute(pageNumber))
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onResultClick = { surahNumber, ayahNumber, page ->
                    // Navigate to Quran Reader at the ayah's page with the ayah highlighted
                    navController.navigate(Screen.QuranReader.createRoute(
                        page = page,
                        highlightSurah = surahNumber,
                        highlightAyah = ayahNumber
                    ))
                }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen(
                onNavigateBack = { navController.popBackStack() },
                onDownloadClick = { reciterId, surahNumber ->
                    // Navigate to QuranReader at the surah's page with the reciter for audio playback
                    val page = QuranMetadata.surahStartPages[surahNumber] ?: 1
                    navController.navigate(Screen.QuranReader.createRoute(page, surahNumber, reciterId))
                }
            )
        }

        composable(
            route = Screen.QuranReader.route,
            arguments = listOf(
                navArgument("page") {
                    type = NavType.IntType
                    defaultValue = 1
                },
                navArgument("surah") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("reciter") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                },
                navArgument("highlightSurah") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("highlightAyah") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val initialPage = backStackEntry.arguments?.getInt("page") ?: 1
            val surahNumber = backStackEntry.arguments?.getInt("surah")?.takeIf { it > 0 }
            val reciterId = backStackEntry.arguments?.getString("reciter")?.takeIf { it.isNotEmpty() }
            val highlightSurah = backStackEntry.arguments?.getInt("highlightSurah")?.takeIf { it > 0 }
            val highlightAyah = backStackEntry.arguments?.getInt("highlightAyah")?.takeIf { it > 0 }

            QuranReaderScreen(
                initialPage = initialPage,
                initialSurahNumber = surahNumber,
                initialReciterId = reciterId,
                highlightSurahNumber = highlightSurah,
                highlightAyahNumber = highlightAyah,
                onBack = {
                    // Back from reader goes to Index (which is in the back stack)
                    navController.popBackStack()
                },
                onNavigateToIndex = {
                    // Navigate to index, keeping reader in stack
                    navController.navigate(Screen.QuranIndex.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.QuranIndex.route) {
            QuranIndexScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPage = { page ->
                    // Navigate to reader - keep index in back stack so back goes to index
                    navController.navigate(Screen.QuranReader.createRoute(page))
                },
                onNavigateToPageWithHighlight = { page, surahNumber, ayahNumber ->
                    // Navigate to reader with ayah highlighted (from search)
                    navController.navigate(Screen.QuranReader.createRoute(
                        page = page,
                        highlightSurah = surahNumber,
                        highlightAyah = ayahNumber
                    ))
                }
            )
        }

        // Athkar screens
        composable(Screen.AthkarCategories.route) {
            AthkarCategoriesScreen(
                onNavigateBack = { navController.popBackStack() },
                onCategoryClick = { categoryId ->
                    navController.navigate(Screen.AthkarList.createRoute(categoryId))
                }
            )
        }

        composable(
            route = Screen.AthkarList.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            AthkarListScreen(
                categoryId = categoryId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Prayer Times screen
        composable(Screen.PrayerTimes.route) {
            PrayerTimesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAthanSettings = { navController.navigate(Screen.AthanSettings.route) }
            )
        }

        // Athan Settings screen
        composable(Screen.AthanSettings.route) {
            AthanSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Daily Tracker screen
        composable(Screen.Tracker.route) {
            com.quranmedia.player.presentation.screens.tracker.TrackerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
