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
 * Request za pošiljanje simuliranega trčenja na strežnik.
 *
 * Endpoint: POST /v1/simulation/collision
 * Backend vrne HTTP 200 brez body-ja.
 */
@JsonClass(generateAdapter = true)
data class SimulatedCollisionRequest(
    val magnitude: Float,    // m/s² - nivo pojemanja
    val x: Float,            // x komponenta pospeška
    val y: Float,            // y komponenta pospeška
    val z: Float,            // z komponenta pospeška
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val isSimulated: Boolean = true
)
