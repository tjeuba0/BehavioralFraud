package com.poc.behavioralfraud

import com.poc.behavioralfraud.data.model.BehavioralProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Tests for REQ-01 through REQ-04: BehavioralProfile must expose per-axis
 * gyro/accel stability fields and they must format correctly for UI display.
 *
 * These tests verify the SPECIFICATION, not a particular implementation.
 * The core requirements are:
 *   - BehavioralProfile has avgGyroStabilityX/Y/Z (not a single avgGyroStability)
 *   - BehavioralProfile has avgAccelStabilityX/Y/Z (not a single avgAccelStability)
 *   - Gyro values format with "%.6f" (6 decimal places)
 *   - Accel values format with "%.4f" (4 decimal places)
 *   - No crash when constructing a profile with valid data
 */
class BehavioralProfileFieldsTest {

    private lateinit var defaultProfile: BehavioralProfile

    @Before
    fun setup() {
        defaultProfile = createProfile()
    }

    // -----------------------------------------------------------------------
    // REQ-01: BehavioralProfile has per-axis gyro stability fields
    // -----------------------------------------------------------------------

    @Test
    fun `REQ-01 happy path - profile exposes avgGyroStabilityX field`() {
        val profile = createProfile(gyroX = 0.001234)
        assertEquals(0.001234, profile.avgGyroStabilityX, 0.0)
    }

    @Test
    fun `REQ-01 happy path - profile exposes avgGyroStabilityY field`() {
        val profile = createProfile(gyroY = 0.005678)
        assertEquals(0.005678, profile.avgGyroStabilityY, 0.0)
    }

    @Test
    fun `REQ-01 happy path - profile exposes avgGyroStabilityZ field`() {
        val profile = createProfile(gyroZ = 0.009012)
        assertEquals(0.009012, profile.avgGyroStabilityZ, 0.0)
    }

    @Test
    fun `REQ-01 happy path - all three gyro axes are independent values`() {
        val profile = createProfile(gyroX = 0.1, gyroY = 0.2, gyroZ = 0.3)
        assertEquals(0.1, profile.avgGyroStabilityX, 0.0)
        assertEquals(0.2, profile.avgGyroStabilityY, 0.0)
        assertEquals(0.3, profile.avgGyroStabilityZ, 0.0)
    }

    @Test
    fun `REQ-01 edge case - gyro stability fields accept zero values`() {
        val profile = createProfile(gyroX = 0.0, gyroY = 0.0, gyroZ = 0.0)
        assertEquals(0.0, profile.avgGyroStabilityX, 0.0)
        assertEquals(0.0, profile.avgGyroStabilityY, 0.0)
        assertEquals(0.0, profile.avgGyroStabilityZ, 0.0)
    }

    @Test
    fun `REQ-01 edge case - gyro stability fields accept negative values`() {
        val profile = createProfile(gyroX = -0.003, gyroY = -0.007, gyroZ = -0.001)
        assertEquals(-0.003, profile.avgGyroStabilityX, 0.0)
        assertEquals(-0.007, profile.avgGyroStabilityY, 0.0)
        assertEquals(-0.001, profile.avgGyroStabilityZ, 0.0)
    }

    @Test
    fun `REQ-01 edge case - gyro stability fields accept very small values`() {
        val profile = createProfile(gyroX = 0.000001, gyroY = 0.0000001, gyroZ = 0.00000001)
        assertEquals(0.000001, profile.avgGyroStabilityX, 1e-15)
        assertEquals(0.0000001, profile.avgGyroStabilityY, 1e-15)
        assertEquals(0.00000001, profile.avgGyroStabilityZ, 1e-15)
    }

    @Test
    fun `REQ-01 edge case - gyro stability fields accept large values`() {
        val profile = createProfile(gyroX = 999.999999, gyroY = 1000.0, gyroZ = 12345.6789)
        assertEquals(999.999999, profile.avgGyroStabilityX, 1e-10)
        assertEquals(1000.0, profile.avgGyroStabilityY, 0.0)
        assertEquals(12345.6789, profile.avgGyroStabilityZ, 1e-10)
    }

    // -----------------------------------------------------------------------
    // REQ-02: BehavioralProfile has per-axis accel stability fields
    // -----------------------------------------------------------------------

    @Test
    fun `REQ-02 happy path - profile exposes avgAccelStabilityX field`() {
        val profile = createProfile(accelX = 0.1234)
        assertEquals(0.1234, profile.avgAccelStabilityX, 0.0)
    }

    @Test
    fun `REQ-02 happy path - profile exposes avgAccelStabilityY field`() {
        val profile = createProfile(accelY = 0.5678)
        assertEquals(0.5678, profile.avgAccelStabilityY, 0.0)
    }

