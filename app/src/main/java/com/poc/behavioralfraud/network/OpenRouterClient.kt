package com.poc.behavioralfraud.network

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.poc.behavioralfraud.BuildConfig
import com.poc.behavioralfraud.data.model.BehavioralFeatures
import com.poc.behavioralfraud.data.model.BehavioralProfile
import com.poc.behavioralfraud.data.model.FraudAnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Client for calling LLM via OpenRouter API.
 *
 * Handles two use cases:
 * 1. Enrollment: Build behavioral profile summary from features
 * 2. Verification: Compare current features against profile, return risk score
 */
class OpenRouterClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val apiKey = BuildConfig.OPENROUTER_API_KEY
    private val baseUrl = "https://openrouter.ai/api/v1/chat/completions"
    // Dùng model giá rẻ cho POC
    private val model = "z-ai/glm-5"

    /**
     * Call LLM for enrollment: summarize behavioral features into a profile
     */
    suspend fun enrollProfile(
        featuresList: List<BehavioralFeatures>
    ): String = withContext(Dispatchers.IO) {
        val featuresJson = gson.toJson(featuresList)

        val systemPrompt = """You are a behavioral biometrics analyst for a mobile banking fraud detection system.
Your task is to analyze behavioral features collected during money transfer sessions and create a behavioral profile summary.

Analyze the following features and describe the user's behavioral patterns in a structured way:
- Typing rhythm (speed, consistency)
- Touch patterns (size, duration, velocity)
- Device handling (gyroscope/accelerometer stability)
- Navigation habits (session duration, paste behavior)

Be specific with numbers. This profile will be used as a baseline to detect anomalies in future sessions."""

        val userPrompt = """Here are the behavioral features from ${featuresList.size} enrollment sessions:

$featuresJson

Please create a behavioral profile summary describing this user's typical patterns. Include specific numeric ranges for each behavioral dimension."""

        callLLM(systemPrompt, userPrompt)
    }

    /**
     * Call LLM for verification: compare current features against profile
     */
    suspend fun verifyTransaction(
        currentFeatures: BehavioralFeatures,
        profile: BehavioralProfile
    ): FraudAnalysisResult = withContext(Dispatchers.IO) {
        val featuresJson = gson.toJson(currentFeatures)

        val systemPrompt = """You are a behavioral biometrics fraud detection AI for a mobile banking app.
Your task is to compare the current session's behavioral features against the user's established behavioral profile and determine if this is the legitimate account owner or a potential fraudster.

You MUST respond in EXACTLY this JSON format (no markdown, no code blocks, just raw JSON):
{
  "riskScore": <0-100>,
  "riskLevel": "<LOW|MEDIUM|HIGH>",
  "anomalies": ["anomaly1", "anomaly2"],
  "explanation": "<detailed explanation in Vietnamese>",
  "recommendation": "<APPROVE|STEP_UP_AUTH|BLOCK>"
}

Scoring guidelines:
- 0-30: LOW risk - behavior matches profile well
- 31-70: MEDIUM risk - some anomalies detected
- 71-100: HIGH risk - significant behavioral deviation

Key indicators of fraud:
- Typing speed significantly different from profile
- Gyroscope too stable (device on table = possible RAT/remote control)
- Paste behavior when user normally types manually
- Session duration very different from normal
- Touch size/duration significantly different (different person's fingers)"""

        val userPrompt = """## Established Behavioral Profile
User ID: ${profile.userId}
Enrollment sessions: ${profile.enrollmentCount}
Profile summary: ${profile.profileSummary}

Average metrics:
- Session duration: ${String.format("%.0f", profile.avgSessionDuration)}ms
- Inter-char delay: ${String.format("%.1f", profile.avgInterCharDelay)}ms (std: ${String.format("%.1f", profile.stdInterCharDelay)})
- Touch size: ${String.format("%.4f", profile.avgTouchSize)}
- Touch duration: ${String.format("%.1f", profile.avgTouchDuration)}ms
- Gyro stability: ${String.format("%.6f", profile.avgGyroStability)}
- Accel stability: ${String.format("%.4f", profile.avgAccelStability)}
- Paste count avg: ${String.format("%.1f", profile.avgPasteCount)}

## Current Session Features
$featuresJson

Compare the current session against the profile and provide your fraud analysis."""

        val response = callLLM(systemPrompt, userPrompt)

        // Parse JSON response from LLM
        try {
            // Clean response: remove markdown code blocks if present
            val cleaned = response
                .replace("```json", "")
                .replace("```", "")
                .trim()
            gson.fromJson(cleaned, FraudAnalysisResult::class.java)
        } catch (e: Exception) {
            // Fallback if LLM response is not valid JSON
            FraudAnalysisResult(
                riskScore = 50,
                riskLevel = "MEDIUM",
                anomalies = listOf("Could not parse LLM response"),
                explanation = "LLM response: $response",
                recommendation = "STEP_UP_AUTH"
            )
        }
    }

    /**
     * Internal: call OpenRouter API
     */
    private fun callLLM(systemPrompt: String, userPrompt: String): String {
        val requestBody = gson.toJson(
            mapOf(
                "model" to model,
                "messages" to listOf(
                    mapOf("role" to "system", "content" to systemPrompt),
                    mapOf("role" to "user", "content" to userPrompt)
                ),
                "temperature" to 0.3,
                "max_tokens" to 2000
            )
        )

        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            throw Exception("API error ${response.code}: $body")
        }

        // Extract content from OpenRouter response
        val jsonResponse = JsonParser.parseString(body).asJsonObject
        return jsonResponse
            .getAsJsonArray("choices")
            .get(0).asJsonObject
            .getAsJsonObject("message")
            .get("content").asString
    }
}
