package com.quranmedia.player.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont

@Composable
fun ColorPickerDialog(
    title: String,
    currentColor: Color,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var selectedColor by remember { mutableStateOf(currentColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Preset colors grid
                Text(
                    text = if (language == AppLanguage.ARABIC) "ألوان جاهزة" else "Preset Colors",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetColors) { color ->
                        ColorOption(
                            color = color,
                            isSelected = selectedColor.toArgb() == color.toArgb(),
                            onClick = { selectedColor = color }
                        )
                    }
                }

                // Current color preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "اللون المختار" else "Selected Color",
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 14.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                            .border(2.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onColorSelected(selectedColor)
                onDismiss()
            }) {
                Text(
                    if (language == AppLanguage.ARABIC) "حفظ" else "Save",
                    color = Color(0xFF2E7D32)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    if (language == AppLanguage.ARABIC) "إلغاء" else "Cancel",
                    color = Color.Gray
                )
            }
        },
        containerColor = Color.White
    )
}

@Composable
private fun ColorOption(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFF2E7D32) else Color.Gray.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Preset color palette
 */
private val presetColors = listOf(
    // Whites and creams
    Color(0xFFFFFFFF), Color(0xFFFFFFF5), Color(0xFFFAF8F3), Color(0xFFF5E6D3),
    // Grays
    Color(0xFFE0E0E0), Color(0xFFBDBDBD), Color(0xFF757575), Color(0xFF424242),
    // Blacks
    Color(0xFF212121), Color(0xFF000000), Color(0xFF121212), Color(0xFF1B1B1B),
    // Greens
    Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF388E3C), Color(0xFF4CAF50),
    Color(0xFF66BB6A), Color(0xFF81C784), Color(0xFFA5D6A7), Color(0xFFC8E6C9),
    // Blues
    Color(0xFF0D47A1), Color(0xFF1565C0), Color(0xFF1976D2), Color(0xFF1E88E5),
    Color(0xFF2196F3), Color(0xFF42A5F5), Color(0xFF64B5F6), Color(0xFF90CAF9),
    // Browns
    Color(0xFF3E2723), Color(0xFF4E342E), Color(0xFF5D4037), Color(0xFF6D4C41),
    Color(0xFF795548), Color(0xFF8D6E63), Color(0xFFA1887F), Color(0xFFBCAAA4),
    // Golds
    Color(0xFFD4AF37), Color(0xFFFFD700), Color(0xFFFFA000), Color(0xFFFF8F00)
)

/**
 * Extension to calculate color luminance
 */
private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
