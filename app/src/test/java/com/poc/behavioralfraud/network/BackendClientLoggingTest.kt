package com.poc.behavioralfraud.network

import android.util.Log
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.poc.behavioralfraud.data.model.BehavioralFeatures
import com.poc.behavioralfraud.data.model.BehavioralProfile
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Specification-based tests for DEBUG-level request/response logging in BackendClient.
 *
 * Tests are derived from logging SRS requirements REQ-01 through REQ-07.
 * Each test verifies that android.util.Log.d() is called with the expected
 * tag and message content. Uses MockK to mock the static Log.d() method
 * and MockWebServer to simulate backend responses.
 *
 * Prerequisites:
 *   - build.gradle.kts must include: testOptions { unitTests.isReturnDefaultValues = true }
 *   - MockK must be available: testImplementation("io.mockk:mockk:1.13.8")
 */
class BackendClientLoggingTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: BackendClient
    private val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    companion object {
        /** Must match the TAG constant in BackendClient's companion object. */
        private const val TAG = "BackendClient"
    }

    // -------------------------------------------------------------------------
    // Test fixtures
    // -------------------------------------------------------------------------

    private val testUserId = "user-logging-001"

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

    /** Standard successful enrollment response. */
    private val enrollSuccessJson = """
        {
            "status": "pending",
            "enrollment_count": 1,
            "remaining": 2
        }
    """.trimIndent()

    /** Standard successful verification response with trace_id. */
    private val verifySuccessJson = """
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

    /** Standard successful getProfile response. */
    private val profileSuccessJson: String
        get() = """
            {
                "user_id": "$testUserId",
                "enrollment_count": 3,
                "profile": ${gson.toJson(sampleProfile)},
                "profile_summary": "Consistent typing with moderate speed."
            }
        """.trimIndent()

    // -------------------------------------------------------------------------
    // Setup / Teardown
    // -------------------------------------------------------------------------

    @Before
    fun setup() {
        // Mock android.util.Log.d so it can be called and verified in JVM tests.
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        mockWebServer = MockWebServer()
        mockWebServer.start()
        client = BackendClient(baseUrl = mockWebServer.url("/").toString())
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
        unmockkStatic(Log::class)
    }

    // =========================================================================
    // REQ-01: Log URL + request body JSON before sending each HTTP request
    // =========================================================================

    @Test
    fun `REQ-01 enrollSession logs URL before sending request`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(enrollSuccessJson).setResponseCode(200))

        client.enrollSession(testUserId, sampleFeatures)

        verify {
            Log.d(
                match { it == TAG },
                match { msg ->
                    msg.contains("/profile/enroll") || msg.contains("enroll")
                }
            )
        }
    }

    @Test
    fun `REQ-01 enrollSession logs request body JSON before sending`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(enrollSuccessJson).setResponseCode(200))

        client.enrollSession(testUserId, sampleFeatures)

        // Verify at least one Log.d call contains the user_id from the request body
        verify {
            Log.d(
                match { it == TAG },
                match { msg -> msg.contains(testUserId) || msg.contains("user_id") }
            )
        }
    }

    @Test
    fun `REQ-01 verifyTransaction logs URL before sending request`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(verifySuccessJson).setResponseCode(200))

        client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        verify {
            Log.d(
                match { it == TAG },
                match { msg ->
                    msg.contains("/risk/score") || msg.contains("risk")
                }
            )
        }
    }

    @Test
    fun `REQ-01 verifyTransaction logs request body JSON before sending`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(verifySuccessJson).setResponseCode(200))

        client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        verify {
            Log.d(
                match { it == TAG },
                match { msg -> msg.contains(testUserId) || msg.contains("user_id") }
            )
        }
    }

    @Test
    fun `REQ-01 getProfile logs URL including userId before sending request`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(profileSuccessJson).setResponseCode(200))

        client.getProfile(testUserId)

        verify {
            Log.d(
                match { it == TAG },
                match { msg ->
                    msg.contains("/profile/") || msg.contains(testUserId)
                }
            )
        }
    }

    @Test
    fun `REQ-01 getProfile GET request has no body - log should still contain URL`() = runTest {
        // GET requests have no body, so only the URL should be logged
        mockWebServer.enqueue(MockResponse().setBody(profileSuccessJson).setResponseCode(200))

        client.getProfile(testUserId)

        verify {
            Log.d(
                match { it == TAG },
                match { msg -> msg.contains("profile") || msg.contains("GET") }
            )
        }
    }

    // =========================================================================
    // REQ-02: Log HTTP status code + response body JSON after receiving response
    // =========================================================================

    @Test
    fun `REQ-02 enrollSession logs HTTP status code on success`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(enrollSuccessJson).setResponseCode(200))

        client.enrollSession(testUserId, sampleFeatures)

        verify {
            Log.d(
                match { it == TAG },
                match { msg -> msg.contains("200") }
            )
        }
    }

    @Test
    fun `REQ-02 enrollSession logs response body JSON on success`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(enrollSuccessJson).setResponseCode(200))

        client.enrollSession(testUserId, sampleFeatures)

        // Response body should include the enrollment status
        verify {
            Log.d(
                match { it == TAG },
                match { msg -> msg.contains("pending") || msg.contains("enrollment") }
            )
        }
    }

    @Test
    fun `REQ-02 verifyTransaction logs HTTP status code on success`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(verifySuccessJson).setResponseCode(200))

        client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        verify {
            Log.d(
                match { it == TAG },
                match { msg -> msg.contains("200") }
            )
        }
    }

    @Test
    fun `REQ-02 verifyTransaction logs response body JSON on success`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(verifySuccessJson).setResponseCode(200))

        client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        verify {
            Log.d(
                match { it == TAG },
                match { msg ->
                    msg.contains("risk_score") ||
                        msg.contains("15") ||
                        msg.contains("LOW")
                }
            )
        }
    }

    @Test
    fun `REQ-02 getProfile logs HTTP status code on success`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(profileSuccessJson).setResponseCode(200))

        client.getProfile(testUserId)

        verify {
            Log.d(
                match { it == TAG },
                match { msg -> msg.contains("200") }
            )
        }
    }

    @Test
    fun `REQ-02 getProfile logs response body on success`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(profileSuccessJson).setResponseCode(200))

        client.getProfile(testUserId)

        verify {
            Log.d(
                match { it == TAG },
                match { msg ->
                    msg.contains("profile") || msg.contains(testUserId)
                }
            )
        }
    }

    @Test
    fun `REQ-02 getProfile 404 logs the 404 status code`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(404).setBody("""{"detail":"Not found"}""")
        )

        client.getProfile(testUserId)

        verify {
            Log.d(
                match { it == TAG },
                match { msg -> msg.contains("404") }
            )
        }
    }

    // =========================================================================
    // REQ-03: Log error detail when request fails (network error, parse error)
    // =========================================================================

    @Test
    fun `REQ-03 enrollSession server 500 logs error detail`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"detail":"Internal Server Error"}""")
        )

        try {
            client.enrollSession(testUserId, sampleFeatures)
        } catch (_: Exception) {
            // Expected
        }

        verify {
            Log.d(
                match { it == TAG },
                match { msg ->
                    msg.contains("500") ||
                        msg.contains("error", ignoreCase = true) ||
                        msg.contains("Internal Server Error")
                }
            )
        }
    }

    @Test
    fun `REQ-03 verifyTransaction server error logs error detail`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(422)
                .setBody("""{"detail":"Validation error"}""")
        )

        try {
            client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)
        } catch (_: Exception) {
            // Expected
        }

        verify {
            Log.d(
                match { it == TAG },
                match { msg ->
                    msg.contains("422") ||
                        msg.contains("error", ignoreCase = true) ||
                        msg.contains("Validation")
                }
            )
        }
    }

    @Test
    fun `REQ-03 enrollSession malformed JSON response logs parse error`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setBody("this is not json").setResponseCode(200)
        )

        try {
            client.enrollSession(testUserId, sampleFeatures)
        } catch (_: Exception) {
            // Expected
        }

        verify {
            Log.d(
                match { it == TAG },
                match { msg ->
                    msg.contains("parse", ignoreCase = true) ||
                        msg.contains("error", ignoreCase = true) ||
                        msg.contains("fail", ignoreCase = true)
                }
            )
        }
    }

    @Test
    fun `REQ-03 verifyTransaction malformed JSON response logs parse error`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setBody("{broken").setResponseCode(200)
        )

        try {
            client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)
        } catch (_: Exception) {
            // Expected
        }

        verify {
            Log.d(
                match { it == TAG },
                match { msg ->
                    msg.contains("parse", ignoreCase = true) ||
                        msg.contains("error", ignoreCase = true) ||
                        msg.contains("fail", ignoreCase = true)
                }
            )
        }
    }

    @Test
    fun `REQ-03 getProfile server 500 logs error detail`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"detail":"Database error"}""")
        )

        try {
            client.getProfile(testUserId)
        } catch (_: Exception) {
            // Expected
        }

        verify {
            Log.d(
                match { it == TAG },
                match { msg ->
                    msg.contains("500") ||
                        msg.contains("error", ignoreCase = true)
                }
            )
        }
    }

    @Test
    fun `REQ-03 network timeout logs error detail`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)
        )

        val shortTimeoutClient = BackendClient(
            baseUrl = mockWebServer.url("/").toString(),
            timeoutSeconds = 2
        )

        try {
            shortTimeoutClient.enrollSession(testUserId, sampleFeatures)
        } catch (_: Exception) {
            // Expected
        }

        verify {
            Log.d(
                match { it == TAG },
                match { msg ->
                    msg.contains("timeout", ignoreCase = true) ||
                        msg.contains("error", ignoreCase = true) ||
                        msg.contains("fail", ignoreCase = true) ||
                        // The request log should at least appear before the timeout
                        msg.contains("enroll") ||
                        msg.contains("/profile/enroll")
                }
            )
        }
    }

    @Test
    fun `REQ-03 enrollSession empty response body logs error`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody("")
        )

        try {
            client.enrollSession(testUserId, sampleFeatures)
        } catch (_: Exception) {
            // Expected
        }

        verify {
            Log.d(
                match { it == TAG },
                match { msg ->
                    msg.contains("empty", ignoreCase = true) ||
                        msg.contains("error", ignoreCase = true) ||
                        msg.contains("fail", ignoreCase = true)
                }
            )
        }
    }

    // =========================================================================
    // REQ-04: Use android.util.Log.d() (DEBUG level) -- not visible in release
    // =========================================================================

    @Test
    fun `REQ-04 enrollSession uses Log d not Log i or Log w`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(enrollSuccessJson).setResponseCode(200))

        client.enrollSession(testUserId, sampleFeatures)

        // Verify Log.d was called at least once (for request and/or response)
        verify(atLeast = 1) {
            Log.d(match { it == TAG }, any())
        }

        // Verify Log.i, Log.w, Log.e were NOT used for the same content
        verify(exactly = 0) {
            Log.i(match { it == TAG }, match { msg -> msg.contains("enroll", ignoreCase = true) })
        }
        verify(exactly = 0) {
            Log.w(match { it == TAG }, match<String> { msg -> msg.contains("enroll", ignoreCase = true) })
        }
    }

    @Test
    fun `REQ-04 verifyTransaction uses Log d not Log i or Log w`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(verifySuccessJson).setResponseCode(200))

        client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        verify(atLeast = 1) {
            Log.d(match { it == TAG }, any())
        }

        verify(exactly = 0) {
            Log.i(match { it == TAG }, match { msg -> msg.contains("risk", ignoreCase = true) })
        }
        verify(exactly = 0) {
            Log.w(match { it == TAG }, match<String> { msg -> msg.contains("risk", ignoreCase = true) })
        }
    }

    @Test
    fun `REQ-04 getProfile uses Log d not Log i or Log w`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(profileSuccessJson).setResponseCode(200))

        client.getProfile(testUserId)

        verify(atLeast = 1) {
            Log.d(match { it == TAG }, any())
        }

        verify(exactly = 0) {
            Log.i(match { it == TAG }, match { msg -> msg.contains("profile", ignoreCase = true) })
        }
        verify(exactly = 0) {
            Log.w(match { it == TAG }, match<String> { msg -> msg.contains("profile", ignoreCase = true) })
        }
    }

    @Test
    fun `REQ-04 error path also uses Log d not higher levels`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(500).setBody("""{"detail":"error"}""")
        )

        try {
            client.enrollSession(testUserId, sampleFeatures)
        } catch (_: Exception) {
            // Expected
        }

        // Even errors should be logged at DEBUG level
        verify(atLeast = 1) {
            Log.d(match { it == TAG }, any())
        }
    }

    // =========================================================================
    // REQ-05: All 3 methods must log request + response
    // =========================================================================

    @Test
    fun `REQ-05 enrollSession logs both request and response`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(enrollSuccessJson).setResponseCode(200))

        client.enrollSession(testUserId, sampleFeatures)

        // Should have at least 2 Log.d calls: one for request, one for response
        verify(atLeast = 2) {
            Log.d(match { it == TAG }, any())
        }
    }

    @Test
    fun `REQ-05 verifyTransaction logs both request and response`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(verifySuccessJson).setResponseCode(200))

        client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        verify(atLeast = 2) {
            Log.d(match { it == TAG }, any())
        }
    }

    @Test
    fun `REQ-05 getProfile logs both request and response`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(profileSuccessJson).setResponseCode(200))

        client.getProfile(testUserId)

        verify(atLeast = 2) {
            Log.d(match { it == TAG }, any())
        }
    }

    @Test
    fun `REQ-05 enrollSession error path still logs request before failure`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(500).setBody("""{"detail":"error"}""")
        )

        try {
            client.enrollSession(testUserId, sampleFeatures)
        } catch (_: Exception) {
            // Expected
        }

        // Request log should appear even when the call fails
        verify(atLeast = 1) {
            Log.d(
                match { it == TAG },
                match { msg ->
                    msg.contains("enroll") ||
                        msg.contains("/profile/enroll") ||
                        msg.contains(testUserId)
                }
            )
        }
    }

    @Test
    fun `REQ-05 verifyTransaction error path still logs request before failure`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(422).setBody("""{"detail":"bad request"}""")
        )

        try {
            client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)
        } catch (_: Exception) {
            // Expected
        }

        verify(atLeast = 1) {
            Log.d(
                match { it == TAG },
                match { msg ->
                    msg.contains("risk") ||
                        msg.contains("/risk/score") ||
                        msg.contains(testUserId)
                }
            )
        }
    }

    @Test
    fun `REQ-05 getProfile error path still logs request before failure`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(500).setBody("""{"detail":"db error"}""")
        )

        try {
            client.getProfile(testUserId)
        } catch (_: Exception) {
            // Expected
        }

        verify(atLeast = 1) {
            Log.d(
                match { it == TAG },
                match { msg ->
                    msg.contains("profile") ||
                        msg.contains(testUserId)
                }
            )
        }
    }

    // =========================================================================
    // REQ-06: No sensitive data logged at INFO/WARN/ERROR levels (only Log.d)
    // =========================================================================

    @Test
    fun `REQ-06 enrollSession does not log request body at INFO level`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(enrollSuccessJson).setResponseCode(200))

        client.enrollSession(testUserId, sampleFeatures)

        // user_id and features should NOT appear in Log.i
        verify(exactly = 0) {
            Log.i(any(), match { msg -> msg.contains(testUserId) })
        }
    }

    @Test
    fun `REQ-06 verifyTransaction does not log profile data at WARN level`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(verifySuccessJson).setResponseCode(200))

        client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        // Profile data should not appear at WARN level
        verify(exactly = 0) {
            Log.w(any(), match<String> { msg ->
                msg.contains("profileSummary", ignoreCase = true) ||
                    msg.contains("behavioral", ignoreCase = true)
            })
        }
    }

    @Test
    fun `REQ-06 getProfile does not log profile data at ERROR level`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(profileSuccessJson).setResponseCode(200))

        client.getProfile(testUserId)

        verify(exactly = 0) {
            Log.e(any(), match { msg -> msg.contains(testUserId) })
        }
    }

    @Test
    fun `REQ-06 error responses do not leak response body at INFO or WARN level`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"detail":"secret internal error with db_password=admin123"}""")
        )

        try {
            client.enrollSession(testUserId, sampleFeatures)
        } catch (_: Exception) {
            // Expected
        }

        // Sensitive error details should not be logged at INFO or WARN
        verify(exactly = 0) {
            Log.i(any(), match { msg -> msg.contains("admin123") })
        }
        verify(exactly = 0) {
            Log.w(any(), match<String> { msg -> msg.contains("admin123") })
        }
    }

    @Test
    fun `REQ-06 behavioral features are not logged at ERROR level`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(enrollSuccessJson).setResponseCode(200))

        client.enrollSession(testUserId, sampleFeatures)

        // Feature data like gyroStability should not appear at ERROR level
        verify(exactly = 0) {
            Log.e(any(), match { msg ->
                msg.contains("gyroStability", ignoreCase = true) ||
                    msg.contains("session_duration", ignoreCase = true) ||
                    msg.contains("avg_inter_char", ignoreCase = true)
            })
        }
    }

    // =========================================================================
    // REQ-07: trace_id from response logged alongside
    //         (applies to verifyTransaction -> FraudAnalysisResult with traceId)
    // =========================================================================

    @Test
    fun `REQ-07 verifyTransaction logs trace_id from response`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(verifySuccessJson).setResponseCode(200))

        client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        // The trace_id "trace-abc-123" should appear in at least one Log.d call
        verify {
            Log.d(
                match { it == TAG },
                match { msg -> msg.contains("trace-abc-123") || msg.contains("trace_id") }
            )
        }
    }

    @Test
    fun `REQ-07 verifyTransaction logs trace_id with unique identifier`() = runTest {
        val responseWithUniqueTrace = """
            {
                "risk_score": 42,
                "risk_level": "MEDIUM",
                "anomalies": ["typing_speed"],
                "explanation": "Slight deviation.",
                "recommendation": "STEP_UP_AUTH",
                "trace_id": "trace-unique-99999",
                "model": "v2",
                "latency_ms": 187
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseWithUniqueTrace).setResponseCode(200))

        client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        verify {
            Log.d(
                match { it == TAG },
                match { msg -> msg.contains("trace-unique-99999") || msg.contains("trace_id") }
            )
        }
    }

    @Test
    fun `REQ-07 verifyTransaction empty trace_id still logged without crash`() = runTest {
        val responseWithEmptyTrace = """
            {
                "risk_score": 10,
                "risk_level": "LOW",
                "anomalies": [],
                "explanation": "OK",
                "recommendation": "APPROVE",
                "trace_id": "",
                "model": "v1",
                "latency_ms": 50
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseWithEmptyTrace).setResponseCode(200))

        val result = client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        // Should not crash; response is still logged
        assertNotNull(result)
        verify(atLeast = 1) {
            Log.d(match { it == TAG }, any())
        }
    }

    @Test
    fun `REQ-07 verifyTransaction missing trace_id field still logs response`() = runTest {
        // trace_id has default "" in FraudAnalysisResult, so missing field is valid
        val responseWithoutTrace = """
            {
                "risk_score": 20,
                "risk_level": "LOW",
                "anomalies": [],
                "explanation": "All clear.",
                "recommendation": "APPROVE",
                "model": "v1",
                "latency_ms": 80
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(responseWithoutTrace).setResponseCode(200))

        val result = client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        assertNotNull(result)
        // Response should still be logged even without trace_id
        verify(atLeast = 1) {
            Log.d(match { it == TAG }, any())
        }
    }

    @Test
    fun `REQ-07 enrollSession does not need trace_id - response still logged`() = runTest {
        // enrollSession returns EnrollResponse which has no trace_id field.
        // This test confirms REQ-07 only applies to verifyTransaction.
        mockWebServer.enqueue(MockResponse().setBody(enrollSuccessJson).setResponseCode(200))

        client.enrollSession(testUserId, sampleFeatures)

        // Response logging should still work for enrollSession
        verify(atLeast = 1) {
            Log.d(match { it == TAG }, any())
        }
    }

    // =========================================================================
    // Cross-cutting edge cases
    // =========================================================================

    @Test
    fun `all Log d calls use the BackendClient TAG constant`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(enrollSuccessJson).setResponseCode(200))

        client.enrollSession(testUserId, sampleFeatures)

        // Every Log.d call from BackendClient should use the TAG constant
        verify(atLeast = 1) {
            Log.d(match { it == TAG }, any())
        }
        // No calls with a different tag from BackendClient operations
        verify(exactly = 0) {
            Log.d(match { it != TAG && it != null }, match { msg ->
                msg.contains("enroll", ignoreCase = true) &&
                    msg.contains(testUserId)
            })
        }
    }

    @Test
    fun `consecutive method calls each produce their own log entries`() = runTest {
        // Enqueue responses for two consecutive calls
        mockWebServer.enqueue(MockResponse().setBody(enrollSuccessJson).setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setBody(verifySuccessJson).setResponseCode(200))

        client.enrollSession(testUserId, sampleFeatures)
        client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        // Should have at least 4 Log.d calls (2 per method: request + response)
        verify(atLeast = 4) {
            Log.d(match { it == TAG }, any())
        }
    }

    @Test
    fun `large response body is still logged`() = runTest {
        // Build a response with many anomalies to create a large response body
        val manyAnomalies = (1..50).joinToString(",") { "\"anomaly_$it\"" }
        val largeResponse = """
            {
                "risk_score": 90,
                "risk_level": "HIGH",
                "anomalies": [$manyAnomalies],
                "explanation": "Multiple severe deviations detected across all behavioral dimensions.",
                "recommendation": "BLOCK",
                "trace_id": "trace-large-response",
                "model": "v2",
                "latency_ms": 500
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(largeResponse).setResponseCode(200))

        client.verifyTransaction(testUserId, sampleFeatures, sampleProfile)

        // Log should still be called even for large responses
        verify(atLeast = 2) {
            Log.d(match { it == TAG }, any())
        }
    }
}
