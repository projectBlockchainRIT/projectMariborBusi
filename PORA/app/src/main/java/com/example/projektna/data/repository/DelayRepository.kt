package com.example.projektna.data.repository

import com.example.projektna.data.api.ApiClient
import com.example.projektna.data.api.model.DelayRequest
import com.example.projektna.data.api.model.DelayResponse
import com.example.projektna.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DelayRepository {

    private val api = ApiClient.api

    suspend fun submitDelay(
        stationId: Long,
        stationName: String,
        lineId: String,
        delayMinutes: Int,
        latitude: Double? = null,
        longitude: Double? = null
    ): Resource<DelayResponse> = withContext(Dispatchers.IO) {
        try {
            val request = DelayRequest(
                stationId = stationId,
                stationName = stationName,
                lineId = lineId,
                delayMinutes = delayMinutes,
                timestamp = System.currentTimeMillis(),
                latitude = latitude,
                longitude = longitude
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
