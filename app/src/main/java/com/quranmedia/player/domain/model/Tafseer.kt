package com.quranmedia.player.domain.model

/**
 * Represents a tafseer/interpretation source that can be downloaded.
 */
data class TafseerInfo(
    val id: String,              // e.g., "ibn-kathir"
    val nameArabic: String?,     // e.g., "تفسير ابن كثير"
    val nameEnglish: String,     // e.g., "Tafsir Ibn Kathir"
    val language: String,        // "arabic" or "english"
    val type: TafseerType,       // TAFSEER or WORD_MEANING
    val downloadUrl: String      // API endpoint for download
)

/**
 * Type of tafseer content.
 */
enum class TafseerType {
    TAFSEER,        // Full tafseer/interpretation
    WORD_MEANING,   // Word-by-word meanings
    GRAMMAR         // Grammatical analysis (Irab)
}

/**
 * Represents tafseer content for a specific ayah.
 */
data class TafseerContent(
    val tafseerId: String,
    val surah: Int,
    val ayah: Int,
    val text: String
)

/**
 * Represents download status of a tafseer.
 */
data class TafseerDownload(
    val tafseerInfo: TafseerInfo,
    val isDownloaded: Boolean,
    val downloadedAt: Long? = null,
    val totalSizeBytes: Long? = null,
    val downloadProgress: Float = 0f  // 0.0 to 1.0
)

/**
 * Available tafseers that can be downloaded.
 * Fetched from alfurqan.online API.
 *
 * API Endpoint: GET https://alfurqan.online/api/v1/tafseer/downloads
 *
 * Available tafseers:
 * - word-by-word-english: Word by Word Translation (English)
 * - mufradat: Quran Mufradat - مفردات القرآن (Arabic)
 * - ibn-kathir-english: Tafsir Ibn Kathir (English)
 * - maarif-ul-quran: Ma'ariful Quran - معارف القرآن (English)
 * - al-saddi: Tafsir Al-Saddi - تفسير السعدي (Arabic)
 * - al-tabari: Tafsir Al-Tabari - تفسير الطبري (Arabic)
 * - ibn-kathir: Tafsir Ibn Kathir - تفسير ابن كثير (Arabic)
 * - muyassar: Al-Tafsir Al-Muyassar - التفسير الميسر (Arabic)
 */
object AvailableTafseers {
    const val API_BASE = "https://alfurqan.online/api/v1"

    // Static list as fallback (API provides dynamic list)
    val tafseers = listOf(
        // Bundled - Grammar Analysis (Irab)
        TafseerInfo(
            id = "quran-irab",
            nameArabic = "إعراب القرآن",
            nameEnglish = "Quran Grammar (Irab)",
            language = "arabic",
            type = TafseerType.GRAMMAR,
            downloadUrl = "bundled:quran_irab_by_surah.json"  // Bundled in assets
        ),
        // Word Meanings
        TafseerInfo(
            id = "word-by-word-english",
            nameArabic = null,
            nameEnglish = "Word by Word Translation",
            language = "english",
            type = TafseerType.WORD_MEANING,
            downloadUrl = "/api/v1/tafseer/download/word-by-word-english"
        ),
        TafseerInfo(
            id = "mufradat",
            nameArabic = "مفردات القرآن",
            nameEnglish = "Quran Mufradat",
            language = "arabic",
            type = TafseerType.WORD_MEANING,
            downloadUrl = "/api/v1/tafseer/download/mufradat"
        ),
        // English Tafseers
        TafseerInfo(
            id = "ibn-kathir-english",
            nameArabic = "تفسير ابن كثير",
            nameEnglish = "Tafsir Ibn Kathir",
            language = "english",
            type = TafseerType.TAFSEER,
            downloadUrl = "/api/v1/tafseer/download/ibn-kathir-english"
        ),
        TafseerInfo(
            id = "maarif-ul-quran",
            nameArabic = "معارف القرآن",
            nameEnglish = "Ma'ariful Quran",
            language = "english",
            type = TafseerType.TAFSEER,
            downloadUrl = "/api/v1/tafseer/download/maarif-ul-quran"
        ),
        // Arabic Tafseers
        TafseerInfo(
            id = "al-saddi",
            nameArabic = "تفسير السعدي",
            nameEnglish = "Tafsir Al-Saddi",
            language = "arabic",
            type = TafseerType.TAFSEER,
            downloadUrl = "/api/v1/tafseer/download/al-saddi"
        ),
        TafseerInfo(
            id = "al-tabari",
            nameArabic = "تفسير الطبري",
            nameEnglish = "Tafsir Al-Tabari",
            language = "arabic",
            type = TafseerType.TAFSEER,
            downloadUrl = "/api/v1/tafseer/download/al-tabari"
        ),
        TafseerInfo(
            id = "ibn-kathir",
            nameArabic = "تفسير ابن كثير",
            nameEnglish = "Tafsir Ibn Kathir",
            language = "arabic",
            type = TafseerType.TAFSEER,
            downloadUrl = "/api/v1/tafseer/download/ibn-kathir"
        ),
        TafseerInfo(
            id = "muyassar",
            nameArabic = "التفسير الميسر",
            nameEnglish = "Al-Tafsir Al-Muyassar",
            language = "arabic",
            type = TafseerType.TAFSEER,
            downloadUrl = "bundled:Tafseer-muyassar.json"  // Bundled in assets
        )
    )

