package com.quranmedia.player.domain.util

/**
 * Represents a segment of text with an optional Tajweed rule
 * Used for word-based Tajweed rendering with proper color coding
 */
data class TajweedWord(
    val text: String,        // The Arabic text (may include Tashkeel and Kashida)
    val rule: String? = null // Tajweed rule code (e.g., "ghunnah", "qalqalah"), null for uncolored text
)
