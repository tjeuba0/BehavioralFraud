package com.poc.behavioralfraud.data.model

/**
 * Raw touch event captured from MotionEvent
 */
data class TouchEvent(
    val timestamp: Long,
    val action: Int,        // ACTION_DOWN=0, ACTION_UP=1, ACTION_MOVE=2
    val x: Float,
    val y: Float,
    val size: Float,        // getSize()
    val touchMajor: Float,  // getTouchMajor()
    val downTime: Long,     // getDownTime()
    val eventTime: Long     // getEventTime()
)

/**
 * Text change event captured from TextField onValueChange
 */
data class TextChangeEvent(
    val timestamp: Long,
    val fieldName: String,      // "accountNumber", "amount", "note"
    val previousLength: Int,
    val newLength: Int,
    val lengthDelta: Int,       // newLength - previousLength
    val isPaste: Boolean,       // lengthDelta >= 3
    val isDeletion: Boolean     // lengthDelta < 0
)

/**
 * Sensor reading from Accelerometer or Gyroscope
 */
data class SensorEvent(
    val timestamp: Long,
    val type: String,   // "accelerometer" or "gyroscope"
    val x: Float,
    val y: Float,
    val z: Float
)

/**
 * Navigation event (screen transitions, field focus)
 */
data class NavigationEvent(
    val timestamp: Long,
    val eventType: String,  // "screen_enter", "screen_exit", "field_focus", "confirm_tap"
    val detail: String      // screen name or field name
)

/**
 * All raw behavioral data collected during a transfer session
 */
data class BehavioralSession(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long,
    val touchEvents: List<TouchEvent>,
    val textChangeEvents: List<TextChangeEvent>,
    val sensorEvents: List<SensorEvent>,
    val navigationEvents: List<NavigationEvent>,
    val transaction: TransactionInfo
)

/**
 * Transaction details
 */
data class TransactionInfo(
    val accountNumber: String,
    val amount: String,
    val note: String
)

/**
 * Extracted features from raw behavioral data (sent to LLM)
 */
data class BehavioralFeatures(
    // Session
    val sessionDurationMs: Long,

    // Text input
    val avgInterCharDelayMs: Double,
    val stdInterCharDelayMs: Double,
    val maxInterCharDelayMs: Long,
    val minInterCharDelayMs: Long,
    val totalTextChanges: Int,
    val pasteCount: Int,

    // Touch
    val totalTouchEvents: Int,
    val avgTouchSize: Double,
    val avgTouchDurationMs: Double,
    val avgSwipeVelocity: Double,

    // Sensor - Gyroscope stability
    val gyroStabilityX: Double,
    val gyroStabilityY: Double,
    val gyroStabilityZ: Double,

    // Sensor - Accelerometer stability
    val accelStabilityX: Double,
    val accelStabilityY: Double,
    val accelStabilityZ: Double,

    // Touch pressure
    val avgTouchPressure: Double,

    // Per-field typing rhythm
    val perFieldAvgDelay: Map<String, Double>,

    // Hesitation & deletion patterns
    val avgInterFieldPauseMs: Double,
    val deletionCount: Int,
    val deletionRatio: Double,

    // Navigation
    val fieldFocusSequence: String,
    val timeToFirstInput: Long,
    val timeFromLastInputToConfirm: Long
)

/**
 * LLM analysis result
 */
data class FraudAnalysisResult(
    val riskScore: Int,             // 0-100
    val riskLevel: String,          // "LOW", "MEDIUM", "HIGH"
    val anomalies: List<String>,
    val explanation: String,
    val recommendation: String      // "APPROVE", "STEP_UP_AUTH", "BLOCK"
)

/**
 * Stored behavioral profile (baseline from enrollment sessions)
 */
data class BehavioralProfile(
    val userId: String,
    val enrollmentCount: Int,
    val avgSessionDuration: Double,
    val avgInterCharDelay: Double,
    val stdInterCharDelay: Double,
    val avgTouchSize: Double,
    val avgTouchDuration: Double,
    // Per-axis sensor stability (not collapsed)
    val avgGyroStabilityX: Double,
    val avgGyroStabilityY: Double,
    val avgGyroStabilityZ: Double,
    val avgAccelStabilityX: Double,
    val avgAccelStabilityY: Double,
    val avgAccelStabilityZ: Double,
    // Previously extracted but not stored
    val avgSwipeVelocity: Double,
    val avgPasteCount: Double,
    val avgTimeToFirstInput: Double,
    val avgTimeFromLastInputToConfirm: Double,
    val typicalFieldFocusSequence: String,
    // New features from Phase 2
    val avgTouchPressure: Double,
    val avgInterFieldPause: Double,
    val avgDeletionRatio: Double,
    val profileSummary: String      // LLM-generated summary
)
