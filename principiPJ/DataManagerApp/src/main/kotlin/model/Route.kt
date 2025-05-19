package model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Route(
    val id: Int,
    var name: String,
    val path: JsonElement,  //geoJSON array koordinat
    val lineId: Int
)
