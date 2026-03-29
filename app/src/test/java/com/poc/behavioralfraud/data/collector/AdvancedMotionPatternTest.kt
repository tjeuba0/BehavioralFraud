package com.poc.behavioralfraud.data.collector

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.BatteryManager
import android.util.DisplayMetrics
import com.poc.behavioralfraud.data.model.SensorEvent
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt

/**
 * Specification-based tests for Phase 3: Advanced Motion & Pattern Analysis (TASK-009).
 *
 * Tests verify the 7 features defined in FR-CL-07 of the SRS (REQ-01 through REQ-07).
 * Each REQ-ID has at minimum one happy-path test and one edge-case test.
 *
 * BehavioralCollector requires an Android Context (for SensorManager, AudioManager, etc.).
 * We mock Context plus all system services. Touch, text, and sensor events
 * are fed through the public API methods.
 *
 * Naming convention: `REQ-XX - description`
 */
class AdvancedMotionPatternTest {

    private lateinit var collector: BehavioralCollector
    private lateinit var mockContext: Context
    private lateinit var mockSensorManager: SensorManager
    private lateinit var mockAudioManager: AudioManager
    private lateinit var mockBatteryManager: BatteryManager
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockSharedPreferencesEditor: SharedPreferences.Editor
    private lateinit var mockResources: Resources
    private lateinit var mockDisplayMetrics: DisplayMetrics

    private companion object {
        const val DELTA = 0.01
        /** Relaxed tolerance for timing-dependent tests where Thread.sleep drift is possible. */
        const val RELAXED_DELTA = 5.0
        const val ACTION_DOWN = 0
        const val ACTION_UP = 1
        const val ACTION_MOVE = 2
    }

    @Before
    fun setup() {
        mockSensorManager = mockk(relaxed = true)
        mockAudioManager = mockk(relaxed = true)
        mockBatteryManager = mockk(relaxed = true)
        mockSharedPreferences = mockk(relaxed = true)
        mockSharedPreferencesEditor = mockk(relaxed = true)

        mockDisplayMetrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 2340
            density = 2.75f
        }
        mockResources = mockk(relaxed = true)
        every { mockResources.displayMetrics } returns mockDisplayMetrics

