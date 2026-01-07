package com.example.projektna.data.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * API response wrapper for list of stations.
 */
@JsonClass(generateAdapter = true)
data class StationsResponse(
    @Json(name = "data") val data: List<BusStop>
)

/**
 * API response wrapper for station metadata.
 */
@JsonClass(generateAdapter = true)
data class StationMetadataResponse(
    @Json(name = "data") val data: StopMetadata
)

/**
 * API response wrapper for list of routes.
 */
@JsonClass(generateAdapter = true)
data class RoutesResponse(
    @Json(name = "data") val data: List<BusRoute>
)

/**
 * API response wrapper for single route.
 */
@JsonClass(generateAdapter = true)
data class RouteResponse(
    @Json(name = "data") val data: BusRoute
)
