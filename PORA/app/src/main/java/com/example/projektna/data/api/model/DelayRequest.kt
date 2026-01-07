package com.example.projektna.data.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for submitting a delay report.
 */
@JsonClass(generateAdapter = true)
data class DelayRequest(
    @Json(name = "stationId") val stationId: Long,
    @Json(name = "stationName") val stationName: String,
    @Json(name = "lineId") val lineId: String,
    @Json(name = "delayMinutes") val delayMinutes: Int,
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null
)

/**
 * Response wrapper for delay submission.
 */
@JsonClass(generateAdapter = true)
data class DelayResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null
)
