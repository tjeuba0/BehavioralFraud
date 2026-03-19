package com.poc.behavioralfraud.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.poc.behavioralfraud.data.model.BehavioralFeatures
import com.poc.behavioralfraud.data.model.BehavioralProfile

/**
 * Repository for storing and retrieving behavioral profiles.
 * Uses SharedPreferences for simplicity in POC.
 */
class ProfileRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("behavioral_profiles", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_PROFILE = "user_profile"
        private const val KEY_ENROLLMENT_FEATURES = "enrollment_features"
    }

    /**
     * Save behavioral profile
     */
    fun saveProfile(profile: BehavioralProfile) {
        prefs.edit()
            .putString(KEY_PROFILE, gson.toJson(profile))
            .apply()
    }

    /**
     * Get stored behavioral profile
     */
    fun getProfile(): BehavioralProfile? {
        val json = prefs.getString(KEY_PROFILE, null) ?: return null
        return try {
            gson.fromJson(json, BehavioralProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save enrollment features for averaging
     */
    fun addEnrollmentFeatures(features: BehavioralFeatures) {
        val existing = getEnrollmentFeaturesList().toMutableList()
        existing.add(features)
        val json = gson.toJson(existing)
        prefs.edit().putString(KEY_ENROLLMENT_FEATURES, json).apply()
    }

    /**
     * Get all enrollment features
     */
    fun getEnrollmentFeaturesList(): List<BehavioralFeatures> {
        val json = prefs.getString(KEY_ENROLLMENT_FEATURES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<BehavioralFeatures>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get enrollment count
     */
    fun getEnrollmentCount(): Int {
        return getEnrollmentFeaturesList().size
    }

    /**
     * Build averaged profile from enrollment features
     */
    fun buildAveragedProfile(llmSummary: String): BehavioralProfile {
        val featuresList = getEnrollmentFeaturesList()
        if (featuresList.isEmpty()) {
            return BehavioralProfile(
                userId = "default_user",
                enrollmentCount = 0,
                avgSessionDuration = 0.0,
                avgInterCharDelay = 0.0,
                stdInterCharDelay = 0.0,
                avgTouchSize = 0.0,
                avgTouchDuration = 0.0,
                avgGyroStabilityX = 0.0,
                avgGyroStabilityY = 0.0,
                avgGyroStabilityZ = 0.0,
                avgAccelStabilityX = 0.0,
                avgAccelStabilityY = 0.0,
                avgAccelStabilityZ = 0.0,
                avgSwipeVelocity = 0.0,
                avgPasteCount = 0.0,
                avgTimeToFirstInput = 0.0,
                avgTimeFromLastInputToConfirm = 0.0,
                typicalFieldFocusSequence = "",
                avgTouchPressure = 0.0,
                avgInterFieldPause = 0.0,
                avgDeletionRatio = 0.0,
                profileSummary = "No enrollment data"
            )
        }

        // Find most common field focus sequence
        val focusSequences = featuresList.map { it.fieldFocusSequence }
        val typicalSequence = focusSequences.groupBy { it }
            .maxByOrNull { it.value.size }?.key ?: ""

        return BehavioralProfile(
            userId = "default_user",
            enrollmentCount = featuresList.size,
            avgSessionDuration = featuresList.map { it.sessionDurationMs.toDouble() }.average(),
            avgInterCharDelay = featuresList.map { it.avgInterCharDelayMs }.average(),
            stdInterCharDelay = featuresList.map { it.stdInterCharDelayMs }.average(),
            avgTouchSize = featuresList.map { it.avgTouchSize }.average(),
            avgTouchDuration = featuresList.map { it.avgTouchDurationMs }.average(),
            avgGyroStabilityX = featuresList.map { it.gyroStabilityX }.average(),
            avgGyroStabilityY = featuresList.map { it.gyroStabilityY }.average(),
            avgGyroStabilityZ = featuresList.map { it.gyroStabilityZ }.average(),
            avgAccelStabilityX = featuresList.map { it.accelStabilityX }.average(),
            avgAccelStabilityY = featuresList.map { it.accelStabilityY }.average(),
            avgAccelStabilityZ = featuresList.map { it.accelStabilityZ }.average(),
            avgSwipeVelocity = featuresList.map { it.avgSwipeVelocity }.average(),
            avgPasteCount = featuresList.map { it.pasteCount.toDouble() }.average(),
            avgTimeToFirstInput = featuresList.map { it.timeToFirstInput.toDouble() }.average(),
            avgTimeFromLastInputToConfirm = featuresList.map { it.timeFromLastInputToConfirm.toDouble() }.average(),
            typicalFieldFocusSequence = typicalSequence,
            avgTouchPressure = featuresList.map { it.avgTouchPressure }.average(),
            avgInterFieldPause = featuresList.map { it.avgInterFieldPauseMs }.average(),
            avgDeletionRatio = featuresList.map { it.deletionRatio }.average(),
            profileSummary = llmSummary
        )
    }

    /**
     * Clear all data (reset)
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
