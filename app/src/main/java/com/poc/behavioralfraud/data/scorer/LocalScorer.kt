package com.poc.behavioralfraud.data.scorer

import com.poc.behavioralfraud.data.model.BehavioralFeatures
import com.poc.behavioralfraud.data.model.BehavioralProfile
import com.poc.behavioralfraud.data.model.FraudAnalysisResult
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * FR-CL-15 / TASK-030 — Deterministic pre-filter rules.
 *
 * Mirrors backend `app/services/risk_rule_engine.py` (BE TASK-015) exact:
 * same weights, same thresholds, same Vietnamese reasons. Ensures online
 * (backend) and offline (LocalScorer fallback) verdicts stay consistent
 * for hard fraud indicators that don't need profile baseline.
 *
 * Returned score is summed weights capped at SCORE_CAP. Reasons preserve
 * evaluation order (gps → bot tap → synthetic velocity → otp → dark).
 */
internal data class RuleResult(val score: Int, val reasons: List<String>)

internal object LocalScoreRules {
    // ── Rule weights (must match BE exactly) ───────────────────────
    const val WEIGHT_GPS_SPOOFING = 30
    const val WEIGHT_BOT_TAP_PRECISION = 25
    const val WEIGHT_SYNTHETIC_VELOCITY = 20
    const val WEIGHT_OTP_PASTE_VIOLATION = 20
    const val WEIGHT_DARK_ANOMALY = 15

    // ── Trigger thresholds ─────────────────────────────────────────
    const val TAP_PRECISION_OFFSET_MAX_PX = 2.0
    const val TAP_PRECISION_STD_MAX_PX = 1.0
    const val TAP_PRECISION_MIN_TOUCHES = 5
    const val VELOCITY_STD_MAX = 0.005
    const val VELOCITY_MIN_TOUCHES = 5
    const val DARK_LIGHT_LUX_MAX = 10.0
    const val DARK_HOUR_MIN = 0
    const val DARK_HOUR_MAX = 5  // inclusive
    const val SCORE_CAP = 100

    // ── Reason strings (Vietnamese, must match BE exactly) ─────────
    const val REASON_GPS_SPOOFING = "Phát hiện giả mạo GPS"
    const val REASON_BOT_TAP_PRECISION = "Mẫu tap dead-center bất thường (nghi ngờ bot)"
    const val REASON_SYNTHETIC_VELOCITY = "Tốc độ giữa các tap đồng đều bất thường"
    const val REASON_OTP_PASTE_VIOLATION =
        "OTP nhập bằng paste — vi phạm UX (Soft OTP DISPLAYED only)"
    const val REASON_DARK_ANOMALY = "Môi trường tối + thời gian bất thường"

    fun evaluate(features: BehavioralFeatures): RuleResult {
        var score = 0
        val reasons = mutableListOf<String>()

        if (features.mockLocationDetected) {
            score += WEIGHT_GPS_SPOOFING
            reasons.add(REASON_GPS_SPOOFING)
        }
        if (features.avgTapPrecisionOffsetPx < TAP_PRECISION_OFFSET_MAX_PX &&
            features.tapPrecisionStdDev < TAP_PRECISION_STD_MAX_PX &&
            features.totalTouchEvents >= TAP_PRECISION_MIN_TOUCHES
        ) {
            score += WEIGHT_BOT_TAP_PRECISION
            reasons.add(REASON_BOT_TAP_PRECISION)
        }
        if (features.interTapVelocityStdDev < VELOCITY_STD_MAX &&
            features.totalTouchEvents >= VELOCITY_MIN_TOUCHES
        ) {
            score += WEIGHT_SYNTHETIC_VELOCITY
            reasons.add(REASON_SYNTHETIC_VELOCITY)
        }
        if (features.otpPasted) {
            score += WEIGHT_OTP_PASTE_VIOLATION
            reasons.add(REASON_OTP_PASTE_VIOLATION)
        }
        if (features.lightAvgLux < DARK_LIGHT_LUX_MAX &&
            features.sessionHourOfDay in DARK_HOUR_MIN..DARK_HOUR_MAX
        ) {
            score += WEIGHT_DARK_ANOMALY
            reasons.add(REASON_DARK_ANOMALY)
        }

        return RuleResult(score = score.coerceAtMost(SCORE_CAP), reasons = reasons.toList())
    }
}

/**
 * Local Z-score based anomaly detector.
 * Compares current session features against enrollment distribution.
 * Used as fallback when LLM API is unavailable.
 */
