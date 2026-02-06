package com.quranmedia.player.presentation.screens.tracker

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.domain.model.ActivityType
import com.quranmedia.player.domain.model.GoalType
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.screens.reader.components.islamicGreen
import com.quranmedia.player.presentation.screens.reader.components.lightGreen
import com.quranmedia.player.presentation.screens.reader.components.darkGreen
import com.quranmedia.player.presentation.screens.reader.components.goldAccent
import com.quranmedia.player.presentation.screens.reader.components.creamBackground
import com.quranmedia.player.presentation.components.CommonOverflowMenu
import com.quranmedia.player.presentation.util.Strings
import com.quranmedia.player.presentation.util.layoutDirection
import com.quranmedia.player.domain.util.ArabicNumeralUtils
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    viewModel: TrackerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPrayerTimes: () -> Unit = {},
    onNavigateToAthkar: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToReading: () -> Unit = {},
    onNavigateToImsakiya: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState(initial = com.quranmedia.player.data.repository.UserSettings())

    // Get language from settings
    val language = settings.appLanguage

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "المتابعة اليومية" else "Daily Tracker",
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            fontSize = 22.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                if (language == AppLanguage.ARABIC) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        CommonOverflowMenu(
                            language = language,
                            onNavigateToSettings = onNavigateToSettings,
                            onNavigateToReading = onNavigateToReading,
                            onNavigateToPrayerTimes = onNavigateToPrayerTimes,
                            onNavigateToImsakiya = onNavigateToImsakiya,
                            onNavigateToAthkar = onNavigateToAthkar,
                            onNavigateToDownloads = onNavigateToDownloads,
                            onNavigateToAbout = onNavigateToAbout,
                            hideTracker = true  // Hide Tracker since we're on this screen
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = islamicGreen,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            containerColor = creamBackground
        ) { paddingValues ->
            val useIndoArabic = language == AppLanguage.ARABIC && settings.useIndoArabicNumerals
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Hijri Date Header
                item {
                    HijriDateCard(
                        hijriDate = uiState.hijriDate,
                        language = language,
                        useIndoArabic = useIndoArabic
                    )
                }

                // Daily Activities (Azkar)
                item {
                    DailyActivitiesCard(
                        activities = uiState.todayActivities,
                        language = language,
                        onActivityToggle = { activityType ->
                            viewModel.toggleActivity(activityType)
                        }
                    )
                }

                // Quran Progress Today
                item {
                    QuranProgressCard(
                        progress = uiState.todayProgress,
                        language = language,
                        onIncrementListened = { viewModel.incrementPagesListened(1) },
                        onDecrementListened = { viewModel.incrementPagesListened(-1) },
                        useIndoArabic = useIndoArabic
                    )
                }

                // Active Khatmah Goal
                item {
                    ActiveKhatmahGoalCard(
                        goalProgress = uiState.activeGoalProgress,
                        language = language,
                        onCreateGoal = { viewModel.showCreateGoalDialog() },
                        useIndoArabic = useIndoArabic
                    )
                }

                // All Goals Section
                item {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "جميع الأهداف" else "All Goals",
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkGreen,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(uiState.allGoals) { goal ->
                    GoalListItem(
                        goal = goal,
                        isActive = goal.isActive,
                        language = language,
                        onSetActive = { viewModel.setActiveGoal(goal.id) },
                        onDelete = { viewModel.deleteGoal(goal.id) }
                    )
                }

                // Create Goal Button
                item {
                    Button(
                        onClick = { viewModel.showCreateGoalDialog() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = islamicGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.ARABIC) "إنشاء هدف جديد" else "Create New Goal",
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // Create Goal Dialog
        if (uiState.showCreateGoalDialog) {
            CreateKhatmahGoalDialog(
                language = language,
                onDismiss = { viewModel.hideCreateGoalDialog() },
                onCreate = { name, startDate, endDate, goalType ->
                    viewModel.createKhatmahGoal(name, startDate, endDate, goalType)
                }
            )
        }

        // Error Snackbar
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // Show snackbar for error
                viewModel.clearError()
            }
        }
    }
}

