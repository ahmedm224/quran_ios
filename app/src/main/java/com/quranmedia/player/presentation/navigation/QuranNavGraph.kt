package com.quranmedia.player.presentation.navigation

import androidx.activity.compose.BackHandler
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
import com.quranmedia.player.presentation.screens.settings.UnifiedSettingsScreen
import com.quranmedia.player.presentation.screens.surahs.SurahsScreenNew
import com.quranmedia.player.presentation.screens.onboarding.OnboardingScreen
import com.quranmedia.player.presentation.screens.tracker.TrackerScreen
import com.quranmedia.player.presentation.screens.imsakiya.ImskaiyaScreen
import com.quranmedia.player.recite.presentation.ReciteMushafScreen
import com.quranmedia.player.recite.presentation.ReciteStreamingScreen

// Helper to navigate to Home clearing back stack
private fun NavHostController.navigateToHome() {
    navigate(Screen.Home.route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

// Helper to navigate to Index clearing back stack
private fun NavHostController.navigateToIndex() {
    navigate(Screen.QuranIndex.route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun QuranNavGraph(
    navController: NavHostController,
    shouldShowOnboarding: Boolean = false
) {
    NavHost(
        navController = navController,
        startDestination = if (shouldShowOnboarding) Screen.Onboarding.route else Screen.QuranIndex.route
    ) {
        // Onboarding Screen (first-run setup)
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    // Navigate to Quran Index and clear back stack
                    navController.navigate(Screen.QuranIndex.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
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
                onNavigateToTracker = { navController.navigate(Screen.Tracker.route) },
                onNavigateToRecite = { navController.navigate(Screen.ReciteIndex.route) },
                onNavigateToImsakiya = { navController.navigate(Screen.Imsakiya.route) }
            )
        }

        composable(Screen.Reciters.route) {
            BackHandler { navController.navigateToHome() }
            RecitersScreenNew(
                onReciterClick = { reciter ->
                    // Navigate to unified Surahs screen (reciter selection handled in that screen)
                    navController.navigate(Screen.Surahs.route)
                },
                onBack = { navController.navigateToHome() }
            )
        }

        composable(Screen.Surahs.route) {
            BackHandler { navController.navigateToHome() }
            SurahsScreenNew(
                onSurahClick = { reciterId, surah ->
                    navController.navigate(Screen.Player.createRoute(reciterId, surah.number))
                },
                onBack = { navController.navigateToHome() }
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

            BackHandler { navController.navigateToHome() }
            PlayerScreenNew(
                reciterId = reciterId,
                surahNumber = surahNumber,
                resumeFromSaved = resume,
                startFromAyah = startAyah,
                onBack = { navController.navigateToHome() }
            )
        }

        composable(Screen.Bookmarks.route) {
            BackHandler { navController.navigateToHome() }
            BookmarksScreen(
                onNavigateBack = { navController.navigateToHome() },
                onBookmarkClick = { reciterId, surahNumber, ayahNumber, positionMs ->
                    // Navigate to player with the bookmarked ayah
                    navController.navigate(Screen.Player.createRoute(reciterId, surahNumber, resume = false, startAyah = ayahNumber))
                },
                onReadingBookmarkClick = { pageNumber ->
                    // Navigate to reader at the bookmarked page
                    navController.navigate(Screen.QuranReader.createRoute(pageNumber))
                },
                // 3-dot menu navigation - pop back to home first so back button goes to home
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToPrayerTimes = { navController.navigate(Screen.PrayerTimes.route) { popUpTo(Screen.Home.route) } },
                onNavigateToAthkar = { navController.navigate(Screen.AthkarCategories.route) { popUpTo(Screen.Home.route) } },
                onNavigateToTracker = { navController.navigate(Screen.Tracker.route) { popUpTo(Screen.Home.route) } },
                onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) { popUpTo(Screen.Home.route) } },
                onNavigateToAbout = { navController.navigate(Screen.About.route) { popUpTo(Screen.Home.route) } },
                onNavigateToReading = { navController.navigate(Screen.QuranIndex.route) { popUpTo(Screen.Home.route) } },
                onNavigateToImsakiya = { navController.navigate(Screen.Imsakiya.route) }
            )
        }

        composable(Screen.Search.route) {
            BackHandler { navController.navigateToHome() }
            SearchScreen(
                onNavigateBack = { navController.navigateToHome() },
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
            BackHandler { navController.navigateToHome() }
            AboutScreen(
                onNavigateBack = { navController.navigateToHome() }
            )
        }

        composable(
            route = Screen.Settings.route,
            arguments = listOf(
                navArgument("tab") {
                    type = NavType.StringType
                    defaultValue = "reading"
                }
            )
        ) { backStackEntry ->
            val tab = backStackEntry.arguments?.getString("tab") ?: "reading"
            UnifiedSettingsScreen(
                initialTab = tab,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Downloads.route) {
            BackHandler { navController.navigateToHome() }
            DownloadsScreen(
                onNavigateBack = { navController.navigateToHome() },
                onDownloadClick = { reciterId, surahNumber ->
                    // Navigate to QuranReader at the surah's page with the reciter for audio playback
                    val page = QuranMetadata.surahStartPages[surahNumber] ?: 1
                    navController.navigate(Screen.QuranReader.createRoute(page, surahNumber, reciterId))
                },
                // 3-dot menu navigation - pop back to home first so back button goes to home
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToPrayerTimes = { navController.navigate(Screen.PrayerTimes.route) { popUpTo(Screen.Home.route) } },
                onNavigateToAthkar = { navController.navigate(Screen.AthkarCategories.route) { popUpTo(Screen.Home.route) } },
                onNavigateToTracker = { navController.navigate(Screen.Tracker.route) { popUpTo(Screen.Home.route) } },
                onNavigateToAbout = { navController.navigate(Screen.About.route) { popUpTo(Screen.Home.route) } },
                onNavigateToReading = { navController.navigate(Screen.QuranIndex.route) { popUpTo(Screen.Home.route) } },
                onNavigateToImsakiya = { navController.navigate(Screen.Imsakiya.route) }
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

            // Back from Reader always goes to Index
            BackHandler { navController.navigateToIndex() }
            QuranReaderScreen(
                initialPage = initialPage,
                initialSurahNumber = surahNumber,
                initialReciterId = reciterId,
                highlightSurahNumber = highlightSurah,
                highlightAyahNumber = highlightAyah,
                onBack = { navController.navigateToIndex() },
                onNavigateToIndex = {
                    // Navigate to index, keeping reader in stack
                    navController.navigate(Screen.QuranIndex.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.createRoute("reading"))
                },
                onNavigateToPrayerTimes = {
                    navController.navigate(Screen.PrayerTimes.route)
                },
                onNavigateToAthkar = {
                    navController.navigate(Screen.AthkarCategories.route)
                },
                onNavigateToTracker = {
                    navController.navigate(Screen.Tracker.route)
                },
                onNavigateToDownloads = {
                    navController.navigate(Screen.Downloads.route)
                },
                onNavigateToAbout = {
                    navController.navigate(Screen.About.route)
                },
                onNavigateToImsakiya = {
                    navController.navigate(Screen.Imsakiya.route)
                }
            )
        }

        composable(Screen.QuranIndex.route) {
            BackHandler { navController.navigateToHome() }
            QuranIndexScreen(
                onBack = { navController.navigateToHome() },
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
                },
                // 3-dot menu navigation - pop back to home first so back button goes to home
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToPrayerTimes = { navController.navigate(Screen.PrayerTimes.route) { popUpTo(Screen.Home.route) } },
                onNavigateToAthkar = { navController.navigate(Screen.AthkarCategories.route) { popUpTo(Screen.Home.route) } },
                onNavigateToTracker = { navController.navigate(Screen.Tracker.route) { popUpTo(Screen.Home.route) } },
                onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) { popUpTo(Screen.Home.route) } },
                onNavigateToAbout = { navController.navigate(Screen.About.route) { popUpTo(Screen.Home.route) } },
                onNavigateToImsakiya = { navController.navigate(Screen.Imsakiya.route) }
            )
        }

        // Athkar screens
        composable(Screen.AthkarCategories.route) {
            BackHandler { navController.navigateToHome() }
            AthkarCategoriesScreen(
                onNavigateBack = { navController.navigateToHome() },
                onCategoryClick = { categoryId ->
                    navController.navigate(Screen.AthkarList.createRoute(categoryId))
                },
                // 3-dot menu navigation - pop back to home first so back button goes to home
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToPrayerTimes = { navController.navigate(Screen.PrayerTimes.route) { popUpTo(Screen.Home.route) } },
                onNavigateToTracker = { navController.navigate(Screen.Tracker.route) { popUpTo(Screen.Home.route) } },
                onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) { popUpTo(Screen.Home.route) } },
                onNavigateToAbout = { navController.navigate(Screen.About.route) { popUpTo(Screen.Home.route) } },
                onNavigateToReading = { navController.navigate(Screen.QuranIndex.route) { popUpTo(Screen.Home.route) } },
                onNavigateToImsakiya = { navController.navigate(Screen.Imsakiya.route) }
            )
        }

        composable(
            route = Screen.AthkarList.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            BackHandler { navController.navigateToHome() }
            AthkarListScreen(
                categoryId = categoryId,
                onNavigateBack = { navController.navigateToHome() },
                // 3-dot menu navigation - pop back to home first so back button goes to home
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToPrayerTimes = { navController.navigate(Screen.PrayerTimes.route) { popUpTo(Screen.Home.route) } },
                onNavigateToTracker = { navController.navigate(Screen.Tracker.route) { popUpTo(Screen.Home.route) } },
                onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) { popUpTo(Screen.Home.route) } },
                onNavigateToAbout = { navController.navigate(Screen.About.route) { popUpTo(Screen.Home.route) } },
                onNavigateToReading = { navController.navigate(Screen.QuranIndex.route) { popUpTo(Screen.Home.route) } },
                onNavigateToImsakiya = { navController.navigate(Screen.Imsakiya.route) }
            )
        }

        // Prayer Times screen
        composable(Screen.PrayerTimes.route) {
            BackHandler { navController.navigateToHome() }
            PrayerTimesScreen(
                onNavigateBack = { navController.navigateToHome() },
                onNavigateToAthanSettings = { navController.navigate(Screen.AthanSettings.route) },
                // 3-dot menu navigation - open Settings with Prayer tab selected
                onNavigateToSettings = { navController.navigate(Screen.Settings.createRoute("prayer")) },
                onNavigateToAthkar = { navController.navigate(Screen.AthkarCategories.route) { popUpTo(Screen.Home.route) } },
                onNavigateToTracker = { navController.navigate(Screen.Tracker.route) { popUpTo(Screen.Home.route) } },
                onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) { popUpTo(Screen.Home.route) } },
                onNavigateToAbout = { navController.navigate(Screen.About.route) { popUpTo(Screen.Home.route) } },
                onNavigateToReading = { navController.navigate(Screen.QuranIndex.route) { popUpTo(Screen.Home.route) } },
                onNavigateToImsakiya = { navController.navigate(Screen.Imsakiya.route) }
            )
        }

        // Athan Settings screen
        composable(Screen.AthanSettings.route) {
            BackHandler { navController.navigateToHome() }
            AthanSettingsScreen(
                onNavigateBack = { navController.navigateToHome() }
            )
        }

        // Daily Tracker screen
        composable(Screen.Tracker.route) {
            BackHandler { navController.navigateToHome() }
            TrackerScreen(
                onNavigateBack = { navController.navigateToHome() },
                // 3-dot menu navigation - pop back to home first so back button goes to home
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToPrayerTimes = { navController.navigate(Screen.PrayerTimes.route) { popUpTo(Screen.Home.route) } },
                onNavigateToAthkar = { navController.navigate(Screen.AthkarCategories.route) { popUpTo(Screen.Home.route) } },
                onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) { popUpTo(Screen.Home.route) } },
                onNavigateToAbout = { navController.navigate(Screen.About.route) { popUpTo(Screen.Home.route) } },
                onNavigateToReading = { navController.navigate(Screen.QuranIndex.route) { popUpTo(Screen.Home.route) } },
                onNavigateToImsakiya = { navController.navigate(Screen.Imsakiya.route) }
            )
        }

        // Ramadan Imsakiya screen (TODO: Remove after Ramadan)
        composable(Screen.Imsakiya.route) {
            BackHandler { navController.navigateToHome() }
            ImskaiyaScreen(
                onBack = { navController.navigateToHome() }
            )
        }

        // Recite (تسميع) - Full Mushaf experience
        // ReciteIndex: Same as QuranIndex but navigates to ReciteReader
        composable(Screen.ReciteIndex.route) {
            BackHandler { navController.navigateToHome() }
            QuranIndexScreen(
                onBack = { navController.navigateToHome() },
                onNavigateToPage = { page ->
                    // Navigate to ReciteReader instead of QuranReader
                    navController.navigate(Screen.ReciteReader.createRoute(page))
                },
                onNavigateToPageWithHighlight = { page, _, _ ->
                    // For recite, we don't need highlight - just go to page
                    navController.navigate(Screen.ReciteReader.createRoute(page))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // ReciteReader: Full Mushaf page with Mic FAB for recitation
        composable(
            route = Screen.ReciteReader.route,
            arguments = listOf(
                navArgument("page") {
                    type = NavType.IntType
                    defaultValue = 1
                }
            )
        ) { backStackEntry ->
            val initialPage = backStackEntry.arguments?.getInt("page") ?: 1
            // Back from ReciteReader goes to ReciteIndex
            BackHandler {
                navController.navigate(Screen.ReciteIndex.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
            ReciteMushafScreen(
                initialPage = initialPage,
                onBack = {
                    navController.navigate(Screen.ReciteIndex.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToIndex = {
                    navController.navigate(Screen.ReciteIndex.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Legacy Recite route - redirect to ReciteIndex
        composable(Screen.Recite.route) {
            // Redirect to new flow
            androidx.compose.runtime.LaunchedEffect(Unit) {
                navController.navigate(Screen.ReciteIndex.route) {
                    popUpTo(Screen.Recite.route) { inclusive = true }
                }
            }
        }
    }
}
