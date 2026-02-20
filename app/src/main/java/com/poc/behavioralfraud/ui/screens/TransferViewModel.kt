package com.poc.behavioralfraud.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.poc.behavioralfraud.data.collector.BehavioralCollector
import com.poc.behavioralfraud.data.model.*
import com.poc.behavioralfraud.data.repository.ProfileRepository
import com.poc.behavioralfraud.network.OpenRouterClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI State for the Transfer screen
 */
sealed class TransferUiState {
    data object Idle : TransferUiState()
    data object Collecting : TransferUiState()
    data object Analyzing : TransferUiState()
    data class EnrollmentComplete(val count: Int, val message: String) : TransferUiState()
    data class VerificationResult(
        val result: FraudAnalysisResult,
        val features: BehavioralFeatures
    ) : TransferUiState()
    data class Error(val message: String) : TransferUiState()
}

/**
 * ViewModel for Transfer screen
 */
class TransferViewModel(application: Application) : AndroidViewModel(application) {

    val collector = BehavioralCollector(application)
    private val repository = ProfileRepository(application)
    private val llmClient = OpenRouterClient()

    private val _uiState = MutableStateFlow<TransferUiState>(TransferUiState.Idle)
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    private val _enrollmentCount = MutableStateFlow(repository.getEnrollmentCount())
    val enrollmentCount: StateFlow<Int> = _enrollmentCount.asStateFlow()

    private val _eventCounts = MutableStateFlow(mapOf<String, Int>())
    val eventCounts: StateFlow<Map<String, Int>> = _eventCounts.asStateFlow()

    private val _profile = MutableStateFlow(repository.getProfile())
    val profile: StateFlow<BehavioralProfile?> = _profile.asStateFlow()

    companion object {
        const val MIN_ENROLLMENT = 3
    }

    /**
     * Start collecting behavioral data
     */
    fun startCollection() {
        collector.startSession()
        _uiState.value = TransferUiState.Collecting
    }

    /**
     * Update event counts for live display
     */
    fun refreshEventCounts() {
        _eventCounts.value = collector.getEventCounts()
    }

    /**
     * Submit transfer: stop collection, extract features, call LLM
     */
    fun submitTransfer(accountNumber: String, amount: String, note: String) {
        collector.stopSession()
        val features = collector.extractFeatures()
        val currentCount = repository.getEnrollmentCount()

        _uiState.value = TransferUiState.Analyzing

        viewModelScope.launch {
            try {
                if (currentCount < MIN_ENROLLMENT) {
                    // ENROLLMENT MODE: build profile
                    repository.addEnrollmentFeatures(features)
                    val newCount = repository.getEnrollmentCount()
                    _enrollmentCount.value = newCount

                    if (newCount >= MIN_ENROLLMENT) {
                        // Enough enrollments, build profile via LLM
                        val allFeatures = repository.getEnrollmentFeaturesList()
                        val summary = llmClient.enrollProfile(allFeatures)
                        val profile = repository.buildAveragedProfile(summary)
                        repository.saveProfile(profile)
                        _profile.value = profile

                        _uiState.value = TransferUiState.EnrollmentComplete(
                            count = newCount,
                            message = "Profile đã được tạo thành công!\n\n$summary"
                        )
                    } else {
                        _uiState.value = TransferUiState.EnrollmentComplete(
                            count = newCount,
                            message = "Enrollment ${newCount}/$MIN_ENROLLMENT hoàn tất.\nCần thêm ${MIN_ENROLLMENT - newCount} lần nữa để tạo profile."
                        )
                    }
                } else {
                    // VERIFICATION MODE: compare against profile
                    val profile = repository.getProfile()
                        ?: throw Exception("No profile found. Please re-enroll.")

                    val result = llmClient.verifyTransaction(features, profile)
                    _uiState.value = TransferUiState.VerificationResult(
                        result = result,
                        features = features
                    )
                }
            } catch (e: Exception) {
                _uiState.value = TransferUiState.Error(
                    "Lỗi: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    /**
     * Reset to idle state
     */
    fun resetState() {
        _uiState.value = TransferUiState.Idle
    }

    /**
     * Clear all data and reset
     */
    fun clearAllData() {
        repository.clearAll()
        _enrollmentCount.value = 0
        _profile.value = null
        _uiState.value = TransferUiState.Idle
    }
}
