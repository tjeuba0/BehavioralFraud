package com.poc.behavioralfraud.data.collector

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.BatteryManager
import android.util.DisplayMetrics
import com.poc.behavioralfraud.data.model.BehavioralFeatures
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Specification-based tests for Phase 2: Cognitive & Intent Signal Collection (TASK-008).
 *
 * Tests verify the 17 features defined in FR-CL-06 of the SRS (REQ-01 through REQ-17).
 * Each REQ-ID has at minimum one happy-path test and one edge-case test.
 *
 * BehavioralCollector requires an Android Context (for SensorManager, AudioManager, etc.).
 * We mock Context plus all system services. Touch, text, and navigation events
 * are fed through the public API methods.
 *
 * Naming convention: `REQ-XX - description`
 */
class CognitiveIntentFeaturesTest {

    private lateinit var collector: BehavioralCollector
    private lateinit var mockContext: Context
    private lateinit var mockSensorManager: SensorManager
    private lateinit var mockAudioManager: AudioManager
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockSharedPreferencesEditor: SharedPreferences.Editor
    private lateinit var mockBatteryManager: BatteryManager
    private lateinit var mockResources: Resources
    private lateinit var mockDisplayMetrics: DisplayMetrics

    private companion object {
        const val DELTA = 0.01
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

        // Display metrics via context.resources.displayMetrics
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

        // SharedPreferences for timeSinceLastSessionMs (REQ-09)
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.putLong(any(), any()) } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.apply() } returns Unit

        // Default: no call active
        every { mockAudioManager.mode } returns AudioManager.MODE_NORMAL

        // Default battery state
        every { mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } returns 75
        every { mockBatteryManager.isCharging } returns false

        // Default: first session (no previous session timestamp)
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

    private fun feedTouchGesture(
        startX: Float, startY: Float,
        downTime: Long,
        durationMs: Long,
        pointerCount: Int = 1
    ) {
        collector.onTouchEvent(ACTION_DOWN, startX, startY, 0.5f, 10f, downTime, downTime, pointerCount)
        val upTime = downTime + durationMs
        collector.onTouchEvent(ACTION_UP, startX, startY, 0.5f, 10f, downTime, upTime, pointerCount)
    }

    // =========================================================================
    // REQ-01: isCallActiveDuringSession
    // =========================================================================

    @Test
    fun `REQ-01 - should detect active call during session when MODE_IN_CALL`() {
        every { mockAudioManager.mode } returns AudioManager.MODE_IN_CALL

        collector = BehavioralCollector(mockContext)
        collector.startSession()

        // Simulate some activity so a check point occurs
        collector.onTextChanged("accountNumber", 0, 1)
        Thread.sleep(50)
        collector.stopSession()

        val features = collector.extractFeatures()
        assertTrue(
            "isCallActiveDuringSession should be true when MODE_IN_CALL detected",
            features.isCallActiveDuringSession
        )
    }

    @Test
    fun `REQ-01 - should detect active call when MODE_IN_COMMUNICATION`() {
        every { mockAudioManager.mode } returns AudioManager.MODE_IN_COMMUNICATION

        collector = BehavioralCollector(mockContext)
        collector.startSession()
        collector.onTextChanged("accountNumber", 0, 1)
        Thread.sleep(50)
        collector.stopSession()

        val features = collector.extractFeatures()
        assertTrue(
            "isCallActiveDuringSession should be true when MODE_IN_COMMUNICATION detected",
            features.isCallActiveDuringSession
        )
    }

    @Test
    fun `REQ-01 - should be false when no call active during entire session`() {
        every { mockAudioManager.mode } returns AudioManager.MODE_NORMAL

        collector = BehavioralCollector(mockContext)
        collector.startSession()
        collector.onTextChanged("accountNumber", 0, 1)
        collector.stopSession()

        val features = collector.extractFeatures()
        assertFalse(
            "isCallActiveDuringSession should be false when no call detected",
            features.isCallActiveDuringSession
        )
    }

    @Test
    fun `REQ-01 - should be true if call detected at ANY check point even if not at others`() {
        // Start with no call, then switch to in-call mid-session
        every { mockAudioManager.mode } returnsMany listOf(
            AudioManager.MODE_NORMAL,  // first check
            AudioManager.MODE_IN_CALL, // second check
            AudioManager.MODE_NORMAL   // third check
        )

        collector = BehavioralCollector(mockContext)
        collector.startSession()
        collector.onTextChanged("accountNumber", 0, 1)
        Thread.sleep(50)
        collector.onTextChanged("accountNumber", 1, 2)
        Thread.sleep(50)
        collector.onTextChanged("accountNumber", 2, 3)
        collector.stopSession()

        val features = collector.extractFeatures()
        assertTrue(
            "isCallActiveDuringSession should be true if call active at ANY check point",
            features.isCallActiveDuringSession
        )
    }

    @Test
    fun `REQ-01 - should be false when AudioManager mode is MODE_RINGTONE`() {
        // Ringing but not answered should not count as active call
        every { mockAudioManager.mode } returns AudioManager.MODE_RINGTONE

        collector = BehavioralCollector(mockContext)
        collector.startSession()
        collector.onTextChanged("accountNumber", 0, 1)
        collector.stopSession()

        val features = collector.extractFeatures()
        assertFalse(
            "isCallActiveDuringSession should be false for MODE_RINGTONE (ringing, not active)",
            features.isCallActiveDuringSession
        )
    }

    // =========================================================================
    // REQ-02: callStartedDuringSession
    // =========================================================================

    @Test
    fun `REQ-02 - should be true when no call at start but call detected later`() {
        // No call at session start, call starts mid-session
        every { mockAudioManager.mode } returnsMany listOf(
            AudioManager.MODE_NORMAL,   // at session start
            AudioManager.MODE_IN_CALL   // at later check point
        )

        collector = BehavioralCollector(mockContext)
        collector.startSession()
        Thread.sleep(50)
        collector.onTextChanged("accountNumber", 0, 1)
        collector.stopSession()

        val features = collector.extractFeatures()
        assertTrue(
            "callStartedDuringSession should be true when call starts after session begins",
            features.callStartedDuringSession
        )
    }

    @Test
    fun `REQ-02 - should be false when call active from the beginning`() {
        // Call already active at session start
        every { mockAudioManager.mode } returns AudioManager.MODE_IN_CALL

        collector = BehavioralCollector(mockContext)
        collector.startSession()
        collector.onTextChanged("accountNumber", 0, 1)
        collector.stopSession()

        val features = collector.extractFeatures()
        assertFalse(
            "callStartedDuringSession should be false when call was active from session start",
            features.callStartedDuringSession
        )
    }

    @Test
    fun `REQ-02 - should be false when no call at any point`() {
        every { mockAudioManager.mode } returns AudioManager.MODE_NORMAL

        collector = BehavioralCollector(mockContext)
        collector.startSession()
        collector.onTextChanged("accountNumber", 0, 1)
        collector.stopSession()

        val features = collector.extractFeatures()
        assertFalse(
            "callStartedDuringSession should be false when no call detected at any point",
            features.callStartedDuringSession
        )
    }

    // =========================================================================
    // REQ-03: backgroundSwitchCount
    // =========================================================================

    @Test
    fun `REQ-03 - should count background-foreground round trips`() {
        collector.onPause()   // go to background
        Thread.sleep(100)
        collector.onResume()  // return to foreground

        collector.onPause()   // go to background again
        Thread.sleep(100)
        collector.onResume()  // return again

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "backgroundSwitchCount should be 2 after two pause/resume cycles",
            2, features.backgroundSwitchCount
        )
    }

    @Test
    fun `REQ-03 - should be 0 when app never goes to background`() {
        collector.onTextChanged("accountNumber", 0, 1)
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "backgroundSwitchCount should be 0 when no background switches occurred",
            0, features.backgroundSwitchCount
        )
    }

    @Test
    fun `REQ-03 - should handle single pause without resume at session end`() {
        // User goes to background but session ends before returning
        collector.onPause()
        Thread.sleep(50)
        collector.stopSession()
        val features = collector.extractFeatures()

        // Per spec: count is only incremented on return to foreground (onResume)
        // A pause without resume should not count as a complete switch
        assertEquals(
            "backgroundSwitchCount should be 0 when pause occurs without resume",
            0, features.backgroundSwitchCount
        )
    }

    @Test
    fun `REQ-03 - should count correctly with many rapid switches`() {
        for (i in 1..5) {
            collector.onPause()
            Thread.sleep(10)
            collector.onResume()
        }
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "backgroundSwitchCount should be 5 after 5 rapid pause/resume cycles",
            5, features.backgroundSwitchCount
        )
    }

    // =========================================================================
    // REQ-04: totalBackgroundTimeMs
    // =========================================================================

    @Test
    fun `REQ-04 - should accumulate total background time across multiple switches`() {
        collector.onPause()
        Thread.sleep(200)
        collector.onResume()

        collector.onPause()
        Thread.sleep(300)
        collector.onResume()

        collector.stopSession()
        val features = collector.extractFeatures()

        // Allow 100ms tolerance for Thread.sleep imprecision
        assertTrue(
            "totalBackgroundTimeMs should be ~500ms, got ${features.totalBackgroundTimeMs}",
            features.totalBackgroundTimeMs in 400..700
        )
    }

    @Test
    fun `REQ-04 - should be 0 when no background time`() {
        collector.onTextChanged("accountNumber", 0, 1)
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "totalBackgroundTimeMs should be 0 when app never went to background",
            0L, features.totalBackgroundTimeMs
        )
    }

    @Test
    fun `REQ-04 - should include partial background time if session ends while backgrounded`() {
        collector.onPause()
        Thread.sleep(200)
        // Session stops while still in background (no onResume before stopSession)
        collector.stopSession()
        val features = collector.extractFeatures()

        assertTrue(
            "totalBackgroundTimeMs should include partial background time (~200ms), got ${features.totalBackgroundTimeMs}",
            features.totalBackgroundTimeMs in 150..350
        )
    }

    // =========================================================================
    // REQ-05: avgBackgroundDurationMs
    // =========================================================================

    @Test
    fun `REQ-05 - should compute average per-switch background duration`() {
        // Switch 1: ~100ms in background
        collector.onPause()
        Thread.sleep(100)
        collector.onResume()

        // Switch 2: ~300ms in background
        collector.onPause()
        Thread.sleep(300)
        collector.onResume()

        collector.stopSession()
        val features = collector.extractFeatures()

        // Average should be ~200ms, allow tolerance
        assertTrue(
            "avgBackgroundDurationMs should be ~200ms, got ${features.avgBackgroundDurationMs}",
            features.avgBackgroundDurationMs in 150.0..300.0
        )
    }

    @Test
    fun `REQ-05 - should be 0 when no background switches`() {
        collector.onTextChanged("accountNumber", 0, 1)
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "avgBackgroundDurationMs should be 0.0 when no background switches",
            0.0, features.avgBackgroundDurationMs, DELTA
        )
    }

    @Test
    fun `REQ-05 - should handle single switch correctly`() {
        collector.onPause()
        Thread.sleep(250)
        collector.onResume()
        collector.stopSession()
        val features = collector.extractFeatures()

        assertTrue(
            "avgBackgroundDurationMs with single switch should be ~250ms, got ${features.avgBackgroundDurationMs}",
            features.avgBackgroundDurationMs in 200.0..350.0
        )
    }

    // =========================================================================
    // REQ-06: preSubmitHesitationCategory
    // =========================================================================

    @Test
    fun `REQ-06 - should return UNKNOWN when no enrollment baseline provided`() {
        collector.onTextChanged("accountNumber", 0, 1)
        Thread.sleep(50)
        collector.stopSession()

        // extractFeatures with no profile/enrollment data
        val features = collector.extractFeatures()
        assertEquals(
            "preSubmitHesitationCategory should be UNKNOWN with no baseline",
            "UNKNOWN", features.preSubmitHesitationCategory
        )
    }

    @Test
    fun `REQ-06 - should return NORMAL when within baseline range`() {
        // Simulate text input then normal pause before confirm
        collector.onTextChanged("accountNumber", 0, 1)
        Thread.sleep(500)
        collector.stopSession()

        // Enrollment features with timeFromLastInputToConfirm ~500ms, std ~100ms
        // NORMAL range: [500 - 2*100, 500 + 2*100] = [300, 700]
        val mockProfile = createMockProfile(avgTimeFromLastInputToConfirm = 500.0)
        val enrollFeatures = createEnrollmentFeatures(listOf(400L, 500L, 600L))
        val features = collector.extractFeatures(
            profile = mockProfile,
            enrollmentFeatures = enrollFeatures
        )

        assertEquals(
            "preSubmitHesitationCategory should be NORMAL when within baseline +/- 2*std",
            "NORMAL", features.preSubmitHesitationCategory
        )
    }

    @Test
    fun `REQ-06 - should return RUSHED when significantly faster than baseline`() {
        // Very short pause before confirm
        collector.onTextChanged("accountNumber", 0, 1)
        // Immediately stop => timeFromLastInputToConfirm ~0ms
        collector.stopSession()

        // Enrollment features with mean=2000, std~115 => RUSHED threshold: < 2000 - 2*115 = ~1770
        val mockProfile = createMockProfile(avgTimeFromLastInputToConfirm = 2000.0)
        val enrollFeatures = createEnrollmentFeatures(listOf(1900L, 2000L, 2100L))
        val features = collector.extractFeatures(
            profile = mockProfile,
            enrollmentFeatures = enrollFeatures
        )

        assertEquals(
            "preSubmitHesitationCategory should be RUSHED when timeFromLastInput < baseline - 2*std",
            "RUSHED", features.preSubmitHesitationCategory
        )
    }

    @Test
    fun `REQ-06 - should return HESITANT when significantly slower than baseline`() {
        collector.onTextChanged("accountNumber", 0, 1)
        // Long pause before confirm
        Thread.sleep(3000)
        collector.stopSession()

        // Enrollment features with mean=500, std~115 => HESITANT threshold: > 500 + 2*115 = ~730
        val mockProfile = createMockProfile(avgTimeFromLastInputToConfirm = 500.0)
        val enrollFeatures = createEnrollmentFeatures(listOf(400L, 500L, 600L))
        val features = collector.extractFeatures(
            profile = mockProfile,
            enrollmentFeatures = enrollFeatures
        )

        assertEquals(
            "preSubmitHesitationCategory should be HESITANT when timeFromLastInput > baseline + 2*std",
            "HESITANT", features.preSubmitHesitationCategory
        )
    }

    @Test
    fun `REQ-06 - should return NORMAL when baseline std is 0`() {
        // All enrollment features have identical timeFromLastInputToConfirm
        // std=0 → any value is considered normal (no measurable variance)
        collector.onTextChanged("accountNumber", 0, 1)
        Thread.sleep(100)
        collector.stopSession()

        val mockProfile = createMockProfile(avgTimeFromLastInputToConfirm = 500.0)
        val enrollFeatures = createEnrollmentFeatures(listOf(500L, 500L, 500L))
        val features = collector.extractFeatures(
            profile = mockProfile,
            enrollmentFeatures = enrollFeatures
        )

        assertEquals(
            "preSubmitHesitationCategory should be NORMAL when baseline has zero variance",
            "NORMAL", features.preSubmitHesitationCategory
        )
    }

    // =========================================================================
    // REQ-07: sessionHourOfDay
    // =========================================================================

    @Test
    fun `REQ-07 - should capture hour of day when session started`() {
        collector.stopSession()
        val features = collector.extractFeatures()
        val hour = features.sessionHourOfDay

        assertTrue(
            "sessionHourOfDay should be 0-23, got $hour",
            hour in 0..23
        )
    }

    @Test
    fun `REQ-07 - should be consistent within the same session`() {
        // Extract features twice - hour should not change for same session
        Thread.sleep(10)
        collector.stopSession()
        val features1 = collector.extractFeatures()
        val features2 = collector.extractFeatures()

        assertEquals(
            "sessionHourOfDay should be consistent for the same session",
            features1.sessionHourOfDay, features2.sessionHourOfDay
        )
    }

    // =========================================================================
    // REQ-08: sessionDayOfWeek
    // =========================================================================

    @Test
    fun `REQ-08 - should capture day of week with Monday=1 Sunday=7 (ISO)`() {
        collector.stopSession()
        val features = collector.extractFeatures()
        val day = features.sessionDayOfWeek

        assertTrue(
            "sessionDayOfWeek should be 1-7 (Monday=1, Sunday=7), got $day",
            day in 1..7
        )
    }

    @Test
    fun `REQ-08 - should NOT use Java Calendar convention where Sunday=1`() {
        // This test documents the requirement: ISO 8601 day numbering
        // Monday=1, Tuesday=2, ..., Sunday=7
        // Java Calendar uses Sunday=1, Monday=2
        // We cannot deterministically test a specific day, but we verify
        // the range is 1-7 and the value matches LocalDate's DayOfWeek.
        collector.stopSession()
        val features = collector.extractFeatures()

        // Get the expected ISO day from Java time API
        val expectedIsoDay = java.time.LocalDate.now().dayOfWeek.value // Monday=1, Sunday=7

        assertEquals(
            "sessionDayOfWeek should match ISO 8601 (Monday=1, Sunday=7), expected $expectedIsoDay",
            expectedIsoDay, features.sessionDayOfWeek
        )
    }

    // =========================================================================
    // REQ-09: timeSinceLastSessionMs
    // =========================================================================

    @Test
    fun `REQ-09 - should return -1 when first session (no previous session)`() {
        every { mockSharedPreferences.getLong("lastSessionEndTimestamp", -1L) } returns -1L

        collector = BehavioralCollector(mockContext)
        collector.startSession()
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "timeSinceLastSessionMs should be -1 for first session",
            -1L, features.timeSinceLastSessionMs
        )
    }

    @Test
    fun `REQ-09 - should compute time since previous session ended`() {
        val previousEndTime = System.currentTimeMillis() - 60_000L // 60 seconds ago
        every { mockSharedPreferences.getLong("lastSessionEndTimestamp", -1L) } returns previousEndTime

        collector = BehavioralCollector(mockContext)
        collector.startSession()
        collector.stopSession()
        val features = collector.extractFeatures()

        assertTrue(
            "timeSinceLastSessionMs should be ~60000ms, got ${features.timeSinceLastSessionMs}",
            features.timeSinceLastSessionMs in 55_000L..70_000L
        )
    }

    @Test
    fun `REQ-09 - should store session end time in SharedPreferences on stopSession`() {
        collector.stopSession()
        collector.extractFeatures()

        verify {
            mockSharedPreferencesEditor.putLong("lastSessionEndTimestamp", any())
            mockSharedPreferencesEditor.apply()
        }
    }

    @Test
    fun `REQ-09 - should handle very old previous session`() {
        // Previous session was 30 days ago
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        every { mockSharedPreferences.getLong("lastSessionEndTimestamp", -1L) } returns thirtyDaysAgo

        collector = BehavioralCollector(mockContext)
        collector.startSession()
        collector.stopSession()
        val features = collector.extractFeatures()

        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        assertTrue(
            "timeSinceLastSessionMs should be ~30 days in ms, got ${features.timeSinceLastSessionMs}",
            features.timeSinceLastSessionMs in (thirtyDaysMs - 10_000L)..(thirtyDaysMs + 10_000L)
        )
    }

    // =========================================================================
    // REQ-10: deviceModel
    // =========================================================================

    @Test
    fun `REQ-10 - should capture Build MODEL string`() {
        collector.stopSession()
        val features = collector.extractFeatures()

        // Build.MODEL returns default value in unit tests (unitTests.isReturnDefaultValues = true)
        // The important thing is it should not be null
        assertTrue(
            "deviceModel should not be null or empty",
            features.deviceModel != null
        )
    }

    @Test
    fun `REQ-10 - should return a string value`() {
        collector.stopSession()
        val features = collector.extractFeatures()

        // With isReturnDefaultValues=true, Build.MODEL returns null or empty string
        // The collector should handle this gracefully and return a non-null string
        assertTrue(
            "deviceModel should be a String (not null)",
            features.deviceModel is String
        )
    }

    // =========================================================================
    // REQ-11: screenWidthPx
    // =========================================================================

    @Test
    fun `REQ-11 - should capture screen width in pixels`() {
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "screenWidthPx should match display metrics width",
            1080, features.screenWidthPx
        )
    }

    @Test
    fun `REQ-11 - should handle zero width gracefully`() {
        val zeroDm = DisplayMetrics().apply {
            widthPixels = 0
            heightPixels = 0
            density = 0f
        }
        every { mockResources.displayMetrics } returns zeroDm

        collector = BehavioralCollector(mockContext)
        collector.startSession()
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "screenWidthPx should be 0 when display reports 0",
            0, features.screenWidthPx
        )
    }

    // =========================================================================
    // REQ-12: screenHeightPx
    // =========================================================================

    @Test
    fun `REQ-12 - should capture screen height in pixels`() {
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "screenHeightPx should match display metrics height",
            2340, features.screenHeightPx
        )
    }

    @Test
    fun `REQ-12 - should handle various screen resolutions`() {
        val otherDm = DisplayMetrics().apply {
            widthPixels = 720
            heightPixels = 1280
            density = 2.0f
        }
        every { mockResources.displayMetrics } returns otherDm

        collector = BehavioralCollector(mockContext)
        collector.startSession()
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals("screenHeightPx should be 1280", 1280, features.screenHeightPx)
        assertEquals("screenWidthPx should be 720", 720, features.screenWidthPx)
    }

    // =========================================================================
    // REQ-13: screenDensity
    // =========================================================================

    @Test
    fun `REQ-13 - should capture display density`() {
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "screenDensity should match DisplayMetrics.density",
            2.75, features.screenDensity, DELTA
        )
    }

    @Test
    fun `REQ-13 - should handle different density values`() {
        val hdpiDm = DisplayMetrics().apply {
            widthPixels = 1440
            heightPixels = 3200
            density = 3.5f
        }
        every { mockResources.displayMetrics } returns hdpiDm

        collector = BehavioralCollector(mockContext)
        collector.startSession()
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "screenDensity should be 3.5 for xxxhdpi display",
            3.5, features.screenDensity, DELTA
        )
    }

    // =========================================================================
    // REQ-14: batteryLevel
    // =========================================================================

    @Test
    fun `REQ-14 - should capture battery percentage 0-100`() {
        every { mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } returns 75

        collector = BehavioralCollector(mockContext)
        collector.startSession()
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "batteryLevel should be 75",
            75, features.batteryLevel
        )
    }

    @Test
    fun `REQ-14 - should handle fully charged battery`() {
        every { mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } returns 100

        collector = BehavioralCollector(mockContext)
        collector.startSession()
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals("batteryLevel should be 100", 100, features.batteryLevel)
    }

    @Test
    fun `REQ-14 - should handle critically low battery`() {
        every { mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } returns 1

        collector = BehavioralCollector(mockContext)
        collector.startSession()
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals("batteryLevel should be 1", 1, features.batteryLevel)
    }

    @Test
    fun `REQ-14 - should handle zero battery level`() {
        every { mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } returns 0

        collector = BehavioralCollector(mockContext)
        collector.startSession()
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals("batteryLevel should be 0", 0, features.batteryLevel)
    }

    // =========================================================================
    // REQ-15: isCharging
    // =========================================================================

    @Test
    fun `REQ-15 - should detect charging state true`() {
        every { mockBatteryManager.isCharging } returns true

        collector = BehavioralCollector(mockContext)
        collector.startSession()
        collector.stopSession()
        val features = collector.extractFeatures()

        assertTrue("isCharging should be true when device is charging", features.isCharging)
    }

    @Test
    fun `REQ-15 - should detect charging state false`() {
        every { mockBatteryManager.isCharging } returns false

        collector = BehavioralCollector(mockContext)
        collector.startSession()
        collector.stopSession()
        val features = collector.extractFeatures()

        assertFalse("isCharging should be false when device is not charging", features.isCharging)
    }

    // =========================================================================
    // REQ-16: multiTouchCount
    // =========================================================================

    @Test
    fun `REQ-16 - should count MotionEvents with pointerCount greater than 1`() {
        val now = System.currentTimeMillis()
        // Single touch events (pointerCount=1) should NOT be counted
        collector.onTouchEvent(ACTION_DOWN, 100f, 200f, 0.5f, 10f, now, now, 1)
        collector.onTouchEvent(ACTION_UP, 100f, 200f, 0.5f, 10f, now, now + 100, 1)

        // Multi-touch events (pointerCount=2) SHOULD be counted
        collector.onTouchEvent(ACTION_DOWN, 100f, 200f, 0.5f, 10f, now + 200, now + 200, 2)
        collector.onTouchEvent(ACTION_MOVE, 110f, 210f, 0.5f, 10f, now + 200, now + 250, 2)
        collector.onTouchEvent(ACTION_UP, 120f, 220f, 0.5f, 10f, now + 200, now + 300, 2)

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "multiTouchCount should be 3 (three events with pointerCount>1)",
            3, features.multiTouchCount
        )
    }

    @Test
    fun `REQ-16 - should be 0 when all touches are single finger`() {
        val now = System.currentTimeMillis()
        collector.onTouchEvent(ACTION_DOWN, 100f, 200f, 0.5f, 10f, now, now, 1)
        collector.onTouchEvent(ACTION_UP, 100f, 200f, 0.5f, 10f, now, now + 100, 1)
        collector.onTouchEvent(ACTION_DOWN, 200f, 300f, 0.5f, 10f, now + 200, now + 200, 1)
        collector.onTouchEvent(ACTION_UP, 200f, 300f, 0.5f, 10f, now + 200, now + 300, 1)

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "multiTouchCount should be 0 when all pointerCount == 1",
            0, features.multiTouchCount
        )
    }

    @Test
    fun `REQ-16 - should be 0 when no touch events recorded`() {
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "multiTouchCount should be 0 with no touch events",
            0, features.multiTouchCount
        )
    }

    @Test
    fun `REQ-16 - should count events with 3 or more pointers`() {
        val now = System.currentTimeMillis()
        // 3-finger gesture
        collector.onTouchEvent(ACTION_DOWN, 100f, 200f, 0.5f, 10f, now, now, 3)
        collector.onTouchEvent(ACTION_UP, 100f, 200f, 0.5f, 10f, now, now + 100, 3)

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "multiTouchCount should be 2 for events with pointerCount=3",
            2, features.multiTouchCount
        )
    }

    // =========================================================================
    // REQ-17: maxPointerCount
    // =========================================================================

    @Test
    fun `REQ-17 - should track maximum pointerCount observed`() {
        val now = System.currentTimeMillis()
        // Single touch
        collector.onTouchEvent(ACTION_DOWN, 100f, 200f, 0.5f, 10f, now, now, 1)
        collector.onTouchEvent(ACTION_UP, 100f, 200f, 0.5f, 10f, now, now + 50, 1)

        // Two-finger touch
        collector.onTouchEvent(ACTION_DOWN, 100f, 200f, 0.5f, 10f, now + 100, now + 100, 2)
        collector.onTouchEvent(ACTION_UP, 100f, 200f, 0.5f, 10f, now + 100, now + 150, 2)

        // Three-finger touch
        collector.onTouchEvent(ACTION_DOWN, 100f, 200f, 0.5f, 10f, now + 200, now + 200, 3)
        collector.onTouchEvent(ACTION_UP, 100f, 200f, 0.5f, 10f, now + 200, now + 250, 3)

        // Back to single touch
        collector.onTouchEvent(ACTION_DOWN, 100f, 200f, 0.5f, 10f, now + 300, now + 300, 1)
        collector.onTouchEvent(ACTION_UP, 100f, 200f, 0.5f, 10f, now + 300, now + 350, 1)

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "maxPointerCount should be 3 (highest pointerCount in session)",
            3, features.maxPointerCount
        )
    }

    @Test
    fun `REQ-17 - should be 1 when only single touches recorded`() {
        val now = System.currentTimeMillis()
        collector.onTouchEvent(ACTION_DOWN, 100f, 200f, 0.5f, 10f, now, now, 1)
        collector.onTouchEvent(ACTION_UP, 100f, 200f, 0.5f, 10f, now, now + 100, 1)

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "maxPointerCount should be 1 when only single-finger touches",
            1, features.maxPointerCount
        )
    }

    @Test
    fun `REQ-17 - should be 0 when no touch events recorded`() {
        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "maxPointerCount should be 0 with no touch events",
            0, features.maxPointerCount
        )
    }

    @Test
    fun `REQ-17 - should handle high pointer counts gracefully`() {
        val now = System.currentTimeMillis()
        // Simulate 5-finger touch (e.g., palm accidentally on screen)
        collector.onTouchEvent(ACTION_DOWN, 100f, 200f, 0.5f, 10f, now, now, 5)
        collector.onTouchEvent(ACTION_UP, 100f, 200f, 0.5f, 10f, now, now + 100, 5)

        collector.stopSession()
        val features = collector.extractFeatures()

        assertEquals(
            "maxPointerCount should be 5 for 5-finger touch",
            5, features.maxPointerCount
        )
    }

    // =========================================================================
    // Cross-cutting: Combined cognitive signal tests
    // =========================================================================

    @Test
    fun `CROSS - should collect all cognitive features in a single session`() {
        // This integration-level test verifies that all 17 REQ features
        // are populated after a realistic session with mixed events
        every { mockAudioManager.mode } returns AudioManager.MODE_NORMAL
        every { mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } returns 42
        every { mockBatteryManager.isCharging } returns true

        collector = BehavioralCollector(mockContext)
        collector.startSession()

        // Background switch
        collector.onPause()
        Thread.sleep(50)
        collector.onResume()

        // Text input
        collector.onTextChanged("accountNumber", 0, 1)
        Thread.sleep(50)
        collector.onTextChanged("accountNumber", 1, 2)

        // Touch events with multi-touch
        val now = System.currentTimeMillis()
        collector.onTouchEvent(ACTION_DOWN, 100f, 200f, 0.5f, 10f, now, now, 1)
        collector.onTouchEvent(ACTION_UP, 100f, 200f, 0.5f, 10f, now, now + 50, 1)
        collector.onTouchEvent(ACTION_DOWN, 100f, 200f, 0.5f, 10f, now + 100, now + 100, 2)
        collector.onTouchEvent(ACTION_UP, 100f, 200f, 0.5f, 10f, now + 100, now + 150, 2)

        collector.stopSession()
        val features = collector.extractFeatures()

        // Verify all cognitive features have reasonable values
        assertFalse("isCallActiveDuringSession should be false", features.isCallActiveDuringSession)
        assertFalse("callStartedDuringSession should be false", features.callStartedDuringSession)
        assertEquals("backgroundSwitchCount should be 1", 1, features.backgroundSwitchCount)
        assertTrue("totalBackgroundTimeMs should be > 0", features.totalBackgroundTimeMs > 0)
        assertTrue("avgBackgroundDurationMs should be > 0", features.avgBackgroundDurationMs > 0.0)
        assertEquals("preSubmitHesitationCategory should be UNKNOWN (no baseline)", "UNKNOWN", features.preSubmitHesitationCategory)
        assertTrue("sessionHourOfDay should be 0-23", features.sessionHourOfDay in 0..23)
        assertTrue("sessionDayOfWeek should be 1-7", features.sessionDayOfWeek in 1..7)
        assertEquals("batteryLevel should be 42", 42, features.batteryLevel)
        assertTrue("isCharging should be true", features.isCharging)
        assertEquals("multiTouchCount should be 2", 2, features.multiTouchCount)
        assertEquals("maxPointerCount should be 2", 2, features.maxPointerCount)
        assertTrue("screenWidthPx should be > 0", features.screenWidthPx > 0)
        assertTrue("screenHeightPx should be > 0", features.screenHeightPx > 0)
        assertTrue("screenDensity should be > 0", features.screenDensity > 0.0)
    }

    @Test
    fun `CROSS - empty session should return safe defaults for all cognitive features`() {
        // Start and immediately stop - no events
        collector.stopSession()
        val features = collector.extractFeatures()

        assertFalse("isCallActiveDuringSession default should be false", features.isCallActiveDuringSession)
        assertFalse("callStartedDuringSession default should be false", features.callStartedDuringSession)
        assertEquals("backgroundSwitchCount default should be 0", 0, features.backgroundSwitchCount)
        assertEquals("totalBackgroundTimeMs default should be 0", 0L, features.totalBackgroundTimeMs)
        assertEquals("avgBackgroundDurationMs default should be 0.0", 0.0, features.avgBackgroundDurationMs, DELTA)
        assertEquals("preSubmitHesitationCategory default should be UNKNOWN", "UNKNOWN", features.preSubmitHesitationCategory)
        assertEquals("multiTouchCount default should be 0", 0, features.multiTouchCount)
        assertEquals("maxPointerCount default should be 0", 0, features.maxPointerCount)
    }

    // =========================================================================
    // Helper: Create mock profile for REQ-06 baseline comparisons
    // =========================================================================

    /**
     * Creates a minimal mock BehavioralProfile for REQ-06 baseline comparison.
     */
    private fun createMockProfile(
        avgTimeFromLastInputToConfirm: Double
    ): com.poc.behavioralfraud.data.model.BehavioralProfile {
        return com.poc.behavioralfraud.data.model.BehavioralProfile(
            userId = "test-user",
            enrollmentCount = 3,
            avgSessionDuration = 10000.0,
            avgInterCharDelay = 100.0,
            stdInterCharDelay = 20.0,
            avgTouchSize = 0.5,
            avgTouchDuration = 100.0,
            avgGyroStabilityX = 0.1,
            avgGyroStabilityY = 0.1,
            avgGyroStabilityZ = 0.1,
            avgAccelStabilityX = 0.1,
            avgAccelStabilityY = 0.1,
            avgAccelStabilityZ = 0.1,
            avgSwipeVelocity = 500.0,
            avgPasteCount = 0.0,
            avgTimeToFirstInput = 1000.0,
            avgTimeFromLastInputToConfirm = avgTimeFromLastInputToConfirm,
            typicalFieldFocusSequence = "accountNumber -> amount -> note",
            avgTouchPressure = 10.0,
            avgInterFieldPause = 200.0,
            avgDeletionRatio = 0.05,
            profileSummary = "Test profile"
        )
    }

    /**
     * Creates enrollment features with specified timeFromLastInputToConfirm values.
     * Used for REQ-06 std dev computation.
     */
    private fun createEnrollmentFeatures(
        timesFromLastInput: List<Long>
    ): List<BehavioralFeatures> {
        return timesFromLastInput.map { time ->
            BehavioralFeatures(
                sessionDurationMs = 10000L,
                avgInterCharDelayMs = 100.0,
                stdInterCharDelayMs = 20.0,
                maxInterCharDelayMs = 200L,
                minInterCharDelayMs = 50L,
                totalTextChanges = 10,
                pasteCount = 0,
                totalTouchEvents = 5,
                avgTouchSize = 0.5,
                avgTouchDurationMs = 100.0,
                avgSwipeVelocity = 0.0,
                gyroStabilityX = 0.0,
                gyroStabilityY = 0.0,
                gyroStabilityZ = 0.0,
                accelStabilityX = 0.0,
                accelStabilityY = 0.0,
                accelStabilityZ = 0.0,
                avgTouchPressure = 10.0,
                perFieldAvgDelay = emptyMap(),
                avgInterFieldPauseMs = 0.0,
                deletionCount = 0,
                deletionRatio = 0.0,
                fieldFocusSequence = "",
                timeToFirstInput = 500L,
                timeFromLastInputToConfirm = time
            )
        }
    }
}
