package com.quranmedia.player.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.quranmedia.player.BuildConfig
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.presentation.theme.QuranMediaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            QuranMediaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QuranMediaApp(settingsRepository)
                }
            }
        }
    }
}

@Composable
fun QuranMediaApp(settingsRepository: SettingsRepository) {
    val navController = androidx.navigation.compose.rememberNavController()

    // Check if What's New screen should be shown
    val shouldShowWhatsNew = remember {
        settingsRepository.shouldShowWhatsNew(BuildConfig.VERSION_CODE)
    }

    com.quranmedia.player.presentation.navigation.QuranNavGraph(
        navController = navController,
        shouldShowWhatsNew = shouldShowWhatsNew
    )
}
