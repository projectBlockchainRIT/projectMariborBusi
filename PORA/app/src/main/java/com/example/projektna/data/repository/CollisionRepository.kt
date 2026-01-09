package com.example.projektna.data.repository

import com.example.projektna.data.api.ApiClient
import com.example.projektna.data.api.model.CollisionRequest
import com.example.projektna.data.api.model.CollisionResponse
import com.example.projektna.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CollisionRepository {

    private val api = ApiClient.api

    suspend fun reportCollision(
        magnitude: Float,
        accelerometerX: Float,
        accelerometerY: Float,
        accelerometerZ: Float,
        latitude: Double? = null,
        longitude: Double? = null
    ): Resource<CollisionResponse> = withContext(Dispatchers.IO) {
        try {
            val request = CollisionRequest(
                magnitude = magnitude,
                accelerometerX = accelerometerX,
                accelerometerY = accelerometerY,
                accelerometerZ = accelerometerZ,
                timestamp = System.currentTimeMillis(),
                latitude = latitude,
                longitude = longitude
            )
            val response = api.reportCollision(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Success(CollisionResponse(success = true))
            } else {
                Resource.Error("Napaka: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Napaka omrežja")
        }
    }
}
