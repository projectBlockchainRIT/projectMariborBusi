package model

import kotlinx.serialization.Serializable

@Serializable
data class Direction(
    val id: Int? = null,
    var lineId: Int,
    var name: String
)
