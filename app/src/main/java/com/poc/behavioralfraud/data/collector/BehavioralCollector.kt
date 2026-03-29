package com.poc.behavioralfraud.data.collector

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.poc.behavioralfraud.data.model.*
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Collects behavioral data during a transfer session.
 *
 * Usage:
 *   1. Call startSession() when transfer screen opens
 *   2. Feed events via onTouchEvent(), onTextChanged(), onFieldFocus()
 *   3. Call stopSession() when user confirms transfer
 *   4. Call extractFeatures() to get computed features
 */
class BehavioralCollector(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Raw event buffers (thread-safe)
    private val touchEvents = CopyOnWriteArrayList<TouchEvent>()
    private val textChangeEvents = CopyOnWriteArrayList<TextChangeEvent>()
    private val sensorEvents = CopyOnWriteArrayList<SensorEvent>()
    private val navigationEvents = CopyOnWriteArrayList<NavigationEvent>()

    private var sessionId = ""
    private var sessionStartTime = 0L
    private var sessionEndTime = 0L
    @Volatile
    private var isCollecting = false

    // Sensor listener
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: android.hardware.SensorEvent) {
            if (!isCollecting) return
            val type = when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> "accelerometer"
                Sensor.TYPE_GYROSCOPE -> "gyroscope"
                else -> return
            }
            sensorEvents.add(
                SensorEvent(
                    timestamp = System.currentTimeMillis(),
                    type = type,
                    x = event.values[0],
                    y = event.values[1],
                    z = event.values[2]
                )
            )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    /**
     * Start collecting behavioral data
     */
    fun startSession() {
        // Stop any previous session to prevent double-registered listeners
        isCollecting = false
        sensorManager.unregisterListener(sensorListener)

        // Clear previous data
        touchEvents.clear()
        textChangeEvents.clear()
        sensorEvents.clear()
        navigationEvents.clear()

        sessionId = UUID.randomUUID().toString().take(8)
        sessionStartTime = System.currentTimeMillis()
        isCollecting = true

        // Register sensors with GAME delay (~50Hz) for behavioral biometrics
        // Research shows 20-100 Hz needed for behavioral biometrics (MDPI, Johns Hopkins)
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
        }

        navigationEvents.add(
            NavigationEvent(
                timestamp = sessionStartTime,
                eventType = "screen_enter",
                detail = "transfer_screen"
            )
        )
    }

    /**
     * Stop collecting and unregister sensors
     */
    fun stopSession() {
        sessionEndTime = System.currentTimeMillis()
        isCollecting = false
        sensorManager.unregisterListener(sensorListener)

        navigationEvents.add(
            NavigationEvent(
                timestamp = sessionEndTime,
                eventType = "confirm_tap",
                detail = "transfer_confirm"
            )
        )
    }

    /**
     * Record a touch event from MotionEvent
     */
    fun onTouchEvent(
        action: Int,
        x: Float,
        y: Float,
        size: Float,
        touchMajor: Float,
        downTime: Long,
        eventTime: Long
    ) {
        if (!isCollecting) return
        touchEvents.add(
            TouchEvent(
                timestamp = System.currentTimeMillis(),
                action = action,
                x = x,
                y = y,
                size = size,
                touchMajor = touchMajor,
                downTime = downTime,
                eventTime = eventTime
            )
        )
    }

    /**
     * Record a text change event from TextField onValueChange
     */
    fun onTextChanged(
        fieldName: String,
        previousLength: Int,
        newLength: Int
    ) {
        if (!isCollecting) return
        val delta = newLength - previousLength
        textChangeEvents.add(
            TextChangeEvent(
                timestamp = System.currentTimeMillis(),
                fieldName = fieldName,
                previousLength = previousLength,
                newLength = newLength,
                lengthDelta = delta,
                isPaste = delta >= 3,  // 3+ chars at once = likely paste (avoids autocorrect false positives)
                isDeletion = delta < 0
            )
        )
    }

    /**
     * Record field focus event
     */
    fun onFieldFocus(fieldName: String) {
        if (!isCollecting) return
        navigationEvents.add(
            NavigationEvent(
                timestamp = System.currentTimeMillis(),
                eventType = "field_focus",
                detail = fieldName
            )
        )
    }

    /**
     * Get the complete session data
     */
    fun getSession(transaction: TransactionInfo): BehavioralSession {
        return BehavioralSession(
            sessionId = sessionId,
            startTime = sessionStartTime,
            endTime = if (sessionEndTime > 0) sessionEndTime else System.currentTimeMillis(),
            touchEvents = touchEvents.toList(),
            textChangeEvents = textChangeEvents.toList(),
            sensorEvents = sensorEvents.toList(),
            navigationEvents = navigationEvents.toList(),
            transaction = transaction
        )
    }

    /**
     * Extract computed features from raw data
     */
    fun extractFeatures(): BehavioralFeatures {
        val endTime = if (sessionEndTime > 0) sessionEndTime else System.currentTimeMillis()

        // Snapshot all lists for consistent computation across the method
        val textSnapshot = textChangeEvents.toList()
        val touchSnapshot = touchEvents.toList()
        val sensorSnapshot = sensorEvents.toList()
        val navSnapshot = navigationEvents.toList()

        // --- Inter-char delays ---
        val interCharDelays = mutableListOf<Long>()
        val sortedTextEvents = textSnapshot.sortedBy { it.timestamp }
        for (i in 1 until sortedTextEvents.size) {
            val delay = sortedTextEvents[i].timestamp - sortedTextEvents[i - 1].timestamp
            if (delay in 1..5000) { // Filter out unreasonable delays
                interCharDelays.add(delay)
            }
        }
        val avgInterChar = if (interCharDelays.isNotEmpty()) interCharDelays.average() else 0.0
        val stdInterChar = if (interCharDelays.size > 1) {
            val mean = interCharDelays.average()
            kotlin.math.sqrt(interCharDelays.map { (it - mean) * (it - mean) }.average())
        } else 0.0

        // --- Touch features ---
        val touchSizes = touchSnapshot.map { it.size.toDouble() }
        val touchDurations = mutableListOf<Long>()
        val downEvents = touchSnapshot.filter { it.action == 0 }.sortedBy { it.eventTime } // ACTION_DOWN
        val remainingUpEvents = touchSnapshot.filter { it.action == 1 }.sortedBy { it.eventTime }.toMutableList() // ACTION_UP
        for (down in downEvents) {
            val matchingUp = remainingUpEvents.firstOrNull { it.eventTime >= down.eventTime && it.downTime == down.downTime }
            if (matchingUp != null) {
                touchDurations.add(matchingUp.eventTime - down.eventTime)
                remainingUpEvents.remove(matchingUp) // Don't reuse this UP event
            }
        }

        // Swipe velocity (from ACTION_MOVE sequences)
        val moveEvents = touchSnapshot.filter { it.action == 2 }.sortedBy { it.timestamp }
        val velocities = mutableListOf<Double>()
        for (i in 1 until moveEvents.size) {
            val dt = (moveEvents[i].eventTime - moveEvents[i - 1].eventTime).toDouble()
            if (dt > 0) {
                val dx = moveEvents[i].x - moveEvents[i - 1].x
                val dy = moveEvents[i].y - moveEvents[i - 1].y
                val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())
                velocities.add(dist / dt * 1000) // pixels per second
            }
        }

        // --- Sensor stability (standard deviation) ---
        val gyroEvents = sensorSnapshot.filter { it.type == "gyroscope" }
        val accelEvents = sensorSnapshot.filter { it.type == "accelerometer" }

        fun stdDev(values: List<Float>): Double {
            if (values.size < 2) return 0.0
            val mean = values.average()
            return kotlin.math.sqrt(values.map { (it - mean) * (it - mean) }.average())
        }

        // --- Touch pressure (from touchMajor) ---
        val touchPressures = touchSnapshot.map { it.touchMajor.toDouble() }
        val avgTouchPressure = if (touchPressures.isNotEmpty()) touchPressures.average() else 0.0

        // --- Per-field typing rhythm ---
        val perFieldAvgDelay = sortedTextEvents
            .groupBy { it.fieldName }
            .mapValues { (_, events) ->
                val delays = events.zipWithNext().map { (a, b) -> b.timestamp - a.timestamp }
                    .filter { it in 1..5000 }
                if (delays.isNotEmpty()) delays.average() else 0.0
            }

        // --- Inter-field hesitation ---
        val fieldTransitions = sortedTextEvents.zipWithNext()
            .filter { (a, b) -> a.fieldName != b.fieldName }
            .map { (a, b) -> b.timestamp - a.timestamp }
        val avgInterFieldPause = if (fieldTransitions.isNotEmpty()) fieldTransitions.average() else 0.0

        // --- Deletion patterns ---
        val deletionCount = textSnapshot.count { it.isDeletion }
        val deletionRatio = if (textSnapshot.isNotEmpty())
            deletionCount.toDouble() / textSnapshot.size else 0.0

        // --- Navigation ---
        val fieldFocusSeq = navSnapshot
            .filter { it.eventType == "field_focus" }
            .joinToString(" → ") { it.detail }

        val firstTextEvent = sortedTextEvents.firstOrNull()
        val timeToFirstInput = if (firstTextEvent != null) {
            firstTextEvent.timestamp - sessionStartTime
        } else 0L

        val lastTextEvent = sortedTextEvents.lastOrNull()
        val timeFromLastInputToConfirm = if (lastTextEvent != null) {
            endTime - lastTextEvent.timestamp
        } else 0L

        // === Phase 1: Enhanced Feature Extraction (FR-CL-05) ===
        fun stdDevDouble(values: List<Double>): Double {
            if (values.size < 2) return 0.0
            val mean = values.average()
            return kotlin.math.sqrt(values.map { (it - mean) * (it - mean) }.average())
        }

        // --- Keystroke: REQ-01 through REQ-05 ---
        // SRS: "Window size: 5 text events" → group events into windows of 5, compute avg delay per window
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
        if (currentBurstEvents >= 3) burstLengths.add(currentBurstEvents) // trailing burst
        val burstCount = burstLengths.size
        val avgBurstLength = if (burstLengths.isNotEmpty()) burstLengths.average() else 0.0

        // --- Touch: REQ-06 through REQ-13 ---
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
        val avgSwipeLength = if (swipeLengths.isNotEmpty()) swipeLengths.average() else 0.0
        val touchDurationStdDev = if (touchDurations.size > 1) {
            val mean = touchDurations.average()
            kotlin.math.sqrt(touchDurations.map { (it - mean) * (it - mean) }.average())
        } else 0.0
        val longPressRatio = if (touchDurations.isNotEmpty()) {
            touchDurations.count { it > 500 }.toDouble() / touchDurations.size
        } else 0.0

        // --- Motion: REQ-14 through REQ-19 ---
        val pitchValues = accelEvents.map { Math.toDegrees(kotlin.math.atan2(it.y.toDouble(), it.z.toDouble())) }
        val rollValues = accelEvents.map { Math.toDegrees(kotlin.math.atan2(it.x.toDouble(), it.z.toDouble())) }
        val avgPitch = if (pitchValues.isNotEmpty()) pitchValues.average() else 0.0
        val avgRoll = if (rollValues.isNotEmpty()) rollValues.average() else 0.0
        val pitchStdDev = stdDevDouble(pitchValues)
        val rollStdDev = stdDevDouble(rollValues)
        var orientationChangeRate = 0
        var maxOrientationShift = 0.0
        for (i in 1 until pitchValues.size) {
            val pitchChange = kotlin.math.abs(pitchValues[i] - pitchValues[i - 1])
            val rollChange = kotlin.math.abs(rollValues[i] - rollValues[i - 1])
            val maxChange = maxOf(pitchChange, rollChange)
            if (pitchChange > 5.0 || rollChange > 5.0) orientationChangeRate++
            if (maxChange > maxOrientationShift) maxOrientationShift = maxChange
        }

        // --- Navigation: REQ-20 through REQ-25 ---
        val allTimestamps = (touchSnapshot.map { it.timestamp } +
                textSnapshot.map { it.timestamp }).sorted()
        val inactivityGaps = if (allTimestamps.size >= 2) {
            allTimestamps.zipWithNext().map { (a, b) -> b - a }.filter { it > 2000 }
        } else emptyList()
        val inactivityGapCount = inactivityGaps.size
        val maxInactivityGapMs = inactivityGaps.maxOrNull() ?: 0L
        val totalInactivityMs = inactivityGaps.sum()
        val focusEvents = navSnapshot.filter { it.eventType == "field_focus" }
        val visitedFields = mutableSetOf<String>()
        var fieldRevisitCount = 0
        for (event in focusEvents) {
            if (event.detail in visitedFields) fieldRevisitCount++
            else visitedFields.add(event.detail)
        }
        val hasBacktracking = fieldRevisitCount > 0
        val timePerField = mutableMapOf<String, Long>()
        for (i in focusEvents.indices) {
            val fieldName = focusEvents[i].detail
            val focusStart = focusEvents[i].timestamp
            val focusEnd = if (i + 1 < focusEvents.size) focusEvents[i + 1].timestamp else endTime
            timePerField[fieldName] = (timePerField[fieldName] ?: 0L) + (focusEnd - focusStart)
        }

        return BehavioralFeatures(
            sessionDurationMs = endTime - sessionStartTime,
            avgInterCharDelayMs = avgInterChar,
            stdInterCharDelayMs = stdInterChar,
            maxInterCharDelayMs = interCharDelays.maxOrNull() ?: 0L,
            minInterCharDelayMs = interCharDelays.minOrNull() ?: 0L,
            totalTextChanges = textSnapshot.size,
            pasteCount = textSnapshot.count { it.isPaste },
            totalTouchEvents = touchSnapshot.size,
            avgTouchSize = if (touchSizes.isNotEmpty()) touchSizes.average() else 0.0,
            avgTouchDurationMs = if (touchDurations.isNotEmpty()) touchDurations.average() else 0.0,
            avgSwipeVelocity = if (velocities.isNotEmpty()) velocities.average() else 0.0,
            gyroStabilityX = stdDev(gyroEvents.map { it.x }),
            gyroStabilityY = stdDev(gyroEvents.map { it.y }),
            gyroStabilityZ = stdDev(gyroEvents.map { it.z }),
            accelStabilityX = stdDev(accelEvents.map { it.x }),
            accelStabilityY = stdDev(accelEvents.map { it.y }),
            accelStabilityZ = stdDev(accelEvents.map { it.z }),
            avgTouchPressure = avgTouchPressure,
            perFieldAvgDelay = perFieldAvgDelay,
            avgInterFieldPauseMs = avgInterFieldPause,
            deletionCount = deletionCount,
            deletionRatio = deletionRatio,
            fieldFocusSequence = fieldFocusSeq,
            timeToFirstInput = timeToFirstInput,
            timeFromLastInputToConfirm = timeFromLastInputToConfirm,
            // Phase 1 — Enhanced features
            typingSpeedTrend = typingSpeedTrend,
            typingSpeedVariance = typingSpeedVariance,
            memoryVsReferenceRatio = memoryVsReferenceRatio,
            burstCount = burstCount,
            avgBurstLength = avgBurstLength,
            touchCentroidX = touchCentroidX,
            touchCentroidY = touchCentroidY,
            touchSpreadX = touchSpreadX,
            touchSpreadY = touchSpreadY,
            dominantSwipeDirection = dominantSwipeDirection,
            avgSwipeLength = avgSwipeLength,
            touchDurationStdDev = touchDurationStdDev,
            longPressRatio = longPressRatio,
            avgPitch = avgPitch,
            avgRoll = avgRoll,
            pitchStdDev = pitchStdDev,
            rollStdDev = rollStdDev,
            orientationChangeRate = orientationChangeRate,
            maxOrientationShift = maxOrientationShift,
            inactivityGapCount = inactivityGapCount,
            maxInactivityGapMs = maxInactivityGapMs,
            totalInactivityMs = totalInactivityMs,
            fieldRevisitCount = fieldRevisitCount,
            hasBacktracking = hasBacktracking,
            timePerField = timePerField.toMap()
        )
    }

    /**
     * Add a sensor event for testing (no SensorManager needed)
     */
    internal fun addSensorEventForTest(event: SensorEvent) {
        sensorEvents.add(event)
    }

    /**
     * Get raw event counts for debug display
     */
    fun getEventCounts(): Map<String, Int> = mapOf(
        "touch" to touchEvents.size,
        "textChange" to textChangeEvents.size,
        "sensor" to sensorEvents.size,
        "navigation" to navigationEvents.size
    )
}
