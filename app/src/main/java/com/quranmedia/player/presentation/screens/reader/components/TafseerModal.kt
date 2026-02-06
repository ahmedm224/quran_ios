package com.quranmedia.player.presentation.screens.reader.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.domain.model.TafseerContent
import com.quranmedia.player.domain.model.TafseerInfo
import com.quranmedia.player.domain.model.TafseerType

/**
 * State for the Tafseer Modal
 */
data class TafseerModalState(
    val isVisible: Boolean = false,
    val surah: Int = 1,
    val ayah: Int = 1,
    val surahName: String = "",
    val ayahText: String = "",
    val availableTafseers: List<Pair<TafseerInfo, TafseerContent>> = emptyList(),
    val selectedTafseerId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

// Custom colors for the tafseer modal
private val tafseerCardBackground = Color(0xFFFAF8F5) // Warm off-white
private val tafseerHeaderBackground = Color(0xFFF5F0E8) // Slightly darker cream
private val tafseerAccent = Color(0xFF2E7D32) // Deep green
private val tafseerGold = Color(0xFFD4AF37) // Gold accent
private val tafseerTextPrimary = Color(0xFF3E2723) // Coffee brown
private val tafseerTextSecondary = Color(0xFF6D4C41) // Lighter brown

/**
 * Cleans Arabic text by removing problematic Unicode characters that render as black circles.
 * Removes ornamental marks, unusual punctuation, and isolated diacritics while preserving
 * Arabic letters, standard diacritics, and common punctuation.
 */
private fun String.cleanArabicText(): String {
    // Characters to completely remove (ornamental/problematic)
    val charsToRemove = setOf(
        '\u060C', // Arabic Comma - causes rendering issues in some fonts
        '\u06DD', // Arabic End of Ayah
        '\u06DE', // Arabic Start of Rub El Hizb
        '\u06E9', // Arabic Place of Sajdah
        '\u25CC', // Dotted Circle
        '\u2022', // Bullet
        '\u2023', // Triangular Bullet
        '\u25E6', // White Bullet
        '\u2219', // Bullet Operator
        '\u2055', // Flower Punctuation Mark
        '\u00B7', // Middle Dot
        '\u0640', // Arabic Tatweel (kashida) - can cause rendering issues
        '\u200B', // Zero Width Space
        '\u200C', // Zero Width Non-Joiner
        '\u200D', // Zero Width Joiner
        '\u200E', // Left-to-Right Mark
        '\u200F', // Right-to-Left Mark
        '\uFEFF'  // Zero Width No-Break Space
    )

    val combiningDiacritics = setOf(
        '\u064B', '\u064C', '\u064D', '\u064E', '\u064F', '\u0650',
        '\u0651', '\u0652', '\u0653', '\u0654', '\u0655', '\u0656',
        '\u0657', '\u0658', '\u0670', '\u06DC', '\u06DF', '\u06E0',
        '\u06E1', '\u06E2', '\u06E3', '\u06E4', '\u06E7', '\u06E8',
        '\u06EA', '\u06EB', '\u06EC', '\u06ED'
    )

    val result = StringBuilder()
    var i = 0
    while (i < this.length) {
        val char = this[i]

        when {
            // Skip characters that should be removed entirely
            char in charsToRemove -> {}

            // Handle combining diacritics - only keep if they have a base character
            char in combiningDiacritics -> {
                if (i > 0 && this[i - 1] !in combiningDiacritics &&
                    !this[i - 1].isWhitespace() && this[i - 1] !in charsToRemove) {
                    result.append(char)
                }
            }

            // Keep all other characters
            else -> result.append(char)
        }
        i++
    }

    return result.toString()
}

/**
 * Formats word meaning text with bold Quran words.
 * Input format: "arabicWord: meaning\n\narabicWord2: meaning2\n\n"
 * Output: AnnotatedString with bold styling on the Arabic words (before the colon)
 */
private fun formatWordMeanings(text: String, boldColor: Color): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n\n").filter { it.isNotBlank() }

        for ((index, line) in lines.withIndex()) {
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                // Bold the Quran word (before colon)
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                    append(line.substring(0, colonIndex))
                }
                // Regular style for the meaning (after colon)
                append(line.substring(colonIndex))
            } else {
                // No colon found, just append the line
                append(line)
            }

            // Add spacing between entries
            if (index < lines.size - 1) {
                append("\n\n")
            }
        }
    }
}

/**
 * A modal dialog for displaying Tafseer (Quran interpretation) for a specific ayah.
 *
 * Features:
 * - Compact, elegant design with soft transparency
 * - Smooth scrolling for long tafseer text
 * - Tab selector for switching between downloaded tafseers
 * - Copy button
 * - Easy close (X button or tap outside)
 */
