package com.quranmedia.player.presentation.screens.surahs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.domain.model.Reciter
import com.quranmedia.player.domain.model.RevelationType
import com.quranmedia.player.domain.model.Surah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahsScreenNew(
    viewModel: SurahsViewModel = hiltViewModel(),
    onSurahClick: (reciterId: String, surah: Surah) -> Unit,
    onBack: () -> Unit
) {
    val surahs by viewModel.surahs.collectAsState()
    val reciters by viewModel.reciters.collectAsState()
    val selectedReciter by viewModel.selectedReciter.collectAsState()
    var reciterDropdownExpanded by remember { mutableStateOf(false) }

    // Auto-select first reciter if none selected
    LaunchedEffect(reciters, selectedReciter) {
        if (selectedReciter == null && reciters.isNotEmpty()) {
            viewModel.selectReciter(reciters[0])
        }
    }

    // Islamic green theme colors
    val islamicGreen = Color(0xFF2E7D32)
    val lightGreen = Color(0xFF66BB6A)
    val darkGreen = Color(0xFF1B5E20)
    val goldAccent = Color(0xFFFFD700)
    val creamBackground = Color(0xFFFAF8F3)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "السور",
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Surahs",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back",
                            tint = Color.White
                        )
                    }
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
        if (surahs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = islamicGreen)
                    Text(
                        text = "Loading surahs...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Reciter Selector Dropdown
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = islamicGreen.copy(alpha = 0.1f)
                    )
                ) {
                    ExposedDropdownMenuBox(
                        expanded = reciterDropdownExpanded,
                        onExpandedChange = { reciterDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedReciter?.name ?: "Select Reciter",
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Column {
                                    Text("القارئ", fontSize = 12.sp, color = islamicGreen)
                                    Text("Reciter", fontSize = 10.sp, color = darkGreen)
                                }
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Reciter",
                                    tint = islamicGreen
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = islamicGreen,
                                unfocusedBorderColor = islamicGreen.copy(alpha = 0.5f),
                                focusedLabelColor = islamicGreen,
                                unfocusedLabelColor = darkGreen,
                                focusedTextColor = darkGreen,
                                unfocusedTextColor = Color.Black,
                                cursorColor = islamicGreen
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .padding(16.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = reciterDropdownExpanded,
                            onDismissRequest = { reciterDropdownExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            reciters.forEach { reciter ->
                                val isSelected = reciter.id == selectedReciter?.id
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = reciter.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 15.sp,
                                                color = if (isSelected) Color.White else darkGreen
                                            )
                                            reciter.nameArabic?.let { arabicName ->
                                                Text(
                                                    text = arabicName,
                                                    fontSize = 13.sp,
                                                    color = if (isSelected) lightGreen.copy(alpha = 0.9f) else Color.Gray
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectReciter(reciter)
                                        reciterDropdownExpanded = false
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = darkGreen,
                                        leadingIconColor = darkGreen,
                                        disabledTextColor = Color.Gray
                                    ),
                                    modifier = Modifier.background(
                                        if (isSelected) islamicGreen else Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }

                // Header card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = lightGreen.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Book,
                            contentDescription = null,
                            tint = islamicGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "114 Surahs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = darkGreen
                            )
                            Text(
                                text = "Select a surah to begin listening",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Surahs list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(surahs) { surah ->
                        SurahItemNew(
                            surah = surah,
                            onClick = {
                                // Only allow clicking if a reciter is selected
                                selectedReciter?.let { reciter ->
                                    onSurahClick(reciter.id, surah)
                                }
                            },
                            isEnabled = selectedReciter != null
                        )
                    }
                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SurahItemNew(
    surah: Surah,
    onClick: () -> Unit,
    isEnabled: Boolean = true
) {
    val islamicGreen = Color(0xFF2E7D32)
    val lightGreen = Color(0xFF66BB6A)
    val goldAccent = Color(0xFFFFD700)
    val darkGreen = Color(0xFF1B5E20)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) Color.White else Color.White.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Surah number circle - smaller
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (surah.number == 1) goldAccent.copy(alpha = 0.2f)
                        else lightGreen.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = surah.number.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (surah.number == 1) Color(0xFFF57C00) else islamicGreen
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Surah info - compact
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah.nameEnglish,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Text(
                    text = "${when (surah.revelationType) {
                        RevelationType.MECCAN -> "Meccan"
                        RevelationType.MEDINAN -> "Medinan"
                    }} • ${surah.ayahCount} ayahs",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Arabic name - smaller
            Text(
                text = surah.nameArabic,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = islamicGreen
            )
        }
    }
}
