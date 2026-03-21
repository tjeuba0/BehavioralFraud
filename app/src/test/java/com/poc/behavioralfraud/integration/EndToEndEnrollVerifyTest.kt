package com.poc.behavioralfraud.integration

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.poc.behavioralfraud.integration.E2ETestFixtures.TEST_USER_ID
import com.poc.behavioralfraud.integration.E2ETestFixtures.baselineProfile
import com.poc.behavioralfraud.integration.E2ETestFixtures.differentPersonVerification
import com.poc.behavioralfraud.integration.E2ETestFixtures.enrollmentSession1
import com.poc.behavioralfraud.integration.E2ETestFixtures.enrollmentSession2
import com.poc.behavioralfraud.integration.E2ETestFixtures.enrollmentSession3
import com.poc.behavioralfraud.integration.E2ETestFixtures.samePersonVerification
import com.poc.behavioralfraud.network.BackendClient
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-end integration tests: Enrollment + Verification flows (REQ-01, REQ-02, REQ-03).
 * Uses MockWebServer to simulate the FastAPI backend.
 */
class EndToEndEnrollVerifyTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: BackendClient
    private val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        client = BackendClient(baseUrl = mockWebServer.url("/").toString(), timeoutSeconds = 5)
    }

    @After
    fun teardown() { mockWebServer.shutdown() }

    // =========================================================================
    // REQ-01: Enrollment 3x → backend creates profile
    // =========================================================================

    @Test
    fun `REQ-01 full enrollment flow - 3 sessions produce pending pending completed`() = runTest {
        enqueueFullEnrollment()

        val r1 = client.enrollSession(TEST_USER_ID, enrollmentSession1)
        assertEquals("pending", r1.status)
        assertEquals(1, r1.enrollmentCount)
        assertEquals(2, r1.remaining)
        assertNull(r1.profile)

        val r2 = client.enrollSession(TEST_USER_ID, enrollmentSession2)
        assertEquals("pending", r2.status)
        assertEquals(2, r2.enrollmentCount)
        assertEquals(1, r2.remaining)
        assertNull(r2.profile)

        val r3 = client.enrollSession(TEST_USER_ID, enrollmentSession3)
        assertEquals("completed", r3.status)
        assertEquals(3, r3.enrollmentCount)
        assertEquals(0, r3.remaining)
        assertNotNull(r3.profile)
        assertNotNull(r3.profileSummary)
        assertEquals(TEST_USER_ID, r3.profile!!.userId)
        assertEquals(3, mockWebServer.requestCount)
    }

    @Test
    fun `REQ-01 all enrollment requests hit POST profile-enroll`() = runTest {
        enqueueFullEnrollment()
        repeat(3) { i ->
            client.enrollSession(TEST_USER_ID, E2ETestFixtures.enrollmentSessions[i])
        }
        repeat(3) {
            val req = mockWebServer.takeRequest()
            assertEquals("POST", req.method)
            assertEquals("/profile/enroll", req.path)
        }
    }

    @Test
    fun `REQ-01 edge - enrollment fails mid-flow on session 2`() = runTest {
        enqueueEnrollPending(1, 2)
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"error"}"""))

        client.enrollSession(TEST_USER_ID, enrollmentSession1)
        try {
            client.enrollSession(TEST_USER_ID, enrollmentSession2)
            org.junit.Assert.fail("Expected exception")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("500"))
        }
    }

    // =========================================================================
    // REQ-02: Same person → low risk (0-30), APPROVE
    // =========================================================================

    @Test
    fun `REQ-02 full flow - enroll 3x then verify same person gets APPROVE`() = runTest {
        enqueueFullEnrollment()
        enqueueVerification(riskScore = 15, riskLevel = "LOW", recommendation = "APPROVE")

        repeat(3) { i -> client.enrollSession(TEST_USER_ID, E2ETestFixtures.enrollmentSessions[i]) }
        val result = client.verifyTransaction(TEST_USER_ID, samePersonVerification, baselineProfile)

        assertTrue("Score 0-30 for same person", result.riskScore in 0..30)
        assertEquals("LOW", result.riskLevel)
        assertEquals("APPROVE", result.recommendation)
    }

    @Test
    fun `REQ-02 same person returns trace_id and model`() = runTest {
        enqueueVerification(10, "LOW", "APPROVE", traceId = "trace-001", model = "gemini-2.0-flash")
        val result = client.verifyTransaction(TEST_USER_ID, samePersonVerification, baselineProfile)
        assertEquals("trace-001", result.traceId)
        assertEquals("gemini-2.0-flash", result.model)
    }

    @Test
    fun `REQ-02 boundary score 30 is still APPROVE`() = runTest {
        enqueueVerification(30, "LOW", "APPROVE")
        val result = client.verifyTransaction(TEST_USER_ID, samePersonVerification, baselineProfile)
        assertEquals(30, result.riskScore)
        assertEquals("APPROVE", result.recommendation)
    }

    // =========================================================================
    // REQ-03: Different person → high risk (71-100), BLOCK
    // =========================================================================

    @Test
    fun `REQ-03 full flow - enroll then verify different person gets BLOCK`() = runTest {
        enqueueFullEnrollment()
        enqueueVerification(
            riskScore = 88, riskLevel = "HIGH", recommendation = "BLOCK",
            anomalies = listOf("typing_speed", "gyro_stable", "paste_detected", "touch_size")
        )

        repeat(3) { i -> client.enrollSession(TEST_USER_ID, E2ETestFixtures.enrollmentSessions[i]) }
        val result = client.verifyTransaction(TEST_USER_ID, differentPersonVerification, baselineProfile)

        assertTrue("Score 71-100 for different person", result.riskScore in 71..100)
        assertEquals("HIGH", result.riskLevel)
        assertEquals("BLOCK", result.recommendation)
        assertTrue(result.anomalies.isNotEmpty())
    }

    @Test
    fun `REQ-03 boundary score 71 is BLOCK`() = runTest {
        enqueueVerification(71, "HIGH", "BLOCK")
        val result = client.verifyTransaction(TEST_USER_ID, differentPersonVerification, baselineProfile)
        assertEquals(71, result.riskScore)
        assertEquals("BLOCK", result.recommendation)
    }

    @Test
    fun `REQ-03 medium risk 50 is STEP_UP_AUTH`() = runTest {
        enqueueVerification(50, "MEDIUM", "STEP_UP_AUTH")
        val result = client.verifyTransaction(TEST_USER_ID, differentPersonVerification, baselineProfile)
        assertEquals(50, result.riskScore)
        assertEquals("STEP_UP_AUTH", result.recommendation)
    }

    @Test
    fun `REQ-03 max score 100`() = runTest {
        enqueueVerification(100, "HIGH", "BLOCK")
        val result = client.verifyTransaction(TEST_USER_ID, differentPersonVerification, baselineProfile)
        assertEquals(100, result.riskScore)
    }

    // =========================================================================
    // Complete E2E scenario
    // =========================================================================

    @Test
    fun `E2E - enroll 3x then verify same person then verify fraudster`() = runTest {
        enqueueFullEnrollment()
        enqueueVerification(12, "LOW", "APPROVE")
        enqueueVerification(92, "HIGH", "BLOCK", anomalies = listOf("typing_speed", "gyro"))

        repeat(3) { i -> client.enrollSession(TEST_USER_ID, E2ETestFixtures.enrollmentSessions[i]) }

        val same = client.verifyTransaction(TEST_USER_ID, samePersonVerification, baselineProfile)
        assertTrue(same.riskScore <= 30)
        assertEquals("APPROVE", same.recommendation)

        val fraud = client.verifyTransaction(TEST_USER_ID, differentPersonVerification, baselineProfile)
        assertTrue(fraud.riskScore >= 71)
        assertEquals("BLOCK", fraud.recommendation)

        assertEquals(5, mockWebServer.requestCount)
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun enqueueFullEnrollment() {
        enqueueEnrollPending(1, 2)
        enqueueEnrollPending(2, 1)
        enqueueEnrollCompleted()
    }

    private fun enqueueEnrollPending(count: Int, remaining: Int) {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"status":"pending","enrollment_count":$count,"remaining":$remaining}""")
                .setResponseCode(200)
        )
    }

    private fun enqueueEnrollCompleted() {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"status":"completed","enrollment_count":3,"remaining":0,"profile":${gson.toJson(baselineProfile)},"profile_summary":"Consistent typing."}""")
                .setResponseCode(200)
        )
    }

    private fun enqueueVerification(
        riskScore: Int, riskLevel: String, recommendation: String,
        anomalies: List<String> = emptyList(),
        traceId: String = "trace-${System.currentTimeMillis()}",
        model: String = "test-model"
    ) {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"risk_score":$riskScore,"risk_level":"$riskLevel","anomalies":${gson.toJson(anomalies)},"explanation":"Score $riskScore","recommendation":"$recommendation","trace_id":"$traceId","model":"$model","latency_ms":150}""")
                .setResponseCode(200)
        )
    }
}
