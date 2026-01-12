package com.example.projektna.data.repository

import com.example.projektna.data.api.ApiClient
import com.example.projektna.data.api.model.DelayRequest
import com.example.projektna.data.api.model.DelayResponse
import com.example.projektna.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DelayRepository {

    private val api = ApiClient.api

    /**
     * Pošlje zamudo na strežnik.
     * API endpoint: POST /v1/delays/user
     */
    suspend fun submitDelay(
        userId: Int,
        stopId: Int,
        lineId: Int,
        delayMinutes: Int
    ): Resource<DelayResponse> = withContext(Dispatchers.IO) {
        try {
            val request = DelayRequest(
                userId = userId,
                date = System.currentTimeMillis(),
                delayMin = delayMinutes,
                stopId = stopId,
                lineId = lineId
            )
            val response = api.submitDelay(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Success(DelayResponse(success = true))
            } else {
                Resource.Error("Napaka: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Napaka omrežja")
        }
    }
}
