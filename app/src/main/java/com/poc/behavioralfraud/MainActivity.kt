package com.poc.behavioralfraud

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.poc.behavioralfraud.ui.navigation.AppRoutes
import com.poc.behavioralfraud.ui.screens.DesignSystemPreviewScreen
import com.poc.behavioralfraud.ui.screens.HomeIPayScreen
import com.poc.behavioralfraud.ui.screens.dev.DevMenuScreen
import com.poc.behavioralfraud.ui.screens.dev.ManualOverrideScreen
import com.poc.behavioralfraud.ui.screens.dev.ProfileInspectorScreen
import com.poc.behavioralfraud.ui.screens.dev.RiskHistoryScreen
import com.poc.behavioralfraud.ui.screens.dev.SessionInspectorScreen
import com.poc.behavioralfraud.ui.screens.ProfileScreen
import com.poc.behavioralfraud.ui.screens.TransferScreen
import com.poc.behavioralfraud.ui.screens.TransferViewModel
import com.poc.behavioralfraud.ui.screens.login.LoginScreen
import com.poc.behavioralfraud.ui.screens.transfer.OtpScreen
import com.poc.behavioralfraud.ui.screens.transfer.RecipientScreen
import com.poc.behavioralfraud.ui.screens.transfer.TransferFormScreen
import com.poc.behavioralfraud.ui.screens.transfer.TransferOrchestratorViewModel
import com.poc.behavioralfraud.ui.screens.transfer.TransferSuccessScreen
import com.poc.behavioralfraud.ui.screens.transfer.TransferType
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

        // Edge-to-edge: app draws under status bar; Compose handles insets via
        // WindowInsets.statusBars. Status bar color = transparent so backdrop
        // gradient (BG/premium/new) shows through behind clock/wifi/battery.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

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
    // Legacy POC VM still drives TRANSFER_LEGACY route (existing TransferScreen)
    // until TASK-024 absorbs its enrollment/verification UI into Dev Menu.
    val viewModel: TransferViewModel = viewModel()
    // New orchestrator VM owning the production-feel transfer flow state
    // (TransferType → Recipient → Form → Otp → Success).
    val orchestratorVm: TransferOrchestratorViewModel = viewModel()

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
            HomeRoute(
                navController = navController,
                viewModel = viewModel,
                orchestratorVm = orchestratorVm,
            )
        }
        // TransferTypeScreen removed — does not exist in Figma 1:15393.
        // Home tap "Chuyển tiền trong nước" navigates directly to Recipient,
        // which determines transferType from the bank user picks (VietinBank →
        // Internal, others → Napas).
        composable(AppRoutes.TRANSFER_RECIPIENT) {
            RecipientScreen(
                transferType = TransferType.Internal,  // default; refactor via bank pick at next Recipient fidelity rewrite
                onContinue = { accountNumber, bank ->
                    val derivedType = if (bank.code == "CTG") TransferType.Internal else TransferType.Napas
                    orchestratorVm.setTransferType(derivedType)
                    orchestratorVm.setRecipient(accountNumber, bank)
                    navController.navigate(AppRoutes.TRANSFER_FORM)
                },
                onBack = { navController.popBackStack() },
                collector = orchestratorVm.collector,
            )
        }
        composable(AppRoutes.TRANSFER_FORM) {
            TransferFormScreen(
                viewModel = orchestratorVm,
                onNavigateToOtp = { navController.navigate(AppRoutes.TRANSFER_OTP) },
                onSheetShown = { viewModel.collector.onOverLimitSheetShown() },
                onSheetDecision = { decision ->
                    viewModel.collector.onOverLimitDecision(decision)
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(AppRoutes.TRANSFER_OTP) {
            OtpScreen(
                viewModel = orchestratorVm,
                onNavigateToSuccess = {
                    navController.navigate(AppRoutes.TRANSFER_SUCCESS) {
                        // Clear transfer flow back stack so back from Success exits to Home
                        popUpTo(AppRoutes.HOME)
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(AppRoutes.TRANSFER_SUCCESS) {
            TransferSuccessScreen(
                viewModel = orchestratorVm,
                onNavigateHome = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.HOME) { inclusive = true }
                    }
                },
                onNavigateNewTransfer = {
                    orchestratorVm.reset()
                    navController.navigate(AppRoutes.TRANSFER_RECIPIENT) {
                        popUpTo(AppRoutes.HOME)
                    }
                },
            )
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

        // ─── Dev Menu (TASK-024) — accessed via long-press logo on Home ───
        composable(AppRoutes.DEV) {
            DevMenuScreen(
                onBack = { navController.popBackStack() },
                onProfileInspector = { navController.navigate(AppRoutes.DEV_PROFILE) },
                onRiskHistory = { navController.navigate(AppRoutes.DEV_RISK_HISTORY) },
                onSessionInspector = { navController.navigate(AppRoutes.DEV_SESSION) },
                onManualOverride = { navController.navigate(AppRoutes.DEV_MANUAL_OVERRIDE) },
                onDesignSystem = { navController.navigate(AppRoutes.DEV_DESIGN_SYSTEM) },
            )
        }
        composable(AppRoutes.DEV_PROFILE) {
            ProfileInspectorScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.DEV_RISK_HISTORY) {
            RiskHistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.DEV_SESSION) {
            SessionInspectorScreen(
                collector = orchestratorVm.collector,
                onBack = { navController.popBackStack() },
            )
        }
        composable(AppRoutes.DEV_MANUAL_OVERRIDE) {
            ManualOverrideScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.DEV_DESIGN_SYSTEM) {
            DesignSystemPreviewScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun HomeRoute(
    navController: NavHostController,
    @Suppress("UNUSED_PARAMETER") viewModel: TransferViewModel,
    orchestratorVm: TransferOrchestratorViewModel,
) {
    // HomeIPayScreen is intentionally stateless w.r.t. enrollment/verification —
    // those signals stay silent until TASK-024 surfaces them in Dev Menu.
    // viewModel is kept on the route signature so TASK-023 can wire
    // `collector.startSession()` here when the user taps "Chuyển tiền trong nước".
    HomeIPayScreen(
        onNavigateToTransfer = {
            // Reset orchestrator state so each transfer flow starts fresh.
            // (Behavioral session start lands in TASK-023.)
            orchestratorVm.reset()
            navController.navigate(AppRoutes.TRANSFER_RECIPIENT)
        },
        // Long-press logo → Dev Menu (REQ-16) — replaces direct Design System link.
        onNavigateToDevPreview = { navController.navigate(AppRoutes.DEV) },
    )
}
