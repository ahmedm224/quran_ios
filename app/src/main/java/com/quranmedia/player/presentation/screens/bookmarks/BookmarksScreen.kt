package com.quranmedia.player.presentation.screens.bookmarks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.database.entity.ReadingBookmarkEntity
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.screens.reader.components.islamicGreen
import com.quranmedia.player.presentation.screens.reader.components.darkGreen
import com.quranmedia.player.presentation.screens.reader.components.creamBackground
import com.quranmedia.player.presentation.screens.reader.components.coffeeBrown
import com.quranmedia.player.presentation.util.layoutDirection
import com.quranmedia.player.presentation.components.CommonOverflowMenu
import com.quranmedia.player.domain.util.ArabicNumeralUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onNavigateBack: () -> Unit,
    onBookmarkClick: (String, Int, Int, Long) -> Unit, // reciterId, surahNumber, ayahNumber, positionMs
    onReadingBookmarkClick: ((Int) -> Unit)? = null, // pageNumber - navigate to reader
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPrayerTimes: () -> Unit = {},
    onNavigateToAthkar: () -> Unit = {},
    onNavigateToTracker: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToReading: () -> Unit = {},
    onNavigateToImsakiya: () -> Unit = {},
    viewModel: BookmarksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val language = state.appLanguage
    val isArabic = language == AppLanguage.ARABIC
    val useIndoArabic = isArabic && state.useIndoArabicNumerals
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var bookmarkToDelete by remember { mutableStateOf<String?>(null) }
    var readingBookmarkToDelete by remember { mutableStateOf<String?>(null) }
    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isArabic) "المفضلة" else "Bookmarks",
                            fontFamily = if (isArabic) scheherazadeFont else null
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        if (state.bookmarks.isNotEmpty()) {
                            IconButton(onClick = { showDeleteAllDialog = true }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Delete all", tint = Color.White)
                            }
                        }

                        CommonOverflowMenu(
                            language = language,
                            onNavigateToSettings = onNavigateToSettings,
                            onNavigateToReading = onNavigateToReading,
                            onNavigateToPrayerTimes = onNavigateToPrayerTimes,
                            onNavigateToImsakiya = onNavigateToImsakiya,
                            onNavigateToAthkar = onNavigateToAthkar,
                            onNavigateToTracker = onNavigateToTracker,
                            onNavigateToDownloads = onNavigateToDownloads,
                            onNavigateToAbout = onNavigateToAbout
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = islamicGreen,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = creamBackground
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val hasAnyBookmarks = state.bookmarks.isNotEmpty() || state.readingBookmarks.isNotEmpty()

                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = islamicGreen
                        )
                    }
                    state.error != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = state.error ?: if (isArabic) "خطأ غير معروف" else "Unknown error",
                                fontFamily = if (isArabic) scheherazadeFont else null,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    !hasAnyBookmarks -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isArabic) "لا توجد علامات محفوظة" else "No bookmarks yet",
                                fontFamily = if (isArabic) scheherazadeFont else null,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isArabic) "ستظهر العلامات المحفوظة هنا" else "Bookmarks you create will appear here",
                                fontFamily = if (isArabic) scheherazadeFont else null,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Reading Bookmarks Section
                            if (state.readingBookmarks.isNotEmpty()) {
                                item {
                                    Text(
                                        text = if (isArabic) "علامات القراءة" else "Reading Bookmarks",
                                        fontFamily = if (isArabic) scheherazadeFont else null,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = islamicGreen,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                items(state.readingBookmarks, key = { "reading_${it.id}" }) { bookmark ->
                                    ReadingBookmarkCard(
                                        bookmark = bookmark,
                                        isArabic = isArabic,
                                        onClick = { onReadingBookmarkClick?.invoke(bookmark.pageNumber) },
                                        onDelete = { readingBookmarkToDelete = bookmark.id },
                                        useIndoArabic = useIndoArabic
                                    )
                                }
                            }

                            // Playback Bookmarks Section
                            if (state.bookmarks.isNotEmpty()) {
                                item {
                                    Text(
                                        text = if (isArabic) "علامات الاستماع" else "Playback Bookmarks",
                                        fontFamily = if (isArabic) scheherazadeFont else null,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = islamicGreen,
                                        modifier = Modifier.padding(top = if (state.readingBookmarks.isNotEmpty()) 16.dp else 0.dp, bottom = 8.dp)
                                    )
                                }
                                items(state.bookmarks, key = { "playback_${it.bookmark.id}" }) { bookmarkWithDetails ->
                                    BookmarkCard(
                                        bookmarkWithDetails = bookmarkWithDetails,
                                        isArabic = isArabic,
                                        onClick = {
                                            onBookmarkClick(
                                                bookmarkWithDetails.bookmark.reciterId,
                                                bookmarkWithDetails.bookmark.surahNumber,
                                                bookmarkWithDetails.bookmark.ayahNumber,
                                                bookmarkWithDetails.bookmark.positionMs
                                            )
                                        },
                                        onDelete = { bookmarkToDelete = bookmarkWithDetails.bookmark.id },
                                        useIndoArabic = useIndoArabic
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    // Delete single playback bookmark confirmation dialog
    bookmarkToDelete?.let { bookmarkId ->
        AlertDialog(
            onDismissRequest = { bookmarkToDelete = null },
            title = { Text(if (isArabic) "حذف العلامة" else "Delete Bookmark", fontFamily = if (isArabic) scheherazadeFont else null) },
            text = { Text(if (isArabic) "هل تريد حذف هذه العلامة؟" else "Are you sure you want to delete this bookmark?", fontFamily = if (isArabic) scheherazadeFont else null) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBookmark(bookmarkId)
                        bookmarkToDelete = null
                    }
                ) {
                    Text(if (isArabic) "حذف" else "Delete", fontFamily = if (isArabic) scheherazadeFont else null)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookmarkToDelete = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", fontFamily = if (isArabic) scheherazadeFont else null)
                }
            }
        )
    }

    // Delete reading bookmark confirmation dialog
    readingBookmarkToDelete?.let { bookmarkId ->
        AlertDialog(
            onDismissRequest = { readingBookmarkToDelete = null },
            title = { Text(if (isArabic) "حذف علامة القراءة" else "Delete Reading Bookmark", fontFamily = if (isArabic) scheherazadeFont else null) },
            text = { Text(if (isArabic) "هل تريد حذف علامة القراءة هذه؟" else "Are you sure you want to delete this reading bookmark?", fontFamily = if (isArabic) scheherazadeFont else null) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteReadingBookmark(bookmarkId)
                        readingBookmarkToDelete = null
                    }
                ) {
                    Text(if (isArabic) "حذف" else "Delete", fontFamily = if (isArabic) scheherazadeFont else null)
                }
            },
            dismissButton = {
                TextButton(onClick = { readingBookmarkToDelete = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", fontFamily = if (isArabic) scheherazadeFont else null)
                }
            }
        )
    }

    // Delete all bookmarks confirmation dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(if (isArabic) "حذف جميع العلامات" else "Delete All Bookmarks", fontFamily = if (isArabic) scheherazadeFont else null) },
            text = { Text(if (isArabic) "هل تريد حذف جميع العلامات؟ لا يمكن التراجع عن هذا." else "Are you sure you want to delete all ${state.bookmarks.size} bookmarks? This action cannot be undone.", fontFamily = if (isArabic) scheherazadeFont else null) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllBookmarks()
                        showDeleteAllDialog = false
                    }
                ) {
                    Text(if (isArabic) "حذف الكل" else "Delete All", color = MaterialTheme.colorScheme.error, fontFamily = if (isArabic) scheherazadeFont else null)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", fontFamily = if (isArabic) scheherazadeFont else null)
                }
            }
        )
    }
    }
}

