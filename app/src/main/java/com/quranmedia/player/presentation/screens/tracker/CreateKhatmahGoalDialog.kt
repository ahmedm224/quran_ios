package com.quranmedia.player.presentation.screens.tracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.domain.model.GoalType
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val islamicGreen = Color(0xFF2E7D32)
private val darkGreen = Color(0xFF1B5E20)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateKhatmahGoalDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onCreate: (name: String, startDate: LocalDate, endDate: LocalDate, goalType: GoalType) -> Unit
) {
    var goalName by remember { mutableStateOf("") }
    var selectedGoalType by remember { mutableStateOf(GoalType.MONTHLY) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(30)) }
    var numberOfDays by remember { mutableStateOf("30") }
    var showGoalTypeMenu by remember { mutableStateOf(false) }

    // Auto-update dates when goal type changes
    LaunchedEffect(selectedGoalType) {
        when (selectedGoalType) {
            GoalType.MONTHLY -> {
                startDate = LocalDate.now()
                // Calculate end of Hijri month
                val currentHijri = com.quranmedia.player.domain.util.HijriCalendarUtils
                    .gregorianToHijri(LocalDate.now())
                endDate = com.quranmedia.player.domain.util.HijriCalendarUtils
                    .getEndOfHijriMonth(LocalDate.now(), currentHijri)
                goalName = if (language == AppLanguage.ARABIC) "ختمة شهرية" else "Monthly Khatmah"
            }
            GoalType.CUSTOM -> {
                startDate = LocalDate.now()
                numberOfDays = "30"
                endDate = LocalDate.now().plusDays(30)
                goalName = if (language == AppLanguage.ARABIC) "ختمة مخصصة" else "Custom Khatmah"
            }
        }
    }

    // Update end date when number of days changes (for CUSTOM type)
    LaunchedEffect(numberOfDays, selectedGoalType) {
        if (selectedGoalType == GoalType.CUSTOM) {
            val days = numberOfDays.toIntOrNull() ?: 30
            endDate = startDate.plusDays(days.toLong())
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = if (language == AppLanguage.ARABIC) "إنشاء هدف ختمة" else "Create Khatmah Goal",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 22.sp,
                    color = darkGreen
                )

                // Goal Type Dropdown
                Box {
                    OutlinedCard(
                        onClick = { showGoalTypeMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (language == AppLanguage.ARABIC) "نوع الهدف" else "Goal Type",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = if (language == AppLanguage.ARABIC)
                                        selectedGoalType.nameArabic
                                    else
                                        selectedGoalType.nameEnglish,
                                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                    fontSize = 16.sp,
                                    color = Color.DarkGray
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = showGoalTypeMenu,
                        onDismissRequest = { showGoalTypeMenu = false }
                    ) {
                        GoalType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (language == AppLanguage.ARABIC) type.nameArabic else type.nameEnglish,
                                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                                    )
                                },
                                onClick = {
                                    selectedGoalType = type
                                    showGoalTypeMenu = false
                                }
                            )
                        }
                    }
                }

                // Goal Name Input
                OutlinedTextField(
                    value = goalName,
                    onValueChange = { goalName = it },
                    label = {
                        Text(
                            if (language == AppLanguage.ARABIC) "اسم الهدف" else "Goal Name",
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = islamicGreen,
                        focusedLabelColor = islamicGreen
                    )
                )

                // Number of Days Input (only for CUSTOM goal type)
                if (selectedGoalType == GoalType.CUSTOM) {
                    OutlinedTextField(
                        value = numberOfDays,
                        onValueChange = { value ->
                            // Only allow digits
                            if (value.all { it.isDigit() } && value.length <= 4) {
                                numberOfDays = value
                            }
                        },
                        label = {
                            Text(
                                if (language == AppLanguage.ARABIC) "عدد الأيام" else "Number of Days",
                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = islamicGreen,
                            focusedLabelColor = islamicGreen
                        )
                    )
                }

                // Date Info (read-only display)
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (language == AppLanguage.ARABIC) "تاريخ البدء" else "Start Date",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                                    fontSize = 16.sp,
                                    color = Color.DarkGray
                                )
                            }
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                Text(
                                    text = if (language == AppLanguage.ARABIC) "تاريخ الانتهاء" else "End Date",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                                    fontSize = 16.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (language == AppLanguage.ARABIC)
                                "${java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate)} يوم"
                            else
                                "${java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate)} days",
                            fontSize = 14.sp,
                            color = islamicGreen
                        )
                    }
                }

                // Info text
                Text(
                    text = if (language == AppLanguage.ARABIC)
                        "سيتم تتبع تقدمك لقراءة ${604} صفحة خلال الفترة المحددة"
                    else
                        "Your progress will be tracked for reading 604 pages during this period",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (language == AppLanguage.ARABIC) "إلغاء" else "Cancel",
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                        )
                    }
                    Button(
                        onClick = {
                            if (goalName.isNotBlank()) {
                                onCreate(goalName, startDate, endDate, selectedGoalType)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = islamicGreen),
                        enabled = goalName.isNotBlank() &&
                            (selectedGoalType == GoalType.MONTHLY ||
                             (numberOfDays.toIntOrNull() ?: 0) > 0)
                    ) {
                        Text(
                            if (language == AppLanguage.ARABIC) "إنشاء" else "Create",
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                        )
                    }
                }
            }
        }
    }
}
