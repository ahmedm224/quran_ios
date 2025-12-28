package com.quranmedia.player.domain.util

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import timber.log.Timber

/**
 * Tajweed annotation for a specific ayah
 */
data class TajweedAyah(
    @SerializedName("surah") val surah: Int,
    @SerializedName("ayah") val ayah: Int,
    @SerializedName("annotations") val annotations: List<TajweedAnnotation>
)

/**
 * Single Tajweed rule annotation with character positions
 */
data class TajweedAnnotation(
    @SerializedName("rule") val rule: String,
    @SerializedName("start") val start: Int,
    @SerializedName("end") val end: Int
)

/**
 * Tajweed rule colors mapping
 */
object TajweedColors {
    val ruleColors = mapOf(
        // Madd (prolongation) - Blue shades
        "madd_2" to Color(0xFF537FFF),      // Normal Madd (2 vowels)
        "madd_246" to Color(0xFF4050FF),    // Permissible Madd (2-6 vowels)
        "madd_6" to Color(0xFF000EBC),      // Necessary Madd (6 vowels)
        "madd_munfasil" to Color(0xFF2144C1), // Obligatory Madd (4-5 vowels)
        "madd_muttasil" to Color(0xFF2144C1),
        "madd_arid" to Color(0xFF4050FF),
        "madd_leen" to Color(0xFF4050FF),
        "madd_lazim" to Color(0xFF000EBC),

        // Qalqalah - Red
        "qalqalah" to Color(0xFFDD0008),

        // Ghunnah - Orange
        "ghunnah" to Color(0xFFFF7E1E),

        // Idgham - Green shades
        "idghaam_wo_ghunnah" to Color(0xFF169200),     // Without Ghunnah
        "idghaam_w_ghunnah" to Color(0xFF169777),      // With Ghunnah
        "idghaam_shafawi" to Color(0xFF58B800),        // Shafawi
        "idghaam_mutajanisayn" to Color(0xFFA1A1A1),   // Similar letters
        "idghaam_mutaqaribayn" to Color(0xFFA1A1A1),   // Nearby letters

        // Ikhfa - Purple shades
        "ikhfa" to Color(0xFF9400A8),
        "ikhfa_shafawi" to Color(0xFFD500B7),

        // Iqlab - Cyan
        "iqlab" to Color(0xFF26BFFD),

        // Silent/Gray
        "hamzat_wasl" to Color(0xFFAAAAAA),
        "lam_shamsiyyah" to Color(0xFFAAAAAA),
        "silent" to Color(0xFFAAAAAA)
    )

    // Embedded marker to color mapping (lowercase a-z markers)
    val embeddedMarkerColors = mapOf(
        'g' to Color(0xFFFF7E1E),      // Ghunnah - Orange
        'd' to Color(0xFF169777),      // Idghaam with Ghunnah - Green
        'n' to Color(0xFF169200),      // Idghaam without Ghunnah - Green
        'k' to Color(0xFF9400A8),      // Ikhfa - Purple
        'i' to Color(0xFF26BFFD),      // Iqlab - Cyan
        'q' to Color(0xFFDD0008),      // Qalqalah - Red
        'o' to Color(0xFF537FFF),      // Madd 2 - Blue
        'x' to Color(0xFF000EBC),      // Madd 6 (Lazim) - Dark Blue
        'm' to Color(0xFF537FFF),      // Madd (general) - Blue
        'e' to Color(0xFFAAAAAA),      // Silent - Gray
        'h' to Color(0xFFAAAAAA),      // Hamzat Wasl - Gray
        'l' to Color(0xFFAAAAAA),      // Lam Shamsiyyah - Gray
        's' to Color(0xFF58B800),      // Idghaam Shafawi - Light Green
        'b' to Color(0xFFD500B7),      // Ikhfa Shafawi - Light Purple
        'w' to Color(0xFF2144C1),      // Madd Munfasil - Medium Blue
        't' to Color(0xFF2144C1),      // Madd Muttasil - Medium Blue
        'f' to Color(0xFF4050FF),      // Madd Arid/Leen - Light Blue
        'j' to Color(0xFFA1A1A1)       // Idghaam Mutajanisayn/Mutaqaribayn - Gray
    )

    fun getColor(rule: String): Color {
        return ruleColors[rule] ?: Color.Black
    }

    fun getColorFromMarker(marker: Char): Color {
        return embeddedMarkerColors[marker] ?: Color.Black
    }
}

/**
 * Singleton object for Tajweed data loading and retrieval
 *
 * Loads Tajweed annotations from bundled JSON files (semarketir/quranjson format)
 * and provides them to TajweedParser for rendering.
 *
 * SCOPE: Only used when Tajweed theme is active.
 */
object TajweedDataLoader {

    private var isLoaded = false
    private val annotationsCache = mutableMapOf<Pair<Int, Int>, List<TajweedAnnotation>>()
    private val gson = Gson()

    /**
     * Load Tajweed data from assets
     * DISABLED: JSON position annotations from semarketir/quranjson don't match our database text
     *
     * The JSON was created for a different Quran text edition, causing:
     * - Wrong coloring (spaces/diacritics get colored)
     * - Tashkeel displacement (marks separated from letters)
     * - Overlapping/rendering issues
     *
     * To enable Tajweed, you need annotation data that matches your database's exact text.
     */
    fun loadTajweedData(context: Context) {
        if (isLoaded) return

        Timber.w("═══ TAJWEED DATA LOADING DISABLED ═══")
        Timber.w("Reason: JSON position annotations don't match database Quran text")
        Timber.w("Solution: Obtain Tajweed data matching your text edition, OR add embedded markers to database text")

        // Mark as loaded (empty) to prevent repeated attempts
        isLoaded = true
    }

    /**
     * Get Tajweed annotations for a specific ayah
     *
     * @param surahNumber Surah number (1-114)
     * @param ayahNumber Ayah number within the surah
     * @return List of Tajweed annotations for this ayah (empty if not available)
     */
    fun getAnnotations(surahNumber: Int, ayahNumber: Int): List<TajweedAnnotation> {
        return annotationsCache[Pair(surahNumber, ayahNumber)] ?: emptyList()
    }

    /**
     * Check if Tajweed data is loaded
     */
    fun isLoaded(): Boolean = isLoaded

    /**
     * Clear cached data (for memory management)
     */
    fun clearCache() {
        annotationsCache.clear()
        isLoaded = false
        Timber.d("Tajweed data cache cleared")
    }
}
