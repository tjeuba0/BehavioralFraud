package com.poc.behavioralfraud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poc.behavioralfraud.ui.screens.*
import com.poc.behavioralfraud.ui.theme.BehavioralFraudTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BehavioralFraudTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val viewModel: TransferViewModel = viewModel()
    val enrollmentCount by viewModel.enrollmentCount.collectAsState()
    val profile by viewModel.profile.collectAsState()

    var currentScreen by remember { mutableStateOf("home") }

    when (currentScreen) {
        "home" -> HomeScreen(
            enrollmentCount = enrollmentCount,
            profile = profile,
            onNavigateToTransfer = { currentScreen = "transfer" },
            onNavigateToProfile = { currentScreen = "profile" },
            onNavigateToThemePreview = { currentScreen = "theme-preview" },
            onClearData = { viewModel.clearAllData() }
        )
        "transfer" -> TransferScreen(
            viewModel = viewModel,
            onBack = { currentScreen = "home" }
        )
        "profile" -> ProfileScreen(
            profile = profile,
            onBack = { currentScreen = "home" }
        )
        // TASK-010 temp route — proof of theme architecture; folded into
        // DesignSystemPreviewScreen at TASK-014.
        "theme-preview" -> ThemePreviewScreen(onBack = { currentScreen = "home" })
    }
}
