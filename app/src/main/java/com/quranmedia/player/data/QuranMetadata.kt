package com.quranmedia.player.data

/**
 * Bundled Quran metadata for page numbers per surah.
 * This ensures page numbers are available immediately without API call.
 * Data based on the Madani Mushaf (standard 604-page Quran).
 */
object QuranMetadata {

    /**
     * Starting page for each surah (1-indexed surah number -> page number)
     * Source: Standard Madani Mushaf page numbering
     */
    val surahStartPages = mapOf(
        1 to 1,    // Al-Fatiha
        2 to 2,    // Al-Baqarah
        3 to 50,   // Aal-E-Imran
        4 to 77,   // An-Nisa
        5 to 106,  // Al-Ma'idah
        6 to 128,  // Al-An'am
        7 to 151,  // Al-A'raf
        8 to 177,  // Al-Anfal
        9 to 187,  // At-Tawbah
        10 to 208, // Yunus
        11 to 221, // Hud
        12 to 235, // Yusuf
        13 to 249, // Ar-Ra'd
        14 to 255, // Ibrahim
        15 to 262, // Al-Hijr
        16 to 267, // An-Nahl
        17 to 282, // Al-Isra
        18 to 293, // Al-Kahf
        19 to 305, // Maryam
        20 to 312, // Ta-Ha
        21 to 322, // Al-Anbiya
        22 to 332, // Al-Hajj
        23 to 342, // Al-Mu'minun
        24 to 350, // An-Nur
        25 to 359, // Al-Furqan
        26 to 367, // Ash-Shu'ara
        27 to 377, // An-Naml
        28 to 385, // Al-Qasas
        29 to 396, // Al-Ankabut
        30 to 404, // Ar-Rum
        31 to 411, // Luqman
        32 to 415, // As-Sajdah
        33 to 418, // Al-Ahzab
        34 to 428, // Saba
        35 to 434, // Fatir
        36 to 440, // Ya-Sin
        37 to 446, // As-Saffat
        38 to 453, // Sad
        39 to 458, // Az-Zumar
        40 to 467, // Ghafir
        41 to 477, // Fussilat
        42 to 483, // Ash-Shura
        43 to 489, // Az-Zukhruf
        44 to 496, // Ad-Dukhan
        45 to 499, // Al-Jathiyah
        46 to 502, // Al-Ahqaf
        47 to 507, // Muhammad
        48 to 511, // Al-Fath
        49 to 515, // Al-Hujurat
        50 to 518, // Qaf
        51 to 520, // Adh-Dhariyat
        52 to 523, // At-Tur
        53 to 526, // An-Najm
        54 to 528, // Al-Qamar
        55 to 531, // Ar-Rahman
        56 to 534, // Al-Waqi'ah
        57 to 537, // Al-Hadid
        58 to 542, // Al-Mujadilah
        59 to 545, // Al-Hashr
        60 to 549, // Al-Mumtahanah
        61 to 551, // As-Saff
        62 to 553, // Al-Jumu'ah
        63 to 554, // Al-Munafiqun
        64 to 556, // At-Taghabun
        65 to 558, // At-Talaq
        66 to 560, // At-Tahrim
        67 to 562, // Al-Mulk
        68 to 564, // Al-Qalam
        69 to 566, // Al-Haqqah
        70 to 568, // Al-Ma'arij
        71 to 570, // Nuh
        72 to 572, // Al-Jinn
        73 to 574, // Al-Muzzammil
        74 to 575, // Al-Muddaththir
        75 to 577, // Al-Qiyamah
        76 to 578, // Al-Insan
        77 to 580, // Al-Mursalat
        78 to 582, // An-Naba
        79 to 583, // An-Nazi'at
        80 to 585, // Abasa
        81 to 586, // At-Takwir
        82 to 587, // Al-Infitar
        83 to 587, // Al-Mutaffifin
        84 to 589, // Al-Inshiqaq
        85 to 590, // Al-Buruj
        86 to 591, // At-Tariq
        87 to 591, // Al-A'la
        88 to 592, // Al-Ghashiyah
        89 to 593, // Al-Fajr
        90 to 594, // Al-Balad
        91 to 595, // Ash-Shams
        92 to 595, // Al-Layl
        93 to 596, // Ad-Duha
        94 to 596, // Ash-Sharh
        95 to 597, // At-Tin
        96 to 597, // Al-Alaq
        97 to 598, // Al-Qadr
        98 to 598, // Al-Bayyinah
        99 to 599, // Az-Zalzalah
        100 to 599, // Al-Adiyat
        101 to 600, // Al-Qari'ah
        102 to 600, // At-Takathur
        103 to 601, // Al-Asr
        104 to 601, // Al-Humazah
        105 to 601, // Al-Fil
        106 to 602, // Quraysh
        107 to 602, // Al-Ma'un
        108 to 602, // Al-Kawthar
        109 to 603, // Al-Kafirun
        110 to 603, // An-Nasr
        111 to 603, // Al-Masad
        112 to 604, // Al-Ikhlas
        113 to 604, // Al-Falaq
        114 to 604  // An-Nas
    )

