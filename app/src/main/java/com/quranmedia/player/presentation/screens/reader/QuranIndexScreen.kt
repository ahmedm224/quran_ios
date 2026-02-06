package com.quranmedia.player.presentation.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.database.dao.HizbQuarterInfo
import com.quranmedia.player.data.database.dao.JuzStartInfo
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.domain.model.Surah
import com.quranmedia.player.presentation.screens.reader.components.creamBackground
import com.quranmedia.player.presentation.screens.reader.components.darkGreen
import com.quranmedia.player.presentation.screens.reader.components.goldAccent
import com.quranmedia.player.presentation.screens.reader.components.islamicGreen
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.util.Strings
import com.quranmedia.player.presentation.util.layoutDirection
import com.quranmedia.player.domain.util.ArabicNumeralUtils
import kotlinx.coroutines.launch
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun QuranIndexScreen(
    viewModel: QuranIndexViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToPage: (Int) -> Unit,
    onNavigateToPageWithHighlight: (page: Int, surahNumber: Int, ayahNumber: Int) -> Unit = { _, _, _ -> },
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPrayerTimes: () -> Unit = {},
    onNavigateToAthkar: () -> Unit = {},
    onNavigateToTracker: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToImsakiya: () -> Unit = {}  // TODO: Remove after Ramadan
) {
    val state by viewModel.state.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val language = settings.appLanguage
    val useIndoArabic = language == AppLanguage.ARABIC && settings.useIndoArabicNumerals
    var pageInput by remember { mutableStateOf("") }
    var pageInputError by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Search state
    var isSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Tab state
    val pagerState = rememberPagerState(initialPage = 0) { 3 }

    // Tab titles
    val tabTitles = listOf(
        if (language == AppLanguage.ARABIC) "الفهرس" else "Index",
        if (language == AppLanguage.ARABIC) "الأجزاء" else "Juz",
        if (language == AppLanguage.ARABIC) "العلامات" else "Bookmarks"
    )

    // Focus on search field when entering search mode
    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            focusRequester.requestFocus()
        }
    }

    // Layout based on language
    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (isSearchMode) {
                            // Search text field in header
                            TextField(
                                value = searchQuery,
                                onValueChange = { query ->
                                    searchQuery = query
                                    viewModel.searchQuran(query)
                                },
                                placeholder = {
                                    Text(
                                        text = if (language == AppLanguage.ARABIC) "ابحث في القرآن..." else "Search Quran...",
                                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = scheherazadeFont,
                                    fontSize = 18.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Start
                                ),
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = {
                                            searchQuery = ""
                                            viewModel.clearSearch()
                                        }) {
                                            Icon(Icons.Default.Clear, "Clear", tint = Color.White)
                                        }
                                    }
                                }
                            )
                        } else {
                            Text(
                                text = Strings.quran.get(language),
                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isSearchMode) {
                                isSearchMode = false
                                searchQuery = ""
                                viewModel.clearSearch()
                            } else {
                                onBack()
                            }
                        }) {
                            Icon(
                                if (isSearchMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (!isSearchMode) {
                            IconButton(onClick = { isSearchMode = true }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = if (language == AppLanguage.ARABIC) "بحث" else "Search",
                                    tint = Color.White
                                )
                            }
                            // 3-dot context menu
                            var showContextMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showContextMenu = true }) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "Menu",
                                        tint = Color.White
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
                                    // Ramadan Imsakiya (TODO: Remove after Ramadan)
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.NightsStay,
                                                    contentDescription = null,
                                                    tint = islamicGreen,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = if (language == AppLanguage.ARABIC) "إمساكية رمضان" else "Ramadan Imsakiya",
                                                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                                    color = darkGreen
                                                )
                                            }
                                        },
                                        onClick = {
                                            showContextMenu = false
                                            onNavigateToImsakiya()
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = islamicGreen,
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = creamBackground
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Show search results when in search mode
                if (isSearchMode) {
                    SearchResultsContent(
                        searchResults = state.searchResults,
                        isSearching = state.isSearching,
                        searchQuery = searchQuery,
                        language = language,
                        onResultClick = { result ->
                            onNavigateToPageWithHighlight(
                                result.ayah.page,
                                result.ayah.surahNumber,
                                result.ayah.ayahNumber
                            )
                        }
                    )
                } else {
                    // Tab Row
                    TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = islamicGreen,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = goldAccent
                        )
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = when (index) {
                                            0 -> Icons.Default.MenuBook
                                            1 -> Icons.AutoMirrored.Filled.List
                                            else -> Icons.Default.Bookmark
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = title,
                                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                        fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                    // Show bookmark count badge
                                    if (index == 2 && state.readingBookmarks.isNotEmpty()) {
                                        Badge(
                                            containerColor = goldAccent,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = state.readingBookmarks.size.toString(),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            },
                            selectedContentColor = Color.White,
                            unselectedContentColor = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                // Pager content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> {
                            // Index Tab
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Page number input field
                                PageInputSection(
                                    pageInput = pageInput,
                                    onPageInputChange = {
                                        pageInput = it
                                        pageInputError = false
                                    },
                                    totalPages = state.totalPages,
                                    onNavigateToPage = { pageNum ->
                                        if (pageNum in 1..state.totalPages) {
                                            onNavigateToPage(pageNum)
                                        } else {
                                            pageInputError = true
                                        }
                                    },
                                    isError = pageInputError,
                                    language = language
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = islamicGreen.copy(alpha = 0.2f)
                                )

                                // Cascaded Surah list with Juz markers
                                CascadedSurahJuzList(
                                    surahsWithPages = state.surahsWithPages,
                                    juzStartInfo = state.juzStartInfo,
                                    language = language,
                                    onSurahClick = { startPage ->
                                        onNavigateToPage(startPage)
                                    },
                                    onJuzClick = { juzPage ->
                                        onNavigateToPage(juzPage)
                                    },
                                    useIndoArabic = useIndoArabic
                                )
                            }
                        }
                        1 -> {
                            // Juz Detail Tab
                            JuzDetailTab(
                                hizbQuartersInfo = state.hizbQuartersInfo,
                                surahs = state.surahs,
                                language = language,
                                onNavigateToPageWithHighlight = onNavigateToPageWithHighlight,
                                useIndoArabic = useIndoArabic
                            )
                        }
                        2 -> {
                            // Bookmarks Tab
                            ReadingBookmarksTab(
                                bookmarks = state.readingBookmarks,
                                language = language,
                                dailyTargetPages = state.dailyTargetPages,
                                onBookmarkClick = { bookmark ->
                                    onNavigateToPage(bookmark.pageNumber)
                                },
                                onDeleteBookmark = { bookmarkId ->
                                    viewModel.deleteReadingBookmark(bookmarkId)
                                },
                                useIndoArabic = useIndoArabic
                            )
                        }
                    }
                }
                } // end else (not search mode)
            }
        }
    }
}

