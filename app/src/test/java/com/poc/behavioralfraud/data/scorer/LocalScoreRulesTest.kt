package com.poc.behavioralfraud.data.scorer

import com.poc.behavioralfraud.data.model.BehavioralFeatures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TASK-030 / FR-CL-15 — LocalScoreRules unit tests.
 *
 * Mirrors backend `tests/test_risk_rule_engine.py` (BE TASK-015) so weights,
 * thresholds, and reasons stay consistent across online/offline paths.
 */
class LocalScoreRulesTest {

    /** Build BehavioralFeatures with sane neutral defaults; pass overrides via copy. */
    private fun neutralFeatures(): BehavioralFeatures = BehavioralFeatures(
        sessionDurationMs = 30_000L,
        avgInterCharDelayMs = 120.0,
        stdInterCharDelayMs = 30.0,
        maxInterCharDelayMs = 500L,
        minInterCharDelayMs = 50L,
        totalTextChanges = 20,
        pasteCount = 0,
        totalTouchEvents = 50,
        avgTouchSize = 50.0,
        avgTouchDurationMs = 100.0,
        avgSwipeVelocity = 1000.0,
        gyroStabilityX = 0.01,
        gyroStabilityY = 0.01,
        gyroStabilityZ = 0.01,
        accelStabilityX = 0.2,
        accelStabilityY = 0.2,
        accelStabilityZ = 0.2,
        avgTouchPressure = 0.5,
        perFieldAvgDelay = emptyMap(),
        avgInterFieldPauseMs = 800.0,
        deletionCount = 1,
        deletionRatio = 0.05,
        fieldFocusSequence = "account>amount",
        timeToFirstInput = 2000L,
        timeFromLastInputToConfirm = 3000L,
        // Time + light: not in dark anomaly range
        sessionHourOfDay = 14,  // afternoon
        lightAvgLux = 300.0,    // lit
        // FR-CL-13 defaults are 0.0 — but bot-precision and synthetic-velocity
        // rules trigger on `< threshold` so 0 < threshold fires unwanted rules.
        // Override to safely above-threshold values for a "neutral" baseline.
        avgTapPrecisionOffsetPx = 10.0,    // > TAP_PRECISION_OFFSET_MAX_PX (2.0)
        tapPrecisionStdDev = 5.0,          // > TAP_PRECISION_STD_MAX_PX (1.0)
        interTapVelocityStdDev = 0.5,      // > VELOCITY_STD_MAX (0.005)
        // mockLocationDetected, otpPasted default false → correct.
    )

    // ── Per-rule fire / silent ─────────────────────────────────────

    @Test
    fun `gps spoofing rule fires when mockLocationDetected true`() {
        val features = neutralFeatures().copy(mockLocationDetected = true)
        val result = LocalScoreRules.evaluate(features)
        assertEquals(LocalScoreRules.WEIGHT_GPS_SPOOFING, result.score)
        assertTrue(LocalScoreRules.REASON_GPS_SPOOFING in result.reasons)
    }

    @Test
    fun `gps spoofing rule silent when mockLocationDetected false`() {
        val result = LocalScoreRules.evaluate(neutralFeatures())
        assertFalse(LocalScoreRules.REASON_GPS_SPOOFING in result.reasons)
    }

    @Test
    fun `bot tap precision rule fires when offset and std below thresholds and enough touches`() {
        val features = neutralFeatures().copy(
            avgTapPrecisionOffsetPx = 0.5,
            tapPrecisionStdDev = 0.3,
            totalTouchEvents = 10,
        )
        val result = LocalScoreRules.evaluate(features)
        assertEquals(LocalScoreRules.WEIGHT_BOT_TAP_PRECISION, result.score)
        assertTrue(LocalScoreRules.REASON_BOT_TAP_PRECISION in result.reasons)
    }

    @Test
    fun `bot tap precision rule silent when offset above threshold`() {
        val features = neutralFeatures().copy(
            avgTapPrecisionOffsetPx = 5.0,
            tapPrecisionStdDev = 0.3,
            totalTouchEvents = 10,
        )
        val result = LocalScoreRules.evaluate(features)
        assertEquals(0, result.score)
    }

    @Test
    fun `bot tap precision rule silent when too few touches`() {
        val features = neutralFeatures().copy(
            avgTapPrecisionOffsetPx = 0.5,
            tapPrecisionStdDev = 0.3,
            totalTouchEvents = 3,
        )
        val result = LocalScoreRules.evaluate(features)
        assertEquals(0, result.score)
    }

    @Test
    fun `synthetic velocity rule fires when std below threshold`() {
        val features = neutralFeatures().copy(
            interTapVelocityStdDev = 0.001,
            totalTouchEvents = 10,
        )
        val result = LocalScoreRules.evaluate(features)
        assertEquals(LocalScoreRules.WEIGHT_SYNTHETIC_VELOCITY, result.score)
        assertTrue(LocalScoreRules.REASON_SYNTHETIC_VELOCITY in result.reasons)
    }

    @Test
    fun `synthetic velocity rule silent when std normal`() {
        val features = neutralFeatures().copy(
            interTapVelocityStdDev = 0.21,
            totalTouchEvents = 10,
        )
        val result = LocalScoreRules.evaluate(features)
        assertEquals(0, result.score)
    }

