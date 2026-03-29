package com.poc.behavioralfraud.data.collector

import android.content.Context
import android.content.SharedPreferences
import android.database.ContentObserver
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.poc.behavioralfraud.data.model.*
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Collects behavioral data during a transfer session.
 *
 * Usage:
 *   1. Register as lifecycle observer for background tracking
 *   2. Call startSession() when transfer screen opens
 *   3. Feed events via onTouchEvent(), onTextChanged(), onFieldFocus()
 *   4. Call stopSession() when user confirms transfer
 *   5. Call extractFeatures() to get computed features
 */
class BehavioralCollector(private val context: Context) : DefaultLifecycleObserver {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val collectorPrefs: SharedPreferences =
        context.getSharedPreferences("behavioral_collector", Context.MODE_PRIVATE)

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

    // Phase 2 — Call detection state
    private var callActiveAtStart = false
    private var isCallActiveFlag = false
    private var callStartedFlag = false

    // Phase 2 — Background tracking state
    private var bgSwitchCount = 0
    private var totalBgTimeMs = 0L
    private var backgroundPauseTime = 0L

    // Phase 2 — Multi-touch tracking
    private var multiTouchEventCount = 0
    private var maxPointerCountObserved = 0

    // Phase 2 — Device context (collected once at session start)
    private var deviceModelValue = ""
    private var screenWidthValue = 0
    private var screenHeightValue = 0
    private var screenDensityValue = 0.0
    private var batteryLevelValue = 0
    private var isChargingValue = false

    // Phase 2 — Time context
    private var sessionHour = 0
    private var sessionDow = 1
    private var timeSinceLastSession = -1L

    // Phase 3 — Screenshot detection (FR-CL-07 REQ-07)
    @Volatile
    private var screenshotDetected = false
    private var screenshotObserver: ContentObserver? = null

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

    override fun onPause(owner: LifecycleOwner) { onPause() }
    override fun onResume(owner: LifecycleOwner) { onResume() }

    fun onPause() {
        if (!isCollecting) return
        backgroundPauseTime = System.currentTimeMillis()
        checkCallState()
    }

    fun onResume() {
        if (!isCollecting) return
        if (backgroundPauseTime > 0) {
            val bgTime = System.currentTimeMillis() - backgroundPauseTime
            totalBgTimeMs += bgTime
            bgSwitchCount++
            backgroundPauseTime = 0L
        }
        checkCallState()
    }

    fun startSession() {
        isCollecting = false
        sensorManager.unregisterListener(sensorListener)
        unregisterScreenshotObserver()

        touchEvents.clear(); textChangeEvents.clear()
        sensorEvents.clear(); navigationEvents.clear()

        // Reset state
        callActiveAtStart = false; isCallActiveFlag = false; callStartedFlag = false
        bgSwitchCount = 0; totalBgTimeMs = 0L; backgroundPauseTime = 0L
        multiTouchEventCount = 0; maxPointerCountObserved = 0
        screenshotDetected = false

        sessionId = UUID.randomUUID().toString().take(8)
        sessionStartTime = System.currentTimeMillis()
        isCollecting = true

        // Phase 2 — Check initial call state
        val inCall = isInCallMode()
        callActiveAtStart = inCall
        isCallActiveFlag = inCall

        // Phase 2 — Collect time context
        val calendar = Calendar.getInstance().apply { timeInMillis = sessionStartTime }
        sessionHour = calendar.get(Calendar.HOUR_OF_DAY)
        val javaDow = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...7=Sat
        sessionDow = if (javaDow == Calendar.SUNDAY) 7 else javaDow - 1 // 1=Mon...7=Sun

        // Phase 2 — Time since last session
        val lastEnd = collectorPrefs.getLong("lastSessionEndTimestamp", -1L)
        timeSinceLastSession = if (lastEnd > 0) sessionStartTime - lastEnd else -1L

        // Phase 2 — Collect device context (once)
        collectDeviceContext()

        // Register sensors with GAME delay (~50Hz) for behavioral biometrics
        // Research shows 20-100 Hz needed for behavioral biometrics (MDPI, Johns Hopkins)
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // Phase 3 — Register screenshot observer (REQ-07)
        registerScreenshotObserver()

        navigationEvents.add(
            NavigationEvent(
                timestamp = sessionStartTime,
                eventType = "screen_enter",
                detail = "transfer_screen"
            )
        )
    }

