package com.quranmedia.player.recite.util

/**
 * Utility for normalizing Arabic text to improve matching accuracy
 * Removes diacritics, kashida, and normalizes Alef variants
 */
object ArabicNormalizer {

    // Arabic diacritics (tashkeel)
    private val DIACRITICS = charArrayOf(
        '\u064B', // Fathatan
        '\u064C', // Dammatan
        '\u064D', // Kasratan
        '\u064E', // Fatha
        '\u064F', // Damma
        '\u0650', // Kasra
        '\u0651', // Shadda
        '\u0652', // Sukun
        '\u0653', // Maddah
        '\u0654', // Hamza above
        '\u0655', // Hamza below
        '\u0656', // Subscript alef
        '\u0657', // Inverted damma
        '\u0658', // Mark noon ghunna
        '\u0670', // Superscript alef

        // Quran-specific marks (U+06D6 - U+06ED)
        '\u06D6', // Small high ligature sad with lam with alef maksura
        '\u06D7', // Small high ligature qaf with lam with alef maksura
        '\u06D8', // Small high meem initial form
        '\u06D9', // Small high lam alef
        '\u06DA', // Small high jeem
        '\u06DB', // Small high three dots
        '\u06DC', // Small high seen
        '\u06DD', // End of ayah
        '\u06DE', // Start of rub el hizb
        '\u06DF', // Small high rounded zero
        '\u06E0', // Small high upright rectangular zero
        '\u06E1', // Small high dotless head of khah (common in Quran for sukun)
        '\u06E2', // Small high meem isolated form
        '\u06E3', // Small low seen
        '\u06E4', // Small high madda
        '\u06E5', // Small waw
        '\u06E6', // Small yeh
        '\u06E7', // Small high yeh
        '\u06E8', // Small high noon
        '\u06E9', // Place of sajdah
        '\u06EA', // Empty centre low stop
        '\u06EB', // Empty centre high stop
        '\u06EC', // Rounded high stop with filled centre
        '\u06ED'  // Small low meem
    )

    // Kashida (tatweel) - elongation character
    private const val KASHIDA = '\u0640'

    // Alef variants to normalize
    private val ALEF_VARIANTS = mapOf(
        'آ' to 'ا', // Alef with madda
        'أ' to 'ا', // Alef with hamza above
        'إ' to 'ا', // Alef with hamza below
        'ٱ' to 'ا'  // Alef wasla
    )

    // Additional letter normalizations
    private val LETTER_NORMALIZATIONS = mapOf(
        'ى' to 'ي', // Alef maksura to yaa
        'ة' to 'ه'  // Taa marbuta to haa
    )

    // Uthmani Quran specific word normalizations
    // These handle differences between Uthmani script and standard Arabic
    private val UTHMANI_WORD_NORMALIZATIONS = mapOf(
        // صرط vs صراط (sirat - with/without alif)
        "صرط" to "صراط",
        "الصرط" to "الصراط",
        "صرطك" to "صراطك",

        // بسطة vs بصطة
        "بصطة" to "بسطة",

        // Common Uthmani variations
        "السموت" to "السماوات",
        "الصلوة" to "الصلاة",
        "الزكوة" to "الزكاة",
        "الحيوة" to "الحياة",
        "النجوة" to "النجاة",
        "مشكوة" to "مشكاة",
        "منوة" to "مناة",
        "الغدوة" to "الغداة",
        "كمشكوة" to "كمشكاة",

        // ي vs ى at end of words (already partially handled, but explicit)
        "هدى" to "هدي",
        "موسى" to "موسي",
        "عيسى" to "عيسي",
        "يحيى" to "يحيي",

        // إبرهم vs إبراهيم
        "ابرهم" to "ابراهيم",
        "ابرهيم" to "ابراهيم",
        "لابرهم" to "لابراهيم",

        // إسمعيل vs إسماعيل
        "اسمعيل" to "اسماعيل",

        // Other common variations
        "ءادم" to "ادم",
        "ءامن" to "امن",
        "ءايت" to "ايات",
        "ءايه" to "ايه",
        "لءايت" to "لايات"
    )

    /**
     * Normalize Arabic text for comparison
     * @param text The Arabic text to normalize
     * @return Normalized text without diacritics, kashida, with normalized letters
     */
    fun normalize(text: String): String {
        var normalized = text

        // Remove kashida (elongation)
        normalized = normalized.replace(KASHIDA.toString(), "")

        // Remove all diacritics
        DIACRITICS.forEach { diacritic ->
            normalized = normalized.replace(diacritic.toString(), "")
        }

        // Normalize Alef variants
        ALEF_VARIANTS.forEach { (variant, normalized_char) ->
            normalized = normalized.replace(variant, normalized_char)
        }

        // Normalize other letters
        LETTER_NORMALIZATIONS.forEach { (variant, normalized_char) ->
            normalized = normalized.replace(variant, normalized_char)
        }

        // Apply Uthmani Quran word normalizations
        // Process words individually to handle word-level variations
        val words = normalized.split(Regex("\\s+"))
        normalized = words.joinToString(" ") { word ->
            UTHMANI_WORD_NORMALIZATIONS[word] ?: word
        }

        // Trim and normalize whitespace
        normalized = normalized.trim().replace(Regex("\\s+"), " ")

        return normalized
    }

    /**
     * Normalize and split text into words
     * @param text The Arabic text to split
     * @return List of normalized words
     */
    fun normalizeAndSplit(text: String): List<String> {
        val normalized = normalize(text)
        return normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
    }
}
