package model

import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Serializable
data class User(
    val id: Int,
    val username: String,
    val email: String,
    val password: String,
    val createdAt: ZonedDateTime,
    val lastLogin: ZonedDateTime?
)
