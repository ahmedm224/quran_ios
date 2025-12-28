package com.quranmedia.player.domain.model

/**
 * Settings for custom group recitation and repeating.
 * Allows users to define a range of ayahs to recite with custom repeat and speed settings.
 */
data class CustomRecitationSettings(
    val startSurahNumber: Int,
    val startAyahNumber: Int,
    val endSurahNumber: Int,
    val endAyahNumber: Int,
    val ayahRepeatCount: Int, // 1-5 or UNLIMITED (-1)
    val groupRepeatCount: Int, // 1-5 or UNLIMITED (-1)
    val speed: Float = 1.0f // 1.0x, 1.25x, 1.5x, 2.0x
) {
    companion object {
        const val UNLIMITED = -1
        const val MIN_REPEAT = 1
        const val MAX_REPEAT = 5

        val SPEED_OPTIONS = listOf(1.0f, 1.25f, 1.5f, 2.0f)
    }

    /**
     * Validates that the settings form a valid ayah range.
     * Note: Custom recitation is limited to a single surah only.
     */
    fun isValid(): Boolean {
        // Must be within a single surah
        if (startSurahNumber != endSurahNumber) return false

        // Start ayah must be before or equal to end ayah
        if (startAyahNumber > endAyahNumber) return false

        // Surah number must be valid (1-114)
        if (startSurahNumber !in 1..114) return false

        // Ayah numbers must be positive
        if (startAyahNumber < 1 || endAyahNumber < 1) return false

        // Repeat counts must be valid (1-5 or UNLIMITED)
        if (ayahRepeatCount != UNLIMITED && ayahRepeatCount !in MIN_REPEAT..MAX_REPEAT) return false
        if (groupRepeatCount != UNLIMITED && groupRepeatCount !in MIN_REPEAT..MAX_REPEAT) return false

        // Speed must be valid
        if (speed !in SPEED_OPTIONS) return false

        return true
    }
}

/**
 * Preset slot for saving custom recitation settings.
 */
data class RecitationPreset(
    val name: String,
    val settings: CustomRecitationSettings
)
