package model

import kotlinx.serialization.Serializable

@Serializable
data class Stop(
    val id: Int,
    val number: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)
