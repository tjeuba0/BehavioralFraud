package com.poc.behavioralfraud.data.collector

import com.poc.behavioralfraud.data.model.*

/**
 * Statistical and feature computation helpers extracted from BehavioralCollector
 * to keep file sizes under 500 lines.
 */

/** Base feature intermediates computed from raw event data */
internal data class BaseFeatures(
    val interCharDelays: List<Long>,
    val sortedTextEvents: List<TextChangeEvent>,
    val avgInterChar: Double,
    val stdInterChar: Double,
    val touchSizes: List<Double>,
    val touchDurations: List<Long>,
    val downEvents: List<TouchEvent>,
    val moveEvents: List<TouchEvent>,
    val velocities: List<Double>,
    val gyroEvents: List<SensorEvent>,
    val accelEvents: List<SensorEvent>,
    val avgTouchPressure: Double,
    val perFieldAvgDelay: Map<String, Double>,
    val avgInterFieldPause: Double,
    val deletionCount: Int,
    val deletionRatio: Double,
    val fieldFocusSequence: String,
    val timeToFirstInput: Long,
    val timeFromLastInputToConfirm: Long
)

internal fun computeBaseFeatures(
    textSnapshot: List<TextChangeEvent>,
    touchSnapshot: List<TouchEvent>,
    sensorSnapshot: List<SensorEvent>,
    navSnapshot: List<NavigationEvent>,
    sessionStartTime: Long,
    endTime: Long
): BaseFeatures {
    val interCharDelays = mutableListOf<Long>()
    val sortedTextEvents = textSnapshot.sortedBy { it.timestamp }
    for (i in 1 until sortedTextEvents.size) {
        val delay = sortedTextEvents[i].timestamp - sortedTextEvents[i - 1].timestamp
        if (delay in 1..5000) interCharDelays.add(delay)
    }
    val avgInterChar = if (interCharDelays.isNotEmpty()) interCharDelays.average() else 0.0
    val stdInterChar = if (interCharDelays.size > 1) {
        val mean = interCharDelays.average()
        kotlin.math.sqrt(interCharDelays.map { (it - mean) * (it - mean) }.average())
    } else 0.0

    val touchSizes = touchSnapshot.map { it.size.toDouble() }
    val touchDurations = mutableListOf<Long>()
    val downEvents = touchSnapshot.filter { it.action == 0 }.sortedBy { it.eventTime }
    val remainingUpEvents = touchSnapshot.filter { it.action == 1 }.sortedBy { it.eventTime }.toMutableList()
    for (down in downEvents) {
        val matchingUp = remainingUpEvents.firstOrNull { it.eventTime >= down.eventTime && it.downTime == down.downTime }
        if (matchingUp != null) {
            touchDurations.add(matchingUp.eventTime - down.eventTime)
            remainingUpEvents.remove(matchingUp)
        }
    }

    val moveEvents = touchSnapshot.filter { it.action == 2 }.sortedBy { it.timestamp }
    val velocities = mutableListOf<Double>()
    for (i in 1 until moveEvents.size) {
        val dt = (moveEvents[i].eventTime - moveEvents[i - 1].eventTime).toDouble()
        if (dt > 0) {
            val dx = moveEvents[i].x - moveEvents[i - 1].x
            val dy = moveEvents[i].y - moveEvents[i - 1].y
            velocities.add(kotlin.math.sqrt((dx * dx + dy * dy).toDouble()) / dt * 1000)
        }
    }

    val gyroEvents = sensorSnapshot.filter { it.type == "gyroscope" }
    val accelEvents = sensorSnapshot.filter { it.type == "accelerometer" }
    val avgTouchPressure = touchSnapshot.map { it.touchMajor.toDouble() }.let {
        if (it.isNotEmpty()) it.average() else 0.0
    }

    val perFieldAvgDelay = sortedTextEvents.groupBy { it.fieldName }.mapValues { (_, events) ->
        val delays = events.zipWithNext().map { (a, b) -> b.timestamp - a.timestamp }.filter { it in 1..5000 }
        if (delays.isNotEmpty()) delays.average() else 0.0
    }

    val fieldTransitions = sortedTextEvents.zipWithNext()
        .filter { (a, b) -> a.fieldName != b.fieldName }
        .map { (a, b) -> b.timestamp - a.timestamp }

    val deletionCount = textSnapshot.count { it.isDeletion }
    val fieldFocusSeq = navSnapshot.filter { it.eventType == "field_focus" }.joinToString(" \u2192 ") { it.detail }
    val firstText = sortedTextEvents.firstOrNull()
    val lastText = sortedTextEvents.lastOrNull()

    return BaseFeatures(
        interCharDelays = interCharDelays,
        sortedTextEvents = sortedTextEvents,
        avgInterChar = avgInterChar,
        stdInterChar = stdInterChar,
        touchSizes = touchSizes,
        touchDurations = touchDurations,
        downEvents = downEvents,
        moveEvents = moveEvents,
        velocities = velocities,
        gyroEvents = gyroEvents,
        accelEvents = accelEvents,
        avgTouchPressure = avgTouchPressure,
        perFieldAvgDelay = perFieldAvgDelay,
        avgInterFieldPause = if (fieldTransitions.isNotEmpty()) fieldTransitions.average() else 0.0,
        deletionCount = deletionCount,
        deletionRatio = if (textSnapshot.isNotEmpty()) deletionCount.toDouble() / textSnapshot.size else 0.0,
        fieldFocusSequence = fieldFocusSeq,
        timeToFirstInput = if (firstText != null) firstText.timestamp - sessionStartTime else 0L,
        timeFromLastInputToConfirm = if (lastText != null) endTime - lastText.timestamp else 0L
    )
}

