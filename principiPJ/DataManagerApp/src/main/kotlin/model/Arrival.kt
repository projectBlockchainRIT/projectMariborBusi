package model

import kotlinx.serialization.Serializable

@Serializable
data class Arrival(
    val id: Int? = null,
    val departureTimes: List<String>, // format "HH:mm:ss"
    val departuresId: Int
) 