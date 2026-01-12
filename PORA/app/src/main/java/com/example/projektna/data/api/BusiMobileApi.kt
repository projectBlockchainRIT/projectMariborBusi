package com.example.projektna.data.api

import com.example.projektna.data.api.model.*
import retrofit2.Response
import retrofit2.http.*

interface BusiMobileApi {

    // ==================== Authentication ====================

    /**
     * Registracija novega uporabnika.
     * Endpoint: POST /v1/authentication/register
     */
    @POST("authentication/register")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    /**
     * Prijava uporabnika.
     * Endpoint: POST /v1/authentication/login
     */
    @POST("authentication/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // ==================== Delays ====================

    @POST("delays/user")
    suspend fun submitDelay(@Body delay: DelayRequest): Response<DelayResponse>

    // ==================== Collisions ====================

    @POST("collision")
    suspend fun reportCollision(@Body collision: CollisionRequest): Response<CollisionResponse>

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

    // ==================== Simulation ====================
    // PLACEHOLDER: Te poti je treba posodobiti, ko bo backend pripravljen

    /**
     * Pošlje simulirane GPS podatke na strežnik.
     * PLACEHOLDER pot: /v1/simulation/gps
     */
    @POST("simulation/gps")
    suspend fun submitSimulatedGps(@Body gpsData: SimulatedGpsRequest): Response<SimulationResponse>

    /**
     * Pošlje simulirane hitrostne podatke na strežnik.
     * PLACEHOLDER pot: /v1/simulation/speed
     */
    @POST("simulation/speed")
    suspend fun submitSimulatedSpeed(@Body speedData: SimulatedSpeedRequest): Response<SimulationResponse>
}
