package com.example.projektna.data.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request za prijavo uporabnika.
 * Endpoint: POST /v1/authentication/login
 */
@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

/**
 * Request za registracijo novega uporabnika.
 * Endpoint: POST /v1/authentication/register
 */
@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

/**
 * Podatki znotraj login response.
 */
@JsonClass(generateAdapter = true)
data class LoginData(
    @Json(name = "token") val token: String,
    @Json(name = "id") val id: Int
)

/**
 * Response od strežnika ob uspešni prijavi.
 * API vrača: { "data": { "token": "..." } }
 */
@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "data") val data: LoginData
)
