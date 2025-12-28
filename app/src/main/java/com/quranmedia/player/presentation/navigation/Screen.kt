package com.quranmedia.player.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object WhatsNew : Screen("whatsNew")
    object Reciters : Screen("reciters")
    object Surahs : Screen("surahs")
    object Player : Screen("player/{reciterId}/{surahNumber}?resume={resume}&startAyah={startAyah}") {
        fun createRoute(reciterId: String, surahNumber: Int, resume: Boolean = false, startAyah: Int? = null) =
            "player/$reciterId/$surahNumber?resume=$resume${startAyah?.let { "&startAyah=$it" } ?: ""}"
    }
    object Bookmarks : Screen("bookmarks")
    object Downloads : Screen("downloads")
    object Search : Screen("search")
    object Settings : Screen("settings")
    object About : Screen("about")

    // Quran Reader screens
    object QuranReader : Screen("quranReader?page={page}&surah={surah}&reciter={reciter}&highlightSurah={highlightSurah}&highlightAyah={highlightAyah}") {
        fun createRoute(page: Int = 1, surahNumber: Int? = null, reciterId: String? = null, highlightSurah: Int? = null, highlightAyah: Int? = null): String {
            var route = "quranReader?page=$page"
            if (surahNumber != null) route += "&surah=$surahNumber"
            if (reciterId != null) route += "&reciter=$reciterId"
            if (highlightSurah != null) route += "&highlightSurah=$highlightSurah"
            if (highlightAyah != null) route += "&highlightAyah=$highlightAyah"
            return route
        }
    }
    object QuranIndex : Screen("quranIndex")

    // Athkar screens
    object AthkarCategories : Screen("athkarCategories")
    object AthkarList : Screen("athkarList/{categoryId}") {
        fun createRoute(categoryId: String) = "athkarList/$categoryId"
    }

    // Prayer Times screen
    object PrayerTimes : Screen("prayerTimes")

    // Athan Settings screen
    object AthanSettings : Screen("athanSettings")

    // Daily Tracker screen
    object Tracker : Screen("tracker")
}
