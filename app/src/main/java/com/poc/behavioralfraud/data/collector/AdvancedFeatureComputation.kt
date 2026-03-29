package com.poc.behavioralfraud.data.collector

import com.poc.behavioralfraud.data.model.*

/**
 * Phase 3 — Advanced Motion & Pattern Analysis (FR-CL-07)
 *
 * Cross-correlation of touch × sensor data and text event sequence analysis.
 * Extracted from FeatureComputation.kt to keep file sizes under 500 lines.
 */

/**
 * Phase 3 advanced motion features (FR-CL-07 REQ-01 through REQ-04)
 */
internal data class MotionAdvanced(
    val avgTapAccelSpike: Double,
    val avgTapRecoveryMs: Double,
    val idleGyroRMS: Double,
    val idleAccelJitter: Double
)

/**
 * Correlate touch DOWN events with accelerometer data to compute grasp resistance
 * and idle period motion metrics.
 *
 * @param downEvents Touch DOWN events sorted by eventTime
 * @param touchSnapshot All touch events (for idle period detection)
 * @param accelEvents Accelerometer readings
 * @param gyroEvents Gyroscope readings
 * @param sessionStartTime Session start timestamp (System.currentTimeMillis)
 * @param sessionEndTime Session end timestamp (System.currentTimeMillis)
 */
internal fun computeMotionAdvanced(
    downEvents: List<TouchEvent>,
    touchSnapshot: List<TouchEvent>,
    accelEvents: List<SensorEvent>,
    gyroEvents: List<SensorEvent>,
    sessionStartTime: Long,
    sessionEndTime: Long
): MotionAdvanced {
    val sortedAccel = accelEvents.sortedBy { it.timestamp }

    // REQ-01 & REQ-02: Tap-accel correlation (requires >=3 taps + accel data)
    val tapSpikes = mutableListOf<Double>()
    val tapRecoveries = mutableListOf<Double>()

    if (downEvents.size >= 3 && sortedAccel.isNotEmpty()) {
        for (down in downEvents) {
            val tapTime = down.timestamp

            // Baseline: avg accel magnitude in [t-200, t]
            val baselineEvents = sortedAccel.filter { it.timestamp in (tapTime - 200)..tapTime }
            if (baselineEvents.isEmpty()) continue
            val baselineMag = baselineEvents.map { accelMagnitude(it) }.average()

            // Peak: max accel magnitude in [t, t+100]
            val postTapEvents = sortedAccel.filter { it.timestamp in tapTime..(tapTime + 100) }
            if (postTapEvents.isEmpty()) continue
            val peakMag = postTapEvents.maxOf { accelMagnitude(it) }

            val spike = (peakMag - baselineMag).coerceAtLeast(0.0)
            tapSpikes.add(spike)

            // REQ-02: Recovery — find when magnitude returns within 10% of baseline
            val recoveryThreshold = baselineMag * 1.1 + 0.5
            val recoveryEvent = sortedAccel.firstOrNull {
                it.timestamp > tapTime + 100 && accelMagnitude(it) <= recoveryThreshold
            }
            if (recoveryEvent != null) {
                tapRecoveries.add((recoveryEvent.timestamp - tapTime).toDouble())
            }
        }
    }

    // REQ-03 & REQ-04: Idle period detection (>500ms gaps between touch events)
    val allTouchTimes = touchSnapshot.map { it.timestamp }.sorted()
    val idlePeriods = mutableListOf<Pair<Long, Long>>()

    if (allTouchTimes.isEmpty()) {
        if (sessionEndTime - sessionStartTime > 500) {
            idlePeriods.add(sessionStartTime to sessionEndTime)
        }
    } else {
        if (allTouchTimes.first() - sessionStartTime > 500) {
            idlePeriods.add(sessionStartTime to allTouchTimes.first())
        }
        for (i in 1 until allTouchTimes.size) {
            if (allTouchTimes[i] - allTouchTimes[i - 1] > 500) {
                idlePeriods.add(allTouchTimes[i - 1] to allTouchTimes[i])
            }
        }
        if (sessionEndTime - allTouchTimes.last() > 500) {
            idlePeriods.add(allTouchTimes.last() to sessionEndTime)
        }
    }

    val idleGyroMags = mutableListOf<Double>()
    val idleAccelMags = mutableListOf<Double>()
    for ((start, end) in idlePeriods) {
        gyroEvents.filter { it.timestamp in start..end }.forEach { e ->
            idleGyroMags.add(kotlin.math.sqrt((e.x * e.x + e.y * e.y + e.z * e.z).toDouble()))
        }
        accelEvents.filter { it.timestamp in start..end }.forEach { e ->
            idleAccelMags.add(accelMagnitude(e))
        }
    }

    // REQ-03: RMS of gyro magnitude during idle = sqrt(mean(mag²))
    val idleGyroRMS = if (idleGyroMags.isNotEmpty()) {
        kotlin.math.sqrt(idleGyroMags.map { it * it }.average())
    } else 0.0

    // REQ-04: Jitter = mean absolute difference of consecutive accel magnitudes
    val idleAccelJitter = if (idleAccelMags.size >= 2) {
        idleAccelMags.zipWithNext().map { (a, b) -> kotlin.math.abs(b - a) }.average()
    } else 0.0

    return MotionAdvanced(
        avgTapAccelSpike = if (tapSpikes.isNotEmpty()) tapSpikes.average() else 0.0,
        avgTapRecoveryMs = if (tapRecoveries.isNotEmpty()) tapRecoveries.average() else 0.0,
        idleGyroRMS = idleGyroRMS,
        idleAccelJitter = idleAccelJitter
    )
}

internal fun accelMagnitude(e: SensorEvent): Double =
    kotlin.math.sqrt((e.x * e.x + e.y * e.y + e.z * e.z).toDouble())

/**
 * Phase 3 advanced cognitive features (FR-CL-07 REQ-05, REQ-06)
 *
 * Analyzes lengthDelta sequences to detect correction patterns.
 * Privacy-preserving: never reads actual text content.
 */
internal data class CognitiveAdvanced(
    val correctionSameCount: Int,
    val correctionDifferentCount: Int
)

internal fun computeCognitiveAdvanced(
    sortedTextEvents: List<TextChangeEvent>
): CognitiveAdvanced {
    if (sortedTextEvents.size < 2) return CognitiveAdvanced(0, 0)

    var sameCount = 0
    var differentCount = 0

    for (i in 0 until sortedTextEvents.size - 1) {
        val current = sortedTextEvents[i]
        val next = sortedTextEvents[i + 1]
        val timeDiff = next.timestamp - current.timestamp

        // REQ-05: typo fix — deletion(1-2 chars) then insertion(1-2 chars) within 1000ms
        if (current.lengthDelta in -2..-1 && next.lengthDelta in 1..2 && timeDiff <= 1000) {
            sameCount++
        }

        // REQ-06: content change — deletion(>=3 chars) then insertion(>=3 chars) after >500ms pause
        if (current.lengthDelta <= -3 && next.lengthDelta >= 3 && timeDiff > 500) {
            differentCount++
        }
    }

    return CognitiveAdvanced(
        correctionSameCount = sameCount,
        correctionDifferentCount = differentCount
    )
}
