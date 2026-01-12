package com.example.projektna.data.api.model

import com.squareup.moshi.JsonClass

/**
 * Request za pošiljanje simuliranih GPS podatkov na strežnik.
 *
 * Endpoint: POST /v1/simulation/gps
 */
@JsonClass(generateAdapter = true)
data class SimulatedGpsRequest(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val isSimulated: Boolean = true
)

/**
 * Request za pošiljanje simuliranih hitrostnih podatkov na strežnik.
 *
 * Endpoint: POST /v1/simulation/speed
 */
@JsonClass(generateAdapter = true)
data class SimulatedSpeedRequest(
    val speed: Float, // km/h
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val isSimulated: Boolean = true
)

/**
 * Response od strežnika za simulacijske podatke.
 */
@JsonClass(generateAdapter = true)
data class SimulationResponse(
    val success: Boolean,
    val message: String? = null,
    val id: Long? = null
)
