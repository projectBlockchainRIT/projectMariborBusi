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

    /**
     * Pošlje simulirane GPS podatke na strežnik.
     * Pot: /v1/simulation/gps
     * Backend vrne samo HTTP 200 brez body-ja.
     */
    @POST("simulation/gps")
    suspend fun submitSimulatedGps(@Body gpsData: SimulatedGpsRequest): Response<Unit>

    /**
     * Pošlje simulirano trčenje na strežnik.
     * Pot: /v1/simulation/collision
     * Uporablja se za testiranje zaznave trkov.
     * Backend vrne HTTP 200 brez body-ja.
     */
    @POST("simulation/collision")
    suspend fun submitSimulatedCollision(@Body collision: SimulatedCollisionRequest): Response<Unit>

    /**
     * Pošlje sliko kamere na strežnik.
     * Pot: /v1/simulation/image
     * Backend vrne HTTP 200 brez body-ja.
     */
    @Multipart
    @POST("simulation/image")
    suspend fun uploadSimulatedImage(
        @Part image: okhttp3.MultipartBody.Part,
        @Part("timestamp") timestamp: okhttp3.RequestBody,
        @Part("latitude") latitude: okhttp3.RequestBody?,
        @Part("longitude") longitude: okhttp3.RequestBody?,
        @Part("isSimulated") isSimulated: okhttp3.RequestBody
    ): Response<Unit>
}
