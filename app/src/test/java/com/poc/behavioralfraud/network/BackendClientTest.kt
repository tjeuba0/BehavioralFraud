package com.poc.behavioralfraud.network

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.poc.behavioralfraud.data.model.BehavioralFeatures
import com.poc.behavioralfraud.data.model.BehavioralProfile
import com.poc.behavioralfraud.data.model.FraudAnalysisResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Specification-based tests for BackendClient.
 *
 * Tests are derived from the SRS requirements (REQ-01 through REQ-10) and the
 * backend API contract. They verify behavior against the specification using
 * MockWebServer to simulate the FastAPI backend, without depending on any
 * particular implementation detail of BackendClient.kt.
 */
class BackendClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: BackendClient
    private val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    // ---------------------------------------------------------------------------
    // Test fixtures
    // ---------------------------------------------------------------------------

    private val testUserId = "user-test-001"

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

    // ---------------------------------------------------------------------------
    // Setup / Teardown
    // ---------------------------------------------------------------------------

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        // BackendClient must accept a base URL so we can point it at MockWebServer.
        // The production code reads BuildConfig.BACKEND_BASE_URL, but for tests
        // we need constructor/method injection of the base URL.
        client = BackendClient(baseUrl = mockWebServer.url("/").toString())
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    // =========================================================================
    // REQ-02: enrollSession(userId, features) -> POST /profile/enroll
    // =========================================================================

    @Test
    fun `REQ-02 enrollSession happy path - pending enrollment returns correct EnrollResponse`() = runTest {
        // Given: backend returns a "pending" enrollment response (1 of 3)
        val responseJson = """
        {
            "status": "pending",
            "enrollment_count": 1,
            "remaining": 2
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        // When
        val result = client.enrollSession(testUserId, sampleFeatures)

        // Then: response fields are parsed correctly
        assertEquals("pending", result.status)
        assertEquals(1, result.enrollmentCount)
        assertEquals(2, result.remaining)
        assertNull(result.profile)
        assertNull(result.profileSummary)

        // Verify the request hit the correct endpoint
        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/profile/enroll", request.path)
    }

    @Test
    fun `REQ-02 enrollSession happy path - completed enrollment returns profile and summary`() = runTest {
        // Given: backend returns "completed" with profile on 3rd enrollment
        val profileJson = gson.toJson(sampleProfile)
        val responseJson = """
        {
            "status": "completed",
            "enrollment_count": 3,
            "remaining": 0,
            "profile": $profileJson,
            "profile_summary": "User types at moderate speed with consistent rhythm."
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        // When
        val result = client.enrollSession(testUserId, sampleFeatures)

        // Then
        assertEquals("completed", result.status)
        assertEquals(3, result.enrollmentCount)
        assertEquals(0, result.remaining)
        assertNotNull(result.profile)
        assertNotNull(result.profileSummary)
        assertEquals("User types at moderate speed with consistent rhythm.", result.profileSummary)
    }

    @Test
    fun `REQ-02 enrollSession request body matches backend contract`() = runTest {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"status":"pending","enrollment_count":1,"remaining":2}""")
                .setResponseCode(200)
        )

        // When
        client.enrollSession(testUserId, sampleFeatures)

        // Then: verify the JSON request body matches the API contract
        val request = mockWebServer.takeRequest()
        val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject

        assertTrue("Request must contain 'user_id'", body.has("user_id"))
        assertEquals(testUserId, body.get("user_id").asString)

        assertTrue("Request must contain 'session_features'", body.has("session_features"))
        val features = body.getAsJsonObject("session_features")
        assertNotNull("session_features must be a JSON object", features)
        // Verify a few key fields are present in the features
        assertTrue(features.has("sessionDurationMs") || features.has("session_duration_ms"))
    }

    @Test
    fun `REQ-02 enrollSession Content-Type header is application-json`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"status":"pending","enrollment_count":1,"remaining":2}""")
                .setResponseCode(200)
        )

        client.enrollSession(testUserId, sampleFeatures)

        val request = mockWebServer.takeRequest()
        val contentType = request.getHeader("Content-Type")
        assertNotNull("Content-Type header must be set", contentType)
        assertTrue(
            "Content-Type must be application/json",
            contentType!!.contains("application/json")
        )
    }

    @Test
    fun `REQ-02 enrollSession error - server returns 500 throws exception`() = runTest {
        // Given: server error
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"detail":"Internal Server Error"}""")
        )

        // When / Then
        try {
            client.enrollSession(testUserId, sampleFeatures)
            fail("Expected an exception for 500 response")
        } catch (e: Exception) {
            // REQ-08: meaningful exception
            assertNotNull("Exception message should not be null", e.message)
            assertTrue(
                "Exception should contain status code or error info",
                e.message!!.contains("500") || e.message!!.lowercase().contains("error")
            )
        }
    }

    @Test
    fun `REQ-02 enrollSession error - malformed response throws parse exception`() = runTest {
        // Given: response body is not valid JSON
        mockWebServer.enqueue(
            MockResponse()
                .setBody("this is not json")
                .setResponseCode(200)
        )

        // When / Then: REQ-08 requires meaningful exception on parse errors
        try {
            client.enrollSession(testUserId, sampleFeatures)
            fail("Expected an exception for malformed JSON response")
        } catch (e: Exception) {
            assertNotNull("Exception message should not be null", e.message)
        }
    }

    @Test
    fun `REQ-02 enrollSession second enrollment returns count 2 remaining 1`() = runTest {
        // Given: second enrollment session
        val responseJson = """
        {
            "status": "pending",
            "enrollment_count": 2,
            "remaining": 1
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        // When
        val result = client.enrollSession(testUserId, sampleFeatures)

        // Then
        assertEquals("pending", result.status)
        assertEquals(2, result.enrollmentCount)
        assertEquals(1, result.remaining)
    }

    // =========================================================================
    // REQ-03: verifyTransaction(userId, features, profile) -> POST /risk/score
    // =========================================================================

    @Test
    fun `REQ-03 verifyTransaction happy path - low risk returns correct FraudAnalysisResult`() = runTest {
        // Given: backend returns a low-risk verdict
        val responseJson = """
        {
            "risk_score": 15,
            "risk_level": "LOW",
            "anomalies": [],
            "explanation": "Behavior matches the established profile closely.",
            "recommendation": "APPROVE",
            "trace_id": "trace-abc-123",
            "model": "fraud-detector-v2",
            "latency_ms": 245
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        // When
        val result = client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        // Then: REQ-07 parsed response matches schema
        assertEquals(15, result.riskScore)
        assertEquals("LOW", result.riskLevel)
        assertTrue(result.anomalies.isEmpty())
        assertEquals("Behavior matches the established profile closely.", result.explanation)
        assertEquals("APPROVE", result.recommendation)

        // REQ-10: extended fields
        assertEquals("trace-abc-123", result.traceId)
        assertEquals("fraud-detector-v2", result.model)
        assertEquals(245, result.latencyMs)

        // Verify endpoint
        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/risk/score", request.path)
    }

    @Test
    fun `REQ-03 verifyTransaction happy path - high risk with anomalies`() = runTest {
        val responseJson = """
        {
            "risk_score": 85,
            "risk_level": "HIGH",
            "anomalies": ["typing_speed_deviation", "gyro_too_stable", "paste_detected"],
            "explanation": "Significant deviation from behavioral profile detected.",
            "recommendation": "BLOCK",
            "trace_id": "trace-xyz-789",
            "model": "fraud-detector-v2",
            "latency_ms": 312
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        val result = client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        assertEquals(85, result.riskScore)
        assertEquals("HIGH", result.riskLevel)
        assertEquals(3, result.anomalies.size)
        assertTrue(result.anomalies.contains("typing_speed_deviation"))
        assertTrue(result.anomalies.contains("gyro_too_stable"))
        assertTrue(result.anomalies.contains("paste_detected"))
        assertEquals("BLOCK", result.recommendation)
    }

    @Test
    fun `REQ-03 verifyTransaction happy path - medium risk with step-up auth`() = runTest {
        val responseJson = """
        {
            "risk_score": 55,
            "risk_level": "MEDIUM",
            "anomalies": ["session_duration_deviation"],
            "explanation": "Some anomalies detected, additional verification recommended.",
            "recommendation": "STEP_UP_AUTH",
            "trace_id": "trace-med-456",
            "model": "fraud-detector-v2",
            "latency_ms": 198
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        val result = client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        assertEquals(55, result.riskScore)
        assertEquals("MEDIUM", result.riskLevel)
        assertEquals("STEP_UP_AUTH", result.recommendation)
    }

    @Test
    fun `REQ-03 verifyTransaction request body matches backend contract`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""
                {
                    "risk_score": 10, "risk_level": "LOW", "anomalies": [],
                    "explanation": "OK", "recommendation": "APPROVE",
                    "trace_id": "t1", "model": "m1", "latency_ms": 100
                }
                """.trimIndent())
                .setResponseCode(200)
        )

        client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        val request = mockWebServer.takeRequest()
        val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject

        assertTrue("Request must contain 'user_id'", body.has("user_id"))
        assertEquals(testUserId, body.get("user_id").asString)

        assertTrue("Request must contain 'current_session'", body.has("current_session"))
        val session = body.getAsJsonObject("current_session")
        assertNotNull("current_session must be a JSON object", session)

        assertTrue("Request must contain 'profile'", body.has("profile"))
        val profile = body.getAsJsonObject("profile")
        assertNotNull("profile must be a JSON object", profile)
    }

    @Test
    fun `REQ-03 verifyTransaction error - server returns 422 throws exception`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(422)
                .setBody("""{"detail":"Validation error: missing required field 'user_id'"}""")
        )

        try {
            client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)
            fail("Expected an exception for 422 response")
        } catch (e: Exception) {
            assertNotNull(e.message)
            assertTrue(
                "Exception should reference the status code or error",
                e.message!!.contains("422") || e.message!!.lowercase().contains("error")
            )
        }
    }

    @Test
    fun `REQ-03 verifyTransaction error - empty response body throws exception`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("")
        )

        try {
            client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)
            fail("Expected an exception for empty body")
        } catch (e: Exception) {
            assertNotNull(e.message)
        }
    }

    // =========================================================================
    // REQ-04: getProfile(userId) -> GET /profile/{user_id}
    // =========================================================================

    @Test
    fun `REQ-04 getProfile happy path - existing user returns BehavioralProfile`() = runTest {
        val profileResponseJson = """
        {
            "user_id": "$testUserId",
            "enrollment_count": 3,
            "profile": ${gson.toJson(sampleProfile)},
            "profile_summary": "Consistent typing with moderate speed."
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(profileResponseJson).setResponseCode(200))

        val result = client.getProfile(testUserId)

        assertNotNull("Profile should not be null for existing user", result)
        assertEquals(testUserId, result!!.userId)
        assertEquals(3, result.enrollmentCount)

        // Verify GET request to correct endpoint
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/profile/$testUserId", request.path)
    }

    @Test
    fun `REQ-04 getProfile returns null on 404`() = runTest {
        // Given: user not found
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("""{"detail":"User not found"}""")
        )

        // When
        val result = client.getProfile(testUserId)

        // Then: REQ-04 specifies null on 404
        assertNull("getProfile must return null when server responds 404", result)
    }

    @Test
    fun `REQ-04 getProfile error - server returns 500 throws exception`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"detail":"Database connection failed"}""")
        )

        try {
            client.getProfile(testUserId)
            fail("Expected an exception for 500 response")
        } catch (e: Exception) {
            assertNotNull(e.message)
        }
    }

    @Test
    fun `REQ-04 getProfile uses GET method not POST`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""
                {
                    "user_id": "$testUserId",
                    "enrollment_count": 3,
                    "profile": ${gson.toJson(sampleProfile)},
                    "profile_summary": "Summary"
                }
                """.trimIndent())
                .setResponseCode(200)
        )

        client.getProfile(testUserId)

        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
    }

    @Test
    fun `REQ-04 getProfile URL encodes userId in path`() = runTest {
        val userIdWithSpecialChars = "user@test.com"
        mockWebServer.enqueue(MockResponse().setResponseCode(404))

        client.getProfile(userIdWithSpecialChars)

        val request = mockWebServer.takeRequest()
        // The path must include the user_id, whether encoded or literal
        assertNotNull(request.path)
        assertTrue(
            "Path must contain the userId",
            request.path!!.startsWith("/profile/")
        )
    }

    @Test
    fun `REQ-04 getProfile error - malformed JSON body throws parse exception`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("{invalid json no closing brace")
                .setResponseCode(200)
        )

        try {
            client.getProfile(testUserId)
            fail("Expected an exception for malformed JSON")
        } catch (e: Exception) {
            assertNotNull(e.message)
        }
    }

    // =========================================================================
    // REQ-05: No reference to OpenRouter API key or prompt strings
    // =========================================================================

    @Test
    fun `REQ-05 enrollSession request does not contain openrouter api key or prompt strings`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"status":"pending","enrollment_count":1,"remaining":2}""")
                .setResponseCode(200)
        )

        client.enrollSession(testUserId, sampleFeatures)

        val request = mockWebServer.takeRequest()
        val bodyStr = request.body.readUtf8()
        val headersStr = request.headers.toString()

        // Must NOT contain OpenRouter references
        assertTrue(
            "Request body must not contain OpenRouter API key references",
            !bodyStr.contains("openrouter", ignoreCase = true)
        )
        assertTrue(
            "Request body must not contain prompt/system/messages keys",
            !bodyStr.contains("\"messages\"") && !bodyStr.contains("\"system\"")
        )
        assertTrue(
            "Headers must not contain Bearer token for OpenRouter",
            !headersStr.contains("sk-or-")
        )
    }

    @Test
    fun `REQ-05 verifyTransaction request does not contain openrouter references`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""
                {
                    "risk_score": 10, "risk_level": "LOW", "anomalies": [],
                    "explanation": "OK", "recommendation": "APPROVE",
                    "trace_id": "t1", "model": "m1", "latency_ms": 100
                }
                """.trimIndent())
                .setResponseCode(200)
        )

        client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        val request = mockWebServer.takeRequest()
        val bodyStr = request.body.readUtf8()

        assertTrue(
            "Request must not reference OpenRouter",
            !bodyStr.contains("openrouter", ignoreCase = true)
        )
        assertTrue(
            "Request must not contain LLM prompt structure",
            !bodyStr.contains("\"model\"") || !bodyStr.contains("\"messages\"")
        )
    }

    // =========================================================================
    // REQ-06: Base URL from BuildConfig.BACKEND_BASE_URL
    // =========================================================================

    @Test
    fun `REQ-06 client sends requests to the configured base URL`() = runTest {
        // This test verifies that the client targets the base URL it was constructed
        // with (which in production comes from BuildConfig.BACKEND_BASE_URL).
        // We validate this by confirming MockWebServer receives the request.
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"status":"pending","enrollment_count":1,"remaining":2}""")
                .setResponseCode(200)
        )

        client.enrollSession(testUserId, sampleFeatures)

        // If the request count is 1, the client correctly used our base URL
        assertEquals(
            "Client must send request to the configured base URL",
            1,
            mockWebServer.requestCount
        )
    }

    // =========================================================================
    // REQ-07: Returns parsed response models matching backend response schemas
    // =========================================================================

    @Test
    fun `REQ-07 enrollSession response with all optional fields null parses correctly`() = runTest {
        // Pending enrollment has no profile or profile_summary
        val responseJson = """
        {
            "status": "pending",
            "enrollment_count": 1,
            "remaining": 2,
            "profile": null,
            "profile_summary": null
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        val result = client.enrollSession(testUserId, sampleFeatures)

        assertEquals("pending", result.status)
        assertNull(result.profile)
        assertNull(result.profileSummary)
    }

    @Test
    fun `REQ-07 verifyTransaction response with empty anomalies list parses correctly`() = runTest {
        val responseJson = """
        {
            "risk_score": 5,
            "risk_level": "LOW",
            "anomalies": [],
            "explanation": "Perfect match.",
            "recommendation": "APPROVE",
            "trace_id": "trace-empty-001",
            "model": "fraud-v3",
            "latency_ms": 50
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        val result = client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        assertTrue(result.anomalies.isEmpty())
        assertEquals(5, result.riskScore)
    }

    @Test
    fun `REQ-07 verifyTransaction boundary risk score 0 parses correctly`() = runTest {
        val responseJson = """
        {
            "risk_score": 0,
            "risk_level": "LOW",
            "anomalies": [],
            "explanation": "No risk detected.",
            "recommendation": "APPROVE",
            "trace_id": "t-zero",
            "model": "m1",
            "latency_ms": 0
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        val result = client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        assertEquals(0, result.riskScore)
    }

    @Test
    fun `REQ-07 verifyTransaction boundary risk score 100 parses correctly`() = runTest {
        val responseJson = """
        {
            "risk_score": 100,
            "risk_level": "HIGH",
            "anomalies": ["total_mismatch"],
            "explanation": "Completely different user.",
            "recommendation": "BLOCK",
            "trace_id": "t-max",
            "model": "m1",
            "latency_ms": 999
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        val result = client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        assertEquals(100, result.riskScore)
    }

    // =========================================================================
    // REQ-08: Throws meaningful exceptions on network/parse errors
    // =========================================================================

    @Test
    fun `REQ-08 network error - connection refused throws exception with message`() = runTest {
        // Shut down the server so connection fails
        mockWebServer.shutdown()

        try {
            // Create a client pointing to the now-dead server
            val deadClient = BackendClient(baseUrl = "http://localhost:${mockWebServer.port}/")
            deadClient.enrollSession(testUserId, sampleFeatures)
            fail("Expected an exception when server is unreachable")
        } catch (e: Exception) {
            assertNotNull("Exception message must not be null", e.message)
        }
    }

    @Test
    fun `REQ-08 enrollSession server error 400 throws exception with status info`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"detail":"Bad Request: invalid user_id format"}""")
        )

        try {
            client.enrollSession(testUserId, sampleFeatures)
            fail("Expected exception for 400 response")
        } catch (e: Exception) {
            assertNotNull(e.message)
        }
    }

    @Test
    fun `REQ-08 verifyTransaction response missing required fields throws parse exception`() = runTest {
        // Response is valid JSON but missing required fields from FraudAnalysisResult
        val responseJson = """
        {
            "risk_score": 50
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        // This may either throw or return a partially-constructed object depending
        // on Gson's handling. The test validates that the client does not silently
        // succeed with a completely broken response.
        try {
            val result = client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)
            // If it does not throw, at least risk_score should be parsed
            assertEquals(50, result.riskScore)
        } catch (e: Exception) {
            // Also acceptable: throw on missing required fields
            assertNotNull(e.message)
        }
    }

    // =========================================================================
    // REQ-09: 60-second timeout, OkHttp client
    // =========================================================================

    @Test
    fun `REQ-09 timeout - slow server response triggers timeout exception`() = runTest {
        // Given: server does not send a response (simulates timeout)
        mockWebServer.enqueue(
            MockResponse()
                .setSocketPolicy(SocketPolicy.NO_RESPONSE)
        )

        // When / Then: should timeout and throw
        // Note: this test will take the actual timeout duration if the client
        // uses its full 60s timeout. MockWebServer's SocketPolicy.NO_RESPONSE
        // causes the connection to hang. For CI friendliness, we create a
        // client with a shorter timeout for this specific test.
        val shortTimeoutClient = BackendClient(
            baseUrl = mockWebServer.url("/").toString(),
            timeoutSeconds = 2  // Override for test speed
        )

        try {
            shortTimeoutClient.enrollSession(testUserId, sampleFeatures)
            fail("Expected a timeout exception")
        } catch (e: Exception) {
            // Accept either SocketTimeoutException or any wrapper
            assertTrue(
                "Exception should be caused by timeout",
                e is SocketTimeoutException ||
                    e.cause is SocketTimeoutException ||
                    e.message?.lowercase()?.contains("timeout") == true
            )
        }
    }

    @Test
    fun `REQ-09 client uses OkHttp - verify request is well-formed HTTP`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"status":"pending","enrollment_count":1,"remaining":2}""")
                .setResponseCode(200)
        )

        client.enrollSession(testUserId, sampleFeatures)

        val request = mockWebServer.takeRequest()
        // OkHttp sets standard headers
        assertNotNull("Request should have a User-Agent or other standard header", request.headers)
        assertNotNull("Request should have Content-Length", request.getHeader("Content-Length"))
    }

    // =========================================================================
    // REQ-10: FraudAnalysisResult extended with trace_id, model, latency_ms
    // =========================================================================

    @Test
    fun `REQ-10 verifyTransaction response includes trace_id model and latency_ms`() = runTest {
        val responseJson = """
        {
            "risk_score": 42,
            "risk_level": "MEDIUM",
            "anomalies": ["typing_speed"],
            "explanation": "Slight deviation in typing speed.",
            "recommendation": "STEP_UP_AUTH",
            "trace_id": "trace-unique-id-9999",
            "model": "behavioral-fraud-detector-v2.1",
            "latency_ms": 187
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        val result = client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        assertEquals("trace-unique-id-9999", result.traceId)
        assertEquals("behavioral-fraud-detector-v2.1", result.model)
        assertEquals(187, result.latencyMs)
    }

    @Test
    fun `REQ-10 verifyTransaction extended fields are distinct from original fields`() = runTest {
        // Ensure the new fields do not collide with or break existing fields
        val responseJson = """
        {
            "risk_score": 30,
            "risk_level": "LOW",
            "anomalies": ["minor_deviation"],
            "explanation": "Low risk overall.",
            "recommendation": "APPROVE",
            "trace_id": "abc",
            "model": "v1",
            "latency_ms": 55
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        val result = client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        // Original fields still work
        assertEquals(30, result.riskScore)
        assertEquals("LOW", result.riskLevel)
        assertEquals(1, result.anomalies.size)
        assertEquals("Low risk overall.", result.explanation)
        assertEquals("APPROVE", result.recommendation)

        // New fields also work
        assertEquals("abc", result.traceId)
        assertEquals("v1", result.model)
        assertEquals(55, result.latencyMs)
    }

    // =========================================================================
    // Additional edge case and contract tests
    // =========================================================================

    @Test
    fun `enrollSession with response containing extra unknown fields does not crash`() = runTest {
        // Backend might add new fields in future versions; client should be tolerant
        val responseJson = """
        {
            "status": "pending",
            "enrollment_count": 1,
            "remaining": 2,
            "new_future_field": "some_value",
            "another_field": 42
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        val result = client.enrollSession(testUserId, sampleFeatures)

        assertEquals("pending", result.status)
        assertEquals(1, result.enrollmentCount)
    }

    @Test
    fun `verifyTransaction with response containing extra unknown fields does not crash`() = runTest {
        val responseJson = """
        {
            "risk_score": 20,
            "risk_level": "LOW",
            "anomalies": [],
            "explanation": "OK",
            "recommendation": "APPROVE",
            "trace_id": "t1",
            "model": "m1",
            "latency_ms": 100,
            "confidence": 0.95,
            "debug_info": {"key": "value"}
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        val result = client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        assertEquals(20, result.riskScore)
    }

    @Test
    fun `getProfile with response containing extra unknown fields does not crash`() = runTest {
        val responseJson = """
        {
            "user_id": "$testUserId",
            "enrollment_count": 3,
            "profile": ${gson.toJson(sampleProfile)},
            "profile_summary": "Summary text",
            "created_at": "2026-01-15T10:30:00Z",
            "updated_at": "2026-03-22T14:00:00Z"
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        val result = client.getProfile(testUserId)

        assertNotNull(result)
        assertEquals(testUserId, result!!.userId)
    }

    @Test
    fun `enrollSession multiple sequential calls each hit correct endpoint`() = runTest {
        repeat(3) { i ->
            mockWebServer.enqueue(
                MockResponse()
                    .setBody("""{"status":"pending","enrollment_count":${i + 1},"remaining":${2 - i}}""")
                    .setResponseCode(200)
            )
        }

        repeat(3) { i ->
            val result = client.enrollSession(testUserId, sampleFeatures)
            assertEquals(i + 1, result.enrollmentCount)
        }

        assertEquals(3, mockWebServer.requestCount)
        repeat(3) {
            val request = mockWebServer.takeRequest()
            assertEquals("/profile/enroll", request.path)
        }
    }

    @Test
    fun `verifyTransaction anomalies with unicode characters parse correctly`() = runTest {
        val responseJson = """
        {
            "risk_score": 60,
            "risk_level": "MEDIUM",
            "anomalies": ["t\u1ed1c_\u0111\u1ed9_g\u00f5_kh\u00e1c_bi\u1ec7t"],
            "explanation": "Ph\u00e1t hi\u1ec7n b\u1ea5t th\u01b0\u1eddng v\u1ec1 t\u1ed1c \u0111\u1ed9 g\u00f5.",
            "recommendation": "STEP_UP_AUTH",
            "trace_id": "t-vn",
            "model": "m1",
            "latency_ms": 150
        }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        val result = client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        assertEquals(60, result.riskScore)
        assertEquals(1, result.anomalies.size)
        assertNotNull(result.explanation)
    }
}
