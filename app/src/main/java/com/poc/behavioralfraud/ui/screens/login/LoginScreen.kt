package com.poc.behavioralfraud.ui.screens.login

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poc.behavioralfraud.ui.components.IPayNumericKeypad
import com.poc.behavioralfraud.ui.components.IPayPinDots
import com.poc.behavioralfraud.ui.components.safeClickable
import com.poc.behavioralfraud.ui.theme.IPayTheme

/**
 * Login screen — FR-CL-09 REQ-01.
 *
 * Layout (top → bottom):
 *  - VietinBank logo placeholder (Icons.Default.AccountBalance)
 *  - Greeting "Xin chào, Vandz" (POC hard-coded username)
 *  - Subtitle "Nhập mã PIN của bạn"
 *  - 6 PIN dots (filled-circle mode)
 *  - Inline error text (when present)
 *  - Numeric keypad with optional biometric key
 *  - "Quên mật khẩu?" link (visual only)
 *
 * **No behavioral session is started here** — Login is auth only. The transfer
 * behavioral session begins at Home tap "Chuyển tiền" (TASK-023).
 *
 * Hosting requirement: must be hosted in a [FragmentActivity] (BiometricPrompt
 * dependency). MainActivity now extends [FragmentActivity] in this task.
 */
@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
        ?: error("LoginScreen must be hosted in a FragmentActivity for BiometricPrompt")

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                LoginEvent.NavigateToHome -> onAuthenticated()
                is LoginEvent.ShowError -> Unit // POC: skip snackbar
            }
        }
    }

    val biometricPrompt = remember(activity) {
        BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    viewModel.onBiometricSuccess()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    viewModel.onBiometricError(errString.toString())
                }
            },
        )
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Đăng nhập iPay")
            .setSubtitle("Sử dụng vân tay/Face ID")
            .setNegativeButtonText("Hủy")
            .build()
    }

    LoginContent(
        pinLength = state.pin.length,
        authError = state.authError,
        showBiometric = state.isBiometricAvailable,
        onDigitTap = viewModel::onDigitTap,
        onBackspaceTap = viewModel::onBackspace,
        onBiometricTap = { biometricPrompt.authenticate(promptInfo) },
        modifier = modifier,
    )
}

@Composable
private fun LoginContent(
    pinLength: Int,
    authError: String?,
    showBiometric: Boolean,
    onDigitTap: (Char) -> Unit,
    onBackspaceTap: () -> Unit,
    onBiometricTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IPayTheme.colors.bgNeutralPrimary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = IPayTheme.spacing.s24)
                .padding(vertical = IPayTheme.spacing.s32),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(Modifier.height(IPayTheme.spacing.s40))

            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = "VietinBank",
                tint = IPayTheme.colors.textBrandPrimary,
                modifier = Modifier.size(LoginScreenDefaults.LOGO_SIZE_DP.dp),
            )

            Spacer(Modifier.height(IPayTheme.spacing.s24))

            Text(
                text = "Xin chào, Vandz",
                style = IPayTheme.typography.headingSmall,
                color = IPayTheme.colors.textNeutralPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(IPayTheme.spacing.s8))

            Text(
                text = "Nhập mã PIN của bạn",
                style = IPayTheme.typography.bodyMedium,
                color = IPayTheme.colors.textNeutralSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(IPayTheme.spacing.s40))

            IPayPinDots(
                length = LoginScreenDefaults.PIN_LENGTH,
                enteredCount = pinLength,
            )

            Spacer(Modifier.height(IPayTheme.spacing.s16))

            if (authError != null) {
                Text(
                    text = authError,
                    style = IPayTheme.typography.bodyMedium,
                    color = IPayTheme.colors.iconWarning,
                    textAlign = TextAlign.Center,
                )
            } else {
                Spacer(Modifier.height(IPayTheme.spacing.s16))
            }

            Spacer(Modifier.height(IPayTheme.spacing.s32))

            IPayNumericKeypad(
                onDigitTap = onDigitTap,
                onBackspaceTap = onBackspaceTap,
                onBiometricTap = if (showBiometric) onBiometricTap else null,
            )

            Spacer(Modifier.height(IPayTheme.spacing.s24))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeClickable { /* POC: visual only */ }
                    .padding(IPayTheme.spacing.s12),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Quên mật khẩu?",
                    style = IPayTheme.typography.labelMedium,
                    color = IPayTheme.colors.textBrandPrimary,
                )
            }
        }
    }
}

private object LoginScreenDefaults {
    const val PIN_LENGTH: Int = 6
    const val LOGO_SIZE_DP: Int = 64
}
