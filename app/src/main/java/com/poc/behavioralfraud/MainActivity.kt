package com.poc.behavioralfraud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.poc.behavioralfraud.ui.navigation.AppRoutes
import com.poc.behavioralfraud.ui.screens.DesignSystemPreviewScreen
import com.poc.behavioralfraud.ui.screens.HomeScreen
import com.poc.behavioralfraud.ui.screens.ProfileScreen
import com.poc.behavioralfraud.ui.screens.TransferScreen
import com.poc.behavioralfraud.ui.screens.TransferViewModel
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

/**
 * Root navigation graph — FR-CL-10 REQ-09.
 *
 * Replaces the previous `when (currentScreen)` switch-case with a NavHost.
 * Routes registered in [AppRoutes]. Future tasks (TASK-016 Login, TASK-018..022
 * transfer flow, TASK-024 Dev Menu) extend this graph by adding `composable(...)`
 * blocks for new routes.
 *
 * Start destination: [AppRoutes.HOME] for now. Will switch to [AppRoutes.LOGIN]
 * at TASK-016 when LoginScreen lands.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // Single shared TransferViewModel scoped to MainActivity — preserves existing
    // POC behavior. Will be replaced by per-flow VM at TASK-019
    // (TransferOrchestratorViewModel) when transfer flow splits across screens.
    val viewModel: TransferViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME,
    ) {
        composable(AppRoutes.HOME) {
            HomeRoute(navController = navController, viewModel = viewModel)
        }
        composable(AppRoutes.TRANSFER_LEGACY) {
            TransferScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(AppRoutes.PROFILE_LEGACY) {
            val profile by viewModel.profile.collectAsState()
            ProfileScreen(
                profile = profile,
                onBack = { navController.popBackStack() },
            )
        }
        composable(AppRoutes.DESIGN_SYSTEM_LEGACY) {
            DesignSystemPreviewScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun HomeRoute(
    navController: NavHostController,
    viewModel: TransferViewModel,
) {
    val enrollmentCount by viewModel.enrollmentCount.collectAsState()
    val profile by viewModel.profile.collectAsState()

    HomeScreen(
        enrollmentCount = enrollmentCount,
        profile = profile,
        onNavigateToTransfer = { navController.navigate(AppRoutes.TRANSFER_LEGACY) },
        onNavigateToProfile = { navController.navigate(AppRoutes.PROFILE_LEGACY) },
        onNavigateToThemePreview = { navController.navigate(AppRoutes.DESIGN_SYSTEM_LEGACY) },
        onClearData = { viewModel.clearAllData() },
    )
}
