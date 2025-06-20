package model

import kotlinx.serialization.Serializable

@Serializable
data class Departure(
    val id: Int? = null,
    val stopId: Int,
    val directionId: Int,
    val date: String // format "YYYY-MM-DD"
)
