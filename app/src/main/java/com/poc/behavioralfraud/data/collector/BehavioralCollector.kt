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

        // --- Inter-char delays ---
        val interCharDelays = mutableListOf<Long>()
        val sortedTextEvents = textChangeEvents.sortedBy { it.timestamp }
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
        val touchSizes = touchEvents.map { it.size.toDouble() }
        val touchDurations = mutableListOf<Long>()
        val downEvents = touchEvents.filter { it.action == 0 }.sortedBy { it.eventTime } // ACTION_DOWN
        val remainingUpEvents = touchEvents.filter { it.action == 1 }.sortedBy { it.eventTime }.toMutableList() // ACTION_UP
        for (down in downEvents) {
            val matchingUp = remainingUpEvents.firstOrNull { it.eventTime >= down.eventTime && it.downTime == down.downTime }
            if (matchingUp != null) {
                touchDurations.add(matchingUp.eventTime - down.eventTime)
                remainingUpEvents.remove(matchingUp) // Don't reuse this UP event
            }
        }

        // Swipe velocity (from ACTION_MOVE sequences)
        val moveEvents = touchEvents.filter { it.action == 2 }.sortedBy { it.timestamp }
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
        val gyroEvents = sensorEvents.filter { it.type == "gyroscope" }
        val accelEvents = sensorEvents.filter { it.type == "accelerometer" }

        fun stdDev(values: List<Float>): Double {
            if (values.size < 2) return 0.0
            val mean = values.average()
            return kotlin.math.sqrt(values.map { (it - mean) * (it - mean) }.average())
        }

        // --- Touch pressure (from touchMajor) ---
        val touchPressures = touchEvents.map { it.touchMajor.toDouble() }
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
        val deletionCount = textChangeEvents.count { it.isDeletion }
        val deletionRatio = if (textChangeEvents.isNotEmpty())
            deletionCount.toDouble() / textChangeEvents.size else 0.0

        // --- Navigation ---
        val fieldFocusSeq = navigationEvents
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

        return BehavioralFeatures(
            sessionDurationMs = endTime - sessionStartTime,
            avgInterCharDelayMs = avgInterChar,
            stdInterCharDelayMs = stdInterChar,
            maxInterCharDelayMs = interCharDelays.maxOrNull() ?: 0L,
            minInterCharDelayMs = interCharDelays.minOrNull() ?: 0L,
            totalTextChanges = textChangeEvents.size,
            pasteCount = textChangeEvents.count { it.isPaste },
            totalTouchEvents = touchEvents.size,
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
            timeFromLastInputToConfirm = timeFromLastInputToConfirm
        )
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
