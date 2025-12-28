package com.quranmedia.player.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.LayoutDirection
import com.quranmedia.player.data.repository.AppLanguage

/**
 * Localization strings for the app
 */
object Strings {
    // App name and branding
    val appName = LocalizedString("الفرقان", "Alfurqan")
    val appNameEnglish = LocalizedString("Alfurqan", "Alfurqan")
    val bismillah = LocalizedString("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "In the name of Allah, the Most Gracious, the Most Merciful")

    // Home screen
    val quran = LocalizedString("القرآن الكريم", "The Holy Quran")
    val readAndListen = LocalizedString("قراءة واستماع", "Read & Listen")
    val continueReading = LocalizedString("متابعة القراءة", "Continue Reading")
    val bookmarks = LocalizedString("المفضلة", "Bookmarks")
    val downloads = LocalizedString("التنزيلات", "Downloads")
    val settings = LocalizedString("الإعدادات", "Settings")
    val about = LocalizedString("حول التطبيق", "About")
    val language = LocalizedString("اللغة", "Language")
    val arabic = LocalizedString("العربية", "Arabic")
    val english = LocalizedString("English", "English")

    // Reader screen
    val page = LocalizedString("صفحة", "Page")
    val surah = LocalizedString("سورة", "Surah")
    val ayah = LocalizedString("آية", "Ayah")
    val juz = LocalizedString("الجزء", "Juz")
    val loadingQuran = LocalizedString("جاري تحميل القرآن...", "Loading Quran...")
    val preparingPages = LocalizedString("جاري تحضير صفحات القرآن...", "Preparing Quran pages...")
    val firstTimeMessage = LocalizedString("قد يستغرق هذا لحظات في أول استخدام", "This may take a moment on first use")
    val loadingPage = LocalizedString("جاري تحميل الصفحة...", "Loading page...")

    // Index screen
    val index = LocalizedString("الفهرس", "Index")
    val goToPage = LocalizedString("انتقل إلى صفحة", "Go to page")
    val pageErrorMessage = LocalizedString("الصفحة يجب أن تكون بين", "Page must be between")

    // Player controls
    val selectReciter = LocalizedString("اختر القارئ", "Select Reciter")
    val play = LocalizedString("تشغيل", "Play")
    val pause = LocalizedString("إيقاف مؤقت", "Pause")
    val stop = LocalizedString("إيقاف", "Stop")
    val previousAyah = LocalizedString("الآية السابقة", "Previous Ayah")
    val nextAyah = LocalizedString("الآية التالية", "Next Ayah")
    val playbackSpeed = LocalizedString("سرعة التشغيل", "Playback Speed")
    val close = LocalizedString("إغلاق", "Close")
    val done = LocalizedString("تم", "Done")
    val cancel = LocalizedString("إلغاء", "Cancel")
    val go = LocalizedString("اذهب", "Go")

    // Bookmarks
    val readingBookmarks = LocalizedString("علامات القراءة", "Reading Bookmarks")
    val noBookmarks = LocalizedString("لا توجد علامات محفوظة", "No bookmarks saved")
    val addBookmark = LocalizedString("إضافة علامة", "Add Bookmark")
    val removeBookmark = LocalizedString("إزالة العلامة", "Remove Bookmark")
    val deleteBookmark = LocalizedString("حذف العلامة", "Delete Bookmark")

    // Surah types
    val meccan = LocalizedString("مكية", "Meccan")
    val medinan = LocalizedString("مدنية", "Medinan")

    // About screen
    val version = LocalizedString("الإصدار", "Version")
    val developer = LocalizedString("المطور", "Developer")
    val features = LocalizedString("المميزات", "Features")
    val privacyPolicy = LocalizedString("سياسة الخصوصية", "Privacy Policy")

    // Athkar
    val athkar = LocalizedString("الأذكار", "Athkar")
    val morningAthkar = LocalizedString("أذكار الصباح", "Morning Athkar")
    val eveningAthkar = LocalizedString("أذكار المساء", "Evening Athkar")
    val afterPrayerAthkar = LocalizedString("أذكار بعد الصلاة", "After Prayer")
    val beforeSleepAthkar = LocalizedString("أذكار النوم", "Before Sleep")
    val wakingUpAthkar = LocalizedString("أذكار الاستيقاظ", "Waking Up")
    val enteringHomeAthkar = LocalizedString("دخول المنزل", "Entering Home")
    val leavingHomeAthkar = LocalizedString("الخروج من المنزل", "Leaving Home")
    val eatingAthkar = LocalizedString("أذكار الطعام", "Eating")
    val travelingAthkar = LocalizedString("أذكار السفر", "Traveling")
    val protectionAthkar = LocalizedString("أذكار الحماية", "Protection")
    val repeat = LocalizedString("تكرار", "Repeat")
    val times = LocalizedString("مرات", "times")
    val remaining = LocalizedString("متبقي", "remaining")
    val tapToCount = LocalizedString("اضغط للعد", "Tap to count")
    val completed = LocalizedString("اكتمل", "Completed")
    val resetCounter = LocalizedString("إعادة العداد", "Reset Counter")

    // Prayer Times
    val prayerTimes = LocalizedString("مواقيت الصلاة", "Prayer Times")
    val fajr = LocalizedString("الفجر", "Fajr")
    val sunrise = LocalizedString("الشروق", "Sunrise")
    val dhuhr = LocalizedString("الظهر", "Dhuhr")
    val asr = LocalizedString("العصر", "Asr")
    val maghrib = LocalizedString("المغرب", "Maghrib")
    val isha = LocalizedString("العشاء", "Isha")
    val nextPrayer = LocalizedString("الصلاة القادمة", "Next Prayer")
    val timeRemaining = LocalizedString("الوقت المتبقي", "Time Remaining")
    val location = LocalizedString("الموقع", "Location")
    val detectLocation = LocalizedString("تحديد الموقع", "Detect Location")
    val enterCity = LocalizedString("أدخل اسم المدينة", "Enter city name")
    val setLocation = LocalizedString("تعيين الموقع", "Set Location")
    val calculationMethod = LocalizedString("طريقة الحساب", "Calculation Method")
    val refresh = LocalizedString("تحديث", "Refresh")
    val noLocation = LocalizedString("لم يتم تحديد الموقع", "No location set")
    val locationPermissionRequired = LocalizedString("مطلوب إذن الموقع", "Location permission required")
    val grantPermission = LocalizedString("منح الإذن", "Grant Permission")
    val orEnterManually = LocalizedString("أو أدخل يدوياً", "Or enter manually")
    val hijriDate = LocalizedString("التاريخ الهجري", "Hijri Date")
    val today = LocalizedString("اليوم", "Today")

    // Calculation Methods
    val mwl = LocalizedString("رابطة العالم الإسلامي", "Muslim World League")
    val isna = LocalizedString("الجمعية الإسلامية لأمريكا الشمالية", "Islamic Society of North America")
    val egypt = LocalizedString("الهيئة المصرية العامة للمساحة", "Egyptian General Authority")
    val makkah = LocalizedString("أم القرى", "Umm Al-Qura University")
    val karachi = LocalizedString("جامعة العلوم الإسلامية كراتشي", "University of Islamic Sciences, Karachi")
    val tehran = LocalizedString("معهد الجيوفيزياء بطهران", "Institute of Geophysics, Tehran")
    val jafari = LocalizedString("الشيعة إثنا عشرية", "Shia Ithna-Ashari")
}

/**
 * A string that has both Arabic and English versions
 */
data class LocalizedString(
    val arabic: String,
    val english: String
) {
    fun get(language: AppLanguage): String = when (language) {
        AppLanguage.ARABIC -> arabic
        AppLanguage.ENGLISH -> english
    }
}

/**
 * Get layout direction based on language
 */
fun AppLanguage.layoutDirection(): LayoutDirection = when (this) {
    AppLanguage.ARABIC -> LayoutDirection.Rtl
    AppLanguage.ENGLISH -> LayoutDirection.Ltr
}

/**
 * Check if language is RTL
 */
fun AppLanguage.isRtl(): Boolean = this == AppLanguage.ARABIC
