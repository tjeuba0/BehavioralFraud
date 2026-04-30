package com.poc.behavioralfraud.ui.screens.login

import android.app.Application
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for [LoginScreen] — FR-CL-09 REQ-01..05.
 *
 *  - [pin]: digits entered so far (max 6)
 *  - [isBiometricAvailable]: result of `BiometricManager.canAuthenticate(...)`
 *  - [authError]: inline error string (non-null = render error text)
 */
data class LoginUiState(
    val pin: String = "",
    val isBiometricAvailable: Boolean = false,
    val authError: String? = null,
)

/**
 * One-time events emitted by [LoginViewModel] — Channel pattern (banking
 * mandatory per CLAUDE.md) prevents duplicate navigation on configuration
 * changes.
 */
sealed class LoginEvent {
    data object NavigateToHome : LoginEvent()
    data class ShowError(val message: String) : LoginEvent()
}

/**
 * Login auth ViewModel.
 *
 * - Pin entry: any 6 digits passes (POC mock — see warning log on submit).
 * - Biometric: availability checked at init; success/error callbacks routed
 *   in from `BiometricPrompt` callbacks owned by [LoginScreen].
 *
 * **Does NOT open a behavioral session** — Login is auth only per FR-CL-09.
 * Transfer behavioral session is started later at Home tap "Chuyển tiền"
 * (TASK-023).
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        checkBiometricAvailability()
    }

    private fun checkBiometricAvailability() {
        val manager = BiometricManager.from(getApplication())
        val canAuth = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK,
        )
        val available = canAuth == BiometricManager.BIOMETRIC_SUCCESS
        _state.update { it.copy(isBiometricAvailable = available) }
    }

    fun onDigitTap(digit: Char) {
        val current = _state.value.pin
        if (current.length >= PIN_LENGTH) return
        val next = current + digit
        _state.update { it.copy(pin = next, authError = null) }
        if (next.length == PIN_LENGTH) submitPin()
    }

    fun onBackspace() {
        _state.update {
            it.copy(pin = it.pin.dropLast(1), authError = null)
        }
    }

    private fun submitPin() {
        Log.w(
            TAG,
            "POC PIN mock — KHÔNG dùng cho production. Any 6-digit PIN passes.",
        )
        viewModelScope.launch {
            _events.send(LoginEvent.NavigateToHome)
        }
    }

    fun onBiometricSuccess() {
        viewModelScope.launch {
            _events.send(LoginEvent.NavigateToHome)
        }
    }

    fun onBiometricError(message: String) {
        _state.update { it.copy(authError = message) }
    }

    /**
     * Called by [LoginScreen] when it re-enters composition (e.g., user
     * popped back from Home). Clears any stale entry so dots reset to empty.
     */
    fun resetEntry() {
        _state.update { it.copy(pin = "", authError = null) }
    }

    private companion object {
        const val PIN_LENGTH = 6
        const val TAG = "LoginViewModel"
    }
}
