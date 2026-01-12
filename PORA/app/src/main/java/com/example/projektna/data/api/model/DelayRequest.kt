package com.example.projektna.data.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for submitting a delay report.
 * API endpoint: POST /v1/delays/user
 */
@JsonClass(generateAdapter = true)
data class DelayRequest(
    @Json(name = "user_id") val userId: Int,
    @Json(name = "date") val date: Long,
    @Json(name = "delay_min") val delayMin: Int,
    @Json(name = "stop_id") val stopId: Int,
    @Json(name = "line_id") val lineId: Int
)

/**
 * Response wrapper for delay submission.
 */
@JsonClass(generateAdapter = true)
data class DelayResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null
)