    fun stopSession() {
        if (backgroundPauseTime > 0) {
            totalBgTimeMs += System.currentTimeMillis() - backgroundPauseTime
            backgroundPauseTime = 0L
        }

        sessionEndTime = System.currentTimeMillis()
        isCollecting = false
        sensorManager.unregisterListener(sensorListener)

        // Phase 3 — Unregister screenshot observer
        unregisterScreenshotObserver()

        // Phase 2 — Final call state check
        checkCallState()

        // Phase 2 — Persist session end timestamp for timeSinceLastSession
        collectorPrefs.edit()
            .putLong("lastSessionEndTimestamp", sessionEndTime)
            .apply()

        navigationEvents.add(
            NavigationEvent(
                timestamp = sessionEndTime,
                eventType = "confirm_tap",
                detail = "transfer_confirm"
            )
        )
    }

    fun onTouchEvent(
        action: Int,
        x: Float,
        y: Float,
        size: Float,
        touchMajor: Float,
        downTime: Long,
        eventTime: Long,
        pointerCount: Int = 1
    ) {
        if (!isCollecting) return

        // Phase 2 — Multi-touch tracking
        if (pointerCount > 1) multiTouchEventCount++
        if (pointerCount > maxPointerCountObserved) maxPointerCountObserved = pointerCount

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

    fun extractFeatures(
        profile: BehavioralProfile? = null,
        enrollmentFeatures: List<BehavioralFeatures> = emptyList()
    ): BehavioralFeatures {
        val endTime = if (sessionEndTime > 0) sessionEndTime else System.currentTimeMillis()
        val textSnapshot = textChangeEvents.toList()
        val touchSnapshot = touchEvents.toList()
        val sensorSnapshot = sensorEvents.toList()
        val navSnapshot = navigationEvents.toList()

        // Compute base + enhanced features (delegated to FeatureComputation.kt)
        val base = computeBaseFeatures(textSnapshot, touchSnapshot, sensorSnapshot, navSnapshot, sessionStartTime, endTime)
        val keystroke = computeKeystrokeEnhanced(base.sortedTextEvents, base.interCharDelays, base.perFieldAvgDelay)
        val touchEnh = computeTouchEnhanced(base.downEvents, base.moveEvents, base.touchDurations)
        val motion = computeMotionEnhanced(base.accelEvents)
        val nav = computeNavigationEnhanced(touchSnapshot, textSnapshot, navSnapshot, endTime)

        // === Phase 2: Cognitive & Intent Signals (FR-CL-06) ===
        val avgBgDuration = if (bgSwitchCount > 0) totalBgTimeMs.toDouble() / bgSwitchCount else 0.0
        val hesitationCategory = computeHesitationCategory(
            base.timeFromLastInputToConfirm, profile, enrollmentFeatures
        )

        // === Phase 3: Advanced Motion & Pattern Analysis (FR-CL-07) ===
        val motionAdv = computeMotionAdvanced(
            base.downEvents, touchSnapshot, base.accelEvents, base.gyroEvents,
            sessionStartTime, endTime
        )
        val cognitiveAdv = computeCognitiveAdvanced(base.sortedTextEvents)

        return BehavioralFeatures(
            sessionDurationMs = endTime - sessionStartTime,
            avgInterCharDelayMs = base.avgInterChar,
            stdInterCharDelayMs = base.stdInterChar,
            maxInterCharDelayMs = base.interCharDelays.maxOrNull() ?: 0L,
            minInterCharDelayMs = base.interCharDelays.minOrNull() ?: 0L,
            totalTextChanges = textSnapshot.size,
            pasteCount = textSnapshot.count { it.isPaste },
            totalTouchEvents = touchSnapshot.size,
            avgTouchSize = if (base.touchSizes.isNotEmpty()) base.touchSizes.average() else 0.0,
            avgTouchDurationMs = if (base.touchDurations.isNotEmpty()) base.touchDurations.average() else 0.0,
            avgSwipeVelocity = if (base.velocities.isNotEmpty()) base.velocities.average() else 0.0,
            gyroStabilityX = stdDev(base.gyroEvents.map { it.x }),
            gyroStabilityY = stdDev(base.gyroEvents.map { it.y }),
            gyroStabilityZ = stdDev(base.gyroEvents.map { it.z }),
            accelStabilityX = stdDev(base.accelEvents.map { it.x }),
            accelStabilityY = stdDev(base.accelEvents.map { it.y }),
            accelStabilityZ = stdDev(base.accelEvents.map { it.z }),
            avgTouchPressure = base.avgTouchPressure,
            perFieldAvgDelay = base.perFieldAvgDelay,
            avgInterFieldPauseMs = base.avgInterFieldPause,
            deletionCount = base.deletionCount,
            deletionRatio = base.deletionRatio,
            fieldFocusSequence = base.fieldFocusSequence,
            timeToFirstInput = base.timeToFirstInput,
            timeFromLastInputToConfirm = base.timeFromLastInputToConfirm,
            // Phase 1 — Enhanced features
            typingSpeedTrend = keystroke.typingSpeedTrend,
            typingSpeedVariance = keystroke.typingSpeedVariance,
            memoryVsReferenceRatio = keystroke.memoryVsReferenceRatio,
            burstCount = keystroke.burstCount,
            avgBurstLength = keystroke.avgBurstLength,
            touchCentroidX = touchEnh.touchCentroidX,
            touchCentroidY = touchEnh.touchCentroidY,
            touchSpreadX = touchEnh.touchSpreadX,
            touchSpreadY = touchEnh.touchSpreadY,
            dominantSwipeDirection = touchEnh.dominantSwipeDirection,
            avgSwipeLength = touchEnh.avgSwipeLength,
            touchDurationStdDev = touchEnh.touchDurationStdDev,
            longPressRatio = touchEnh.longPressRatio,
            avgPitch = motion.avgPitch,
            avgRoll = motion.avgRoll,
            pitchStdDev = motion.pitchStdDev,
            rollStdDev = motion.rollStdDev,
            orientationChangeRate = motion.orientationChangeRate,
            maxOrientationShift = motion.maxOrientationShift,
            inactivityGapCount = nav.inactivityGapCount,
            maxInactivityGapMs = nav.maxInactivityGapMs,
            totalInactivityMs = nav.totalInactivityMs,
            fieldRevisitCount = nav.fieldRevisitCount,
            hasBacktracking = nav.hasBacktracking,
            timePerField = nav.timePerField,
            // Phase 2 — Cognitive / Intent
            isCallActiveDuringSession = isCallActiveFlag,
            callStartedDuringSession = callStartedFlag,
            backgroundSwitchCount = bgSwitchCount,
            totalBackgroundTimeMs = totalBgTimeMs,
            avgBackgroundDurationMs = avgBgDuration,
            preSubmitHesitationCategory = hesitationCategory,
            sessionHourOfDay = sessionHour,
            sessionDayOfWeek = sessionDow,
            timeSinceLastSessionMs = timeSinceLastSession,
            // Phase 2 — Device context
            deviceModel = deviceModelValue,
            screenWidthPx = screenWidthValue,
            screenHeightPx = screenHeightValue,
            screenDensity = screenDensityValue,
            batteryLevel = batteryLevelValue,
            isCharging = isChargingValue,
            // Phase 2 — Touch addition
            multiTouchCount = multiTouchEventCount,
            maxPointerCount = maxPointerCountObserved,
            // Phase 3 — Advanced Motion & Pattern Analysis
            avgTapAccelSpike = motionAdv.avgTapAccelSpike,
            avgTapRecoveryMs = motionAdv.avgTapRecoveryMs,
            idleGyroRMS = motionAdv.idleGyroRMS,
            idleAccelJitter = motionAdv.idleAccelJitter,
            correctionSameCount = cognitiveAdv.correctionSameCount,
            correctionDifferentCount = cognitiveAdv.correctionDifferentCount,
            screenshotDuringInput = screenshotDetected
        )
    }

    // --- Phase 2 private helpers ---

    private fun isInCallMode(): Boolean {
        val mode = audioManager.mode
        return mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION
    }

    private fun checkCallState() {
        if (isInCallMode()) {
            isCallActiveFlag = true
            if (!callActiveAtStart) {
                callStartedFlag = true
            }
        }
    }

    private fun collectDeviceContext() {
        @Suppress("UNNECESSARY_SAFE_CALL")
        deviceModelValue = Build.MODEL ?: ""

        val displayMetrics = context.resources.displayMetrics
        screenWidthValue = displayMetrics.widthPixels
        screenHeightValue = displayMetrics.heightPixels
        screenDensityValue = displayMetrics.density.toDouble()

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        batteryLevelValue = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
        isChargingValue = batteryManager?.isCharging ?: false
    }

    // --- Phase 3 private helpers ---

    // Note: ContentObserver fires for ANY image write to MediaStore (not just screenshots).
    // For production, filter by path containing "screenshot". Acceptable for POC per SRS.
    private fun registerScreenshotObserver() {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                if (isCollecting) screenshotDetected = true
            }
        }
        screenshotObserver = observer
        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
        } catch (_: Exception) {
            // Some devices may restrict ContentObserver — acceptable for POC
        }
    }

    private fun unregisterScreenshotObserver() {
        screenshotObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
            } catch (_: Exception) {}
        }
        screenshotObserver = null
    }

    // --- Test helpers ---
    internal fun addSensorEventForTest(event: SensorEvent) { sensorEvents.add(event) }
    internal fun addTouchEventForTest(event: TouchEvent) { touchEvents.add(event) }
    internal fun setScreenshotDetectedForTest(detected: Boolean) { screenshotDetected = detected }

    fun getEventCounts(): Map<String, Int> = mapOf(
        "touch" to touchEvents.size, "textChange" to textChangeEvents.size,
        "sensor" to sensorEvents.size, "navigation" to navigationEvents.size
    )
}
