package com.quranmedia.player.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.BuildConfig
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.domain.model.Reciter
import com.quranmedia.player.domain.model.Surah
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.util.Strings
import com.quranmedia.player.presentation.util.layoutDirection
import com.quranmedia.player.domain.util.ArabicNumeralUtils

// Updated theme colors - Green headers with warm beige background
private val islamicGreen = Color(0xFF2E7D32)      // Green for headers/buttons
private val darkGreen = Color(0xFF1B5E20)         // Dark green for emphasis
private val lightGreen = Color(0xFF4CAF50)        // Light green accent
private val goldAccent = Color(0xFFD4AF37)        // Gold accent
private val creamBackground = Color(0xFFFDFBF7)   // Warm Beige/Cream background
private val coffeeBrown = Color(0xFF3E2723)       // Dark Coffee Brown for body text
private val softWoodBrown = Color(0xFFA1887F)     // Soft Wood Brown for dividers/borders

// Card colors
private val tealColor = Color(0xFF00897B)         // Teal
private val purpleColor = Color(0xFF7E57C2)       // Purple
private val orangeColor = Color(0xFFFF8A65)       // Orange
private val blueColor = Color(0xFF42A5F5)         // Blue
private val greyColor = Color(0xFF78909C)         // Grey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenNew(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToReciters: () -> Unit = {},
    onNavigateToSurahs: () -> Unit = {},
    onNavigateToPlayer: (String, Int, Boolean) -> Unit = { _, _, _ -> },
    onNavigateToBookmarks: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToQuranReader: (Int?) -> Unit = { _ -> },
    onNavigateToQuranIndex: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAthkar: () -> Unit = {},
    onNavigateToPrayerTimes: () -> Unit = {},
    onNavigateToTracker: () -> Unit = {},
    onNavigateToRecite: () -> Unit = {},
    onNavigateToImsakiya: () -> Unit = {}  // TODO: Remove after Ramadan
) {
    val lastPlaybackInfo by viewModel.lastPlaybackInfo.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val reciters by viewModel.reciters.collectAsState()
    val selectedReciter by viewModel.selectedReciter.collectAsState()
    val surahs by viewModel.surahs.collectAsState()
    val language = settings.appLanguage
    val useIndoArabic = language == AppLanguage.ARABIC && settings.useIndoArabicNumerals
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refreshLastPlaybackInfo()
        viewModel.refreshSelectedReciter()
    }

    val navigateToReaderSmart: () -> Unit = {
        coroutineScope.launch {
            val currentPlaybackPage = viewModel.getCurrentPlaybackPage()
            onNavigateToQuranReader(currentPlaybackPage)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        // 3-dot menu state
        var showContextMenu by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                // Compact single-line header with wood theme
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                            ambientColor = darkGreen.copy(alpha = 0.4f),
                            spotColor = darkGreen.copy(alpha = 0.4f)
                        )
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    darkGreen,
                                    islamicGreen,
                                    lightGreen
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // App name
                        Text(
                            text = Strings.appName.get(language),
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // Bismillah in the center (full version)
                        Text(
                            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                            fontFamily = scheherazadeFont,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = goldAccent,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )

                        // Language toggle
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .shadow(3.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable {
                                    val newLang = if (language == AppLanguage.ARABIC)
                                        AppLanguage.ENGLISH else AppLanguage.ARABIC
                                    viewModel.setLanguage(newLang)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (language == AppLanguage.ARABIC) "EN" else "ع",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 3-dot context menu
                        Box {
                            IconButton(
                                onClick = { showContextMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Menu",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showContextMenu,
                                onDismissRequest = { showContextMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                // Settings
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Settings,
                                                contentDescription = null,
                                                tint = islamicGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = Strings.settings.get(language),
                                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                                color = darkGreen
                                            )
                                        }
                                    },
                                    onClick = {
                                        showContextMenu = false
                                        onNavigateToSettings()
                                    }
                                )
                                // Prayer Times
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = islamicGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = if (language == AppLanguage.ARABIC) "مواقيت الصلاة" else "Prayer Times",
                                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                                color = darkGreen
                                            )
                                        }
                                    },
                                    onClick = {
                                        showContextMenu = false
                                        onNavigateToPrayerTimes()
                                    }
                                )
                                // Athkar
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.WbSunny,
                                                contentDescription = null,
                                                tint = islamicGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = if (language == AppLanguage.ARABIC) "الأذكار" else "Athkar",
                                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                                color = darkGreen
                                            )
                                        }
                                    },
                                    onClick = {
                                        showContextMenu = false
                                        onNavigateToAthkar()
                                    }
                                )
                                // Daily Tracker
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = islamicGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = if (language == AppLanguage.ARABIC) "المتابعة اليومية" else "Daily Tracker",
                                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                                color = darkGreen
                                            )
                                        }
                                    },
                                    onClick = {
                                        showContextMenu = false
                                        onNavigateToTracker()
                                    }
                                )
                                // Downloads
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.CloudDownload,
                                                contentDescription = null,
                                                tint = islamicGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = Strings.downloads.get(language),
                                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                                color = darkGreen
                                            )
                                        }
                                    },
                                    onClick = {
                                        showContextMenu = false
                                        onNavigateToDownloads()
                                    }
                                )
                                // About
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Info,
                                                contentDescription = null,
                                                tint = islamicGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = Strings.about.get(language),
                                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                                color = darkGreen
                                            )
                                        }
                                    },
                                    onClick = {
                                        showContextMenu = false
                                        onNavigateToAbout()
                                    }
                                )
                            }
                        }
                    }
                }
            },
            containerColor = creamBackground
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Media Control Panel - always visible for quick access to playback
                MediaControlPanel(
                    isPlaying = playbackState.isPlaying,
                    currentSurah = playbackState.currentSurahNameArabic ?: lastPlaybackInfo?.surah?.nameArabic,
                    currentSurahNumber = playbackState.currentSurah ?: lastPlaybackInfo?.surah?.number,
                    currentAyah = playbackState.currentAyah,
                    totalAyahs = playbackState.totalAyahs,
                    selectedReciter = selectedReciter ?: lastPlaybackInfo?.reciter,
                    reciters = reciters,
                    surahs = surahs,
                    playbackSpeed = playbackState.playbackSpeed,
                    ayahRepeatCount = settings.ayahRepeatCount,
                    language = language,
                    onReciterSelected = { viewModel.selectReciter(it) },
                    onSurahSelected = { viewModel.selectSurah(it) },
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onStopClick = { viewModel.stopPlayback() },
                    onPreviousAyah = { viewModel.previousAyah() },
                    onNextAyah = { viewModel.nextAyah() },
                    onSpeedClick = { viewModel.cyclePlaybackSpeed() },
                    onRepeatClick = { viewModel.cycleAyahRepeatCount() },
                    onOpenReader = navigateToReaderSmart,
                    useIndoArabic = useIndoArabic
                )

                // Main Quran Card - MAIN FEATURE - Always at top after media controller
                MainQuranCard(
                    language = language,
                    onClick = onNavigateToQuranIndex
                )

                // Prayer Times Card - Long card for prominence
                FeatureCard(
                    title = if (language == AppLanguage.ARABIC) "مواقيت الصلاة" else "Prayer Times",
                    icon = Icons.Default.Schedule,
                    cardColor = coffeeBrown,
                    language = language,
                    onClick = onNavigateToPrayerTimes,
                    modifier = Modifier.fillMaxWidth()
                )

                // Ramadan Imsakiya (TODO: Remove after Ramadan)
                FeatureCard(
                    title = if (language == AppLanguage.ARABIC) "إمساكية رمضان" else "Ramadan Imsakiya",
                    icon = Icons.Default.NightsStay,
                    cardColor = Color(0xFF1A1A2E), // Ramadan dark purple
                    language = language,
                    onClick = onNavigateToImsakiya,
                    modifier = Modifier.fillMaxWidth()
                )

                // Athkar and Daily Tracker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureCard(
                        title = if (language == AppLanguage.ARABIC) "الأذكار" else "Athkar",
                        icon = Icons.Default.WbSunny,
                        cardColor = tealColor,
                        language = language,
                        onClick = onNavigateToAthkar,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        title = if (language == AppLanguage.ARABIC) "المتابعة اليومية" else "Daily Tracker",
                        icon = Icons.Default.CheckCircle,
                        cardColor = Color(0xFF00897B), // Teal color
                        language = language,
                        onClick = onNavigateToTracker,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Bookmarks and Downloads
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureCard(
                        title = Strings.bookmarks.get(language),
                        icon = Icons.Default.Bookmark,
                        cardColor = orangeColor,
                        language = language,
                        onClick = onNavigateToBookmarks,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        title = Strings.downloads.get(language),
                        icon = Icons.Default.CloudDownload,
                        cardColor = lightGreen,
                        language = language,
                        onClick = onNavigateToDownloads,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun MediaControlPanel(
    isPlaying: Boolean,
    currentSurah: String?,
    currentSurahNumber: Int?,
    currentAyah: Int?,
    totalAyahs: Int?,
    selectedReciter: Reciter?,
    reciters: List<Reciter>,
    surahs: List<Surah>,
    playbackSpeed: Float,
    ayahRepeatCount: Int,
    language: AppLanguage,
    onReciterSelected: (Reciter) -> Unit,
    onSurahSelected: (Surah) -> Unit,
    onPlayPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    onPreviousAyah: () -> Unit,
    onNextAyah: () -> Unit,
    onSpeedClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onOpenReader: () -> Unit,
    useIndoArabic: Boolean = false
) {
    var showReciterMenu by remember { mutableStateOf(false) }
    var showSurahMenu by remember { mutableStateOf(false) }

    val hasActivePlayback = currentAyah != null

    val reciterDisplayName = if (selectedReciter != null) {
        if (language == AppLanguage.ARABIC) {
            selectedReciter.nameArabic?.takeIf { it.isNotBlank() && !it.all { c -> c == '.' || c == ' ' } }
                ?: selectedReciter.name
        } else {
            selectedReciter.name
        }
    } else {
        if (language == AppLanguage.ARABIC) "قارئ" else "Reciter"
    }

    val currentSurahObj = surahs.find { it.number == currentSurahNumber }
    val surahDisplayName = if (currentSurahObj != null) {
        if (language == AppLanguage.ARABIC) currentSurahObj.nameArabic else currentSurahObj.nameEnglish
    } else {
        if (language == AppLanguage.ARABIC) "سورة" else "Surah"
    }

    val sortedReciters = remember(reciters, language) {
        reciters.sortedBy { if (language == AppLanguage.ARABIC) it.nameArabic ?: it.name else it.name }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = islamicGreen.copy(alpha = 0.3f),
                spotColor = islamicGreen.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            islamicGreen.copy(alpha = 0.08f),
                            islamicGreen.copy(alpha = 0.02f)
                        )
                    )
                )
                .padding(10.dp)
        ) {
            // Top row: Reciter and Surah selectors + Open reader button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Reciter dropdown - compact
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedCard(
                        onClick = { showReciterMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = islamicGreen.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, null, tint = islamicGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                reciterDisplayName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = darkGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (showReciterMenu) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                null, tint = islamicGreen, modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showReciterMenu,
                        onDismissRequest = { showReciterMenu = false },
                        modifier = Modifier.heightIn(max = 300.dp).background(Color.White)
                    ) {
                        sortedReciters.forEach { reciter ->
                            val isSelected = reciter.id == selectedReciter?.id
                            val displayName = if (language == AppLanguage.ARABIC) {
                                reciter.nameArabic?.takeIf { it.isNotBlank() && !it.all { c -> c == '.' || c == ' ' } }
                                    ?: reciter.name
                            } else reciter.name
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        displayName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) islamicGreen else Color.DarkGray
                                    )
                                },
                                onClick = { onReciterSelected(reciter); showReciterMenu = false },
                                leadingIcon = if (isSelected) {{ Icon(Icons.Default.Check, null, tint = islamicGreen, modifier = Modifier.size(16.dp)) }} else null
                            )
                        }
                    }
                }

                // Surah dropdown - compact
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedCard(
                        onClick = { showSurahMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = islamicGreen.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LibraryMusic, null, tint = islamicGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                surahDisplayName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                color = darkGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (showSurahMenu) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                null, tint = islamicGreen, modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showSurahMenu,
                        onDismissRequest = { showSurahMenu = false },
                        modifier = Modifier.heightIn(max = 350.dp).background(Color.White)
                    ) {
                        surahs.forEach { surah ->
                            val isSelected = surah.number == currentSurahNumber
                            val displayName = if (language == AppLanguage.ARABIC) {
                                "${ArabicNumeralUtils.formatNumber(surah.number, useIndoArabic)}. ${surah.nameArabic}"
                            } else {
                                "${surah.number}. ${surah.nameEnglish}"
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        displayName,
                                        fontSize = 13.sp,
                                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) islamicGreen else Color.DarkGray
                                    )
                                },
                                onClick = { onSurahSelected(surah); showSurahMenu = false },
                                leadingIcon = if (isSelected) {{ Icon(Icons.Default.Check, null, tint = islamicGreen, modifier = Modifier.size(16.dp)) }} else null
                            )
                        }
                    }
                }

                // Open reader button - compact
                FilledIconButton(
                    onClick = onOpenReader,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = islamicGreen.copy(alpha = 0.15f),
                        contentColor = islamicGreen
                    ),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.MenuBook, "Open Reader", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom row: Ayah info and controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ayah info - compact
                Column(modifier = Modifier.weight(1f)) {
                    if (currentAyah != null) {
                        val formattedAyah = ArabicNumeralUtils.formatNumber(currentAyah, useIndoArabic)
                        val formattedTotal = totalAyahs?.let { ArabicNumeralUtils.formatNumber(it, useIndoArabic) }
                        Text(
                            text = if (language == AppLanguage.ARABIC)
                                "آية $formattedAyah${formattedTotal?.let { " / $it" } ?: ""}"
                            else
                                "Ayah $currentAyah${totalAyahs?.let { " / $it" } ?: ""}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = darkGreen
                        )
                    } else {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "اضغط للتشغيل" else "Tap to play",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // =============================================================
                // MEDIA CONTROLS - RTL HANDLING
                // =============================================================
                // IMPORTANT: Media controls must be kept in LTR layout to prevent
                // the system from mirroring button positions. The icons should
                // always point OUTWARD from the play button:
                //   - Left button: SkipPrevious (<<) pointing left
                //   - Right button: SkipNext (>>) pointing right
                //
                // However, the ACTIONS must swap for RTL (Arabic):
                //   - In LTR (English): Left = Previous, Right = Next
                //   - In RTL (Arabic): Left = Next, Right = Previous
                //
                // This is because in Arabic, "forward" in the Quran is to the LEFT
                // (right-to-left reading direction), so the left button advances.
                // =============================================================
                val isRtl = language == AppLanguage.ARABIC
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (hasActivePlayback) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(islamicGreen.copy(alpha = 0.1f))
                                    .clickable { onSpeedClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${playbackSpeed}x",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = islamicGreen
                                )
                            }

                            // Repeat control - compact
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (ayahRepeatCount > 1) islamicGreen.copy(alpha = 0.2f) else islamicGreen.copy(alpha = 0.1f))
                                    .clickable { onRepeatClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${ayahRepeatCount}×",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ayahRepeatCount > 1) islamicGreen else Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Left button - SkipPrevious icon (<<), action swaps for RTL
                            IconButton(
                                onClick = if (isRtl) onNextAyah else onPreviousAyah,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.SkipPrevious,
                                    if (isRtl) "Next" else "Previous",
                                    tint = islamicGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        FilledIconButton(
                            onClick = onPlayPauseClick,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = islamicGreen,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                "Play/Pause",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        if (hasActivePlayback) {
                            // Right button - SkipNext icon (>>), action swaps for RTL
                            IconButton(
                                onClick = if (isRtl) onPreviousAyah else onNextAyah,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.SkipNext,
                                    if (isRtl) "Previous" else "Next",
                                    tint = islamicGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(onClick = onStopClick, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Stop, "Stop", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainQuranCard(
    language: AppLanguage,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp) // Slightly taller to stand out as main card
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = darkGreen.copy(alpha = 0.4f),
                spotColor = islamicGreen.copy(alpha = 0.4f)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            islamicGreen.copy(alpha = 0.18f),
                            lightGreen.copy(alpha = 0.08f),
                            goldAccent.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .shadow(5.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    lightGreen,
                                    islamicGreen,
                                    darkGreen
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = goldAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.quran.get(language),
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = if (language == AppLanguage.ARABIC) 20.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${Strings.readAndListen.get(language)}",
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 10.sp,
                        color = Color.Gray.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(lightGreen, islamicGreen)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (language == AppLanguage.ARABIC) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    icon: ImageVector,
    cardColor: Color,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(62.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = cardColor.copy(alpha = 0.3f),
                spotColor = cardColor.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            cardColor.copy(alpha = 0.15f),
                            cardColor.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(4.dp, RoundedCornerShape(11.dp))
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                cardColor.copy(alpha = 0.9f),
                                cardColor
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(9.dp))

            Text(
                text = title,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = if (language == AppLanguage.ARABIC) 18.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                color = cardColor.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
