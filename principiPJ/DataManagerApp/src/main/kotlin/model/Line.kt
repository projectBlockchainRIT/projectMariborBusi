package model

import kotlinx.serialization.Serializable

@Serializable
data class Line(
    val id: Int? = null,
    var lineCode: String
)
