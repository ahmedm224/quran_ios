package com.quranmedia.player.domain.util

/**
 * Utility object for converting Western Arabic numerals (0-9) to
 * Eastern Arabic/Indo-Arabic numerals (٠-٩).
 *
 * Indo-Arabic numerals are commonly used in Arabic-speaking countries
 * for displaying numbers in Arabic text.
 */
object ArabicNumeralUtils {

    // Eastern Arabic (Indo-Arabic) digits
    private val INDO_ARABIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    /**
     * Convert a string containing Western Arabic numerals to Indo-Arabic numerals.
     * Non-digit characters are preserved.
     *
     * @param text The input string with Western Arabic numerals
     * @return String with Indo-Arabic numerals
     *
     * Example: "123" -> "١٢٣"
     * Example: "Page 5 of 10" -> "Page ٥ of ١٠"
     */
    fun toIndoArabic(text: String): String {
        val result = StringBuilder(text.length)
        for (char in text) {
            if (char in '0'..'9') {
                result.append(INDO_ARABIC_DIGITS[char - '0'])
            } else {
                result.append(char)
            }
        }
        return result.toString()
    }

    /**
     * Convert an integer to Indo-Arabic numeral string.
     *
     * @param number The integer to convert
     * @return String with Indo-Arabic numerals
     *
     * Example: 123 -> "١٢٣"
     * Example: -45 -> "-٤٥"
     */
    fun toIndoArabic(number: Int): String {
        return toIndoArabic(number.toString())
    }

    /**
     * Convert a long to Indo-Arabic numeral string.
     *
     * @param number The long to convert
     * @return String with Indo-Arabic numerals
     */
    fun toIndoArabic(number: Long): String {
        return toIndoArabic(number.toString())
    }

    /**
     * Convert a float to Indo-Arabic numeral string.
     *
     * @param number The float to convert
     * @return String with Indo-Arabic numerals (decimal point preserved)
     *
     * Example: 3.14 -> "٣.١٤"
     */
    fun toIndoArabic(number: Float): String {
        return toIndoArabic(number.toString())
    }

    /**
     * Convert a double to Indo-Arabic numeral string.
     *
     * @param number The double to convert
     * @return String with Indo-Arabic numerals (decimal point preserved)
     */
    fun toIndoArabic(number: Double): String {
        return toIndoArabic(number.toString())
    }

    /**
     * Conditionally convert numbers based on settings.
     * Use this for app-wide number formatting.
     *
     * @param text The input string with numbers
     * @param useIndoArabic Whether to convert to Indo-Arabic numerals
     * @return Converted string if useIndoArabic is true, otherwise original string
     */
    fun formatNumber(text: String, useIndoArabic: Boolean): String {
        return if (useIndoArabic) toIndoArabic(text) else text
    }

    /**
     * Conditionally convert an integer based on settings.
     *
     * @param number The integer to format
     * @param useIndoArabic Whether to convert to Indo-Arabic numerals
     * @return Converted string if useIndoArabic is true, otherwise standard string
     */
    fun formatNumber(number: Int, useIndoArabic: Boolean): String {
        return if (useIndoArabic) toIndoArabic(number) else number.toString()
    }

    /**
     * Conditionally convert a long based on settings.
     */
    fun formatNumber(number: Long, useIndoArabic: Boolean): String {
        return if (useIndoArabic) toIndoArabic(number) else number.toString()
    }

    /**
     * Format time string (e.g., "5:30") with Indo-Arabic numerals if enabled.
     *
     * @param time The time string to format
     * @param useIndoArabic Whether to convert to Indo-Arabic numerals
     * @return Formatted time string
     *
     * Example: formatTime("5:30", true) -> "٥:٣٠"
     */
    fun formatTime(time: String, useIndoArabic: Boolean): String {
        return formatNumber(time, useIndoArabic)
    }

    /**
     * Format date string with Indo-Arabic numerals if enabled.
     *
     * @param date The date string to format (e.g., "15/3", "2025-03-01")
     * @param useIndoArabic Whether to convert to Indo-Arabic numerals
     * @return Formatted date string
     *
     * Example: formatDate("15/3", true) -> "١٥/٣"
     */
    fun formatDate(date: String, useIndoArabic: Boolean): String {
        return formatNumber(date, useIndoArabic)
    }
}
