package model

import kotlinx.serialization.Serializable

@Serializable
data class Departure(
    val id: Int? = null,
    val stopId: Int,
    val directionId: Int,
    var departure: String //format "HH:mm:ss"
)
