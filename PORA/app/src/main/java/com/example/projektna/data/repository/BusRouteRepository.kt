package com.example.projektna.data.repository

import com.example.projektna.data.api.ApiClient
import com.example.projektna.data.api.model.BusRoute
import com.example.projektna.data.api.model.BusStop
import com.example.projektna.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BusRouteRepository {

    private val api = ApiClient.api

    suspend fun getAllRoutes(): Resource<List<BusRoute>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAllRoutes()
            if (response.isSuccessful) {
                Resource.Success(response.body()?.data ?: emptyList())
            } else {
                Resource.Error("Error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getRouteByLineId(lineId: Long): Resource<BusRoute> = withContext(Dispatchers.IO) {
        try {
            val response = api.getRouteByLineId(lineId)
            if (response.isSuccessful) {
                response.body()?.data?.let {
                    Resource.Success(it)
                } ?: Resource.Error("Route not found")
            } else {
                Resource.Error("Error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getStationsForRoute(lineId: Long): Resource<List<BusStop>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getStationsForRoute(lineId)
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
