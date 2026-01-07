package com.example.projektna.data.api

import com.example.projektna.data.api.model.*
import retrofit2.Response
import retrofit2.http.*

interface BusiMobileApi {

    // ==================== Stations ====================

    @GET("stations/list")
    suspend fun getAllStations(): Response<StationsResponse>

    @GET("stations/{stationId}")
    suspend fun getStationMetadata(@Path("stationId") stationId: Long): Response<StationMetadataResponse>

    @GET("stations/location/{stationId}")
    suspend fun getStationLocation(@Path("stationId") stationId: Long): Response<StationsResponse>

    @POST("stations/closeBy")
    suspend fun getNearbyStations(@Body location: LocationRequest): Response<StationsResponse>

    // ==================== Routes ====================

    @GET("routes/list")
    suspend fun getAllRoutes(): Response<RoutesResponse>

    @GET("routes/{lineId}")
    suspend fun getRouteByLineId(@Path("lineId") lineId: Long): Response<RouteResponse>

    @GET("routes/stations/{lineId}")
    suspend fun getStationsForRoute(@Path("lineId") lineId: Long): Response<StationsResponse>
}