    @Test
    fun `REQ-02 happy path - profile exposes avgAccelStabilityZ field`() {
        val profile = createProfile(accelZ = 0.9012)
        assertEquals(0.9012, profile.avgAccelStabilityZ, 0.0)
    }

    @Test
    fun `REQ-02 happy path - all three accel axes are independent values`() {
        val profile = createProfile(accelX = 1.1, accelY = 2.2, accelZ = 3.3)
        assertEquals(1.1, profile.avgAccelStabilityX, 0.0)
        assertEquals(2.2, profile.avgAccelStabilityY, 0.0)
        assertEquals(3.3, profile.avgAccelStabilityZ, 0.0)
    }

    @Test
    fun `REQ-02 edge case - accel stability fields accept zero values`() {
        val profile = createProfile(accelX = 0.0, accelY = 0.0, accelZ = 0.0)
        assertEquals(0.0, profile.avgAccelStabilityX, 0.0)
        assertEquals(0.0, profile.avgAccelStabilityY, 0.0)
        assertEquals(0.0, profile.avgAccelStabilityZ, 0.0)
    }

    @Test
    fun `REQ-02 edge case - accel stability fields accept negative values`() {
        val profile = createProfile(accelX = -9.81, accelY = -0.5, accelZ = -1.0)
        assertEquals(-9.81, profile.avgAccelStabilityX, 0.0)
        assertEquals(-0.5, profile.avgAccelStabilityY, 0.0)
        assertEquals(-1.0, profile.avgAccelStabilityZ, 0.0)
    }

    @Test
    fun `REQ-02 edge case - accel stability fields accept very small values`() {
        val profile = createProfile(accelX = 0.0001, accelY = 0.00001, accelZ = 0.000001)
        assertEquals(0.0001, profile.avgAccelStabilityX, 1e-15)
        assertEquals(0.00001, profile.avgAccelStabilityY, 1e-15)
        assertEquals(0.000001, profile.avgAccelStabilityZ, 1e-15)
    }

    @Test
    fun `REQ-02 edge case - accel stability fields accept large values`() {
        val profile = createProfile(accelX = 500.1234, accelY = 1000.0, accelZ = 9999.9999)
        assertEquals(500.1234, profile.avgAccelStabilityX, 1e-10)
        assertEquals(1000.0, profile.avgAccelStabilityY, 0.0)
        assertEquals(9999.9999, profile.avgAccelStabilityZ, 1e-10)
    }

    // -----------------------------------------------------------------------
    // REQ-03: Formatting — gyro uses "%.6f", accel uses "%.4f"
    // -----------------------------------------------------------------------

    @Test
    fun `REQ-03 happy path - gyro X formats to 6 decimal places`() {
        val value = 0.001234
        val formatted = String.format("%.6f", value)
        assertEquals("0.001234", formatted)
    }

    @Test
    fun `REQ-03 happy path - gyro Y formats to 6 decimal places`() {
        val value = 0.005678
        val formatted = String.format("%.6f", value)
        assertEquals("0.005678", formatted)
    }

    @Test
    fun `REQ-03 happy path - gyro Z formats to 6 decimal places`() {
        val value = 0.009012
        val formatted = String.format("%.6f", value)
        assertEquals("0.009012", formatted)
    }

    @Test
    fun `REQ-03 happy path - accel X formats to 4 decimal places`() {
        val value = 0.1234
        val formatted = String.format("%.4f", value)
        assertEquals("0.1234", formatted)
    }

    @Test
    fun `REQ-03 happy path - accel Y formats to 4 decimal places`() {
        val value = 0.5678
        val formatted = String.format("%.4f", value)
        assertEquals("0.5678", formatted)
    }

    @Test
    fun `REQ-03 happy path - accel Z formats to 4 decimal places`() {
        val value = 9.81
        val formatted = String.format("%.4f", value)
        assertEquals("9.8100", formatted)
    }

    @Test
    fun `REQ-03 happy path - gyro format pads zeros for short values`() {
        val value = 0.01
        val formatted = String.format("%.6f", value)
        assertEquals("0.010000", formatted)
    }

    @Test
    fun `REQ-03 happy path - accel format pads zeros for short values`() {
        val value = 1.0
        val formatted = String.format("%.4f", value)
        assertEquals("1.0000", formatted)
    }

    @Test
    fun `REQ-03 happy path - gyro format truncates extra precision`() {
        val value = 0.00123456789
        val formatted = String.format("%.6f", value)
        assertEquals("0.001235", formatted)
    }

    @Test
    fun `REQ-03 happy path - accel format truncates extra precision`() {
        val value = 0.12345678
        val formatted = String.format("%.4f", value)
        assertEquals("0.1235", formatted)
    }

