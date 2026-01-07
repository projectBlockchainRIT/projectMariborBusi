package com.example.projektna.data.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents a bus route with path coordinates.
 */
@JsonClass(generateAdapter = true)
data class BusRoute(
    @Json(name = "id") val id: Long,
    @Json(name = "line_id") val lineId: Long,
    @Json(name = "name") val name: String,
    @Json(name = "path") val path: List<List<Double>> = emptyList()
)
