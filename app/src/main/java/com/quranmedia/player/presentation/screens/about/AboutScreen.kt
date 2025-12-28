package com.quranmedia.player.presentation.screens.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.BuildConfig
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.util.Strings
import com.quranmedia.player.presentation.util.layoutDirection

private val islamicGreen = Color(0xFF2E7D32)
private val lightGreen = Color(0xFF66BB6A)
private val darkGreen = Color(0xFF1B5E20)
private val creamBackground = Color(0xFFFAF8F3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: AboutViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val language = settings.appLanguage

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = Strings.about.get(language),
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App Info Card - Compact
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = islamicGreen,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "الفرقان",
                                    fontFamily = scheherazadeFont,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = islamicGreen
                                )
                                Text(
                                    text = "Alfurqan",
                                    fontSize = 16.sp,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = "${Strings.version.get(language)} ${BuildConfig.VERSION_NAME}",
                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Developed By Card - Compact (no description)
                CreditCardCompact(
                    icon = Icons.Default.Code,
                    title = if (language == AppLanguage.ARABIC) "تطوير" else "Developed By",
                    subtitle = "cloudlinqed.com",
                    iconColor = Color(0xFF1976D2),
                    backgroundColor = Color(0xFFE3F2FD),
                    language = language
                )

                // Data Sources Card - Detailed
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = islamicGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (language == AppLanguage.ARABIC) "مصادر البيانات" else "Data Sources",
                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = islamicGreen
                            )
                        }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                        // Tanzil
                        DataSourceCard(
                            title = "tanzil.net",
                            description = if (language == AppLanguage.ARABIC)
                                "نص القرآن الكريم بالرسم العثماني - مرجع موثوق لنص القرآن المحقق"
                            else
                                "Quran text in Uthmani script - Verified and authentic Quran text source",
                            language = language
                        )

                        // Al-Quran Cloud
                        DataSourceCard(
                            title = "alquran.cloud",
                            description = if (language == AppLanguage.ARABIC)
                                "واجهة برمجة للتلاوات الصوتية - توفر تلاوات لأكثر من ١٢٠ قارئ"
                            else
                                "Audio recitations API - Provides recitations from 120+ reciters",
                            language = language
                        )

                        // EveryAyah
                        DataSourceCard(
                            title = "everyayah.com",
                            description = if (language == AppLanguage.ARABIC)
                                "تلاوات إضافية بجودة عالية - مع توقيت دقيق على مستوى الآيات"
                            else
                                "Additional high-quality recitations - With precise ayah-level timing",
                            language = language
                        )
                    }
                }

                // Privacy & Contact - Combined Compact
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Privacy row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = islamicGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == AppLanguage.ARABIC) "الخصوصية" else "Privacy",
                                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                )
                                Text(
                                    text = if (language == AppLanguage.ARABIC)
                                        "لا جمع بيانات • لا تتبع • تخزين محلي فقط"
                                    else
                                        "No data collection • No tracking • Local storage only",
                                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                        // Contact row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = islamicGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == AppLanguage.ARABIC) "التواصل" else "Contact",
                                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                )
                                Text(
                                    text = "info@cloudlinqed.com",
                                    fontSize = 12.sp,
                                    color = islamicGreen
                                )
                            }
                        }
                    }
                }

                // Copyright - Simple
                Text(
                    text = "© 2025 cloudlinqed.com",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CreditCardCompact(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    backgroundColor: Color,
    language: AppLanguage
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = backgroundColor
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = subtitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun DataSourceCard(
    title: String,
    description: String,
    language: AppLanguage
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = islamicGreen
        )
        Text(
            text = description,
            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
            fontSize = 12.sp,
            color = Color.Gray,
            lineHeight = 16.sp
        )
    }
}