    @Test
    fun `otp paste violation rule fires when otpPasted true`() {
        val features = neutralFeatures().copy(otpPasted = true)
        val result = LocalScoreRules.evaluate(features)
        assertEquals(LocalScoreRules.WEIGHT_OTP_PASTE_VIOLATION, result.score)
        assertTrue(LocalScoreRules.REASON_OTP_PASTE_VIOLATION in result.reasons)
    }

    @Test
    fun `dark anomaly rule fires when low light and 3am`() {
        val features = neutralFeatures().copy(
            lightAvgLux = 2.0,
            sessionHourOfDay = 3,
        )
        val result = LocalScoreRules.evaluate(features)
        assertEquals(LocalScoreRules.WEIGHT_DARK_ANOMALY, result.score)
        assertTrue(LocalScoreRules.REASON_DARK_ANOMALY in result.reasons)
    }

    @Test
    fun `dark anomaly rule silent when daytime`() {
        val features = neutralFeatures().copy(
            lightAvgLux = 2.0,
            sessionHourOfDay = 14,
        )
        val result = LocalScoreRules.evaluate(features)
        assertEquals(0, result.score)
    }

    @Test
    fun `dark anomaly rule silent when lit at 3am`() {
        val features = neutralFeatures().copy(
            lightAvgLux = 300.0,
            sessionHourOfDay = 3,
        )
        val result = LocalScoreRules.evaluate(features)
        assertEquals(0, result.score)
    }

    // ── Boundary tests ─────────────────────────────────────────────

    @Test
    fun `tap precision offset at exact threshold does not fire`() {
        // offset == 2.0 px is `<` strict, so does NOT fire
        val features = neutralFeatures().copy(
            avgTapPrecisionOffsetPx = 2.0,
            tapPrecisionStdDev = 0.3,
            totalTouchEvents = 10,
        )
        val result = LocalScoreRules.evaluate(features)
        assertEquals(0, result.score)
    }

    @Test
    fun `tap precision offset just below threshold fires`() {
        val features = neutralFeatures().copy(
            avgTapPrecisionOffsetPx = 1.99,
            tapPrecisionStdDev = 0.3,
            totalTouchEvents = 10,
        )
        val result = LocalScoreRules.evaluate(features)
        assertEquals(LocalScoreRules.WEIGHT_BOT_TAP_PRECISION, result.score)
    }

    @Test
    fun `dark hour at max inclusive fires`() {
        val features = neutralFeatures().copy(
            lightAvgLux = 2.0,
            sessionHourOfDay = 5,
        )
        val result = LocalScoreRules.evaluate(features)
        assertEquals(LocalScoreRules.WEIGHT_DARK_ANOMALY, result.score)
    }

    @Test
    fun `dark hour just after max silent`() {
        val features = neutralFeatures().copy(
            lightAvgLux = 2.0,
            sessionHourOfDay = 6,
        )
        val result = LocalScoreRules.evaluate(features)
        assertEquals(0, result.score)
    }

    // ── Combined ───────────────────────────────────────────────────

    @Test
    fun `no rules fire when neutral features then zero`() {
        val result = LocalScoreRules.evaluate(neutralFeatures())
        assertEquals(0, result.score)
        assertTrue(result.reasons.isEmpty())
    }

    @Test
    fun `three rules fire then score sums`() {
        // GPS spoof + bot tap + dark = 30 + 25 + 15 = 70
        val features = neutralFeatures().copy(
            mockLocationDetected = true,
            avgTapPrecisionOffsetPx = 0.5,
            tapPrecisionStdDev = 0.3,
            totalTouchEvents = 10,
            lightAvgLux = 2.0,
            sessionHourOfDay = 3,
        )
        val result = LocalScoreRules.evaluate(features)
        val expected = LocalScoreRules.WEIGHT_GPS_SPOOFING +
            LocalScoreRules.WEIGHT_BOT_TAP_PRECISION +
            LocalScoreRules.WEIGHT_DARK_ANOMALY
        assertEquals(expected, result.score)
        assertEquals(3, result.reasons.size)
    }

    @Test
    fun `all five rules fire then score capped at 100`() {
        // 30 + 25 + 20 + 20 + 15 = 110, cap at SCORE_CAP = 100
        val features = neutralFeatures().copy(
            mockLocationDetected = true,
            avgTapPrecisionOffsetPx = 0.5,
            tapPrecisionStdDev = 0.3,
            totalTouchEvents = 10,
            interTapVelocityStdDev = 0.001,
            otpPasted = true,
            lightAvgLux = 2.0,
            sessionHourOfDay = 3,
        )
        val result = LocalScoreRules.evaluate(features)
        assertEquals(LocalScoreRules.SCORE_CAP, result.score)
        assertEquals(5, result.reasons.size)
    }

    @Test
    fun `reasons preserve evaluation order gps then bot then otp`() {
        val features = neutralFeatures().copy(
            mockLocationDetected = true,
            avgTapPrecisionOffsetPx = 0.5,
            tapPrecisionStdDev = 0.3,
            totalTouchEvents = 10,
            otpPasted = true,
        )
        val result = LocalScoreRules.evaluate(features)
        val gpsIdx = result.reasons.indexOf(LocalScoreRules.REASON_GPS_SPOOFING)
        val botIdx = result.reasons.indexOf(LocalScoreRules.REASON_BOT_TAP_PRECISION)
        val otpIdx = result.reasons.indexOf(LocalScoreRules.REASON_OTP_PASTE_VIOLATION)
        assertTrue("gps before bot", gpsIdx < botIdx)
        assertTrue("bot before otp", botIdx < otpIdx)
    }
}