@Composable
private fun SearchResultsContent(
    searchResults: List<SearchResultWithPage>,
    isSearching: Boolean,
    searchQuery: String,
    language: AppLanguage,
    onResultClick: (SearchResultWithPage) -> Unit
) {
    when {
        isSearching -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = islamicGreen)
            }
        }
        searchQuery.trim().length < 2 -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = islamicGreen.copy(alpha = 0.4f)
                    )
                    Text(
                        text = if (language == AppLanguage.ARABIC) "اكتب كلمتين على الأقل للبحث" else "Type at least 2 characters to search",
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        searchResults.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (language == AppLanguage.ARABIC) "لا توجد نتائج" else "No results found",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
        else -> {
            Column(modifier = Modifier.fillMaxSize()) {
                // Results count
                Text(
                    text = if (language == AppLanguage.ARABIC) "${searchResults.size} نتيجة" else "${searchResults.size} results",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(searchResults) { result ->
                        SearchResultItem(
                            result = result,
                            language = language,
                            onClick = { onResultClick(result) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    result: SearchResultWithPage,
    language: AppLanguage,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Surah and ayah info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${result.surahName} : ${result.ayah.ayahNumber}",
                    fontFamily = scheherazadeFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = islamicGreen
                )
                Text(
                    text = if (language == AppLanguage.ARABIC) "ص ${result.ayah.page}" else "p. ${result.ayah.page}",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Ayah text (truncated)
            Text(
                text = result.ayah.textArabic,
                fontFamily = scheherazadeFont,
                fontSize = 16.sp,
                color = darkGreen,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PageInputSection(
    pageInput: String,
    onPageInputChange: (String) -> Unit,
    totalPages: Int,
    onNavigateToPage: (Int) -> Unit,
    isError: Boolean,
    language: AppLanguage
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = Strings.goToPage.get(language),
            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = darkGreen
        )

        // Input field - use LTR for number input
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            OutlinedTextField(
                value = pageInput,
                onValueChange = { value ->
                    if (value.isEmpty() || value.all { it.isDigit() }) {
                        onPageInputChange(value)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                placeholder = {
                    Text(
                        text = "1 - $totalPages",
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        pageInput.toIntOrNull()?.let { onNavigateToPage(it) }
                    }
                ),
                singleLine = true,
                isError = isError,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = islamicGreen,
                    unfocusedBorderColor = islamicGreen.copy(alpha = 0.5f),
                    cursorColor = islamicGreen,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 16.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        // Go button
        FilledIconButton(
            onClick = {
                pageInput.toIntOrNull()?.let { onNavigateToPage(it) }
            },
            enabled = pageInput.isNotEmpty(),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = islamicGreen,
                contentColor = Color.White
            ),
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = Strings.go.get(language),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CascadedSurahJuzList(
    surahsWithPages: List<SurahWithPage>,
    juzStartInfo: List<JuzStartInfo>,
    language: AppLanguage,
    onSurahClick: (Int) -> Unit,
    onJuzClick: (Int) -> Unit,
    useIndoArabic: Boolean = false
) {
    // Juz names (Arabic only - these are traditional names)
    val juzNames = mapOf(
        1 to "آلم", 2 to "سيقول", 3 to "تلك الرسل", 4 to "لن تنالوا", 5 to "والمحصنات",
        6 to "لا يحب الله", 7 to "وإذا سمعوا", 8 to "ولو أننا", 9 to "قال الملأ", 10 to "واعلموا",
        11 to "يعتذرون", 12 to "وما من دابة", 13 to "وما أبرئ", 14 to "ربما", 15 to "سبحان الذي",
        16 to "قال ألم", 17 to "اقترب", 18 to "قد أفلح", 19 to "وقال الذين", 20 to "أمن خلق",
        21 to "اتل ما أوحي", 22 to "ومن يقنت", 23 to "وما لي", 24 to "فمن أظلم", 25 to "إليه يرد",
        26 to "حم", 27 to "قال فما خطبكم", 28 to "قد سمع الله", 29 to "تبارك", 30 to "عم"
    )

    // Build a combined list of items (juz dividers and surahs) sorted by page
    data class ListItem(
        val isJuz: Boolean,
        val page: Int,
        val juzInfo: JuzStartInfo? = null,
        val surahWithPage: SurahWithPage? = null
    )

    val combinedItems = remember(surahsWithPages, juzStartInfo) {
        val items = mutableListOf<ListItem>()

        // Add all juz dividers
        juzStartInfo.forEach { juz ->
            items.add(ListItem(isJuz = true, page = juz.page, juzInfo = juz))
        }

        // Add all surahs
        surahsWithPages.forEach { surah ->
            items.add(ListItem(isJuz = false, page = surah.startPage, surahWithPage = surah))
        }

        // Sort by page, with juz dividers before surahs on the same page
        items.sortedWith(compareBy({ it.page }, { !it.isJuz }))
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        combinedItems.forEach { item ->
            if (item.isJuz && item.juzInfo != null) {
                item(key = "juz_${item.juzInfo.juz}") {
                    JuzDivider(
                        juzNumber = item.juzInfo.juz,
                        juzName = juzNames[item.juzInfo.juz] ?: "",
                        startPage = item.juzInfo.page,
                        language = language,
                        onClick = { onJuzClick(item.juzInfo.page) },
                        useIndoArabic = useIndoArabic
                    )
                }
            } else if (!item.isJuz && item.surahWithPage != null) {
                val surah = item.surahWithPage.surah
                val startPage = item.surahWithPage.startPage

                item(key = "surah_${surah.number}") {
                    SurahListItem(
                        surahNumber = surah.number,
                        nameArabic = surah.nameArabic,
                        nameEnglish = surah.nameEnglish,
                        ayahCount = surah.ayahCount,
                        revelationType = surah.revelationType.name,
                        startPage = startPage,
                        language = language,
                        onClick = { onSurahClick(startPage) },
                        useIndoArabic = useIndoArabic
                    )
                }
            }
        }
    }
}

@Composable
private fun JuzDivider(
    juzNumber: Int,
    juzName: String,
    startPage: Int,
    language: AppLanguage,
    onClick: () -> Unit,
    useIndoArabic: Boolean = false
) {
    val formattedJuzNumber = ArabicNumeralUtils.formatNumber(juzNumber, useIndoArabic)
    val formattedPage = ArabicNumeralUtils.formatNumber(startPage, useIndoArabic)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = goldAccent.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Juz info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Juz badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(goldAccent)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "الجزء $formattedJuzNumber" else "Juz $juzNumber",
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Juz name (always Arabic - traditional name)
                if (juzName.isNotEmpty()) {
                    Text(
                        text = juzName,
                        fontFamily = scheherazadeFont,
                        fontSize = 16.sp,
                        color = darkGreen
                    )
                }
            }

            // Page number
            Text(
                text = if (language == AppLanguage.ARABIC) "ص $formattedPage" else "p. $startPage",
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun SurahListItem(
    surahNumber: Int,
    nameArabic: String,
    nameEnglish: String,
    ayahCount: Int,
    revelationType: String,
    startPage: Int,
    language: AppLanguage,
    onClick: () -> Unit,
    useIndoArabic: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Surah number badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(islamicGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ArabicNumeralUtils.formatNumber(surahNumber, useIndoArabic),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkGreen
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Surah names and info
            Column(modifier = Modifier.weight(1f)) {
                // Primary name based on language (only show one language)
                Text(
                    text = if (language == AppLanguage.ARABIC) nameArabic else nameEnglish,
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkGreen
                )

                // Secondary info (ayah count and revelation type only - no secondary name)
                val ayahLabel = if (language == AppLanguage.ARABIC) "آية" else "ayahs"
                val typeLabel = if (language == AppLanguage.ARABIC) {
                    if (revelationType == "MECCAN") "مكية" else "مدنية"
                } else {
                    if (revelationType == "MECCAN") "Meccan" else "Medinan"
                }

                Text(
                    text = "${ArabicNumeralUtils.formatNumber(ayahCount, useIndoArabic)} $ayahLabel • $typeLabel",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Page number
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (language == AppLanguage.ARABIC) "صفحة" else "Page",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Text(
                    text = ArabicNumeralUtils.formatNumber(startPage, useIndoArabic),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = islamicGreen
                )
            }
        }
    }
}

@Composable
private fun JuzDetailTab(
    hizbQuartersInfo: List<HizbQuarterInfo>,
    surahs: List<Surah>,
    language: AppLanguage,
    onNavigateToPageWithHighlight: (page: Int, surahNumber: Int, ayahNumber: Int) -> Unit,
    useIndoArabic: Boolean = false
) {
    // Create a map of surah numbers to names
    val surahNames = remember(surahs) {
        surahs.associate { it.number to (it.nameArabic to it.nameEnglish) }
    }

    if (hizbQuartersInfo.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = islamicGreen)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            var currentJuz = 0
            var currentHizb = 0

            hizbQuartersInfo.forEach { quarterInfo ->
                // Calculate juz, hizb, and quarter position
                val juz = (quarterInfo.hizbQuarter - 1) / 8 + 1
                val hizb = (quarterInfo.hizbQuarter - 1) / 4 + 1
                val quarterIndex = (quarterInfo.hizbQuarter - 1) % 4

                // Add Juz header if new juz
                if (juz != currentJuz) {
                    currentJuz = juz
                    item(key = "juz_header_$juz") {
                        JuzHeaderItem(
                            juzNumber = juz,
                            language = language,
                            onClick = {
                                onNavigateToPageWithHighlight(
                                    quarterInfo.page,
                                    quarterInfo.surahNumber,
                                    quarterInfo.ayahNumber
                                )
                            },
                            useIndoArabic = useIndoArabic
                        )
                    }
                }

                // Add Hizb header if new hizb (and it's the start of hizb - quarterIndex == 0)
                if (hizb != currentHizb && quarterIndex == 0) {
                    currentHizb = hizb
                    item(key = "hizb_header_$hizb") {
                        HizbHeaderItem(
                            hizbNumber = hizb,
                            language = language,
                            useIndoArabic = useIndoArabic
                        )
                    }
                }

                // Add quarter item
                item(key = "quarter_${quarterInfo.hizbQuarter}") {
                    val surahNamePair = surahNames[quarterInfo.surahNumber]
                    val surahName = if (language == AppLanguage.ARABIC) {
                        surahNamePair?.first ?: ""
                    } else {
                        surahNamePair?.second ?: ""
                    }

                    HizbQuarterItem(
                        quarterIndex = quarterIndex,
                        surahName = surahName,
                        ayahNumber = quarterInfo.ayahNumber,
                        page = quarterInfo.page,
                        ayahText = quarterInfo.textArabic.take(50) + if (quarterInfo.textArabic.length > 50) "..." else "",
                        language = language,
                        onClick = {
                            onNavigateToPageWithHighlight(
                                quarterInfo.page,
                                quarterInfo.surahNumber,
                                quarterInfo.ayahNumber
                            )
                        },
                        useIndoArabic = useIndoArabic
                    )
                }
            }
        }
    }
}

@Composable
private fun JuzHeaderItem(
    juzNumber: Int,
    language: AppLanguage,
    onClick: () -> Unit,
    useIndoArabic: Boolean = false
) {
    val formattedNumber = ArabicNumeralUtils.formatNumber(juzNumber, useIndoArabic)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = islamicGreen
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (language == AppLanguage.ARABIC) "الجزء $formattedNumber" else "Juz $juzNumber",
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun HizbHeaderItem(
    hizbNumber: Int,
    language: AppLanguage,
    useIndoArabic: Boolean = false
) {
    val formattedNumber = ArabicNumeralUtils.formatNumber(hizbNumber, useIndoArabic)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = goldAccent.copy(alpha = 0.5f)
        )
        Text(
            text = if (language == AppLanguage.ARABIC) "  الحزب $formattedNumber  " else "  Hizb $hizbNumber  ",
            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = goldAccent
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = goldAccent.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun QuarterPieIcon(
    quarterIndex: Int,
    modifier: Modifier = Modifier,
    fillColor: Color = goldAccent,
    backgroundColor: Color = goldAccent.copy(alpha = 0.2f)
) {
    // quarterIndex: 0 = full (start of hizb), 1 = 1/4, 2 = 1/2, 3 = 3/4
    val sweepAngle = when (quarterIndex) {
        0 -> 360f  // Full circle for start of hizb
        1 -> 90f   // 1/4
        2 -> 180f  // 1/2
        3 -> 270f  // 3/4
        else -> 0f
    }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val radius = size.minDimension / 2
        val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)

        // Draw background circle
        drawCircle(
            color = backgroundColor,
            radius = radius,
            center = center
        )

        // Draw filled arc (pie slice)
        if (sweepAngle > 0) {
            drawArc(
                color = fillColor,
                startAngle = -90f, // Start from top
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = androidx.compose.ui.geometry.Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
        }

        // Draw border
        drawCircle(
            color = fillColor,
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
private fun HizbQuarterItem(
    quarterIndex: Int,
    surahName: String,
    ayahNumber: Int,
    page: Int,
    ayahText: String,
    language: AppLanguage,
    onClick: () -> Unit,
    useIndoArabic: Boolean = false
) {
    val formattedAyah = ArabicNumeralUtils.formatNumber(ayahNumber, useIndoArabic)
    val formattedPage = ArabicNumeralUtils.formatNumber(page, useIndoArabic)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quarter pie icon
            QuarterPieIcon(
                quarterIndex = quarterIndex,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Surah and ayah info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = surahName,
                        fontFamily = scheherazadeFont,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkGreen
                    )
                    Text(
                        text = if (language == AppLanguage.ARABIC) "آية $formattedAyah" else "Ayah $ayahNumber",
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Ayah text preview (always RTL for Arabic text)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = ayahText,
                        fontFamily = scheherazadeFont,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
            }

            // Page number
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (language == AppLanguage.ARABIC) "ص" else "p.",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Text(
                    text = formattedPage,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = islamicGreen
                )
            }
        }
    }
}

@Composable
private fun ReadingBookmarksTab(
    bookmarks: List<IndexReadingBookmark>,
    language: AppLanguage,
    dailyTargetPages: Float?,
    onBookmarkClick: (IndexReadingBookmark) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    useIndoArabic: Boolean = false
) {
    if (bookmarks.isEmpty()) {
        // Empty state
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = islamicGreen.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = if (language == AppLanguage.ARABIC) "لا توجد علامات محفوظة" else "No bookmarks yet",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = darkGreen
                )
                Text(
                    text = if (language == AppLanguage.ARABIC)
                        "اضغط على أيقونة العلامة أثناء القراءة لحفظ الصفحة"
                    else
                        "Tap the bookmark icon while reading to save a page",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(bookmarks, key = { it.id }) { bookmark ->
                BookmarkListItem(
                    bookmark = bookmark,
                    language = language,
                    dailyTargetPages = dailyTargetPages,
                    onClick = { onBookmarkClick(bookmark) },
                    onDelete = { onDeleteBookmark(bookmark.id) },
                    useIndoArabic = useIndoArabic
                )
            }
        }
    }
}

@Composable
private fun BookmarkListItem(
    bookmark: IndexReadingBookmark,
    language: AppLanguage,
    dailyTargetPages: Float?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    useIndoArabic: Boolean = false
) {
    val formattedPageNumber = ArabicNumeralUtils.formatNumber(bookmark.pageNumber, useIndoArabic)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bookmark icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(goldAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = goldAccent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Bookmark info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (language == AppLanguage.ARABIC) "صفحة $formattedPageNumber" else "Page ${bookmark.pageNumber}",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkGreen
                )
                bookmark.surahName?.let { surahName ->
                    Text(
                        text = surahName,
                        fontFamily = scheherazadeFont,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                // Daily target information (detailed)
                dailyTargetPages?.let { target ->
                    Spacer(modifier = Modifier.height(6.dp))

                    // Calculate pages remaining from this bookmark
                    val pagesRemaining = 604 - bookmark.pageNumber
                    val daysNeeded = if (target > 0) (pagesRemaining / target).toInt() else 0
                    val formattedTarget = ArabicNumeralUtils.formatNumber(String.format("%.0f", target).toInt(), useIndoArabic)
                    val formattedRemaining = ArabicNumeralUtils.formatNumber(pagesRemaining, useIndoArabic)
                    val formattedDays = ArabicNumeralUtils.formatNumber(daysNeeded, useIndoArabic)

                    // Daily target display
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = goldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (language == AppLanguage.ARABIC)
                                "الهدف اليومي: $formattedTarget صفحة/يوم"
                            else
                                "Daily target: ${String.format("%.0f", target)} pages/day",
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            fontSize = 12.sp,
                            color = goldAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Pages remaining from this bookmark
                    Text(
                        text = if (language == AppLanguage.ARABIC)
                            "المتبقي: $formattedRemaining صفحة (~$formattedDays يوم بالهدف الحالي)"
                        else
                            "Remaining: $pagesRemaining pages (~$daysNeeded days at current target)",
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 11.sp,
                        color = Color.Gray.copy(alpha = 0.8f)
                    )
                }
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = if (language == AppLanguage.ARABIC) "حذف العلامة" else "Delete bookmark",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