    @Test
    fun `REQ-03 edge case - gyro format handles zero`() {
        val formatted = String.format("%.6f", 0.0)
        assertEquals("0.000000", formatted)
    }

    @Test
    fun `REQ-03 edge case - accel format handles zero`() {
        val formatted = String.format("%.4f", 0.0)
        assertEquals("0.0000", formatted)
    }

    @Test
    fun `REQ-03 edge case - gyro format handles negative value`() {
        val formatted = String.format("%.6f", -0.003456)
        assertEquals("-0.003456", formatted)
    }

    @Test
    fun `REQ-03 edge case - accel format handles negative value`() {
        val formatted = String.format("%.4f", -9.81)
        assertEquals("-9.8100", formatted)
    }

    @Test
    fun `REQ-03 edge case - gyro format handles very small value near zero`() {
        val formatted = String.format("%.6f", 0.000001)
        assertEquals("0.000001", formatted)
    }

    @Test
    fun `REQ-03 edge case - accel format handles large value`() {
        val formatted = String.format("%.4f", 999.9999)
        assertEquals("999.9999", formatted)
    }

    @Test
    fun `REQ-03 integration - format all gyro axes from profile`() {
        val profile = createProfile(gyroX = 0.001234, gyroY = 0.005678, gyroZ = 0.009012)
        val formattedX = String.format("%.6f", profile.avgGyroStabilityX)
        val formattedY = String.format("%.6f", profile.avgGyroStabilityY)
        val formattedZ = String.format("%.6f", profile.avgGyroStabilityZ)
        assertEquals("0.001234", formattedX)
        assertEquals("0.005678", formattedY)
        assertEquals("0.009012", formattedZ)
    }

    @Test
    fun `REQ-03 integration - format all accel axes from profile`() {
        val profile = createProfile(accelX = 0.1234, accelY = 0.5678, accelZ = 9.81)
        val formattedX = String.format("%.4f", profile.avgAccelStabilityX)
        val formattedY = String.format("%.4f", profile.avgAccelStabilityY)
        val formattedZ = String.format("%.4f", profile.avgAccelStabilityZ)
        assertEquals("0.1234", formattedX)
        assertEquals("0.5678", formattedY)
        assertEquals("9.8100", formattedZ)
    }

    // -----------------------------------------------------------------------
    // REQ-04: No crash with valid profile data or null profile
    // -----------------------------------------------------------------------

    @Test
    fun `REQ-04 happy path - profile construction with typical values does not throw`() {
        val profile = createProfile()
        assertNotNull(profile)
        assertNotNull(profile.userId)
    }

    @Test
    fun `REQ-04 happy path - profile construction with all zero sensor values does not throw`() {
        val profile = createProfile(
            gyroX = 0.0, gyroY = 0.0, gyroZ = 0.0,
            accelX = 0.0, accelY = 0.0, accelZ = 0.0
        )
        assertNotNull(profile)
    }

    @Test
    fun `REQ-04 happy path - profile construction with negative sensor values does not throw`() {
        val profile = createProfile(
            gyroX = -1.0, gyroY = -2.0, gyroZ = -3.0,
            accelX = -4.0, accelY = -5.0, accelZ = -6.0
        )
        assertNotNull(profile)
    }

    @Test
    fun `REQ-04 happy path - profile construction with extreme sensor values does not throw`() {
        val profile = createProfile(
            gyroX = Double.MAX_VALUE, gyroY = Double.MIN_VALUE, gyroZ = Double.MAX_VALUE,
            accelX = Double.MAX_VALUE, accelY = Double.MIN_VALUE, accelZ = Double.MAX_VALUE
        )
        assertNotNull(profile)
    }

    @Test
    fun `REQ-04 edge case - nullable profile variable can be null`() {
        val profile: BehavioralProfile? = null
        assertEquals(null, profile)
    }

    @Test
    fun `REQ-04 edge case - null profile safe access to gyro fields returns null`() {
        val profile: BehavioralProfile? = null
        val gyroX: Double? = profile?.avgGyroStabilityX
        val gyroY: Double? = profile?.avgGyroStabilityY
        val gyroZ: Double? = profile?.avgGyroStabilityZ
        assertEquals(null, gyroX)
        assertEquals(null, gyroY)
        assertEquals(null, gyroZ)
    }

    @Test
    fun `REQ-04 edge case - null profile safe access to accel fields returns null`() {
        val profile: BehavioralProfile? = null
        val accelX: Double? = profile?.avgAccelStabilityX
        val accelY: Double? = profile?.avgAccelStabilityY
        val accelZ: Double? = profile?.avgAccelStabilityZ
        assertEquals(null, accelX)
        assertEquals(null, accelY)
        assertEquals(null, accelZ)
    }

