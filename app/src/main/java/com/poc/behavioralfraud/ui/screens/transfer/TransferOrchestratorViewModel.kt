package com.poc.behavioralfraud.ui.screens.transfer

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.poc.behavioralfraud.data.collector.BehavioralCollector
import com.poc.behavioralfraud.data.mock.MockBank
import com.poc.behavioralfraud.data.mock.MockTransferLimits
import com.poc.behavioralfraud.data.model.BehavioralFeatures
import com.poc.behavioralfraud.data.model.BehavioralProfile
import com.poc.behavioralfraud.data.model.FraudAnalysisResult
import com.poc.behavioralfraud.data.repository.ProfileRepository
import com.poc.behavioralfraud.data.scorer.LocalScorer
import com.poc.behavioralfraud.network.BackendClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single VM owning the transfer flow state — FR-CL-10 REQ-08, REQ-10..15.
 *
 * Lifecycle (FR-CL-10 REQ-10): behavioral session bound to transfer flow only.
 *  - [reset] called from Home tap "Chuyển tiền trong nước" — starts a new
 *    [BehavioralCollector] session.
 *  - [onOtpComplete] runs the silent verification pipeline (REQ-15) then
 *    stops the session.
 *  - [onCleared] stops any leftover session if MainActivity destroyed mid-flow.
 *
 * The collector is owned by this VM (not the legacy [TransferViewModel]) so
 * each transfer flow has its own session id. Production-feel rule: verification
 * result lives silently in [ProfileRepository.VerificationRecord] history; UI
 * never binds to it. Dev Menu (TASK-024) surfaces it.
 *
 * Login + Home browsing intentionally outside the session — only transfer
 * inputs/decisions are captured.
 */
class TransferOrchestratorViewModel(application: Application) : AndroidViewModel(application) {

    val collector: BehavioralCollector = BehavioralCollector(application)
    private val profileRepo = ProfileRepository(application)
    private val backendClient = BackendClient()
    private val localScorer = LocalScorer()

    private var sessionActive: Boolean = false

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

    /**
     * Reset state for a new flow — called from Home tap before navigating to
     * RecipientScreen. Starts a fresh [BehavioralCollector] session so each
     * transfer has its own session id (FR-CL-10 REQ-10).
     */
    fun reset() {
        if (sessionActive) {
            // Edge case: user re-enters flow without completing previous one.
            // Stop the orphan session before starting a new one.
            collector.stopSession()
        }
        _state.value = TransferState()
        collector.startSession()
        sessionActive = true
        Log.d(TAG, "Transfer flow started — session active")
    }

    // ─── Actions emitting one-shot events ──────────────────────────────

    suspend fun onFormContinue() {
        if (_state.value.overLimit) {
            _events.send(TransferEvent.ShowOverLimitSheet)
        } else {
            _events.send(TransferEvent.NavigateToOtp)
        }
    }

    suspend fun onOverLimitProceed() {
        setTransferChannel(TransferChannel.Regular)
        _events.send(TransferEvent.NavigateToOtp)
    }

