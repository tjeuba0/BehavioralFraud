package com.poc.behavioralfraud.integration

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.poc.behavioralfraud.data.scorer.LocalScorer
import com.poc.behavioralfraud.integration.E2ETestFixtures.TEST_USER_ID
import com.poc.behavioralfraud.integration.E2ETestFixtures.baselineProfile
import com.poc.behavioralfraud.integration.E2ETestFixtures.differentPersonVerification
import com.poc.behavioralfraud.integration.E2ETestFixtures.enrollmentSessions
import com.poc.behavioralfraud.integration.E2ETestFixtures.samePersonVerification
import com.poc.behavioralfraud.network.BackendClient
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-end integration tests: Fallback + Latency (REQ-04, REQ-05).
 * Uses MockWebServer to simulate backend failures and real LocalScorer for fallback.
 */
class EndToEndFallbackLatencyTest {

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
    fun teardown() {
        try { mockWebServer.shutdown() } catch (_: Exception) {}
    }

    // =========================================================================
    // REQ-04: Backend unreachable → LocalScorer fallback
    // =========================================================================

    @Test
    fun `REQ-04 backend down - LocalScorer fallback returns valid result`() = runTest {
        mockWebServer.shutdown()
        val deadClient = BackendClient(
            baseUrl = "http://10.255.255.1:1/",
            timeoutSeconds = 1
        )
        val scorer = LocalScorer()

        val result = try {
            deadClient.verifyTransaction(TEST_USER_ID, samePersonVerification, baselineProfile)
        } catch (e: Exception) {
            scorer.score(samePersonVerification, baselineProfile, enrollmentSessions)
        }

        assertNotNull(result)
        assertTrue("Fallback score 0-100", result.riskScore in 0..100)
        assertTrue(result.riskLevel in listOf("LOW", "MEDIUM", "HIGH"))
        assertTrue(result.recommendation in listOf("APPROVE", "STEP_UP_AUTH", "BLOCK"))
    }

    @Test
    fun `REQ-04 backend timeout - LocalScorer fallback works`() = runTest {
        mockWebServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val slowClient = BackendClient(
            baseUrl = mockWebServer.url("/").toString(),
            timeoutSeconds = 1
        )
        val scorer = LocalScorer()

        val result = try {
            slowClient.verifyTransaction(TEST_USER_ID, samePersonVerification, baselineProfile)
        } catch (e: Exception) {
            scorer.score(samePersonVerification, baselineProfile, enrollmentSessions)
        }

        assertNotNull(result)
        assertTrue(result.riskScore in 0..100)
    }

    @Test
    fun `REQ-04 backend 503 - LocalScorer fallback works`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(503).setBody("Service Unavailable"))
        val scorer = LocalScorer()

        val result = try {
            client.verifyTransaction(TEST_USER_ID, samePersonVerification, baselineProfile)
        } catch (e: Exception) {
            scorer.score(samePersonVerification, baselineProfile, enrollmentSessions)
        }

        assertNotNull(result)
        assertTrue(result.riskScore in 0..100)
    }

    @Test
    fun `REQ-04 LocalScorer detects anomalies for fraudster features`() {
        val scorer = LocalScorer()
        val result = scorer.score(differentPersonVerification, baselineProfile, enrollmentSessions)

        assertTrue("Fraudster risk > 0", result.riskScore > 0)
        assertTrue("Should detect anomalies", result.anomalies.isNotEmpty())
    }

    @Test
    fun `REQ-04 LocalScorer returns valid result for same person`() {
        val scorer = LocalScorer()
        val result = scorer.score(samePersonVerification, baselineProfile, enrollmentSessions)

        assertNotNull(result)
        assertTrue(result.riskScore in 0..100)
    }

    @Test
    fun `REQ-04 backend disconnect mid-response - fallback works`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        )
        val scorer = LocalScorer()

        val result = try {
            client.verifyTransaction(TEST_USER_ID, samePersonVerification, baselineProfile)
        } catch (e: Exception) {
            scorer.score(samePersonVerification, baselineProfile, enrollmentSessions)
        }

        assertNotNull(result)
        assertTrue(result.riskScore in 0..100)
    }

    // =========================================================================
    // REQ-05: Latency — responses complete within timeout
    // =========================================================================

    @Test
    fun `REQ-05 enrollment response completes within timeout`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"status":"pending","enrollment_count":1,"remaining":2}""")
                .setResponseCode(200)
        )

        val start = System.currentTimeMillis()
        client.enrollSession(TEST_USER_ID, E2ETestFixtures.enrollmentSession1)
        val elapsed = System.currentTimeMillis() - start

        assertTrue("Enrollment < 5000ms, took ${elapsed}ms", elapsed < 5000)
    }

    @Test
    fun `REQ-05 verification response completes within timeout`() = runTest {
        enqueueVerification(15, "LOW", "APPROVE")

        val start = System.currentTimeMillis()
        client.verifyTransaction(TEST_USER_ID, samePersonVerification, baselineProfile)
        val elapsed = System.currentTimeMillis() - start

        assertTrue("Verification < 5000ms, took ${elapsed}ms", elapsed < 5000)
    }

    @Test
    fun `REQ-05 full 3-enrollment flow completes within timeout`() = runTest {
        enqueueFullEnrollment()

        val start = System.currentTimeMillis()
        repeat(3) { i ->
            client.enrollSession(TEST_USER_ID, enrollmentSessions[i])
        }
        val elapsed = System.currentTimeMillis() - start

        assertTrue("3 enrollments < 15000ms, took ${elapsed}ms", elapsed < 15000)
    }

    @Test
    fun `REQ-05 slow server responds within timeout succeeds`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"status":"pending","enrollment_count":1,"remaining":2}""")
                .setResponseCode(200)
                .setBodyDelay(500, java.util.concurrent.TimeUnit.MILLISECONDS)
        )

        val result = client.enrollSession(TEST_USER_ID, E2ETestFixtures.enrollmentSession1)
        assertNotNull(result)
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun enqueueFullEnrollment() {
        mockWebServer.enqueue(MockResponse().setBody("""{"status":"pending","enrollment_count":1,"remaining":2}""").setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setBody("""{"status":"pending","enrollment_count":2,"remaining":1}""").setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setBody("""{"status":"completed","enrollment_count":3,"remaining":0,"profile":${gson.toJson(baselineProfile)},"profile_summary":"Consistent typing."}""").setResponseCode(200))
    }

    private fun enqueueVerification(riskScore: Int, riskLevel: String, recommendation: String) {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"risk_score":$riskScore,"risk_level":"$riskLevel","anomalies":[],"explanation":"Score $riskScore","recommendation":"$recommendation","trace_id":"t1","model":"m1","latency_ms":100}""")
                .setResponseCode(200)
        )
    }
}
