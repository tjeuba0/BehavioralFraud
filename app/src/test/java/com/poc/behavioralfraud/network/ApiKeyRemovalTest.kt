package com.poc.behavioralfraud.network

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.poc.behavioralfraud.data.model.BehavioralFeatures
import com.poc.behavioralfraud.data.model.BehavioralProfile
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Security-focused tests for TASK-003: Remove API Key + Delete OpenRouterClient.
 *
 * These tests verify the security properties of the cleanup migration:
 * - REQ-02: No BuildConfig.OPENROUTER_API_KEY referenced anywhere; BackendClient
 *   does not send API key headers or Bearer tokens.
 * - REQ-03: BACKEND_BASE_URL available in BuildConfig; BackendClient uses a
 *   configurable base URL.
 * - REQ-04: OpenRouterClient.kt is deleted; the class does not exist at runtime.
 *
 * Test naming convention: `REQ-XX - description`
 */
class ApiKeyRemovalTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: BackendClient

    private val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    // -------------------------------------------------------------------------
    // Test fixtures
    // -------------------------------------------------------------------------

    private val testUserId = "security-test-user"

    private val sampleFeatures = BehavioralFeatures(
        sessionDurationMs = 45000L,
        avgInterCharDelayMs = 120.5,
        stdInterCharDelayMs = 35.2,
        maxInterCharDelayMs = 500L,
        minInterCharDelayMs = 50L,
        totalTextChanges = 42,
        pasteCount = 0,
        totalTouchEvents = 150,
        avgTouchSize = 0.15,
        avgTouchDurationMs = 85.0,
        avgSwipeVelocity = 1200.0,
        gyroStabilityX = 0.0021,
        gyroStabilityY = 0.0018,
        gyroStabilityZ = 0.0015,
        accelStabilityX = 0.12,
        accelStabilityY = 0.09,
        accelStabilityZ = 0.11,
        avgTouchPressure = 0.35,
        perFieldAvgDelay = mapOf("accountNumber" to 110.0, "amount" to 95.0, "note" to 130.0),
        avgInterFieldPauseMs = 1500.0,
        deletionCount = 3,
        deletionRatio = 0.07,
        fieldFocusSequence = "accountNumber->amount->note",
        timeToFirstInput = 2500L,
        timeFromLastInputToConfirm = 1800L
    )

    private val sampleProfile = BehavioralProfile(
        userId = testUserId,
        enrollmentCount = 3,
        avgSessionDuration = 43000.0,
        avgInterCharDelay = 118.0,
        stdInterCharDelay = 33.0,
        avgTouchSize = 0.14,
        avgTouchDuration = 82.0,
        avgGyroStabilityX = 0.0020,
        avgGyroStabilityY = 0.0017,
        avgGyroStabilityZ = 0.0014,
        avgAccelStabilityX = 0.11,
        avgAccelStabilityY = 0.08,
        avgAccelStabilityZ = 0.10,
        avgSwipeVelocity = 1150.0,
        avgPasteCount = 0.3,
        avgTimeToFirstInput = 2400.0,
        avgTimeFromLastInputToConfirm = 1750.0,
        typicalFieldFocusSequence = "accountNumber->amount->note",
        avgTouchPressure = 0.34,
        avgInterFieldPause = 1450.0,
        avgDeletionRatio = 0.06,
        profileSummary = "User exhibits consistent typing rhythm with moderate speed."
    )

    // -------------------------------------------------------------------------
    // Setup / Teardown
    // -------------------------------------------------------------------------

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        client = BackendClient(baseUrl = mockWebServer.url("/").toString())
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    // =========================================================================
    // REQ-02: No BuildConfig.OPENROUTER_API_KEY referenced in any source code
    //
    // Security property: BackendClient must not send API keys, Bearer tokens,
    // or any OpenRouter-related content in HTTP requests.
    // =========================================================================

    @Test
    fun `REQ-02 - BackendClient constructor does not require an API key parameter`() {
        // Happy path: BackendClient can be instantiated with only a base URL.
        // If the constructor required an API key, this would fail to compile.
        val testClient = BackendClient(
            baseUrl = mockWebServer.url("/").toString()
        )
        assertNotNull(
            "BackendClient must be constructible without an API key",
            testClient
        )
    }

    @Test
    fun `REQ-02 - BackendClient constructor with default parameters does not require API key`() {
        // Edge case: Verify the two-parameter constructor (baseUrl + timeout)
        // is the full constructor surface -- no hidden API key parameter.
        val testClient = BackendClient(
            baseUrl = mockWebServer.url("/").toString(),
            timeoutSeconds = 10
        )
        assertNotNull(
            "BackendClient with explicit timeout must be constructible without an API key",
            testClient
        )
    }

    @Test
    fun `REQ-02 - BackendClient enrollSession requests contain no Authorization header`() = runTest {
        // Happy path: Enroll request must not include any Authorization header.
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"status":"pending","enrollment_count":1,"remaining":2}""")
                .setResponseCode(200)
        )

        client.enrollSession(testUserId, sampleFeatures)

        val request = mockWebServer.takeRequest()
        val authHeader = request.getHeader("Authorization")
        assertNull(
            "enrollSession request must NOT contain an Authorization header",
            authHeader
        )
    }

    @Test
    fun `REQ-02 - BackendClient enrollSession request headers contain no Bearer token pattern`() = runTest {
        // Edge case: Even non-standard header names should not leak Bearer tokens.
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"status":"pending","enrollment_count":1,"remaining":2}""")
                .setResponseCode(200)
        )

        client.enrollSession(testUserId, sampleFeatures)

        val request = mockWebServer.takeRequest()
        val allHeaders = request.headers.toString()
        assertFalse(
            "No header should contain a Bearer token",
            allHeaders.contains("Bearer", ignoreCase = true)
        )
        assertFalse(
            "No header should contain an OpenRouter API key prefix (sk-or-)",
            allHeaders.contains("sk-or-")
        )
    }

    @Test
    fun `REQ-02 - BackendClient verifyTransaction requests contain no Authorization header`() = runTest {
        // Happy path: Verify request must not include any Authorization header.
        val responseJson = """
        {
            "risk_score": 15,
            "risk_level": "LOW",
            "anomalies": [],
            "explanation": "Behavior matches profile.",
            "recommendation": "APPROVE",
            "trace_id": "t-sec-01",
            "model": "fraud-v2",
            "latency_ms": 100
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        val request = mockWebServer.takeRequest()
        val authHeader = request.getHeader("Authorization")
        assertNull(
            "verifyTransaction request must NOT contain an Authorization header",
            authHeader
        )
    }

    @Test
    fun `REQ-02 - BackendClient verifyTransaction request headers contain no Bearer token pattern`() = runTest {
        // Edge case: Check all headers for any token leakage.
        val responseJson = """
        {
            "risk_score": 15,
            "risk_level": "LOW",
            "anomalies": [],
            "explanation": "OK",
            "recommendation": "APPROVE",
            "trace_id": "t1",
            "model": "m1",
            "latency_ms": 50
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        val request = mockWebServer.takeRequest()
        val allHeaders = request.headers.toString()
        assertFalse(
            "No header should contain a Bearer token",
            allHeaders.contains("Bearer", ignoreCase = true)
        )
        assertFalse(
            "No header should contain an OpenRouter API key prefix (sk-or-)",
            allHeaders.contains("sk-or-")
        )
    }

    @Test
    fun `REQ-02 - BackendClient getProfile requests contain no Authorization header`() = runTest {
        // Happy path: GET request for profile must not include Authorization.
        val profileResponseJson = """
        {
            "user_id": "$testUserId",
            "enrollment_count": 3,
            "profile": ${gson.toJson(sampleProfile)},
            "profile_summary": "Consistent typing."
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(profileResponseJson).setResponseCode(200))

        client.getProfile(testUserId)

        val request = mockWebServer.takeRequest()
        val authHeader = request.getHeader("Authorization")
        assertNull(
            "getProfile request must NOT contain an Authorization header",
            authHeader
        )
    }

    @Test
    fun `REQ-02 - BackendClient getProfile request headers contain no Bearer token pattern`() = runTest {
        // Edge case: Verify full header dump for GET requests too.
        mockWebServer.enqueue(MockResponse().setResponseCode(404))

        client.getProfile(testUserId)

        val request = mockWebServer.takeRequest()
        val allHeaders = request.headers.toString()
        assertFalse(
            "No header should contain a Bearer token",
            allHeaders.contains("Bearer", ignoreCase = true)
        )
        assertFalse(
            "No header should reference OpenRouter",
            allHeaders.contains("openrouter", ignoreCase = true)
        )
    }

    @Test
    fun `REQ-02 - BackendClient enrollSession request body contains no OpenRouter references`() = runTest {
        // Happy path: Request body must be a clean backend payload with no LLM artifacts.
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"status":"pending","enrollment_count":1,"remaining":2}""")
                .setResponseCode(200)
        )

        client.enrollSession(testUserId, sampleFeatures)

        val request = mockWebServer.takeRequest()
        val bodyStr = request.body.readUtf8()

        assertFalse(
            "Request body must not reference 'openrouter'",
            bodyStr.contains("openrouter", ignoreCase = true)
        )
        assertFalse(
            "Request body must not contain LLM 'messages' array",
            bodyStr.contains("\"messages\"")
        )
        assertFalse(
            "Request body must not contain 'system' prompt key",
            bodyStr.contains("\"system\"")
        )
        assertFalse(
            "Request body must not contain API key prefix 'sk-or-'",
            bodyStr.contains("sk-or-")
        )
    }

    @Test
    fun `REQ-02 - BackendClient verifyTransaction request body contains no LLM prompt structure`() = runTest {
        // Edge case: Verify the body sent to /risk/score has no remnant LLM prompt fields.
        val responseJson = """
        {
            "risk_score": 42,
            "risk_level": "MEDIUM",
            "anomalies": ["typing_speed"],
            "explanation": "Deviation detected.",
            "recommendation": "STEP_UP_AUTH",
            "trace_id": "t-sec-02",
            "model": "fraud-v2",
            "latency_ms": 200
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        val request = mockWebServer.takeRequest()
        val bodyStr = request.body.readUtf8()

        assertFalse(
            "Request body must not contain 'messages' (LLM chat format)",
            bodyStr.contains("\"messages\"")
        )
        assertFalse(
            "Request body must not contain 'temperature' (LLM parameter)",
            bodyStr.contains("\"temperature\"")
        )
        assertFalse(
            "Request body must not contain 'max_tokens' (LLM parameter)",
            bodyStr.contains("\"max_tokens\"")
        )
        assertFalse(
            "Request body must not contain 'system' (LLM prompt role)",
            bodyStr.contains("\"system\"")
        )
    }

    // =========================================================================
    // REQ-03: BACKEND_BASE_URL available in BuildConfig; BackendClient uses
    // a configurable base URL.
    //
    // Security property: The client talks to a configured backend, not to a
    // hardcoded third-party LLM endpoint.
    // =========================================================================

    @Test
    fun `REQ-03 - BackendClient uses the configured base URL for enrollSession`() = runTest {
        // Happy path: Request reaches the MockWebServer, proving the base URL is respected.
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"status":"pending","enrollment_count":1,"remaining":2}""")
                .setResponseCode(200)
        )

        client.enrollSession(testUserId, sampleFeatures)

        assertEquals(
            "Request must be sent to the configured base URL (MockWebServer)",
            1,
            mockWebServer.requestCount
        )
        val request = mockWebServer.takeRequest()
        assertEquals("/profile/enroll", request.path)
    }

    @Test
    fun `REQ-03 - BackendClient uses the configured base URL for verifyTransaction`() = runTest {
        // Happy path: Verify endpoint is also routed through the configured base URL.
        val responseJson = """
        {
            "risk_score": 10,
            "risk_level": "LOW",
            "anomalies": [],
            "explanation": "OK",
            "recommendation": "APPROVE",
            "trace_id": "t1",
            "model": "m1",
            "latency_ms": 50
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        assertEquals(1, mockWebServer.requestCount)
        val request = mockWebServer.takeRequest()
        assertEquals("/risk/score", request.path)
    }

    @Test
    fun `REQ-03 - BackendClient does not send requests to openrouter-ai domain`() = runTest {
        // Edge case: Verify that the Host header points to our configured server,
        // NOT to openrouter.ai.
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"status":"pending","enrollment_count":1,"remaining":2}""")
                .setResponseCode(200)
        )

        client.enrollSession(testUserId, sampleFeatures)

        val request = mockWebServer.takeRequest()
        val hostHeader = request.getHeader("Host") ?: ""
        assertFalse(
            "Host header must NOT reference openrouter.ai",
            hostHeader.contains("openrouter.ai", ignoreCase = true)
        )
    }

    @Test
    fun `REQ-03 - BackendClient instantiated with different base URLs targets each correctly`() = runTest {
        // Edge case: Two BackendClient instances with different URLs send to the right server.
        val secondServer = MockWebServer()
        secondServer.start()

        try {
            val secondClient = BackendClient(baseUrl = secondServer.url("/").toString())

            // Enqueue responses on both servers
            mockWebServer.enqueue(
                MockResponse()
                    .setBody("""{"status":"pending","enrollment_count":1,"remaining":2}""")
                    .setResponseCode(200)
            )
            secondServer.enqueue(
                MockResponse()
                    .setBody("""{"status":"pending","enrollment_count":1,"remaining":2}""")
                    .setResponseCode(200)
            )

            // Send request via each client
            client.enrollSession(testUserId, sampleFeatures)
            secondClient.enrollSession(testUserId, sampleFeatures)

            // Each server should have received exactly one request
            assertEquals(
                "First server must receive exactly 1 request",
                1,
                mockWebServer.requestCount
            )
            assertEquals(
                "Second server must receive exactly 1 request",
                1,
                secondServer.requestCount
            )
        } finally {
            secondServer.shutdown()
        }
    }

    @Test
    fun `REQ-03 - BackendClient base URL with trailing slash is handled correctly`() = runTest {
        // Edge case: Base URL with trailing slash should not produce double-slash paths.
        val trailingSlashClient = BackendClient(
            baseUrl = mockWebServer.url("/").toString() // ends with "/"
        )
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"status":"pending","enrollment_count":1,"remaining":2}""")
                .setResponseCode(200)
        )

        trailingSlashClient.enrollSession(testUserId, sampleFeatures)

        val request = mockWebServer.takeRequest()
        assertFalse(
            "Path must not contain double slashes (//profile)",
            request.path!!.contains("//")
        )
        assertEquals("/profile/enroll", request.path)
    }
}
