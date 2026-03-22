package com.poc.behavioralfraud.network

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.poc.behavioralfraud.BuildConfig
import com.poc.behavioralfraud.data.model.BehavioralFeatures
import com.poc.behavioralfraud.data.model.BehavioralProfile
import com.poc.behavioralfraud.data.model.EnrollResponse
import com.poc.behavioralfraud.data.model.FraudAnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import android.util.Log
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * HTTP client for the Behavioral Fraud Detection backend.
 *
 * Sends extracted behavioral features to the FastAPI backend for
 * enrollment and verification. No prompt strings, no LLM knowledge.
 */
class BackendClient(
    baseUrl: String = BuildConfig.BACKEND_BASE_URL,
    timeoutSeconds: Long = 60
) {

    companion object {
        private const val TAG = "BackendClient"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    private val baseUrl = baseUrl.trimEnd('/')
    private val jsonMediaType = "application/json".toMediaType()

    /**
     * Enroll one session's behavioral features.
     * Backend accumulates sessions; returns "completed" with profile after 3 sessions.
     */
    suspend fun enrollSession(
        userId: String,
        features: BehavioralFeatures
    ): EnrollResponse = withContext(Dispatchers.IO) {
        val body = JsonObject().apply {
            addProperty("user_id", userId)
            add("session_features", gson.toJsonTree(features))
        }
        val bodyJson = gson.toJson(body)

        val url = "${this@BackendClient.baseUrl}/profile/enroll"
        Log.d(TAG, "→ POST $url")
        Log.d(TAG, "→ Body: $bodyJson")

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody(jsonMediaType))
            .build()

        val responseBody = executeRequest(request)
        try {
            gson.fromJson(responseBody, EnrollResponse::class.java)
        } catch (e: Exception) {
            Log.d(TAG, "✗ Failed to parse enrollment response: ${e.message}")
            throw IOException("Failed to parse enrollment response: ${e.message}", e)
        }
    }

    /**
     * Verify current session against stored behavioral profile.
     * Returns risk analysis with score, anomalies, and recommendation.
     */
    suspend fun verifyTransaction(
        userId: String,
        features: BehavioralFeatures,
        profile: BehavioralProfile
    ): FraudAnalysisResult = withContext(Dispatchers.IO) {
        val body = JsonObject().apply {
            addProperty("user_id", userId)
            add("current_session", gson.toJsonTree(features))
            add("profile", gson.toJsonTree(profile))
        }
        val bodyJson = gson.toJson(body)

        val url = "${this@BackendClient.baseUrl}/risk/score"
        Log.d(TAG, "→ POST $url")
        Log.d(TAG, "→ Body: $bodyJson")

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody(jsonMediaType))
            .build()

        val responseBody = executeRequest(request)
        try {
            val result = gson.fromJson(responseBody, FraudAnalysisResult::class.java)
            if (!result.traceId.isNullOrEmpty()) {
                Log.d(TAG, "← trace_id: ${result.traceId}")
            }
            result
        } catch (e: Exception) {
            Log.d(TAG, "✗ Failed to parse risk score response: ${e.message}")
            throw IOException("Failed to parse risk score response: ${e.message}", e)
        }
    }

    /**
     * Get stored behavioral profile for a user.
     * Returns null if no profile exists (404).
     */
    suspend fun getProfile(
        userId: String
    ): BehavioralProfile? = withContext(Dispatchers.IO) {
        val url = "${this@BackendClient.baseUrl}/profile/$userId"
        Log.d(TAG, "→ GET $url")

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()

                Log.d(TAG, "← ${response.code} $url")
                if (body != null) {
                    Log.d(TAG, "← Body: $body")
                }

                when {
                    response.code == 404 -> {
                        Log.d(TAG, "← Profile not found for user: $userId")
                        null
                    }
                    !response.isSuccessful -> throw IOException(
                        "Backend error ${response.code}: ${body ?: "no response body"}"
                    )
                    body.isNullOrEmpty() -> throw IOException("Empty response from backend")
                    else -> {
                        try {
                            val wrapper = gson.fromJson(body, JsonObject::class.java)
                            gson.fromJson(wrapper.getAsJsonObject("profile"), BehavioralProfile::class.java)
                        } catch (e: Exception) {
                            Log.d(TAG, "✗ Failed to parse profile response: ${e.message}")
                            throw IOException("Failed to parse profile response: ${e.message}", e)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.d(TAG, "✗ GET $url error: ${e.message}")
            throw e
        }
    }

    /**
     * Execute HTTP request and return response body string.
     * Logs response status and body at DEBUG level.
     * Throws IOException with meaningful message on failure.
     */
    private fun executeRequest(request: Request): String {
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()

                Log.d(TAG, "← ${response.code} ${request.url}")
                if (body != null) {
                    Log.d(TAG, "← Body: $body")
                }

                if (!response.isSuccessful) {
                    throw IOException(
                        "Backend error ${response.code}: ${body ?: "no response body"}"
                    )
                }

                if (body.isNullOrEmpty()) {
                    throw IOException("Empty response from backend")
                }

                return body
            }
        } catch (e: IOException) {
            Log.d(TAG, "✗ ${request.method} ${request.url} error: ${e.message}")
            throw e
        }
    }
}