    fun getById(id: String): TafseerInfo? = tafseers.find { it.id == id }

    fun getByLanguage(language: String): List<TafseerInfo> = tafseers.filter { it.language == language }

    fun getByType(type: TafseerType): List<TafseerInfo> = tafseers.filter { it.type == type }

    fun isArabic(tafseerId: String): Boolean = getById(tafseerId)?.language == "arabic"

    /**
     * Get tafseers sorted by app language preference.
     *
     * Arabic app language order:
     * 1. Ibn Kathir Arabic (priority)
     * 2. Other Arabic tafseers (Muyassar, Al-Saddi, Al-Tabari)
     * 3. English tafseers
     * 4. Grammar (Irab) - not preselected
     * 5. Mufradat Arabic
     * 6. Word-by-word English
     *
     * English app language order:
     * 1. English tafseers
     * 2. Arabic tafseers (all together)
     * 3. Grammar (Irab) - not preselected
     * 4. Word-by-word English
     * 5. Mufradat Arabic
     */
    fun getSortedByLanguage(appLanguage: String): List<TafseerInfo> {
        return if (appLanguage == "arabic") {
            // Arabic app: prioritize Arabic content with Ibn Kathir first
            buildList {
                // 1. Ibn Kathir Arabic (priority)
                getById("ibn-kathir")?.let { add(it) }

                // 2. Rest of Arabic tafseers
                tafseers.filter {
                    it.language == "arabic" &&
                    it.type == TafseerType.TAFSEER &&
                    it.id != "ibn-kathir"
                }.forEach { add(it) }

                // 3. English tafseers
                tafseers.filter {
                    it.language == "english" &&
                    it.type == TafseerType.TAFSEER
                }.forEach { add(it) }

                // 4. Grammar (Irab) - after tafseers, not preselected
                tafseers.filter { it.type == TafseerType.GRAMMAR }.forEach { add(it) }

                // 5. Mufradat Arabic
                getById("mufradat")?.let { add(it) }

                // 6. Word-by-word English
                getById("word-by-word-english")?.let { add(it) }
            }
        } else {
            // English app: prioritize English content
            buildList {
                // 1. English tafseers
                tafseers.filter {
                    it.language == "english" &&
                    it.type == TafseerType.TAFSEER
                }.forEach { add(it) }

                // 2. All Arabic tafseers (no special order)
                tafseers.filter {
                    it.language == "arabic" &&
                    it.type == TafseerType.TAFSEER
                }.forEach { add(it) }

                // 3. Grammar (Irab) - after tafseers, not preselected
                tafseers.filter { it.type == TafseerType.GRAMMAR }.forEach { add(it) }

                // 4. Word-by-word English
                getById("word-by-word-english")?.let { add(it) }

                // 5. Mufradat Arabic
                getById("mufradat")?.let { add(it) }
            }
        }
    }
}