    /**
     * Called from OTP screen when user taps "Xác nhận & Hoàn tất".
     *
     * Pipeline (FR-CL-10 REQ-15):
     *  1. Stop collector session and extract behavioral features.
     *  2. Silent baseline accumulation if no profile yet, else silent verify.
     *  3. Persist verification record to history (Dev Menu inspection).
     *  4. Emit NavigateToSuccess regardless of pipeline outcome — pipeline
     *     never blocks UI navigation.
     *
     * All I/O on [Dispatchers.IO]. Backend errors fall through to LocalScorer.
     */
    suspend fun onOtpComplete() {
        // Navigate first (don't block UX on network)
        _events.send(TransferEvent.NavigateToSuccess)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { runVerificationPipeline() }
                    .onFailure { Log.w(TAG, "Verification pipeline error", it) }
            }
        }
    }

    /**
     * Pipeline — extract features → silent enroll/verify → persist history.
     * Caller wraps in withContext(Dispatchers.IO).
     */
    private fun runVerificationPipeline() {
        // 1. Snapshot features then end session
        val profile = profileRepo.getProfile()
        val enrollmentFeatures = profileRepo.getEnrollmentFeaturesList()
        val features = collector.extractFeatures(profile, enrollmentFeatures)
        if (sessionActive) {
            collector.stopSession()
            sessionActive = false
        }

        // 2. Silent enrollment OR verification
        val txSummary = txSummary()
        val record = if (profile == null) {
            silentEnroll(features, txSummary)
        } else {
            silentVerify(features, profile, enrollmentFeatures, txSummary)
        }

        // 3. Persist record
        profileRepo.addVerificationRecord(record)
        Log.d(TAG, "Verification recorded: score=${record.riskScore} src=${record.source}")
    }

    private fun silentEnroll(
        features: BehavioralFeatures,
        txSummary: String,
    ): ProfileRepository.VerificationRecord {
        // Send to backend; if it builds profile, save it. Otherwise just log baseline.
        val (reasoning, score) = try {
            val response = runBlockingIO {
                backendClient.enrollSession(USER_ID, features)
            }
            profileRepo.addEnrollmentFeatures(features)
            if (response.status == "completed" && response.profile != null) {
                profileRepo.saveProfile(response.profile)
                "Profile built (${response.enrollmentCount}/${MIN_BASELINE})" to 0
            } else {
                "Enrollment ${response.enrollmentCount}/${MIN_BASELINE} — profile pending" to 0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Enroll failed; storing baseline locally", e)
            profileRepo.addEnrollmentFeatures(features)
            "Local baseline added (backend unavailable)" to 0
        }

        return ProfileRepository.VerificationRecord(
            timestampMs = System.currentTimeMillis(),
            riskScore = score,
            reasoning = reasoning,
            txSummary = txSummary,
            source = "enrollment",
        )
    }

    private fun silentVerify(
        features: BehavioralFeatures,
        profile: BehavioralProfile,
        enrollmentFeatures: List<BehavioralFeatures>,
        txSummary: String,
    ): ProfileRepository.VerificationRecord {
        val (result, source) = try {
            val backendResult = runBlockingIO {
                backendClient.verifyTransaction(USER_ID, features, profile)
            }
            backendResult to "backend"
        } catch (e: Exception) {
            Log.w(TAG, "Backend verify failed; using LocalScorer fallback", e)
            localScorer.score(features, profile, enrollmentFeatures) to "local-fallback"
        }

        // Update internal riskResult (still NOT bound to UI)
        _state.update {
            it.copy(
                riskResult = TransferRiskResult(
                    riskScore = result.riskScore,
                    reasoning = result.explanation,
                    timestampMs = System.currentTimeMillis(),
                ),
            )
        }

        return ProfileRepository.VerificationRecord(
            timestampMs = System.currentTimeMillis(),
            riskScore = result.riskScore,
            reasoning = "${result.riskLevel} — ${result.explanation}",
            txSummary = txSummary,
            source = source,
        )
    }

    private fun txSummary(): String {
        val s = _state.value
        val recipient = s.recipientBank?.shortName?.let { "to $it" } ?: ""
        return "${"%,d".format(s.amountVnd)} VND $recipient".trim()
    }

    private fun <T> runBlockingIO(block: suspend () -> T): T {
        // Pipeline already runs in Dispatchers.IO context (from onOtpComplete).
        // We need to call suspending backend methods from this synchronous helper.
        return kotlinx.coroutines.runBlocking { block() }
    }

    override fun onCleared() {
        super.onCleared()
        if (sessionActive) {
            collector.stopSession()
            sessionActive = false
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────

    private fun isOverNapasLimit(amountVnd: Long, type: TransferType?): Boolean {
        return type == TransferType.Napas && amountVnd > MockTransferLimits.NAPAS_DAILY_LIMIT_VND
    }

    companion object {
        private const val TAG = "TransferOrchestratorVM"
        private const val USER_ID = "default_user"
        private const val MIN_BASELINE = 3
        const val MAX_AMOUNT_DIGITS = 12
        const val MAX_NOTE_LENGTH = 100
        const val OTP_LENGTH = 6
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// State + supporting types (unchanged from previous version)
// ─────────────────────────────────────────────────────────────────────────────

data class TransferState(
    val transferType: TransferType? = null,
    val recipientAccount: String = "",
    val recipientBank: MockBank? = null,
    val amountRaw: String = "",
    val amountVnd: Long = 0L,
    val note: String = "",
    val source: TransferSource = TransferSource.PaymentAccount,
    val transferChannel: TransferChannel = TransferChannel.Default,
    val overLimit: Boolean = false,
    val otp: String = "",
    val txStatus: TransferTxStatus = TransferTxStatus.Idle,
    val riskResult: TransferRiskResult? = null,
)

enum class TransferType { Internal, Napas }

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

sealed class TransferEvent {
    data object NavigateToOtp : TransferEvent()
    data object NavigateToSuccess : TransferEvent()
    data object ShowOverLimitSheet : TransferEvent()
    data class ShowError(val message: String) : TransferEvent()
}