        mockContext = mockk(relaxed = true)
        every { mockContext.getSystemService(Context.SENSOR_SERVICE) } returns mockSensorManager
        every { mockContext.getSystemService(Context.AUDIO_SERVICE) } returns mockAudioManager
        every { mockContext.getSystemService(Context.BATTERY_SERVICE) } returns mockBatteryManager
        every { mockContext.resources } returns mockResources

        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.putLong(any(), any()) } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.apply() } returns Unit

        every { mockAudioManager.mode } returns AudioManager.MODE_NORMAL
        every { mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } returns 75
        every { mockBatteryManager.isCharging } returns false
        every { mockSharedPreferences.getLong("lastSessionEndTimestamp", -1L) } returns -1L

        collector = BehavioralCollector(mockContext)
        collector.startSession()
    }

    // =========================================================================
    // Helper functions
    // =========================================================================

    private data class TextEventSpec(
        val fieldName: String,
        val prevLen: Int,
        val newLen: Int,
        val delayBeforeMs: Long = 0
    )

    private fun feedTextEventsWithDelays(entries: List<TextEventSpec>) {
        for (entry in entries) {
            if (entry.delayBeforeMs > 0) {
                Thread.sleep(entry.delayBeforeMs)
            }
            collector.onTextChanged(entry.fieldName, entry.prevLen, entry.newLen)
        }
    }

    /**
     * Perform a tap: ACTION_DOWN then ACTION_UP at the given coordinates.
     * Returns the approximate timestamp of the DOWN event.
     */
    private fun performTap(x: Float = 540f, y: Float = 1170f): Long {
        val now = System.currentTimeMillis()
        collector.onTouchEvent(ACTION_DOWN, x, y, 0.5f, 10f, now, now)
        Thread.sleep(30)
        val upTime = System.currentTimeMillis()
        collector.onTouchEvent(ACTION_UP, x, y, 0.5f, 10f, now, upTime)
        return now
    }

    /**
     * Inject accelerometer events around a tap timestamp to simulate pre-tap baseline
     * and post-tap spike. Pre-tap: [-200ms, 0] window, Post-tap: [0, +100ms] window.
     *
     * @param tapTime approximate timestamp of the DOWN event
     * @param baselineMagnitude magnitude during pre-tap period (default low ~9.8, i.e. gravity)
     * @param spikeMagnitude magnitude during post-tap period (spike from grasp resistance)
     */
    private fun injectAccelAroundTap(
        tapTime: Long,
        baselineMagnitude: Float = 9.8f,
        spikeMagnitude: Float = 15.0f
    ) {
        // Pre-tap baseline: events from -200ms to 0ms before tap, every 20ms (10 events)
        for (i in 10 downTo 1) {
            val ts = tapTime - i * 20L
            // Distribute baseline across axes (simple: all on Z)
            collector.addSensorEventForTest(
                SensorEvent(
                    timestamp = ts,
                    type = "accelerometer",
                    x = 0f,
                    y = 0f,
                    z = baselineMagnitude
                )
            )
        }
        // Post-tap spike: events from 0ms to +100ms after tap, every 20ms (5 events)
        for (i in 0 until 5) {
            val ts = tapTime + i * 20L
            collector.addSensorEventForTest(
                SensorEvent(
                    timestamp = ts,
                    type = "accelerometer",
                    x = 0f,
                    y = 0f,
                    z = spikeMagnitude
                )
            )
        }
    }

    /**
     * Inject accelerometer recovery events after a tap, simulating magnitude
     * starting at spike level and gradually returning to baseline.
     *
     * @param tapTime approximate timestamp of the DOWN event
     * @param spikeMagnitude starting magnitude after tap
     * @param baselineMagnitude target baseline magnitude
     * @param recoveryMs how long it takes to return to baseline
     */
    private fun injectAccelWithRecovery(
        tapTime: Long,
        spikeMagnitude: Float = 15.0f,
        baselineMagnitude: Float = 9.8f,
        recoveryMs: Long = 200L
    ) {
        // Pre-tap baseline
        for (i in 10 downTo 1) {
            val ts = tapTime - i * 20L
            collector.addSensorEventForTest(
                SensorEvent(timestamp = ts, type = "accelerometer", x = 0f, y = 0f, z = baselineMagnitude)
            )
        }
        // Post-tap: spike then gradual return
        val steps = (recoveryMs / 20L).toInt()
        for (i in 0..steps) {
            val ts = tapTime + i * 20L
            val fraction = i.toFloat() / steps
            val mag = spikeMagnitude + (baselineMagnitude - spikeMagnitude) * fraction
            collector.addSensorEventForTest(
                SensorEvent(timestamp = ts, type = "accelerometer", x = 0f, y = 0f, z = mag)
            )
        }
    }

    /**
     * Inject gyroscope events during a specified time range (for idle period testing).
     */
    private fun injectGyroEvents(startTime: Long, durationMs: Long, intervalMs: Long = 20L, x: Float, y: Float, z: Float) {
        val count = (durationMs / intervalMs).toInt()
        for (i in 0 until count) {
            collector.addSensorEventForTest(
                SensorEvent(
                    timestamp = startTime + i * intervalMs,
                    type = "gyroscope",
                    x = x, y = y, z = z
                )
            )
        }
    }

    /**
     * Inject accelerometer events during a specified time range (for idle period testing).
     */
    private fun injectAccelEvents(startTime: Long, durationMs: Long, intervalMs: Long = 20L, x: Float, y: Float, z: Float) {
        val count = (durationMs / intervalMs).toInt()
        for (i in 0 until count) {
            collector.addSensorEventForTest(
                SensorEvent(
                    timestamp = startTime + i * intervalMs,
                    type = "accelerometer",
                    x = x, y = y, z = z
                )
            )
        }
    }

    // =========================================================================
    // REQ-01: avgTapAccelSpike
    // Avg peak accel magnitude in 100ms window after touch DOWN minus
    // pre-tap baseline (avg magnitude in [-200ms, 0] window before tap).
    // Default 0 if <3 taps or no accel data.
    // =========================================================================

    @Test
    fun `REQ-01 - should compute non-zero avgTapAccelSpike when device held during taps`() {
        // Perform 3 taps with accel spike data around each
        val baselineMag = 9.8f
        val spikeMag = 15.0f

        repeat(3) {
            val tapTime = performTap()
            injectAccelAroundTap(tapTime, baselineMagnitude = baselineMag, spikeMagnitude = spikeMag)
            Thread.sleep(250) // gap between taps
        }

        collector.stopSession()
        val features = collector.extractFeatures()

        // The spike above baseline should be approximately (spikeMag - baselineMag) = 5.2
        assertTrue(
            "avgTapAccelSpike should be > 0 when there are accel spikes after taps",
            features.avgTapAccelSpike > 0.0
        )
    }

    @Test
    fun `REQ-01 - should return 0 when fewer than 3 taps`() {
        // Only 2 taps -- spec says default 0 if <3 taps
        repeat(2) {
            val tapTime = performTap()
            injectAccelAroundTap(tapTime, baselineMagnitude = 9.8f, spikeMagnitude = 15.0f)
            Thread.sleep(250)
        }

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "avgTapAccelSpike should be 0 when <3 taps",
            0.0,
            features.avgTapAccelSpike,
            DELTA
        )
    }

    @Test
    fun `REQ-01 - should return 0 when no accelerometer data`() {
        // 5 taps but no accel events injected
        repeat(5) {
            performTap()
            Thread.sleep(50)
        }

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "avgTapAccelSpike should be 0 when no accel data available",
            0.0,
            features.avgTapAccelSpike,
            DELTA
        )
    }

    @Test
    fun `REQ-01 - should return 0 when no taps at all`() {
        // No taps, no touch events
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "avgTapAccelSpike should be 0 with no taps at all",
            0.0,
            features.avgTapAccelSpike,
            DELTA
        )
    }

    @Test
    fun `REQ-01 - spike should reflect difference between post-tap and pre-tap magnitudes`() {
        // Use known values: baseline ~10, spike ~20, so delta ~10
        val baselineMag = 10.0f
        val spikeMag = 20.0f

        repeat(4) {
            val tapTime = performTap()
            injectAccelAroundTap(tapTime, baselineMagnitude = baselineMag, spikeMagnitude = spikeMag)
            Thread.sleep(250)
        }

        collector.stopSession()
        val features = collector.extractFeatures()

        // Delta should be around 10 (spike - baseline)
        // Use relaxed tolerance due to timing imprecision
        assertTrue(
            "avgTapAccelSpike should approximate (spike - baseline) magnitude, got ${features.avgTapAccelSpike}",
            features.avgTapAccelSpike > 3.0 // conservative: at least meaningfully above 0
        )
    }

    @Test
    fun `REQ-01 - exactly 3 taps should be sufficient`() {
        // Boundary: exactly 3 taps (minimum allowed count)
        repeat(3) {
            val tapTime = performTap()
            injectAccelAroundTap(tapTime, baselineMagnitude = 9.8f, spikeMagnitude = 14.0f)
            Thread.sleep(250)
        }

        collector.stopSession()
        val features = collector.extractFeatures()

        assertTrue(
            "avgTapAccelSpike should be computed with exactly 3 taps",
            features.avgTapAccelSpike > 0.0
        )
    }

    // =========================================================================
    // REQ-02: avgTapRecoveryMs
    // Avg time (ms) for accel magnitude to return to baseline after tap.
    // Default 0 if <3 taps or no accel data.
    // =========================================================================

    @Test
    fun `REQ-02 - should compute non-zero recovery time when accel returns to baseline`() {
        // Inject accel data that recovers over ~200ms after each tap
        repeat(3) {
            val tapTime = performTap()
            injectAccelWithRecovery(
                tapTime,
                spikeMagnitude = 15.0f,
                baselineMagnitude = 9.8f,
                recoveryMs = 200L
            )
            Thread.sleep(350) // enough gap for next tap
        }

        collector.stopSession()
        val features = collector.extractFeatures()

        assertTrue(
            "avgTapRecoveryMs should be > 0 when accel recovers after taps, got ${features.avgTapRecoveryMs}",
            features.avgTapRecoveryMs > 0.0
        )
    }

    @Test
    fun `REQ-02 - should return 0 when fewer than 3 taps`() {
        repeat(2) {
            val tapTime = performTap()
            injectAccelWithRecovery(tapTime, spikeMagnitude = 15.0f, baselineMagnitude = 9.8f, recoveryMs = 200L)
            Thread.sleep(350)
        }

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "avgTapRecoveryMs should be 0 when <3 taps",
            0.0,
            features.avgTapRecoveryMs,
            DELTA
        )
    }

    @Test
    fun `REQ-02 - should return 0 when no accelerometer data`() {
        repeat(4) {
            performTap()
            Thread.sleep(100)
        }

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "avgTapRecoveryMs should be 0 when no accel data",
            0.0,
            features.avgTapRecoveryMs,
            DELTA
        )
    }

    @Test
    fun `REQ-02 - should return 0 when no taps at all`() {
        // Inject some accel data but no taps
        val now = System.currentTimeMillis()
        injectAccelEvents(now, durationMs = 1000L, x = 0f, y = 0f, z = 9.8f)

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "avgTapRecoveryMs should be 0 when no taps at all",
            0.0,
            features.avgTapRecoveryMs,
            DELTA
        )
    }

    @Test
    fun `REQ-02 - faster recovery should yield smaller avgTapRecoveryMs than slower`() {
        // Fast recovery session
        val fastCollector = BehavioralCollector(mockContext)
        fastCollector.startSession()
        repeat(3) {
            val now = System.currentTimeMillis()
            fastCollector.onTouchEvent(ACTION_DOWN, 540f, 1170f, 0.5f, 10f, now, now)
            Thread.sleep(30)
            val upTime = System.currentTimeMillis()
            fastCollector.onTouchEvent(ACTION_UP, 540f, 1170f, 0.5f, 10f, now, upTime)
            injectAccelWithRecoveryOnCollector(fastCollector, now, recoveryMs = 50L)
            Thread.sleep(200)
        }
        fastCollector.stopSession()
        val fastFeatures = fastCollector.extractFeatures()

        // Slow recovery session
        val slowCollector = BehavioralCollector(mockContext)
        slowCollector.startSession()
        repeat(3) {
            val now = System.currentTimeMillis()
            slowCollector.onTouchEvent(ACTION_DOWN, 540f, 1170f, 0.5f, 10f, now, now)
            Thread.sleep(30)
            val upTime = System.currentTimeMillis()
            slowCollector.onTouchEvent(ACTION_UP, 540f, 1170f, 0.5f, 10f, now, upTime)
            injectAccelWithRecoveryOnCollector(slowCollector, now, recoveryMs = 400L)
            Thread.sleep(550)
        }
        slowCollector.stopSession()
        val slowFeatures = slowCollector.extractFeatures()

        assertTrue(
            "Fast recovery (${fastFeatures.avgTapRecoveryMs}ms) should be less than slow recovery (${slowFeatures.avgTapRecoveryMs}ms)",
            fastFeatures.avgTapRecoveryMs < slowFeatures.avgTapRecoveryMs
        )
    }

    /** Helper for injecting recovery data into a specific collector instance. */
    private fun injectAccelWithRecoveryOnCollector(
        target: BehavioralCollector,
        tapTime: Long,
        spikeMagnitude: Float = 15.0f,
        baselineMagnitude: Float = 9.8f,
        recoveryMs: Long = 200L
    ) {
        // Pre-tap baseline
        for (i in 10 downTo 1) {
            target.addSensorEventForTest(
                SensorEvent(timestamp = tapTime - i * 20L, type = "accelerometer", x = 0f, y = 0f, z = baselineMagnitude)
            )
        }
        // Post-tap: spike then gradual return
        val steps = (recoveryMs / 20L).toInt()
        for (i in 0..steps) {
            val ts = tapTime + i * 20L
            val fraction = i.toFloat() / steps
            val mag = spikeMagnitude + (baselineMagnitude - spikeMagnitude) * fraction
            target.addSensorEventForTest(
                SensorEvent(timestamp = ts, type = "accelerometer", x = 0f, y = 0f, z = mag)
            )
        }
    }

    // =========================================================================
    // REQ-03: idleGyroRMS
    // RMS of gyroscope magnitude during idle periods (no touch for >500ms).
    // Measures hand tremor. Default 0 if no idle periods.
    // =========================================================================

    @Test
    fun `REQ-03 - should compute non-zero idleGyroRMS during idle period with tremor`() {
        // Tap to establish a reference point, then wait >500ms (idle period)
        val tapTime = performTap()

        // Wait 600ms to create an idle period (>500ms threshold)
        Thread.sleep(600)

        // Inject gyro events during the idle period with non-zero values (tremor)
        val idleStart = tapTime + 550 // well into idle period
        injectGyroEvents(idleStart, durationMs = 300L, x = 0.02f, y = 0.03f, z = 0.01f)

        collector.stopSession()
        val features = collector.extractFeatures()

        assertTrue(
            "idleGyroRMS should be > 0 when gyro data present during idle period, got ${features.idleGyroRMS}",
            features.idleGyroRMS > 0.0
        )
    }

    @Test
    fun `REQ-03 - should return 0 when no idle periods exist`() {
        // Continuous tapping with <500ms between taps -- no idle period
        repeat(10) {
            performTap()
            Thread.sleep(100) // only 100ms gap, well below 500ms idle threshold
        }

        // Inject some gyro data in the gaps
        val now = System.currentTimeMillis()
        injectGyroEvents(now - 500, durationMs = 500L, x = 0.05f, y = 0.05f, z = 0.05f)

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "idleGyroRMS should be 0 when no idle periods (all gaps < 500ms)",
            0.0,
            features.idleGyroRMS,
            DELTA
        )
    }

    @Test
    fun `REQ-03 - should return 0 when no gyro data during idle periods`() {
        // Create an idle period but inject no gyroscope events
        performTap()
        Thread.sleep(600) // idle period created

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "idleGyroRMS should be 0 when no gyro data during idle",
            0.0,
            features.idleGyroRMS,
            DELTA
        )
    }

    @Test
    fun `REQ-03 - should return 0 when no touch events and no gyro data`() {
        // No touch events and no gyro data → nothing to compute → 0.0
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "idleGyroRMS should be 0 with no events at all",
            0.0,
            features.idleGyroRMS,
            DELTA
        )
    }

    @Test
    fun `REQ-03 - RMS should reflect gyro magnitude during tremor`() {
        // Two scenarios: low tremor vs high tremor
        // Low tremor collector
        val lowCollector = BehavioralCollector(mockContext)
        lowCollector.startSession()
        val lowNow = System.currentTimeMillis()
        lowCollector.onTouchEvent(ACTION_DOWN, 540f, 1170f, 0.5f, 10f, lowNow, lowNow)
        Thread.sleep(30)
        lowCollector.onTouchEvent(ACTION_UP, 540f, 1170f, 0.5f, 10f, lowNow, lowNow + 30)
        Thread.sleep(600) // idle period
        val lowIdleStart = lowNow + 550
        // Low tremor: very small gyro values
        for (i in 0 until 15) {
            lowCollector.addSensorEventForTest(
                SensorEvent(timestamp = lowIdleStart + i * 20L, type = "gyroscope", x = 0.001f, y = 0.001f, z = 0.001f)
            )
        }
        lowCollector.stopSession()
        val lowFeatures = lowCollector.extractFeatures()

        // High tremor collector
        val highCollector = BehavioralCollector(mockContext)
        highCollector.startSession()
        val highNow = System.currentTimeMillis()
        highCollector.onTouchEvent(ACTION_DOWN, 540f, 1170f, 0.5f, 10f, highNow, highNow)
        Thread.sleep(30)
        highCollector.onTouchEvent(ACTION_UP, 540f, 1170f, 0.5f, 10f, highNow, highNow + 30)
        Thread.sleep(600) // idle period
        val highIdleStart = highNow + 550
        // High tremor: large gyro values
        for (i in 0 until 15) {
            highCollector.addSensorEventForTest(
                SensorEvent(timestamp = highIdleStart + i * 20L, type = "gyroscope", x = 0.5f, y = 0.5f, z = 0.5f)
            )
        }
        highCollector.stopSession()
        val highFeatures = highCollector.extractFeatures()

        assertTrue(
            "High tremor idleGyroRMS (${highFeatures.idleGyroRMS}) should exceed low tremor (${lowFeatures.idleGyroRMS})",
            highFeatures.idleGyroRMS > lowFeatures.idleGyroRMS
        )
    }

    // =========================================================================
    // REQ-04: idleAccelJitter
    // High-frequency accel variation during idle periods.
    // Default 0 if no idle periods.
    // =========================================================================

    @Test
    fun `REQ-04 - should compute non-zero idleAccelJitter during idle with variable accel`() {
        performTap()
        Thread.sleep(600) // create idle period

        // Inject high-frequency varying accel data during idle
        val idleStart = System.currentTimeMillis() - 50
        for (i in 0 until 20) {
            // Alternate between two values to create jitter
            val z = if (i % 2 == 0) 10.5f else 9.0f
            collector.addSensorEventForTest(
                SensorEvent(timestamp = idleStart + i * 20L, type = "accelerometer", x = 0f, y = 0f, z = z)
            )
        }

        collector.stopSession()
        val features = collector.extractFeatures()

        assertTrue(
            "idleAccelJitter should be > 0 with variable accel during idle, got ${features.idleAccelJitter}",
            features.idleAccelJitter > 0.0
        )
    }

    @Test
    fun `REQ-04 - should return 0 when no idle periods`() {
        // Continuous tapping with <500ms gaps
        repeat(10) {
            performTap()
            Thread.sleep(100) // no idle period
        }

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "idleAccelJitter should be 0 when no idle periods",
            0.0,
            features.idleAccelJitter,
            DELTA
        )
    }

    @Test
    fun `REQ-04 - should return 0 when no accel data during idle`() {
        performTap()
        Thread.sleep(600) // idle period but no accel data injected

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "idleAccelJitter should be 0 when no accel data during idle",
            0.0,
            features.idleAccelJitter,
            DELTA
        )
    }

    @Test
    fun `REQ-04 - constant accel during idle should yield near-zero jitter`() {
        performTap()
        Thread.sleep(600)

        // Inject perfectly constant accel data during idle
        val idleStart = System.currentTimeMillis() - 50
        for (i in 0 until 20) {
            collector.addSensorEventForTest(
                SensorEvent(timestamp = idleStart + i * 20L, type = "accelerometer", x = 0f, y = 0f, z = 9.81f)
            )
        }

        collector.stopSession()
        val features = collector.extractFeatures()

        // With perfectly constant data, jitter should be 0 or near-zero
        assertTrue(
            "idleAccelJitter should be near 0 with constant accel, got ${features.idleAccelJitter}",
            features.idleAccelJitter < 0.1
        )
    }

    @Test
    fun `REQ-04 - high variation should yield higher jitter than low variation`() {
        // Low variation session
        val lowCollector = BehavioralCollector(mockContext)
        lowCollector.startSession()
        val lowNow = System.currentTimeMillis()
        lowCollector.onTouchEvent(ACTION_DOWN, 540f, 1170f, 0.5f, 10f, lowNow, lowNow)
        Thread.sleep(30)
        lowCollector.onTouchEvent(ACTION_UP, 540f, 1170f, 0.5f, 10f, lowNow, lowNow + 30)
        Thread.sleep(600)
        val lowIdleStart = lowNow + 550
        for (i in 0 until 20) {
            val z = 9.81f + (if (i % 2 == 0) 0.01f else -0.01f) // tiny variation
            lowCollector.addSensorEventForTest(
                SensorEvent(timestamp = lowIdleStart + i * 20L, type = "accelerometer", x = 0f, y = 0f, z = z)
            )
        }
        lowCollector.stopSession()
        val lowFeatures = lowCollector.extractFeatures()

        // High variation session
        val highCollector = BehavioralCollector(mockContext)
        highCollector.startSession()
        val highNow = System.currentTimeMillis()
        highCollector.onTouchEvent(ACTION_DOWN, 540f, 1170f, 0.5f, 10f, highNow, highNow)
        Thread.sleep(30)
        highCollector.onTouchEvent(ACTION_UP, 540f, 1170f, 0.5f, 10f, highNow, highNow + 30)
        Thread.sleep(600)
        val highIdleStart = highNow + 550
        for (i in 0 until 20) {
            val z = 9.81f + (if (i % 2 == 0) 3.0f else -3.0f) // large variation
            highCollector.addSensorEventForTest(
                SensorEvent(timestamp = highIdleStart + i * 20L, type = "accelerometer", x = 0f, y = 0f, z = z)
            )
        }
        highCollector.stopSession()
        val highFeatures = highCollector.extractFeatures()

        assertTrue(
            "High variation jitter (${highFeatures.idleAccelJitter}) should exceed low variation (${lowFeatures.idleAccelJitter})",
            highFeatures.idleAccelJitter > lowFeatures.idleAccelJitter
        )
    }

    // =========================================================================
    // REQ-05: correctionSameCount
    // Count of deletion(1-2 chars) immediately followed by insertion(1-2 chars)
    // within 1000ms. Typo fix pattern.
    // Detected from lengthDelta sequence: -1/-2 then +1/+2 within 1000ms.
    // Default 0.
    // =========================================================================

    @Test
    fun `REQ-05 - should detect single typo fix pattern (delete 1, insert 1)`() {
        // Type some chars, then delete 1 (prevLen=5, newLen=4), then insert 1 (prevLen=4, newLen=5) within 1000ms
        feedTextEventsWithDelays(listOf(
            TextEventSpec("accountNumber", 0, 1),
            TextEventSpec("accountNumber", 1, 2),
            TextEventSpec("accountNumber", 2, 3),
            TextEventSpec("accountNumber", 3, 4),
            TextEventSpec("accountNumber", 4, 5),
            // Typo fix: delete 1 then insert 1, within 1000ms
            TextEventSpec("accountNumber", 5, 4, delayBeforeMs = 100),  // deletion of 1
            TextEventSpec("accountNumber", 4, 5, delayBeforeMs = 200),  // insertion of 1
        ))

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "correctionSameCount should be 1 for single delete-1/insert-1 pair",
            1,
            features.correctionSameCount
        )
    }

    @Test
    fun `REQ-05 - should detect delete-2 then insert-2 as typo fix`() {
        feedTextEventsWithDelays(listOf(
            TextEventSpec("accountNumber", 0, 5),
            // Delete 2, then insert 2 within 1000ms
            TextEventSpec("accountNumber", 5, 3, delayBeforeMs = 100),  // deletion of 2
            TextEventSpec("accountNumber", 3, 5, delayBeforeMs = 200),  // insertion of 2
        ))

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "correctionSameCount should be 1 for delete-2/insert-2 pair",
            1,
            features.correctionSameCount
        )
    }

    @Test
    fun `REQ-05 - should count multiple typo fixes independently`() {
        feedTextEventsWithDelays(listOf(
            TextEventSpec("accountNumber", 0, 5),
            // First typo fix
            TextEventSpec("accountNumber", 5, 4, delayBeforeMs = 100),   // -1
            TextEventSpec("accountNumber", 4, 5, delayBeforeMs = 200),   // +1
            // Second typo fix
            TextEventSpec("accountNumber", 5, 3, delayBeforeMs = 500),   // -2
            TextEventSpec("accountNumber", 3, 5, delayBeforeMs = 200),   // +2
        ))

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "correctionSameCount should be 2 for two separate typo fix patterns",
            2,
            features.correctionSameCount
        )
    }

    @Test
    fun `REQ-05 - should NOT count correction when time between delete and insert exceeds 1000ms`() {
        feedTextEventsWithDelays(listOf(
            TextEventSpec("accountNumber", 0, 5),
            // Delete 1, then wait > 1000ms, then insert 1 -- should NOT count
            TextEventSpec("accountNumber", 5, 4, delayBeforeMs = 100),   // -1
            TextEventSpec("accountNumber", 4, 5, delayBeforeMs = 1200),  // +1, but >1000ms gap
        ))

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "correctionSameCount should be 0 when gap exceeds 1000ms",
            0,
            features.correctionSameCount
        )
    }

    @Test
    fun `REQ-05 - should NOT count deletion of 3 or more chars as same-correction`() {
        // Delete 3 then insert 2 -- deletion too large for same-correction (REQ-05 is 1-2 only)
        feedTextEventsWithDelays(listOf(
            TextEventSpec("accountNumber", 0, 6),
            TextEventSpec("accountNumber", 6, 3, delayBeforeMs = 100),  // -3 (too many for REQ-05)
            TextEventSpec("accountNumber", 3, 5, delayBeforeMs = 200),  // +2
        ))

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "correctionSameCount should be 0 when deletion is >=3 chars",
            0,
            features.correctionSameCount
        )
    }

    @Test
    fun `REQ-05 - should return 0 when no text events`() {
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "correctionSameCount should be 0 with no text events",
            0,
            features.correctionSameCount
        )
    }

    @Test
    fun `REQ-05 - should return 0 when only insertions no deletions`() {
        feedTextEventsWithDelays(listOf(
            TextEventSpec("accountNumber", 0, 1),
            TextEventSpec("accountNumber", 1, 2),
            TextEventSpec("accountNumber", 2, 3),
            TextEventSpec("accountNumber", 3, 4),
            TextEventSpec("accountNumber", 4, 5),
        ))

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "correctionSameCount should be 0 with only insertions",
            0,
            features.correctionSameCount
        )
    }

    @Test
    fun `REQ-05 - boundary deletion of exactly 1 followed by insertion of exactly 1 within 1000ms`() {
        feedTextEventsWithDelays(listOf(
            TextEventSpec("accountNumber", 0, 3),
            TextEventSpec("accountNumber", 3, 2, delayBeforeMs = 50),   // -1 (boundary)
            TextEventSpec("accountNumber", 2, 3, delayBeforeMs = 950),  // +1, just under 1000ms total
        ))

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "correctionSameCount should be 1 at boundary (exactly 1-char delete, 1-char insert, ~1000ms)",
            1,
            features.correctionSameCount
        )
    }

    // =========================================================================
    // REQ-06: correctionDifferentCount
    // Count of deletion(>=3 chars) followed by pause(>500ms) then
    // insertion(>=3 chars). Content change pattern. Default 0.
    // =========================================================================

    @Test
    fun `REQ-06 - should detect content change pattern (delete 3, pause 500ms, insert 3)`() {
        feedTextEventsWithDelays(listOf(
            TextEventSpec("note", 0, 10),
            // Delete 3, wait >500ms, insert 3
            TextEventSpec("note", 10, 7, delayBeforeMs = 100),  // -3
            TextEventSpec("note", 7, 10, delayBeforeMs = 600),  // +3, after 600ms pause
        ))

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "correctionDifferentCount should be 1 for delete-3/pause-500ms/insert-3",
            1,
            features.correctionDifferentCount
        )
    }

    @Test
    fun `REQ-06 - should detect with larger deletions and insertions`() {
        feedTextEventsWithDelays(listOf(
            TextEventSpec("note", 0, 20),
            // Delete 5, pause >500ms, insert 8
            TextEventSpec("note", 20, 15, delayBeforeMs = 100),  // -5
            TextEventSpec("note", 15, 23, delayBeforeMs = 700),  // +8, after 700ms
        ))

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "correctionDifferentCount should be 1 for large content change",
            1,
            features.correctionDifferentCount
        )
    }

    @Test
    fun `REQ-06 - should count multiple content changes`() {
        feedTextEventsWithDelays(listOf(
            TextEventSpec("note", 0, 15),
            // First content change
            TextEventSpec("note", 15, 12, delayBeforeMs = 100),  // -3
            TextEventSpec("note", 12, 16, delayBeforeMs = 600),  // +4
            // Second content change
            TextEventSpec("note", 16, 10, delayBeforeMs = 200),  // -6
            TextEventSpec("note", 10, 18, delayBeforeMs = 800),  // +8
        ))

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "correctionDifferentCount should be 2 for two content change patterns",
            2,
            features.correctionDifferentCount
        )
    }

    @Test
    fun `REQ-06 - should NOT count when pause is 500ms or less`() {
        feedTextEventsWithDelays(listOf(
            TextEventSpec("note", 0, 10),
            // Delete 3, pause exactly 500ms or less, insert 3 -- should NOT count
            TextEventSpec("note", 10, 7, delayBeforeMs = 100),  // -3
            TextEventSpec("note", 7, 10, delayBeforeMs = 400),  // +3, only 400ms pause (<=500)
        ))

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "correctionDifferentCount should be 0 when pause <= 500ms",
            0,
            features.correctionDifferentCount
        )
    }

    @Test
    fun `REQ-06 - should NOT count when deletion is less than 3 chars`() {
        // Delete 2 (too few for content change) then pause then insert 5
        feedTextEventsWithDelays(listOf(
            TextEventSpec("note", 0, 10),
            TextEventSpec("note", 10, 8, delayBeforeMs = 100),  // -2 (too few)
            TextEventSpec("note", 8, 13, delayBeforeMs = 700),  // +5
        ))

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "correctionDifferentCount should be 0 when deletion < 3 chars",
            0,
            features.correctionDifferentCount
        )
    }

    @Test
    fun `REQ-06 - should NOT count when insertion is less than 3 chars`() {
        // Delete 5 then pause then insert 2 (too few for content change)
        feedTextEventsWithDelays(listOf(
            TextEventSpec("note", 0, 10),
            TextEventSpec("note", 10, 5, delayBeforeMs = 100),  // -5
            TextEventSpec("note", 5, 7, delayBeforeMs = 700),   // +2 (too few)
        ))

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "correctionDifferentCount should be 0 when insertion < 3 chars",
            0,
            features.correctionDifferentCount
        )
    }

    @Test
    fun `REQ-06 - should return 0 when no text events`() {
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "correctionDifferentCount should be 0 with no text events",
            0,
            features.correctionDifferentCount
        )
    }

    @Test
    fun `REQ-06 - boundary exactly 3 chars deleted and 3 chars inserted with pause just over 500ms`() {
        feedTextEventsWithDelays(listOf(
            TextEventSpec("note", 0, 10),
            TextEventSpec("note", 10, 7, delayBeforeMs = 100),  // exactly -3 (boundary)
            TextEventSpec("note", 7, 10, delayBeforeMs = 550),  // exactly +3, 550ms pause (just >500)
        ))

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "correctionDifferentCount should be 1 at boundary (exactly 3 deleted, 3 inserted, >500ms)",
            1,
            features.correctionDifferentCount
        )
    }

    // =========================================================================
    // REQ-07: screenshotDuringInput
    // Screenshot captured while session is active. Default false.
    // Test the flag mechanism only.
    // =========================================================================

    @Test
    fun `REQ-07 - should default to false when no screenshot detected`() {
        // Normal session with some activity, no screenshot flagged
        collector.onTextChanged("accountNumber", 0, 1)
        collector.stopSession()

        val features = collector.extractFeatures()

        assertFalse(
            "screenshotDuringInput should default to false",
            features.screenshotDuringInput
        )
    }

    @Test
    fun `REQ-07 - should default to false for empty session`() {
        collector.stopSession()
        val features = collector.extractFeatures()

        assertFalse(
            "screenshotDuringInput should be false for empty session",
            features.screenshotDuringInput
        )
    }

    @Test
    fun `REQ-07 - should report true when screenshot flag is set during active session`() {
        // Start collecting, flag a screenshot, then extract
        collector.onTextChanged("accountNumber", 0, 1)
        collector.setScreenshotDetectedForTest(true)
        collector.stopSession()

        val features = collector.extractFeatures()

        assertTrue(
            "screenshotDuringInput should be true when flagged during session",
            features.screenshotDuringInput
        )
    }

    @Test
    fun `REQ-07 - flag should persist even if more activity happens after screenshot`() {
        collector.onTextChanged("accountNumber", 0, 1)
        collector.setScreenshotDetectedForTest(true)

        // Continue adding more events
        collector.onTextChanged("accountNumber", 1, 2)
        collector.onTextChanged("amount", 0, 5)
        performTap()

        collector.stopSession()
        val features = collector.extractFeatures()

        assertTrue(
            "screenshotDuringInput flag should persist after additional activity",
            features.screenshotDuringInput
        )
    }

    @Test
    fun `REQ-07 - new session should reset screenshot flag`() {
        // First session: flag screenshot
        collector.onTextChanged("accountNumber", 0, 1)
        collector.setScreenshotDetectedForTest(true)
        collector.stopSession()
        val firstFeatures = collector.extractFeatures()
        assertTrue("First session should have screenshot flag", firstFeatures.screenshotDuringInput)

        // Second session: no screenshot
        collector.startSession()
        collector.onTextChanged("accountNumber", 0, 1)
        collector.stopSession()
        val secondFeatures = collector.extractFeatures()

        assertFalse(
            "New session should reset screenshotDuringInput to false",
            secondFeatures.screenshotDuringInput
        )
    }

    // =========================================================================
    // Combined / Integration edge cases
    // =========================================================================

    @Test
    fun `all Phase 3 features should have correct defaults for empty session`() {
        // No events at all
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals("avgTapAccelSpike default", 0.0, features.avgTapAccelSpike, DELTA)
        assertEquals("avgTapRecoveryMs default", 0.0, features.avgTapRecoveryMs, DELTA)
        assertEquals("idleGyroRMS default", 0.0, features.idleGyroRMS, DELTA)
        assertEquals("idleAccelJitter default", 0.0, features.idleAccelJitter, DELTA)
        assertEquals("correctionSameCount default", 0, features.correctionSameCount)
        assertEquals("correctionDifferentCount default", 0, features.correctionDifferentCount)
        assertFalse("screenshotDuringInput default", features.screenshotDuringInput)
    }

    @Test
    fun `REQ-05 and REQ-06 should be independent and not double-count`() {
        // Mix of same-correction (REQ-05) and different-correction (REQ-06)
        feedTextEventsWithDelays(listOf(
            TextEventSpec("accountNumber", 0, 10),
            // Same-correction (REQ-05): delete 1, insert 1 within 1000ms
            TextEventSpec("accountNumber", 10, 9, delayBeforeMs = 100),  // -1
            TextEventSpec("accountNumber", 9, 10, delayBeforeMs = 200),  // +1
            // Different-correction (REQ-06): delete 4, pause >500ms, insert 5
            TextEventSpec("note", 0, 15, delayBeforeMs = 300),
            TextEventSpec("note", 15, 11, delayBeforeMs = 100),  // -4
            TextEventSpec("note", 11, 16, delayBeforeMs = 600),  // +5 after 600ms pause
        ))

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "correctionSameCount should only count the typo fix",
            1,
            features.correctionSameCount
        )
        assertEquals(
            "correctionDifferentCount should only count the content change",
            1,
            features.correctionDifferentCount
        )
    }
}
