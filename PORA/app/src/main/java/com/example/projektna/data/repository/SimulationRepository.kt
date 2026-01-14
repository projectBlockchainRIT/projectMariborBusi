package com.example.projektna.data.repository

import com.example.projektna.data.api.ApiClient
import com.example.projektna.data.api.model.SimulatedGpsRequest
import com.example.projektna.data.api.model.SimulatedSpeedRequest
import com.example.projektna.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
     * Pošlje simulirane hitrostne podatke na strežnik.
     * PLACEHOLDER endpoint: POST /v1/simulation/speed
     * Backend vrne samo HTTP 200 brez body-ja.
     */
    suspend fun submitSimulatedSpeed(
        speed: Float,
        latitude: Double,
        longitude: Double
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = SimulatedSpeedRequest(
                speed = speed,
                latitude = latitude,
                longitude = longitude,
                timestamp = System.currentTimeMillis(),
                isSimulated = true
            )
            val response = api.submitSimulatedSpeed(request)
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