    @Test
    fun `REQ-04 edge case - formatting gyro values from null-safe access`() {
        val profile: BehavioralProfile? = createProfile(gyroX = 0.001234)
        val formatted = profile?.let { String.format("%.6f", it.avgGyroStabilityX) }
        assertEquals("0.001234", formatted)
    }

    @Test
    fun `REQ-04 edge case - formatting accel values from null-safe access`() {
        val profile: BehavioralProfile? = createProfile(accelX = 0.5678)
        val formatted = profile?.let { String.format("%.4f", it.avgAccelStabilityX) }
        assertEquals("0.5678", formatted)
    }

    @Test
    fun `REQ-04 edge case - profile with empty strings does not throw`() {
        val profile = createProfile(userId = "", summary = "", fieldSequence = "")
        assertNotNull(profile)
        assertEquals("", profile.userId)
        assertEquals("", profile.profileSummary)
        assertEquals("", profile.typicalFieldFocusSequence)
    }

    @Test
    fun `REQ-04 edge case - profile data class copy preserves per-axis fields`() {
        val original = createProfile(gyroX = 0.1, gyroY = 0.2, gyroZ = 0.3, accelX = 0.4, accelY = 0.5, accelZ = 0.6)
        val copy = original.copy(userId = "differentUser")
        assertEquals(0.1, copy.avgGyroStabilityX, 0.0)
        assertEquals(0.2, copy.avgGyroStabilityY, 0.0)
        assertEquals(0.3, copy.avgGyroStabilityZ, 0.0)
        assertEquals(0.4, copy.avgAccelStabilityX, 0.0)
        assertEquals(0.5, copy.avgAccelStabilityY, 0.0)
        assertEquals(0.6, copy.avgAccelStabilityZ, 0.0)
        assertEquals("differentUser", copy.userId)
    }

    @Test
    fun `REQ-04 edge case - profile equality includes per-axis fields`() {
        val profile1 = createProfile(gyroX = 0.1, accelX = 0.2)
        val profile2 = createProfile(gyroX = 0.1, accelX = 0.2)
        val profile3 = createProfile(gyroX = 0.999, accelX = 0.2)
        assertEquals(profile1, profile2)
        assert(profile1 != profile3) { "Profiles with different gyroX should not be equal" }
    }

    // -----------------------------------------------------------------------
    // Helper: create a BehavioralProfile with sensible defaults
    // -----------------------------------------------------------------------

    private fun createProfile(
        userId: String = "test-user",
        enrollmentCount: Int = 3,
        avgSessionDuration: Double = 45000.0,
        avgInterCharDelay: Double = 150.0,
        stdInterCharDelay: Double = 30.0,
        avgTouchSize: Double = 0.15,
        avgTouchDuration: Double = 85.0,
        gyroX: Double = 0.002,
        gyroY: Double = 0.003,
        gyroZ: Double = 0.001,
        accelX: Double = 0.25,
        accelY: Double = 0.30,
        accelZ: Double = 9.81,
        avgSwipeVelocity: Double = 500.0,
        avgPasteCount: Double = 0.5,
        avgTimeToFirstInput: Double = 2000.0,
        avgTimeFromLastInputToConfirm: Double = 3000.0,
        fieldSequence: String = "accountNumber,amount,note",
        avgTouchPressure: Double = 0.4,
        avgInterFieldPause: Double = 800.0,
        avgDeletionRatio: Double = 0.05,
        summary: String = "Test profile summary"
    ): BehavioralProfile {
        return BehavioralProfile(
            userId = userId,
            enrollmentCount = enrollmentCount,
            avgSessionDuration = avgSessionDuration,
            avgInterCharDelay = avgInterCharDelay,
            stdInterCharDelay = stdInterCharDelay,
            avgTouchSize = avgTouchSize,
            avgTouchDuration = avgTouchDuration,
            avgGyroStabilityX = gyroX,
            avgGyroStabilityY = gyroY,
            avgGyroStabilityZ = gyroZ,
            avgAccelStabilityX = accelX,
            avgAccelStabilityY = accelY,
            avgAccelStabilityZ = accelZ,
            avgSwipeVelocity = avgSwipeVelocity,
            avgPasteCount = avgPasteCount,
            avgTimeToFirstInput = avgTimeToFirstInput,
            avgTimeFromLastInputToConfirm = avgTimeFromLastInputToConfirm,
            typicalFieldFocusSequence = fieldSequence,
            avgTouchPressure = avgTouchPressure,
            avgInterFieldPause = avgInterFieldPause,
            avgDeletionRatio = avgDeletionRatio,
            profileSummary = summary
        )
    }
}