@Composable
fun TafseerModal(
    state: TafseerModalState,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSelectTafseer: (String) -> Unit,
    onCopy: (String) -> Unit,
    onNavigateToDownload: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // Get selected tafseer content
    val selectedTafseer = state.availableTafseers.find { it.first.id == state.selectedTafseerId }
        ?: state.availableTafseers.firstOrNull()

    // Use RTL for Arabic tafseer content, word meanings, and grammar (they contain Arabic text)
    val isArabicOrWordMeaning = selectedTafseer?.first?.language == "arabic" ||
        selectedTafseer?.first?.type == TafseerType.WORD_MEANING ||
        selectedTafseer?.first?.type == TafseerType.GRAMMAR
    val contentDirection = if (isArabicOrWordMeaning) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    AnimatedVisibility(
        visible = state.isVisible,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // Modal Card
            AnimatedVisibility(
                visible = state.isVisible,
                enter = scaleIn(
                    initialScale = 0.9f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
                exit = scaleOut(targetScale = 0.9f) + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.55f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Prevent click through */ }
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = Color.Black.copy(alpha = 0.2f),
                            spotColor = Color.Black.copy(alpha = 0.3f)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = tafseerCardBackground.copy(alpha = 0.97f)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header with gradient
                        TafseerModalHeader(
                            surah = state.surah,
                            ayah = state.ayah,
                            surahName = state.surahName,
                            language = language,
                            onClose = onDismiss
                        )

                        // Tafseer Selector - Always show if there are tafseers
                        if (state.availableTafseers.isNotEmpty()) {
                            TafseerSelector(
                                tafseers = state.availableTafseers.map { it.first },
                                selectedId = state.selectedTafseerId ?: state.availableTafseers.firstOrNull()?.first?.id,
                                onSelect = onSelectTafseer
                            )
                        }

                        // Content area with scrolling
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            when {
                                state.isLoading -> {
                                    Column(
                                        modifier = Modifier.align(Alignment.Center),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(
                                            color = tafseerAccent,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = if (language == AppLanguage.ARABIC) "جاري التحميل..." else "Loading...",
                                            fontSize = 14.sp,
                                            color = tafseerTextSecondary
                                        )
                                    }
                                }
                                state.error != null -> {
                                    Text(
                                        text = state.error,
                                        color = Color.Red.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        fontSize = 14.sp,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(24.dp)
                                    )
                                }
                                selectedTafseer != null -> {
                                    CompositionLocalProvider(LocalLayoutDirection provides contentDirection) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(scrollState)
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            // Ayah text card
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = Color.White.copy(alpha = 0.7f)
                                                ),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .border(
                                                            width = 1.dp,
                                                            color = tafseerGold.copy(alpha = 0.3f),
                                                            shape = RoundedCornerShape(16.dp)
                                                        )
                                                        .padding(16.dp)
                                                ) {
                                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                                        Text(
                                                            text = state.ayahText,
                                                            fontFamily = scheherazadeFont,
                                                            fontSize = 20.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = tafseerAccent,
                                                            textAlign = TextAlign.Center,
                                                            lineHeight = 34.sp,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Tafseer content card
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = Color.White.copy(alpha = 0.5f)
                                                ),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp)
                                                ) {
                                                    // Tafseer source label
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(bottom = 12.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.MenuBook,
                                                            contentDescription = null,
                                                            tint = tafseerGold,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = if (selectedTafseer.first.language == "arabic")
                                                                selectedTafseer.first.nameArabic ?: selectedTafseer.first.nameEnglish
                                                            else
                                                                selectedTafseer.first.nameEnglish,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = tafseerGold,
                                                            fontFamily = if (selectedTafseer.first.language == "arabic") scheherazadeFont else null
                                                        )
                                                    }

                                                    // Tafseer text - clean to remove problematic Unicode characters
                                                    // Word meanings and grammar always contain Arabic text, so always clean, use Arabic font, and right-align
                                                    val isArabicContent = selectedTafseer.first.language == "arabic"
                                                    val isWordMeaning = selectedTafseer.first.type == TafseerType.WORD_MEANING
                                                    val isGrammar = selectedTafseer.first.type == TafseerType.GRAMMAR
                                                    val useArabicStyle = isArabicContent || isWordMeaning || isGrammar
                                                    val cleanedText = selectedTafseer.second.text.cleanArabicText()

                                                    // Use TextAlign.Start here because we're inside CompositionLocalProvider
                                                    // with RTL direction for Arabic/word meanings - Start in RTL = Right alignment
                                                    if (isWordMeaning) {
                                                        // Word meanings: show Quran word in bold
                                                        Text(
                                                            text = formatWordMeanings(cleanedText, tafseerAccent),
                                                            fontFamily = scheherazadeFont,
                                                            fontSize = 17.sp,
                                                            color = tafseerTextPrimary,
                                                            lineHeight = 30.sp,
                                                            textAlign = TextAlign.Start,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    } else {
                                                        // Regular tafseer text
                                                        Text(
                                                            text = cleanedText,
                                                            fontFamily = if (useArabicStyle) scheherazadeFont else null,
                                                            fontSize = if (useArabicStyle) 17.sp else 15.sp,
                                                            color = tafseerTextPrimary,
                                                            lineHeight = if (useArabicStyle) 30.sp else 24.sp,
                                                            textAlign = TextAlign.Start,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                }
                                            }

                                            // Extra padding at bottom for better scrolling
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                                state.availableTafseers.isEmpty() -> {
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Rounded.MenuBook,
                                            contentDescription = null,
                                            tint = tafseerTextSecondary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = if (language == AppLanguage.ARABIC)
                                                "لم يتم تحميل أي تفسير"
                                            else
                                                "No tafseer downloaded",
                                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = tafseerTextPrimary,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Download button
                                        Button(
                                            onClick = {
                                                onDismiss()
                                                onNavigateToDownload()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = tafseerAccent
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Download,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (language == AppLanguage.ARABIC)
                                                    "تحميل التفسير"
                                                else
                                                    "Download Tafseer",
                                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Footer with Copy button
                        if (selectedTafseer != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                tafseerHeaderBackground.copy(alpha = 0.8f)
                                            )
                                        )
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                // Copy button
                                TextButton(
                                    onClick = {
                                        val textToCopy = "${state.ayahText}\n\n${selectedTafseer.second.text}"
                                        clipboardManager.setText(AnnotatedString(textToCopy))
                                        onCopy(textToCopy)
                                    },
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = tafseerAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == AppLanguage.ARABIC) "نسخ" else "Copy",
                                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                        color = tafseerAccent,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TafseerModalHeader(
    surah: Int,
    ayah: Int,
    surahName: String,
    language: AppLanguage,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        tafseerHeaderBackground,
                        tafseerCardBackground.copy(alpha = 0.5f)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (language == AppLanguage.ARABIC)
                        "تفسير الآية $ayah"
                    else
                        "Tafseer - Ayah $ayah",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = tafseerAccent
                )
                Text(
                    text = surahName,
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 13.sp,
                    color = tafseerTextSecondary
                )
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(tafseerTextSecondary.copy(alpha = 0.1f))
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = tafseerTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TafseerSelector(
    tafseers: List<TafseerInfo>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTafseer = tafseers.find { it.id == selectedId } ?: tafseers.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Selected item display (clickable to open dropdown)
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = tafseerHeaderBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Icon
                    Icon(
                        Icons.Rounded.MenuBook,
                        contentDescription = null,
                        tint = tafseerAccent,
                        modifier = Modifier.size(20.dp)
                    )

                    if (selectedTafseer != null) {
                        val isArabic = selectedTafseer.language == "arabic"

                        // Language badge
                        Text(
                            text = if (isArabic) "ع" else "EN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isArabic) tafseerGold else tafseerAccent,
                            modifier = Modifier
                                .background(
                                    color = if (isArabic) tafseerGold.copy(alpha = 0.15f)
                                    else tafseerAccent.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )

                        // Tafseer name
                        Text(
                            text = if (isArabic) selectedTafseer.nameArabic ?: selectedTafseer.nameEnglish else selectedTafseer.nameEnglish,
                            fontFamily = if (isArabic) scheherazadeFont else null,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = tafseerTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Dropdown arrow
                Icon(
                    imageVector = if (expanded)
                        Icons.Filled.KeyboardArrowUp
                    else
                        Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Select tafseer",
                    tint = tafseerTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Dropdown menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(Color.White, RoundedCornerShape(12.dp))
        ) {
            tafseers.forEach { tafseer ->
                val isSelected = tafseer.id == selectedId
                val isArabic = tafseer.language == "arabic"

                DropdownMenuItem(
                    onClick = {
                        onSelect(tafseer.id)
                        expanded = false
                    },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Language badge
                            Text(
                                text = if (isArabic) "ع" else "EN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isArabic) tafseerGold else tafseerAccent,
                                modifier = Modifier
                                    .background(
                                        color = if (isArabic) tafseerGold.copy(alpha = 0.15f)
                                        else tafseerAccent.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )

                            // Tafseer name
                            Text(
                                text = if (isArabic) tafseer.nameArabic ?: tafseer.nameEnglish else tafseer.nameEnglish,
                                fontFamily = if (isArabic) scheherazadeFont else null,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) tafseerAccent else tafseerTextPrimary
                            )
                        }
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = tafseerAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    modifier = Modifier.background(
                        if (isSelected) tafseerAccent.copy(alpha = 0.08f) else Color.Transparent
                    )
                )
            }
        }
    }
}
