package com.quranmedia.player.domain.model

/**
 * Domain model for an Athan (call to prayer) recording
 */
data class Athan(
    val id: String,
    val name: String,
    val muezzin: String,
    val location: String,
    val audioUrl: String
)

/**
 * Domain model for a Muezzin (person who calls to prayer)
 */
data class Muezzin(
    val name: String,
    val location: String,
    val athanCount: Int
)
