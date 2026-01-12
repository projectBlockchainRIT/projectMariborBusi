package com.example.projektna.data.repository

import com.example.projektna.data.api.ApiClient
import com.example.projektna.data.api.model.LoginRequest
import com.example.projektna.data.api.model.LoginResponse
import com.example.projektna.data.api.model.RegisterRequest
import com.example.projektna.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {

    private val api = ApiClient.api

    /**
     * Registracija novega uporabnika.
     * Endpoint: POST /v1/authentication/register
     */
    suspend fun register(
        username: String,
        email: String,
        password: String
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = RegisterRequest(
                username = username,
                email = email,
                password = password
            )
            val response = api.register(request)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                val errorMessage = when (response.code()) {
                    400 -> "Neveljavni podatki"
                    409 -> "Uporabnik s tem e-naslovom že obstaja"
                    else -> "Napaka: ${response.code()} ${response.message()}"
                }
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Napaka omrežja")
        }
    }

    /**
     * Prijava uporabnika.
     * Endpoint: POST /v1/authentication/login
     */
    suspend fun login(
        email: String,
        password: String
    ): Resource<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(
                email = email,
                password = password
            )
            val response = api.login(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("Prazen odgovor strežnika")
            } else {
                val errorMessage = when (response.code()) {
                    400 -> "Neveljavni podatki"
                    401 -> "Napačen e-naslov ali geslo"
                    404 -> "Uporabnik ne obstaja"
                    else -> "Napaka: ${response.code()} ${response.message()}"
                }
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Napaka omrežja")
        }
    }
}
