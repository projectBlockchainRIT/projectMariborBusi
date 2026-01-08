package com.example.projektna.data.repository

import com.example.projektna.data.api.ApiClient
import com.example.projektna.data.api.model.SimulatedCollisionRequest
import com.example.projektna.data.api.model.SimulatedGpsRequest
import com.example.projektna.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class SimulationRepository {

    private val api = ApiClient.api

    /**
     * Pošlje simulirane GPS podatke na strežnik.
     * PLACEHOLDER endpoint: POST /v1/simulation/gps
     * Backend vrne samo HTTP 200 brez body-ja.
     */
    suspend fun submitSimulatedGps(
        latitude: Double,
        longitude: Double
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = SimulatedGpsRequest(
                latitude = latitude,
                longitude = longitude,
                timestamp = System.currentTimeMillis(),
                isSimulated = true
            )
            val response = api.submitSimulatedGps(request)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Napaka: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Napaka omrežja")
        }
    }

    /**
     * Pošlje simulirano trčenje na strežnik.
     * Endpoint: POST /v1/simulation/collision
     * Backend vrne samo HTTP 200 brez body-ja.
     */
    suspend fun submitSimulatedCollision(
        magnitude: Float,
        x: Float,
        y: Float,
        z: Float,
        latitude: Double,
        longitude: Double
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = SimulatedCollisionRequest(
                magnitude = magnitude,
                x = x,
                y = y,
                z = z,
                latitude = latitude,
                longitude = longitude,
                timestamp = System.currentTimeMillis(),
                isSimulated = true
            )
            val response = api.submitSimulatedCollision(request)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Napaka: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Napaka omrežja")
        }
    }

    /**
     * Pošlje sliko kamere na strežnik.
     * Endpoint: POST /v1/simulation/image
     * Backend vrne samo HTTP 200 brez body-ja.
     */
    suspend fun uploadSimulatedImage(
        imagePath: String,
        timestamp: Long,
        latitude: Double?,
        longitude: Double?
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val imageFile = File(imagePath)
            if (!imageFile.exists()) {
                return@withContext Resource.Error("Slika ne obstaja")
            }

            val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

            val timestampBody = timestamp.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val latitudeBody = latitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val longitudeBody = longitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val isSimulatedBody = "true".toRequestBody("text/plain".toMediaTypeOrNull())

            val response = api.uploadSimulatedImage(
                image = imagePart,
                timestamp = timestampBody,
                latitude = latitudeBody,
                longitude = longitudeBody,
                isSimulated = isSimulatedBody
            )

            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Napaka: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Napaka omrežja")
        }
    }
}