internal fun stdDev(values: List<Float>): Double {
    if (values.size < 2) return 0.0
    val mean = values.average()
    return kotlin.math.sqrt(values.map { (it - mean) * (it - mean) }.average())
}

internal fun stdDevDouble(values: List<Double>): Double {
    if (values.size < 2) return 0.0
    val mean = values.average()
    return kotlin.math.sqrt(values.map { (it - mean) * (it - mean) }.average())
}

/**
 * Phase 1 enhanced keystroke features (FR-CL-05 REQ-01 through REQ-05)
 */
internal data class KeystrokeEnhanced(
    val typingSpeedTrend: Double,
    val typingSpeedVariance: Double,
    val memoryVsReferenceRatio: Double,
    val burstCount: Int,
    val avgBurstLength: Double
)

internal fun computeKeystrokeEnhanced(
    sortedTextEvents: List<TextChangeEvent>,
    interCharDelays: List<Long>,
    perFieldAvgDelay: Map<String, Double>
): KeystrokeEnhanced {
    val windowAvgDelays = if (sortedTextEvents.size >= 5) {
        sortedTextEvents.chunked(5).filter { it.size == 5 }.map { window ->
            val delays = window.zipWithNext().map { (a, b) -> b.timestamp - a.timestamp }
                .filter { it in 1..5000 }
            if (delays.isNotEmpty()) delays.average() else 0.0
        }
    } else emptyList()

    val typingSpeedTrend = if (windowAvgDelays.size >= 2) {
        val n = windowAvgDelays.size.toDouble()
        val indices = windowAvgDelays.indices.map { it.toDouble() }
        val sumX = indices.sum(); val sumY = windowAvgDelays.sum()
        val sumXY = indices.zip(windowAvgDelays).sumOf { (x, y) -> x * y }
        val sumX2 = indices.sumOf { it * it }
        (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
    } else 0.0

    val typingSpeedVariance = if (windowAvgDelays.size >= 2) {
        val mean = windowAvgDelays.average()
        windowAvgDelays.map { (it - mean) * (it - mean) }.average()
    } else 0.0

    val accountDelay = perFieldAvgDelay["accountNumber"] ?: 0.0
    val noteDelay = perFieldAvgDelay["note"] ?: 0.0
    val memoryVsReferenceRatio = if (accountDelay > 0 && noteDelay > 0) accountDelay / noteDelay else 0.0

    var currentBurstEvents = 1
    val burstLengths = mutableListOf<Int>()
    for (i in interCharDelays.indices) {
        if (interCharDelays[i] < 50) {
            currentBurstEvents++
        } else {
            if (currentBurstEvents >= 3 && interCharDelays[i] > 500) burstLengths.add(currentBurstEvents)
            currentBurstEvents = 1
        }
    }
    if (currentBurstEvents >= 3) burstLengths.add(currentBurstEvents)

    return KeystrokeEnhanced(
        typingSpeedTrend = typingSpeedTrend,
        typingSpeedVariance = typingSpeedVariance,
        memoryVsReferenceRatio = memoryVsReferenceRatio,
        burstCount = burstLengths.size,
        avgBurstLength = if (burstLengths.isNotEmpty()) burstLengths.average() else 0.0
    )
}

/**
 * Phase 1 enhanced touch features (FR-CL-05 REQ-06 through REQ-13)
 */
internal data class TouchEnhanced(
    val touchCentroidX: Double,
    val touchCentroidY: Double,
    val touchSpreadX: Double,
    val touchSpreadY: Double,
    val dominantSwipeDirection: String,
    val avgSwipeLength: Double,
    val touchDurationStdDev: Double,
    val longPressRatio: Double
)

internal fun computeTouchEnhanced(
    downEvents: List<TouchEvent>,
    moveEvents: List<TouchEvent>,
    touchDurations: List<Long>
): TouchEnhanced {
    val downXCoords = downEvents.map { it.x.toDouble() }
    val downYCoords = downEvents.map { it.y.toDouble() }
    val touchCentroidX = if (downXCoords.isNotEmpty()) downXCoords.average() else 0.0
    val touchCentroidY = if (downYCoords.isNotEmpty()) downYCoords.average() else 0.0
    val touchSpreadX = stdDev(downEvents.map { it.x })
    val touchSpreadY = stdDev(downEvents.map { it.y })

    val swipeGestures = moveEvents.groupBy { it.downTime }
    val swipeDirections = mutableListOf<String>()
    val swipeLengths = mutableListOf<Double>()
    for ((_, events) in swipeGestures) {
        if (events.size < 2) continue
        val sorted = events.sortedBy { it.eventTime }
        val dx = (sorted.last().x - sorted.first().x).toDouble()
        val dy = (sorted.last().y - sorted.first().y).toDouble()
        swipeLengths.add(kotlin.math.sqrt(dx * dx + dy * dy))
        swipeDirections.add(
            if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                if (dx > 0) "RIGHT" else "LEFT"
            } else {
                if (dy > 0) "DOWN" else "UP"
            }
        )
    }
    val dominantSwipeDirection = if (swipeDirections.isNotEmpty()) {
        swipeDirections.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "NONE"
    } else "NONE"

    val touchDurationStdDev = if (touchDurations.size > 1) {
        val mean = touchDurations.average()
        kotlin.math.sqrt(touchDurations.map { (it - mean) * (it - mean) }.average())
    } else 0.0

    return TouchEnhanced(
        touchCentroidX = touchCentroidX,
        touchCentroidY = touchCentroidY,
        touchSpreadX = touchSpreadX,
        touchSpreadY = touchSpreadY,
        dominantSwipeDirection = dominantSwipeDirection,
        avgSwipeLength = if (swipeLengths.isNotEmpty()) swipeLengths.average() else 0.0,
        touchDurationStdDev = touchDurationStdDev,
        longPressRatio = if (touchDurations.isNotEmpty()) {
            touchDurations.count { it > 500 }.toDouble() / touchDurations.size
        } else 0.0
    )
}

