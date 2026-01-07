package com.example.projektna.data.repository

import com.example.projektna.data.api.ApiClient
import com.example.projektna.data.api.model.BusStop
import com.example.projektna.data.api.model.LocationRequest
import com.example.projektna.data.api.model.StopMetadata
import com.example.projektna.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BusStopRepository {

    private val api = ApiClient.api

    suspend fun getAllStops(): Resource<List<BusStop>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAllStations()
            if (response.isSuccessful) {
                Resource.Success(response.body()?.data ?: emptyList())
            } else {
                Resource.Error("Error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getStopById(stopId: Long): Resource<StopMetadata> = withContext(Dispatchers.IO) {
        try {
            val response = api.getStationMetadata(stopId)
            if (response.isSuccessful) {
                response.body()?.data?.let {
                    Resource.Success(it)
                } ?: Resource.Error("Station not found")
            } else {
                Resource.Error("Error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getNearbyStops(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 500
    ): Resource<List<BusStop>> = withContext(Dispatchers.IO) {
        try {
            val request = LocationRequest(latitude, longitude, radiusMeters)
            val response = api.getNearbyStations(request)
            if (response.isSuccessful) {
                Resource.Success(response.body()?.data ?: emptyList())
            } else {
                Resource.Error("Error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }
}
