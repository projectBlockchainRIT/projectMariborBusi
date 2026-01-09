package com.example.projektna.data.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for reporting a collision event.
 * Sent automatically when accelerometer detects sudden impact.
 */
@JsonClass(generateAdapter = true)
data class CollisionRequest(
    @Json(name = "magnitude") val magnitude: Float,
    @Json(name = "accelerometerX") val accelerometerX: Float,
    @Json(name = "accelerometerY") val accelerometerY: Float,
    @Json(name = "accelerometerZ") val accelerometerZ: Float,
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null
)

/**
 * Response wrapper for collision report submission.
 */
@JsonClass(generateAdapter = true)
data class CollisionResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null,
    @Json(name = "collisionId") val collisionId: String? = null
)