/**
 * Phase 1 enhanced motion features (FR-CL-05 REQ-14 through REQ-19)
 */
internal data class MotionEnhanced(
    val avgPitch: Double,
    val avgRoll: Double,
    val pitchStdDev: Double,
    val rollStdDev: Double,
    val orientationChangeRate: Int,
    val maxOrientationShift: Double
)

internal fun computeMotionEnhanced(accelEvents: List<SensorEvent>): MotionEnhanced {
    val pitchValues = accelEvents.map { Math.toDegrees(kotlin.math.atan2(it.y.toDouble(), it.z.toDouble())) }
    val rollValues = accelEvents.map { Math.toDegrees(kotlin.math.atan2(it.x.toDouble(), it.z.toDouble())) }
    var orientationChangeRate = 0
    var maxOrientationShift = 0.0
    for (i in 1 until pitchValues.size) {
        val pitchChange = kotlin.math.abs(pitchValues[i] - pitchValues[i - 1])
        val rollChange = kotlin.math.abs(rollValues[i] - rollValues[i - 1])
        val maxChange = maxOf(pitchChange, rollChange)
        if (pitchChange > 5.0 || rollChange > 5.0) orientationChangeRate++
        if (maxChange > maxOrientationShift) maxOrientationShift = maxChange
    }
    return MotionEnhanced(
        avgPitch = if (pitchValues.isNotEmpty()) pitchValues.average() else 0.0,
        avgRoll = if (rollValues.isNotEmpty()) rollValues.average() else 0.0,
        pitchStdDev = stdDevDouble(pitchValues),
        rollStdDev = stdDevDouble(rollValues),
        orientationChangeRate = orientationChangeRate,
        maxOrientationShift = maxOrientationShift
    )
}

