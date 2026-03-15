package com.poc.behavioralfraud.data.scorer

import com.poc.behavioralfraud.data.model.BehavioralFeatures
import com.poc.behavioralfraud.data.model.BehavioralProfile
import com.poc.behavioralfraud.data.model.FraudAnalysisResult
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

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
        val riskScore = (avgDeviation * 20).coerceIn(0.0, 100.0).toInt()

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
            anomalies = anomalies,
            explanation = "[Offline - Local Z-score] Phân tích $featureCount features. " +
                "Độ lệch trung bình: ${String.format("%.1f", avgDeviation)} sigma. " +
                if (anomalies.isEmpty()) "Không phát hiện bất thường đáng kể."
                else "${anomalies.size} bất thường được phát hiện.",
            recommendation = recommendation
        )
    }
}