    /**
     * Juz start information: juz number -> (surahNumber, ayahNumber, page)
     */
    val juzStartInfo = mapOf(
        1 to Triple(1, 1, 1),      // Al-Fatiha 1:1
        2 to Triple(2, 142, 22),   // Al-Baqarah 2:142
        3 to Triple(2, 253, 42),   // Al-Baqarah 2:253
        4 to Triple(3, 93, 62),    // Aal-E-Imran 3:93
        5 to Triple(4, 24, 82),    // An-Nisa 4:24
        6 to Triple(4, 148, 102),  // An-Nisa 4:148
        7 to Triple(5, 83, 121),   // Al-Ma'idah 5:83
        8 to Triple(6, 111, 142),  // Al-An'am 6:111
        9 to Triple(7, 88, 162),   // Al-A'raf 7:88
        10 to Triple(8, 41, 182),  // Al-Anfal 8:41
        11 to Triple(9, 93, 201),  // At-Tawbah 9:93
        12 to Triple(11, 6, 222),  // Hud 11:6
        13 to Triple(12, 53, 242), // Yusuf 12:53
        14 to Triple(15, 1, 262),  // Al-Hijr 15:1
        15 to Triple(17, 1, 282),  // Al-Isra 17:1
        16 to Triple(18, 75, 302), // Al-Kahf 18:75
        17 to Triple(21, 1, 322),  // Al-Anbiya 21:1
        18 to Triple(23, 1, 342),  // Al-Mu'minun 23:1
        19 to Triple(25, 21, 362), // Al-Furqan 25:21
        20 to Triple(27, 56, 382), // An-Naml 27:56
        21 to Triple(29, 46, 402), // Al-Ankabut 29:46
        22 to Triple(33, 31, 422), // Al-Ahzab 33:31
        23 to Triple(36, 28, 442), // Ya-Sin 36:28
        24 to Triple(39, 32, 462), // Az-Zumar 39:32
        25 to Triple(41, 47, 482), // Fussilat 41:47
        26 to Triple(46, 1, 502),  // Al-Ahqaf 46:1
        27 to Triple(51, 31, 522), // Adh-Dhariyat 51:31
        28 to Triple(58, 1, 542),  // Al-Mujadilah 58:1
        29 to Triple(67, 1, 562),  // Al-Mulk 67:1
        30 to Triple(78, 1, 582)   // An-Naba 78:1
    )

    /**
     * Get the page number for a surah (uses static data, no async required)
     */
    fun getPageForSurah(surahNumber: Int): Int {
        return surahStartPages[surahNumber] ?: 1
    }

    /**
     * Estimate the page number for an ayah based on surah position
     * This is an approximation for when full metadata isn't loaded
     */
    fun estimatePageForAyah(surahNumber: Int, ayahNumber: Int, totalAyahsInSurah: Int): Int {
        val startPage = surahStartPages[surahNumber] ?: 1
        val nextSurahPage = surahStartPages[surahNumber + 1] ?: (startPage + 1)
        val surahPageSpan = nextSurahPage - startPage

        if (surahPageSpan <= 1 || totalAyahsInSurah <= 1) {
            return startPage
        }

        // Linear interpolation within the surah
        val progress = (ayahNumber - 1).toFloat() / (totalAyahsInSurah - 1)
        return (startPage + (progress * (surahPageSpan - 1))).toInt().coerceIn(startPage, nextSurahPage - 1)
    }
}
