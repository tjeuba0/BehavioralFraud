package com.poc.behavioralfraud.integration

import com.poc.behavioralfraud.data.model.BehavioralFeatures
import com.poc.behavioralfraud.data.model.BehavioralProfile

/**
 * Shared test fixtures for end-to-end integration tests.
 * Contains realistic behavioral data for enrollment, same-person, and different-person scenarios.
 */
object E2ETestFixtures {

    const val TEST_USER_ID = "user-e2e-001"

    val enrollmentSession1 = BehavioralFeatures(
        sessionDurationMs = 42000L,
        avgInterCharDelayMs = 118.0, stdInterCharDelayMs = 32.0,
        maxInterCharDelayMs = 480L, minInterCharDelayMs = 48L,
        totalTextChanges = 40, pasteCount = 0, totalTouchEvents = 145,
        avgTouchSize = 0.14, avgTouchDurationMs = 83.0, avgSwipeVelocity = 1180.0,
        gyroStabilityX = 0.0020, gyroStabilityY = 0.0017, gyroStabilityZ = 0.0014,
        accelStabilityX = 0.11, accelStabilityY = 0.08, accelStabilityZ = 0.10,
        avgTouchPressure = 0.34,
        perFieldAvgDelay = mapOf("accountNumber" to 108.0, "amount" to 92.0, "note" to 128.0),
        avgInterFieldPauseMs = 1450.0, deletionCount = 2, deletionRatio = 0.05,
        fieldFocusSequence = "accountNumber->amount->note",
        timeToFirstInput = 2400L, timeFromLastInputToConfirm = 1750L
    )

    val enrollmentSession2 = BehavioralFeatures(
        sessionDurationMs = 44000L,
        avgInterCharDelayMs = 122.0, stdInterCharDelayMs = 34.0,
        maxInterCharDelayMs = 510L, minInterCharDelayMs = 52L,
        totalTextChanges = 43, pasteCount = 0, totalTouchEvents = 152,
        avgTouchSize = 0.15, avgTouchDurationMs = 86.0, avgSwipeVelocity = 1210.0,
        gyroStabilityX = 0.0022, gyroStabilityY = 0.0019, gyroStabilityZ = 0.0016,
        accelStabilityX = 0.12, accelStabilityY = 0.09, accelStabilityZ = 0.11,
        avgTouchPressure = 0.36,
        perFieldAvgDelay = mapOf("accountNumber" to 112.0, "amount" to 97.0, "note" to 132.0),
        avgInterFieldPauseMs = 1520.0, deletionCount = 3, deletionRatio = 0.07,
        fieldFocusSequence = "accountNumber->amount->note",
        timeToFirstInput = 2550L, timeFromLastInputToConfirm = 1820L
    )

    val enrollmentSession3 = BehavioralFeatures(
        sessionDurationMs = 43000L,
        avgInterCharDelayMs = 120.0, stdInterCharDelayMs = 33.5,
        maxInterCharDelayMs = 495L, minInterCharDelayMs = 50L,
        totalTextChanges = 41, pasteCount = 0, totalTouchEvents = 148,
        avgTouchSize = 0.145, avgTouchDurationMs = 84.5, avgSwipeVelocity = 1195.0,
        gyroStabilityX = 0.0021, gyroStabilityY = 0.0018, gyroStabilityZ = 0.0015,
        accelStabilityX = 0.115, accelStabilityY = 0.085, accelStabilityZ = 0.105,
        avgTouchPressure = 0.35,
        perFieldAvgDelay = mapOf("accountNumber" to 110.0, "amount" to 95.0, "note" to 130.0),
        avgInterFieldPauseMs = 1480.0, deletionCount = 3, deletionRatio = 0.06,
        fieldFocusSequence = "accountNumber->amount->note",
        timeToFirstInput = 2480L, timeFromLastInputToConfirm = 1780L
    )

    val enrollmentSessions = listOf(enrollmentSession1, enrollmentSession2, enrollmentSession3)

    /** Same person verification — close to enrollment baseline */
    val samePersonVerification = BehavioralFeatures(
        sessionDurationMs = 43500L,
        avgInterCharDelayMs = 121.0, stdInterCharDelayMs = 33.0,
        maxInterCharDelayMs = 500L, minInterCharDelayMs = 49L,
        totalTextChanges = 42, pasteCount = 0, totalTouchEvents = 149,
        avgTouchSize = 0.146, avgTouchDurationMs = 84.0, avgSwipeVelocity = 1200.0,
        gyroStabilityX = 0.0021, gyroStabilityY = 0.0018, gyroStabilityZ = 0.0015,
        accelStabilityX = 0.112, accelStabilityY = 0.087, accelStabilityZ = 0.104,
        avgTouchPressure = 0.345,
        perFieldAvgDelay = mapOf("accountNumber" to 111.0, "amount" to 94.0, "note" to 129.0),
        avgInterFieldPauseMs = 1470.0, deletionCount = 2, deletionRatio = 0.05,
        fieldFocusSequence = "accountNumber->amount->note",
        timeToFirstInput = 2500L, timeFromLastInputToConfirm = 1790L
    )

    /** Different person — significantly divergent from baseline (fraud signals) */
    val differentPersonVerification = BehavioralFeatures(
        sessionDurationMs = 15000L,
        avgInterCharDelayMs = 45.0, stdInterCharDelayMs = 8.0,
        maxInterCharDelayMs = 120L, minInterCharDelayMs = 20L,
        totalTextChanges = 38, pasteCount = 5, totalTouchEvents = 60,
        avgTouchSize = 0.25, avgTouchDurationMs = 40.0, avgSwipeVelocity = 3500.0,
        gyroStabilityX = 0.08, gyroStabilityY = 0.07, gyroStabilityZ = 0.06,
        accelStabilityX = 0.50, accelStabilityY = 0.45, accelStabilityZ = 0.48,
        avgTouchPressure = 0.60,
        perFieldAvgDelay = mapOf("amount" to 30.0, "accountNumber" to 35.0, "note" to 40.0),
        avgInterFieldPauseMs = 400.0, deletionCount = 0, deletionRatio = 0.0,
        fieldFocusSequence = "amount->accountNumber->note",
        timeToFirstInput = 500L, timeFromLastInputToConfirm = 300L
    )

    /** Profile from enrollment baseline */
    val baselineProfile = BehavioralProfile(
        userId = TEST_USER_ID,
        enrollmentCount = 3,
        avgSessionDuration = 43000.0, avgInterCharDelay = 120.0, stdInterCharDelay = 33.2,
        avgTouchSize = 0.145, avgTouchDuration = 84.5,
        avgGyroStabilityX = 0.0021, avgGyroStabilityY = 0.0018, avgGyroStabilityZ = 0.0015,
        avgAccelStabilityX = 0.115, avgAccelStabilityY = 0.085, avgAccelStabilityZ = 0.105,
        avgSwipeVelocity = 1195.0, avgPasteCount = 0.0,
        avgTimeToFirstInput = 2477.0, avgTimeFromLastInputToConfirm = 1783.0,
        typicalFieldFocusSequence = "accountNumber->amount->note",
        avgTouchPressure = 0.35, avgInterFieldPause = 1483.0, avgDeletionRatio = 0.06,
        profileSummary = "User exhibits consistent moderate typing rhythm, hand-held device."
    )
}
