package model

import kotlinx.serialization.Serializable

@Serializable
data class Stop(
    val id: Int? = null,
    var number: String,
    var name: String,
    var latitude: Double,
    var longitude: Double
)
