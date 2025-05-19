package model

import kotlinx.serialization.Serializable

@Serializable
data class Direction(
    val id: Int,
    val lineId: Int,
    var name: String
)
