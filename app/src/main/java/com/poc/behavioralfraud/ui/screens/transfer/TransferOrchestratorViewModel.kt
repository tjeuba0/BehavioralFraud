package com.poc.behavioralfraud.ui.screens.transfer

import androidx.lifecycle.ViewModel
import com.poc.behavioralfraud.data.mock.MockBank
import com.poc.behavioralfraud.data.mock.MockTransferLimits
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * Single VM that owns the state of a transfer flow — FR-CL-10 REQ-08.
 *
 * Lives across TransferType → Recipient → Form → Otp → Success. State holds
 * everything needed to render any of those screens; events are one-shot
 * navigation/decision triggers via [Channel] (CLAUDE.md mandatory pattern for
 * banking — prevents duplicate triggers on configuration change).
 *
 * Lifecycle: instantiated when transfer flow starts (Home tap "Chuyển tiền
 * trong nước"). Reset via [reset] before re-entering flow. Behavioral session
 * lifecycle (start/stop) is wired in TASK-023.
 *
 * `riskResult` is intentionally part of state but UI in production-feel
 * screens MUST NOT bind to it — Dev Menu (TASK-024) will surface it. Keeping
 * it here so the field's existence is documented at the orchestrator level.
 */
class TransferOrchestratorViewModel : ViewModel() {

    private val _state = MutableStateFlow(TransferState())
    val state: StateFlow<TransferState> = _state.asStateFlow()

    private val _events = Channel<TransferEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // ─── Setters from screens ──────────────────────────────────────────

    fun setTransferType(type: TransferType) {
        _state.update { it.copy(transferType = type) }
    }

    fun setRecipient(accountNumber: String, bank: MockBank) {
        _state.update { it.copy(recipientAccount = accountNumber, recipientBank = bank) }
    }

    fun setAmount(rawDigits: String) {
        val sanitized = rawDigits.filter { it.isDigit() }.take(MAX_AMOUNT_DIGITS)
        val amountVnd = sanitized.toLongOrNull() ?: 0L
        val overLimit = isOverNapasLimit(amountVnd, _state.value.transferType)
        _state.update {
            it.copy(amountRaw = sanitized, amountVnd = amountVnd, overLimit = overLimit)
        }
    }

    fun setNote(note: String) {
        val trimmed = note.take(MAX_NOTE_LENGTH)
        _state.update { it.copy(note = trimmed) }
    }

    fun setSource(source: TransferSource) {
        _state.update { it.copy(source = source) }
    }

    fun setTransferChannel(channel: TransferChannel) {
        _state.update { it.copy(transferChannel = channel) }
    }

    fun setOtp(otp: String) {
        _state.update { it.copy(otp = otp.take(OTP_LENGTH)) }
    }

    /** Reset state for a new flow — call from Home tap before navigating. */
    fun reset() {
        _state.value = TransferState()
    }

    // ─── Actions emitting one-shot events ──────────────────────────────

    /**
     * Called from Form screen when user taps "Tiếp tục".
     *
     * Emits [TransferEvent.ShowOverLimitSheet] if `state.overLimit` is true
     * (TASK-020 will react). Otherwise emits [TransferEvent.NavigateToOtp].
     */
    suspend fun onFormContinue() {
        if (_state.value.overLimit) {
            _events.send(TransferEvent.ShowOverLimitSheet)
        } else {
            _events.send(TransferEvent.NavigateToOtp)
        }
    }

    /**
     * Called from OverNapasLimit sheet primary tap — switch channel + go OTP.
     * Decision time is recorded via collector at TASK-020.
     */
    suspend fun onOverLimitProceed() {
        setTransferChannel(TransferChannel.Regular)
        _events.send(TransferEvent.NavigateToOtp)
    }

    /** Called from OTP screen when 6 digits entered. */
    suspend fun onOtpComplete() {
        _events.send(TransferEvent.NavigateToSuccess)
    }

    // ─── Helpers ───────────────────────────────────────────────────────

    private fun isOverNapasLimit(amountVnd: Long, type: TransferType?): Boolean {
        // Internal transfers do not hit the Napas threshold. Only Napas channel.
        return type == TransferType.Napas && amountVnd > MockTransferLimits.NAPAS_DAILY_LIMIT_VND
    }

    companion object {
        const val MAX_AMOUNT_DIGITS = 12  // up to 999,999,999,999 VND
        const val MAX_NOTE_LENGTH = 100
        const val OTP_LENGTH = 6
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// State + supporting types
// ─────────────────────────────────────────────────────────────────────────────

data class TransferState(
    val transferType: TransferType? = null,
    val recipientAccount: String = "",
    val recipientBank: MockBank? = null,
    val amountRaw: String = "",        // digits only, vd "1234567"
    val amountVnd: Long = 0L,
    val note: String = "",
    val source: TransferSource = TransferSource.PaymentAccount,
    val transferChannel: TransferChannel = TransferChannel.Default,
    val overLimit: Boolean = false,    // amount > Napas limit (Napas only)
    val otp: String = "",
    val txStatus: TransferTxStatus = TransferTxStatus.Idle,
    /**
     * Risk score from backend verification (silent — surfaced only via Dev Menu
     * at TASK-024). KHÔNG bind vào UI ở Form/OTP/Success screens.
     */
    val riskResult: TransferRiskResult? = null,
)

enum class TransferSource(val label: String, val maskedAccount: String) {
    PaymentAccount("Tài khoản thanh toán", "9999 1111"),
    Savings("Tài khoản tiết kiệm", "8888 2222"),
    CreditCard("Thẻ tín dụng", "7777 3333"),
}

enum class TransferChannel { Default, Regular }

enum class TransferTxStatus { Idle, Processing, Success, Failed }

data class TransferRiskResult(
    val riskScore: Int,
    val reasoning: String,
    val timestampMs: Long,
)

// ─────────────────────────────────────────────────────────────────────────────
// Events — one-shot navigation triggers consumed by NavHost layer.
// ─────────────────────────────────────────────────────────────────────────────

sealed class TransferEvent {
    data object NavigateToOtp : TransferEvent()
    data object NavigateToSuccess : TransferEvent()
    data object ShowOverLimitSheet : TransferEvent()
    data class ShowError(val message: String) : TransferEvent()
}