@Composable
private fun HijriDateCard(
    hijriDate: com.quranmedia.player.domain.model.HijriDate?,
    language: AppLanguage,
    useIndoArabic: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            islamicGreen.copy(alpha = 0.1f),
                            islamicGreen.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = islamicGreen,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (language == AppLanguage.ARABIC) "التاريخ الهجري" else "Hijri Date",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = hijriDate?.let {
                        val dayStr = ArabicNumeralUtils.formatNumber(it.day, useIndoArabic)
                        val yearStr = ArabicNumeralUtils.formatNumber(it.year, useIndoArabic)
                        "$dayStr ${if (language == AppLanguage.ARABIC) it.monthArabic else it.month} $yearStr"
                    } ?: "...",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkGreen
                )
            }
        }
    }
}

@Composable
private fun DailyActivitiesCard(
    activities: List<com.quranmedia.player.domain.model.DailyActivity>,
    language: AppLanguage,
    onActivityToggle: (ActivityType) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = islamicGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == AppLanguage.ARABIC) "الأنشطة اليومية" else "Daily Activities",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            activities.forEach { activity ->
                AthkarCheckboxRow(
                    activity = activity,
                    language = language,
                    onToggle = { onActivityToggle(activity.activityType) }
                )
            }
        }
    }
}

@Composable
private fun AthkarCheckboxRow(
    activity: com.quranmedia.player.domain.model.DailyActivity,
    language: AppLanguage,
    onToggle: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Animation state for scale effect - MORE NOTICEABLE
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,  // Bigger scale change (12% instead of 5%)
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium  // Faster animation
        ),
        label = "scale"
    )

    // Background color animation
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) islamicGreen.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(durationMillis = 100),
        label = "backgroundColor"
    )

    // Animation state for check mark
    var showCheckAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(activity.completed) {
        if (activity.completed) {
            showCheckAnimation = true
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)  // Add background color change
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material.ripple.rememberRipple(
                    bounded = true,
                    color = islamicGreen,
                    radius = 300.dp  // Larger ripple
                )
            ) {
                // STRONGER haptic feedback on tap
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                // Visual press feedback
                isPressed = true

                // Call toggle
                onToggle()

                // Reset press state after animation
                scope.launch {
                    kotlinx.coroutines.delay(200)  // Slightly longer for better feel
                    isPressed = false
                }
            }
            .padding(vertical = 8.dp, horizontal = 4.dp),  // Add horizontal padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = activity.completed,
            onCheckedChange = null, // Handle click on the row instead
            colors = CheckboxDefaults.colors(
                checkedColor = islamicGreen,
                uncheckedColor = Color.Gray
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (language == AppLanguage.ARABIC)
                activity.activityType.nameArabic
            else
                activity.activityType.nameEnglish,
            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
            fontSize = 16.sp,
            color = if (activity.completed) Color.Gray else Color.DarkGray
        )

        // Animated check icon when completed
        if (activity.completed && showCheckAnimation) {
            Spacer(modifier = Modifier.weight(1f))

            val checkScale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "checkScale"
            )

            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = islamicGreen,
                modifier = Modifier
                    .size(20.dp)
                    .scale(checkScale)
            )
        }
    }
}

