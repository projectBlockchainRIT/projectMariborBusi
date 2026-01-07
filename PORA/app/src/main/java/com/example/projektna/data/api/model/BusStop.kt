package com.example.projektna.data.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents a bus station/stop from the API.
 */
@JsonClass(generateAdapter = true)
data class BusStop(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "number") val number: String? = null,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double
)

/**
 * Station metadata with departure times.
 */
@JsonClass(generateAdapter = true)
data class StopMetadata(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "number") val number: String? = null,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "departures") val departures: List<DepartureGroup> = emptyList()
)

/**
 * Departure times grouped by line and direction.
 */
@JsonClass(generateAdapter = true)
data class DepartureGroup(
    @Json(name = "line") val line: String,
    @Json(name = "direction") val direction: String,
    @Json(name = "times") val times: List<String> = emptyList()
)

/**
 * Request body for finding nearby stations.
 */
@JsonClass(generateAdapter = true)
data class LocationRequest(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "radius") val radius: Int
)