@Composable
private fun ReadingBookmarkCard(
    bookmark: ReadingBookmarkEntity,
    isArabic: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    useIndoArabic: Boolean = false
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val formattedDate = remember(bookmark.createdAt) {
        dateFormat.format(Date(bookmark.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                tint = islamicGreen,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isArabic) "صفحة ${ArabicNumeralUtils.formatNumber(bookmark.pageNumber, useIndoArabic)}" else "Page ${bookmark.pageNumber}",
                    fontFamily = if (isArabic) scheherazadeFont else null,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                bookmark.surahName?.let {
                    Text(
                        text = it,
                        fontFamily = scheherazadeFont,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun BookmarkCard(
    bookmarkWithDetails: BookmarkWithDetails,
    isArabic: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    useIndoArabic: Boolean = false
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val formattedDate = remember(bookmarkWithDetails.bookmark.createdAt) {
        dateFormat.format(bookmarkWithDetails.bookmark.createdAt)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                Icons.Default.Headphones,
                contentDescription = null,
                tint = islamicGreen,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmarkWithDetails.surahName,
                    fontFamily = scheherazadeFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = bookmarkWithDetails.reciterName,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isArabic) "آية ${ArabicNumeralUtils.formatNumber(bookmarkWithDetails.bookmark.ayahNumber, useIndoArabic)}" else "Ayah ${bookmarkWithDetails.bookmark.ayahNumber}",
                    fontFamily = if (isArabic) scheherazadeFont else null,
                    fontSize = 12.sp,
                    color = islamicGreen
                )
                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