class LocalScorer {

    fun score(
        current: BehavioralFeatures,
        profile: BehavioralProfile,
        enrollmentFeatures: List<BehavioralFeatures>
    ): FraudAnalysisResult {
        val anomalies = mutableListOf<String>()
        var totalDeviation = 0.0
        var featureCount = 0

        fun checkFeature(name: String, currentVal: Double, enrollmentVals: List<Double>) {
            if (enrollmentVals.size < 2) return
            val mean = enrollmentVals.average()
            val std = sqrt(enrollmentVals.map { (it - mean).pow(2) }.average())
            if (std == 0.0) return
            val zScore = abs((currentVal - mean) / std)
            featureCount++
            totalDeviation += zScore
            if (zScore > 2.0) {
                anomalies.add("$name: z=${String.format("%.1f", zScore)} " +
                    "(hiện tại=${String.format("%.1f", currentVal)}, " +
                    "baseline=${String.format("%.1f", mean)}±${String.format("%.1f", std)})")
            }
        }

        checkFeature("Tốc độ gõ (inter-char delay)",
            current.avgInterCharDelayMs,
            enrollmentFeatures.map { it.avgInterCharDelayMs })

        checkFeature("Độ ổn định gõ (std delay)",
            current.stdInterCharDelayMs,
            enrollmentFeatures.map { it.stdInterCharDelayMs })

        checkFeature("Kích thước chạm",
            current.avgTouchSize,
            enrollmentFeatures.map { it.avgTouchSize })

        checkFeature("Thời gian chạm",
            current.avgTouchDurationMs,
            enrollmentFeatures.map { it.avgTouchDurationMs })

        checkFeature("Áp lực chạm",
            current.avgTouchPressure,
            enrollmentFeatures.map { it.avgTouchPressure })

        checkFeature("Thời gian phiên",
            current.sessionDurationMs.toDouble(),
            enrollmentFeatures.map { it.sessionDurationMs.toDouble() })

        checkFeature("Gyro stability X",
            current.gyroStabilityX,
            enrollmentFeatures.map { it.gyroStabilityX })

        checkFeature("Gyro stability Y",
            current.gyroStabilityY,
            enrollmentFeatures.map { it.gyroStabilityY })

        checkFeature("Gyro stability Z",
            current.gyroStabilityZ,
            enrollmentFeatures.map { it.gyroStabilityZ })

        checkFeature("Tốc độ vuốt",
            current.avgSwipeVelocity,
            enrollmentFeatures.map { it.avgSwipeVelocity })

        checkFeature("Thời gian ngập ngừng giữa fields",
            current.avgInterFieldPauseMs,
            enrollmentFeatures.map { it.avgInterFieldPauseMs })

        checkFeature("Tỷ lệ xóa",
            current.deletionRatio,
            enrollmentFeatures.map { it.deletionRatio })

        val avgDeviation = if (featureCount > 0) totalDeviation / featureCount else 0.0
        val zScore = (avgDeviation * 20).coerceIn(0.0, 100.0).toInt()

        // FR-CL-15 — apply deterministic rule engine (mirror BE TASK-015).
        // Final score is max(rule, z); reasons concatenated rule-first then anomalies.
        val ruleResult = LocalScoreRules.evaluate(current)
        val riskScore = maxOf(ruleResult.score, zScore)
        val mergedAnomalies: List<String> = ruleResult.reasons + anomalies

        val riskLevel = when {
            riskScore <= 30 -> "LOW"
            riskScore <= 70 -> "MEDIUM"
            else -> "HIGH"
        }

        val recommendation = when {
            riskScore <= 30 -> "APPROVE"
            riskScore <= 70 -> "STEP_UP_AUTH"
            else -> "BLOCK"
        }

        return FraudAnalysisResult(
            riskScore = riskScore,
            riskLevel = riskLevel,
            anomalies = mergedAnomalies,
            explanation = buildString {
                append("[Offline - Local fallback] ")
                if (ruleResult.score > 0) {
                    append("RuleEngine score=${ruleResult.score} (${ruleResult.reasons.size} rules fired). ")
                }
                append("Z-score phân tích $featureCount features, ")
                append("độ lệch trung bình ${String.format("%.1f", avgDeviation)} sigma. ")
                if (mergedAnomalies.isEmpty()) {
                    append("Không phát hiện bất thường đáng kể.")
                } else {
                    append("${mergedAnomalies.size} bất thường được phát hiện.")
                }
            },
            recommendation = recommendation
        )
    }
}
