package com.quranmedia.player.presentation.screens.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.domain.model.CustomRecitationSettings
import com.quranmedia.player.domain.model.Reciter
import com.quranmedia.player.domain.model.RecitationPreset
import com.quranmedia.player.domain.model.Surah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRecitationDialog(
    initialStartSurah: Int,
    initialStartAyah: Int,
    currentReciterId: String,
    language: AppLanguage,
    reciters: List<Reciter>,
    surahs: List<Surah>,
    savedPresets: List<RecitationPreset>,
    onDismiss: () -> Unit,
    onConfirm: (String, CustomRecitationSettings) -> Unit,
    onSavePreset: (String, CustomRecitationSettings) -> Unit
) {
    var selectedReciterId by remember { mutableStateOf(currentReciterId) }
    var startSurahNumber by remember { mutableIntStateOf(initialStartSurah) }
    var startAyahNumber by remember { mutableIntStateOf(initialStartAyah) }
    val endSurahNumber = startSurahNumber
    var endAyahNumber by remember { mutableIntStateOf(initialStartAyah) }
    var ayahRepeatCount by remember { mutableIntStateOf(1) }
    var groupRepeatCount by remember { mutableIntStateOf(1) }
    var speed by remember { mutableFloatStateOf(1.0f) }

    var showPresetDialog by remember { mutableStateOf(false) }
    var expandedPresets by remember { mutableStateOf(false) }
    var expandedReciter by remember { mutableStateOf(false) }
    var expandedStartSurah by remember { mutableStateOf(false) }
    var expandedStartAyah by remember { mutableStateOf(false) }
    var expandedEndAyah by remember { mutableStateOf(false) }

    // Get current surah to know max ayah count
    val currentSurah = surahs.find { it.number == startSurahNumber }
    val maxAyah = currentSurah?.ayahCount ?: 7

    // Find selected reciter or use first one if not found
    val selectedReciter = reciters.find { it.id == selectedReciterId } ?: reciters.firstOrNull()

    // Update selectedReciterId if we fell back to first reciter
    LaunchedEffect(selectedReciter) {
        if (selectedReciter != null && selectedReciterId != selectedReciter.id) {
            selectedReciterId = selectedReciter.id
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (language == AppLanguage.ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "تلاوة مخصصة" else "Custom Recitation",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = if (language == AppLanguage.ARABIC) "إغلاق" else "Close",
                            tint = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reciter Selection
                Text(
                    text = if (language == AppLanguage.ARABIC) "القارئ" else "Reciter",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedReciter,
                    onExpandedChange = { expandedReciter = it }
                ) {
                    OutlinedTextField(
                        value = if (language == AppLanguage.ARABIC)
                            selectedReciter?.nameArabic ?: selectedReciter?.name ?: ""
                        else
                            selectedReciter?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedReciter) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expandedReciter,
                        onDismissRequest = { expandedReciter = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        reciters.forEach { reciter ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (language == AppLanguage.ARABIC)
                                            reciter.nameArabic ?: reciter.name
                                        else
                                            reciter.name,
                                        color = Color.Black,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    selectedReciterId = reciter.id
                                    expandedReciter = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Load Preset Section
                if (savedPresets.isNotEmpty()) {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "تحميل إعداد محفوظ" else "Load Preset",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded = expandedPresets,
                        onExpandedChange = { expandedPresets = it }
                    ) {
                        OutlinedTextField(
                            value = if (language == AppLanguage.ARABIC) "اختر إعدادًا…" else "Select a preset…",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPresets) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = expandedPresets,
                            onDismissRequest = { expandedPresets = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            savedPresets.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.name, color = Color.Black, fontSize = 13.sp) },
                                    onClick = {
                                        startSurahNumber = preset.settings.startSurahNumber
                                        startAyahNumber = preset.settings.startAyahNumber
                                        endAyahNumber = preset.settings.endAyahNumber
                                        ayahRepeatCount = preset.settings.ayahRepeatCount
                                        groupRepeatCount = preset.settings.groupRepeatCount
                                        speed = preset.settings.speed
                                        expandedPresets = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Start Section
                Text(
                    text = if (language == AppLanguage.ARABIC) "بداية" else "Start",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Start Surah Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(
                            expanded = expandedStartSurah,
                            onExpandedChange = { expandedStartSurah = it }
                        ) {
                            OutlinedTextField(
                                value = if (language == AppLanguage.ARABIC)
                                    currentSurah?.nameArabic ?: ""
                                else
                                    currentSurah?.nameEnglish ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (language == AppLanguage.ARABIC) "السورة" else "Surah", color = Color.Black, fontSize = 11.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStartSurah) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = expandedStartSurah,
                                onDismissRequest = { expandedStartSurah = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                surahs.forEach { surah ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (language == AppLanguage.ARABIC)
                                                    "${surah.number}. ${surah.nameArabic}"
                                                else
                                                    "${surah.number}. ${surah.nameEnglish}",
                                                color = Color.Black,
                                                fontSize = 13.sp
                                            )
                                        },
                                        onClick = {
                                            startSurahNumber = surah.number
                                            startAyahNumber = 1
                                            endAyahNumber = 1
                                            expandedStartSurah = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Start Ayah Dropdown
                    Box(modifier = Modifier.weight(0.6f)) {
                        ExposedDropdownMenuBox(
                            expanded = expandedStartAyah,
                            onExpandedChange = { expandedStartAyah = it }
                        ) {
                            OutlinedTextField(
                                value = startAyahNumber.toString(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (language == AppLanguage.ARABIC) "الآية" else "Ayah", color = Color.Black, fontSize = 11.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStartAyah) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = expandedStartAyah,
                                onDismissRequest = { expandedStartAyah = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                (1..maxAyah).forEach { ayahNum ->
                                    DropdownMenuItem(
                                        text = { Text(ayahNum.toString(), color = Color.Black, fontSize = 13.sp) },
                                        onClick = {
                                            startAyahNumber = ayahNum
                                            if (endAyahNumber < ayahNum) {
                                                endAyahNumber = ayahNum
                                            }
                                            expandedStartAyah = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // End Ayah
                Text(
                    text = if (language == AppLanguage.ARABIC) "نهاية الآية" else "End Ayah",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedEndAyah,
                    onExpandedChange = { expandedEndAyah = it }
                ) {
                    OutlinedTextField(
                        value = endAyahNumber.toString(),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEndAyah) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expandedEndAyah,
                        onDismissRequest = { expandedEndAyah = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        (startAyahNumber..maxAyah).forEach { ayahNum ->
                            DropdownMenuItem(
                                text = { Text(ayahNum.toString(), color = Color.Black, fontSize = 13.sp) },
                                onClick = {
                                    endAyahNumber = ayahNum
                                    expandedEndAyah = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Repeat Each Ayah
                CompactCounter(
                    label = if (language == AppLanguage.ARABIC) "تكرار كل آية" else "Repeat Each Ayah",
                    language = language,
                    value = ayahRepeatCount,
                    onValueChange = { ayahRepeatCount = it },
                    minValue = 1,
                    maxValue = 5
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Repeat Whole Group
                CompactCounter(
                    label = if (language == AppLanguage.ARABIC) "تكرار المجموعة كاملة" else "Repeat Whole Group",
                    language = language,
                    value = groupRepeatCount,
                    onValueChange = { groupRepeatCount = it },
                    minValue = 1,
                    maxValue = 5
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Speed Control
                CompactSpeedControl(
                    label = if (language == AppLanguage.ARABIC) "السرعة" else "Speed",
                    language = language,
                    speed = speed,
                    onSpeedChange = { speed = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showPresetDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Black
                        )
                    ) {
                        Text(if (language == AppLanguage.ARABIC) "حفظ الإعداد" else "Save Preset", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val settings = CustomRecitationSettings(
                                startSurahNumber = startSurahNumber,
                                startAyahNumber = startAyahNumber,
                                endSurahNumber = endSurahNumber,
                                endAyahNumber = endAyahNumber,
                                ayahRepeatCount = ayahRepeatCount,
                                groupRepeatCount = groupRepeatCount,
                                speed = speed
                            )
                            if (settings.isValid()) {
                                onConfirm(selectedReciterId, settings)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00897B)
                        )
                    ) {
                        Text(if (language == AppLanguage.ARABIC) "بداية" else "Start", fontSize = 13.sp)
                    }
                }
            }
        }
        }
    }

    // Preset Name Dialog
    if (showPresetDialog) {
        var presetName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = { Text(if (language == AppLanguage.ARABIC) "حفظ الإعداد" else "Save Preset", color = Color.Black, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text(if (language == AppLanguage.ARABIC) "اسم الإعداد" else "Preset Name", color = Color.Black, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            val settings = CustomRecitationSettings(
                                startSurahNumber = startSurahNumber,
                                startAyahNumber = startAyahNumber,
                                endSurahNumber = endSurahNumber,
                                endAyahNumber = endAyahNumber,
                                ayahRepeatCount = ayahRepeatCount,
                                groupRepeatCount = groupRepeatCount,
                                speed = speed
                            )
                            onSavePreset(presetName, settings)
                            showPresetDialog = false
                        }
                    }
                ) {
                    Text(if (language == AppLanguage.ARABIC) "حفظ" else "Save", color = Color(0xFF00897B), fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPresetDialog = false }) {
                    Text(if (language == AppLanguage.ARABIC) "إلغاء" else "Cancel", color = Color.Black, fontSize = 13.sp)
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
private fun CompactCounter(
    label: String,
    language: AppLanguage,
    value: Int,
    onValueChange: (Int) -> Unit,
    minValue: Int,
    maxValue: Int
) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (value > minValue) {
                        onValueChange(value - 1)
                    } else {
                        onValueChange(CustomRecitationSettings.UNLIMITED)
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = if (language == AppLanguage.ARABIC) "-" else "-",
                    tint = Color(0xFF00897B),
                    modifier = Modifier.size(18.dp)
                )
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF5F5F5)
            ) {
                Text(
                    text = if (value == CustomRecitationSettings.UNLIMITED)
                        if (language == AppLanguage.ARABIC) "غير محدود" else "Unlimited"
                    else
                        "$value ${if (language == AppLanguage.ARABIC) "مرات" else "times"}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    modifier = Modifier.padding(vertical = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            IconButton(
                onClick = {
                    if (value == CustomRecitationSettings.UNLIMITED) {
                        onValueChange(minValue)
                    } else if (value < maxValue) {
                        onValueChange(value + 1)
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (language == AppLanguage.ARABIC) "+" else "+",
                    tint = Color(0xFF00897B),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactSpeedControl(
    label: String,
    language: AppLanguage,
    speed: Float,
    onSpeedChange: (Float) -> Unit
) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val currentIndex = CustomRecitationSettings.SPEED_OPTIONS.indexOf(speed)
                    if (currentIndex > 0) {
                        onSpeedChange(CustomRecitationSettings.SPEED_OPTIONS[currentIndex - 1])
                    }
                },
                modifier = Modifier.size(32.dp),
                enabled = speed > CustomRecitationSettings.SPEED_OPTIONS.first()
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = if (language == AppLanguage.ARABIC) "-" else "-",
                    tint = if (speed > CustomRecitationSettings.SPEED_OPTIONS.first())
                        Color(0xFF00897B)
                    else
                        Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF5F5F5)
            ) {
                Text(
                    text = "${speed}x",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    modifier = Modifier.padding(vertical = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            IconButton(
                onClick = {
                    val currentIndex = CustomRecitationSettings.SPEED_OPTIONS.indexOf(speed)
                    if (currentIndex < CustomRecitationSettings.SPEED_OPTIONS.size - 1) {
                        onSpeedChange(CustomRecitationSettings.SPEED_OPTIONS[currentIndex + 1])
                    }
                },
                modifier = Modifier.size(32.dp),
                enabled = speed < CustomRecitationSettings.SPEED_OPTIONS.last()
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (language == AppLanguage.ARABIC) "+" else "+",
                    tint = if (speed < CustomRecitationSettings.SPEED_OPTIONS.last())
                        Color(0xFF00897B)
                    else
                        Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
