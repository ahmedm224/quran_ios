package com.quranmedia.player.domain.model

data class Reciter(
    val id: String,
    val name: String,
    val nameArabic: String?,
    val style: String?,
    val version: String,
    val imageUrl: String?
)
