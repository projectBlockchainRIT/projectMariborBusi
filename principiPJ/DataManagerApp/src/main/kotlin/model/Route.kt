package model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Route(
    val id: Int? = null,
    var name: String,
    var path: JsonElement,  //geoJSON array koordinat
    var lineId: Int
)
