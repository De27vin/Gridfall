package com.example.gridfall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.gridfall.ui.GameScreen
import com.example.gridfall.ui.theme.GridfallTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GridfallTheme {
                // Explicit debug-only entry point for production READ-ONLY QA; no guest creation.
                val readOnlyQa = BuildConfig.BUILD_TYPE == "debug" && intent.getBooleanExtra("communityReadOnlyQa", false)
                GameScreen(autoCreateGuest = !readOnlyQa, startInCommunityMaps = readOnlyQa)
            }
        }
    }
}