/**
 * Phase 1 enhanced navigation features (FR-CL-05 REQ-20 through REQ-25)
 */
internal data class NavigationEnhanced(
    val inactivityGapCount: Int,
    val maxInactivityGapMs: Long,
    val totalInactivityMs: Long,
    val fieldRevisitCount: Int,
    val hasBacktracking: Boolean,
    val timePerField: Map<String, Long>
)

internal fun computeNavigationEnhanced(
    touchSnapshot: List<TouchEvent>,
    textSnapshot: List<TextChangeEvent>,
    navSnapshot: List<NavigationEvent>,
    endTime: Long
): NavigationEnhanced {
    val allTimestamps = (touchSnapshot.map { it.timestamp } +
            textSnapshot.map { it.timestamp }).sorted()
    val inactivityGaps = if (allTimestamps.size >= 2) {
        allTimestamps.zipWithNext().map { (a, b) -> b - a }.filter { it > 2000 }
    } else emptyList()

    val focusEvents = navSnapshot.filter { it.eventType == "field_focus" }
    val visitedFields = mutableSetOf<String>()
    var fieldRevisitCount = 0
    for (event in focusEvents) {
        if (event.detail in visitedFields) fieldRevisitCount++
        else visitedFields.add(event.detail)
    }

    val timePerField = mutableMapOf<String, Long>()
    for (i in focusEvents.indices) {
        val fieldName = focusEvents[i].detail
        val focusStart = focusEvents[i].timestamp
        val focusEnd = if (i + 1 < focusEvents.size) focusEvents[i + 1].timestamp else endTime
        timePerField[fieldName] = (timePerField[fieldName] ?: 0L) + (focusEnd - focusStart)
    }

    return NavigationEnhanced(
        inactivityGapCount = inactivityGaps.size,
        maxInactivityGapMs = inactivityGaps.maxOrNull() ?: 0L,
        totalInactivityMs = inactivityGaps.sum(),
        fieldRevisitCount = fieldRevisitCount,
        hasBacktracking = fieldRevisitCount > 0,
        timePerField = timePerField.toMap()
    )
}

/**
 * Phase 2 hesitation category computation (FR-CL-06 REQ-06)
 */
internal fun computeHesitationCategory(
    currentTimeFromLastInput: Long,
    profile: BehavioralProfile?,
    enrollmentFeatures: List<BehavioralFeatures>
): String {
    if (profile == null || enrollmentFeatures.isEmpty()) return "UNKNOWN"

    val baseline = profile.avgTimeFromLastInputToConfirm
    val values = enrollmentFeatures.map { it.timeFromLastInputToConfirm.toDouble() }
    if (values.size < 2) return "UNKNOWN"

    val mean = values.average()
    val sd = kotlin.math.sqrt(values.map { (it - mean) * (it - mean) }.average())
    if (sd == 0.0) return "NORMAL"

    val current = currentTimeFromLastInput.toDouble()
    return when {
        current < baseline - 2 * sd -> "RUSHED"
        current > baseline + 2 * sd -> "HESITANT"
        else -> "NORMAL"
    }
}
