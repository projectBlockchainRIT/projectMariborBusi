package model

import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Serializable
data class User(
    val id: Int? = null,
    var username: String,
    var email: String,
    var password: String,
    val createdAt: String, //format: "2024-05-15T12:00:00+01:00"
    val lastLogin: String?
)
