package model

import kotlinx.serialization.Serializable
import java.time.LocalTime

@Serializable
data class Departure(
    val id: Int,
    val stopId: Int,
    val directionId: Int,
    val departure: LocalTime
)
