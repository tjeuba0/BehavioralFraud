package com.poc.behavioralfraud

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.poc.behavioralfraud.ui.navigation.AppRoutes
import com.poc.behavioralfraud.ui.screens.DesignSystemPreviewScreen
import com.poc.behavioralfraud.ui.screens.HomeIPayScreen
import com.poc.behavioralfraud.ui.screens.ProfileScreen
import com.poc.behavioralfraud.ui.screens.TransferScreen
import com.poc.behavioralfraud.ui.screens.TransferViewModel
import com.poc.behavioralfraud.ui.screens.login.LoginScreen
import com.poc.behavioralfraud.ui.theme.BehavioralFraudTheme

/**
 * Hosts the entire Compose tree.
 *
 * Extends [FragmentActivity] (not bare ComponentActivity) — required by
 * `androidx.biometric.BiometricPrompt` used in [LoginScreen]. FragmentActivity
 * is fully compose-compatible (activity-compose's setContent is an extension
 * on ComponentActivity, FragmentActivity inherits it).
 */
class MainActivity : FragmentActivity() {
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
 * Start destination: [AppRoutes.LOGIN] (TASK-016). After auth, navigate to
 * [AppRoutes.HOME] popping LOGIN inclusive so back from Home exits app.
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
        startDestination = AppRoutes.LOGIN,
    ) {
        composable(AppRoutes.LOGIN) {
            LoginScreen(
                onAuthenticated = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
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
    @Suppress("UNUSED_PARAMETER") viewModel: TransferViewModel,
) {
    // HomeIPayScreen is intentionally stateless w.r.t. enrollment/verification —
    // those signals stay silent until TASK-024 surfaces them in Dev Menu.
    // viewModel is kept on the route signature so TASK-023 can wire
    // `collector.startSession()` here when the user taps "Chuyển tiền trong nước".
    HomeIPayScreen(
        onNavigateToTransfer = { navController.navigate(AppRoutes.TRANSFER_LEGACY) },
        onNavigateToDevPreview = { navController.navigate(AppRoutes.DESIGN_SYSTEM_LEGACY) },
    )
}
