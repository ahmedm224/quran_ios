package com.quranmedia.player.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.quranmedia.player.wear.presentation.navigation.WearNavGraph
import com.quranmedia.player.wear.presentation.theme.AlFurqanWearTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AlFurqanWearTheme {
                WearNavGraph()
            }
        }
    }
}