@Composable
private fun QuranProgressCard(
    progress: com.quranmedia.player.domain.model.QuranProgress?,
    language: AppLanguage,
    onIncrementListened: () -> Unit,
    onDecrementListened: () -> Unit,
    useIndoArabic: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = islamicGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == AppLanguage.ARABIC) "تقدم القرآن اليوم" else "Today's Quran Progress",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkGreen
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Pages Read
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "المقروءة" else "Read",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = ArabicNumeralUtils.formatNumber(progress?.pagesRead ?: 0, useIndoArabic),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = islamicGreen
                    )
                    Text(
                        text = if (language == AppLanguage.ARABIC) "صفحة" else "pages",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(80.dp),
                    color = Color.Gray.copy(alpha = 0.3f)
                )

                // Pages Listened (with manual adjustment)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "المسموعة" else "Listened",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = onDecrementListened,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Decrease",
                                tint = islamicGreen
                            )
                        }

                        Text(
                            text = ArabicNumeralUtils.formatNumber(progress?.pagesListened ?: 0, useIndoArabic),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = islamicGreen,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = onIncrementListened,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Increase",
                                tint = islamicGreen
                            )
                        }
                    }

                    Text(
                        text = if (language == AppLanguage.ARABIC) "صفحة" else "pages",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveKhatmahGoalCard(
    goalProgress: com.quranmedia.player.domain.model.KhatmahProgress?,
    language: AppLanguage,
    onCreateGoal: () -> Unit,
    useIndoArabic: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Flag,
                    contentDescription = null,
                    tint = islamicGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == AppLanguage.ARABIC) "هدف الختمة النشط" else "Active Khatmah Goal",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (goalProgress != null) {
                // Goal name
                Text(
                    text = goalProgress.goal.name,
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkGreen
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar
                Column {
                    LinearProgressIndicator(
                        progress = (goalProgress.progressPercentage / 100f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (goalProgress.isOnTrack) islamicGreen else Color(0xFFFF6B6B),
                        trackColor = Color.Gray.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${ArabicNumeralUtils.formatNumber(goalProgress.progressPercentage.toInt(), useIndoArabic)}% ${if (language == AppLanguage.ARABIC) "مكتمل" else "complete"}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        label = if (language == AppLanguage.ARABIC) "مكتملة" else "Completed",
                        value = ArabicNumeralUtils.formatNumber(goalProgress.pagesCompleted, useIndoArabic),
                        language = language
                    )
                    StatItem(
                        label = if (language == AppLanguage.ARABIC) "متبقية" else "Remaining",
                        value = ArabicNumeralUtils.formatNumber(goalProgress.pagesRemaining, useIndoArabic),
                        language = language
                    )
                    StatItem(
                        label = if (language == AppLanguage.ARABIC) "يومي" else "Daily",
                        value = if (useIndoArabic) ArabicNumeralUtils.toIndoArabic(String.format("%.1f", goalProgress.dailyTargetPages)) else String.format("%.1f", goalProgress.dailyTargetPages),
                        language = language
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // On-track indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (goalProgress.isOnTrack) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (goalProgress.isOnTrack) islamicGreen else Color(0xFFFF8A65),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (goalProgress.isOnTrack) {
                            if (language == AppLanguage.ARABIC) "على المسار الصحيح" else "On Track"
                        } else {
                            if (language == AppLanguage.ARABIC) "تحتاج لتسريع القراءة" else "Need to catch up"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (goalProgress.isOnTrack) islamicGreen else Color(0xFFFF8A65)
                    )
                }
            } else {
                // No active goal
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (language == AppLanguage.ARABIC) "لا يوجد هدف نشط" else "No active goal",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onCreateGoal) {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "إنشاء هدف" else "Create Goal",
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    language: AppLanguage
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = islamicGreen
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun GoalListItem(
    goal: com.quranmedia.player.domain.model.KhatmahGoal,
    isActive: Boolean,
    language: AppLanguage,
    onSetActive: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) islamicGreen.copy(alpha = 0.1f) else Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { if (!isActive) onSetActive() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isActive) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = islamicGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = goal.name,
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 16.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) islamicGreen else Color.DarkGray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${goal.startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} → ${goal.endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (!isActive) {
                        DropdownMenuItem(
                            text = { Text(if (language == AppLanguage.ARABIC) "تعيين كنشط" else "Set as Active") },
                            onClick = {
                                onSetActive()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (language == AppLanguage.ARABIC) "حذف" else "Delete", color = Color.Red) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                    )
                }
            }
        }
    }
}
