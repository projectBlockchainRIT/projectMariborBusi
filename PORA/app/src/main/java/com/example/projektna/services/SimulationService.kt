package com.example.projektna.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.projektna.data.PreferencesManager
import com.example.projektna.data.SimulationSensorType
import com.example.projektna.data.repository.SimulationRepository
import com.example.projektna.util.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import kotlin.random.Random

class SimulationService : Service() {

    companion object {
        const val TAG = "SimulationService"
        const val ACTION_SIMULATION_DATA = "com.example.projektna.SIMULATION_DATA"
        const val ACTION_SIMULATION_STATUS = "com.example.projektna.SIMULATION_STATUS"
        const val EXTRA_SENSOR_TYPE = "sensor_type"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_TIMESTAMP = "timestamp"
        const val EXTRA_SUCCESS = "success"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_PATH_INDEX = "path_index"
        const val EXTRA_PATH_TOTAL = "path_total"
    }

    // Route path data
    private var routePath: List<List<Double>> = emptyList()
    private var currentPathIndex: Int = 0
    private var routeDirection: Int = 1 // 1 = naprej, -1 = nazaj

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var simulationRepository: SimulationRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var simulationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        NotificationHelper.createSimulationNotificationChannel(this)
        preferencesManager = PreferencesManager(this)
        simulationRepository = SimulationRepository()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")

        val sensorType = preferencesManager.simulationSensorType
        val sensorName = when (sensorType) {
            SimulationSensorType.GPS -> "GPS"
            SimulationSensorType.SPEED -> "Hitrost"
        }

        val notification = NotificationHelper.createSimulationNotification(
            this,
            sensorName,
            "Zaganjanje..."
        )
        startForeground(NotificationHelper.SIMULATION_NOTIFICATION_ID, notification)

        startSimulation()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")
        simulationJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = serviceScope.launch {
            val intervalMs = preferencesManager.simulationIntervalSeconds * 1000L
            val sensorType = preferencesManager.simulationSensorType
            val followRoute = preferencesManager.simulationFollowRoute

            // Naloži pot linije, če sledimo liniji
            if (followRoute && sensorType == SimulationSensorType.GPS) {
                loadRoutePath()
                currentPathIndex = preferencesManager.simulationPathIndex
                routeDirection = preferencesManager.simulationRouteDirection
                Log.d(TAG, "Following route with ${routePath.size} points, starting at index $currentPathIndex")
            }

            Log.d(TAG, "Starting simulation: type=$sensorType, interval=${intervalMs}ms, followRoute=$followRoute")

            while (isActive) {
                try {
                    when (sensorType) {
                        SimulationSensorType.GPS -> simulateGps()
                        SimulationSensorType.SPEED -> simulateSpeed()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Simulation error", e)
                    broadcastStatus(false, e.message)
                }

                delay(intervalMs)
            }
        }
    }

    private fun loadRoutePath() {
        val pathJson = preferencesManager.simulationRoutePath
        if (pathJson.isNullOrEmpty()) {
            routePath = emptyList()
            return
        }

        try {
            val jsonArray = JSONArray(pathJson)
            val path = mutableListOf<List<Double>>()
            for (i in 0 until jsonArray.length()) {
                val coordArray = jsonArray.getJSONArray(i)
                val coord = mutableListOf<Double>()
                for (j in 0 until coordArray.length()) {
                    coord.add(coordArray.getDouble(j))
                }
                path.add(coord)
            }
            routePath = path
            Log.d(TAG, "Loaded route path with ${routePath.size} coordinates")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse route path", e)
            routePath = emptyList()
        }
    }

    private suspend fun simulateGps() {
        val latitude: Double
        val longitude: Double
        var pathIndex = -1
        var pathTotal = -1

        val followRoute = preferencesManager.simulationFollowRoute

        if (followRoute && routePath.isNotEmpty()) {
            // Sledimo poti linije
            val coord = routePath[currentPathIndex]
            if (coord.size >= 2) {
                latitude = coord[0]
                longitude = coord[1]
            } else {
                latitude = 0.0
                longitude = 0.0
            }
            pathIndex = currentPathIndex
            pathTotal = routePath.size

            // Premakni se na naslednjo točko
            moveToNextPathPoint()

        } else if (preferencesManager.useManualLocation) {
            latitude = preferencesManager.simulationManualLat.toDouble()
            longitude = preferencesManager.simulationManualLon.toDouble()
        } else {
            latitude = generateRandomValue(
                preferencesManager.simulationGpsLatMin,
                preferencesManager.simulationGpsLatMax
            ).toDouble()
            longitude = generateRandomValue(
                preferencesManager.simulationGpsLonMin,
                preferencesManager.simulationGpsLonMax
            ).toDouble()
        }

        val timestamp = System.currentTimeMillis()

        Log.d(TAG, "Simulating GPS: lat=$latitude, lon=$longitude" +
                if (pathIndex >= 0) " (point ${pathIndex + 1}/$pathTotal)" else "")

        // Pošlji na strežnik
        val result = simulationRepository.submitSimulatedGps(latitude, longitude)

        when (result) {
            is Resource.Success -> {
                Log.i(TAG, "GPS simulation sent successfully")
                broadcastData(SimulationSensorType.GPS, latitude, longitude, 0f, timestamp, pathIndex, pathTotal)
                val notificationText = if (pathIndex >= 0) {
                    "%.5f, %.5f (%d/%d)".format(latitude, longitude, pathIndex + 1, pathTotal)
                } else {
                    "%.5f, %.5f".format(latitude, longitude)
                }
                updateNotification("GPS", notificationText)
                broadcastStatus(true, "GPS podatki poslani")
            }
            is Resource.Error -> {
                Log.e(TAG, "Failed to send GPS simulation: ${result.message}")
                broadcastStatus(false, result.message)
            }
            is Resource.Loading -> {}
        }
    }

    private fun moveToNextPathPoint() {
        if (routePath.isEmpty()) return

        currentPathIndex += routeDirection

        // Ping-pong: ko pridemo do konca, obrnemo smer
        if (currentPathIndex >= routePath.size) {
            currentPathIndex = routePath.size - 2
            routeDirection = -1
            Log.d(TAG, "Reached end of route, reversing direction")
        } else if (currentPathIndex < 0) {
            currentPathIndex = 1
            routeDirection = 1
            Log.d(TAG, "Reached start of route, reversing direction")
        }

        // Shrani stanje za nadaljevanje
        preferencesManager.simulationPathIndex = currentPathIndex
        preferencesManager.simulationRouteDirection = routeDirection
    }

    private suspend fun simulateSpeed() {
        val latitude: Double
        val longitude: Double

        if (preferencesManager.useManualLocation) {
            latitude = preferencesManager.simulationManualLat.toDouble()
            longitude = preferencesManager.simulationManualLon.toDouble()
        } else {
            latitude = generateRandomValue(
                preferencesManager.simulationGpsLatMin,
                preferencesManager.simulationGpsLatMax
            ).toDouble()
            longitude = generateRandomValue(
                preferencesManager.simulationGpsLonMin,
                preferencesManager.simulationGpsLonMax
            ).toDouble()
        }

        val speed = generateRandomValue(
            preferencesManager.simulationSpeedMin,
            preferencesManager.simulationSpeedMax
        )
        val timestamp = System.currentTimeMillis()

        Log.d(TAG, "Simulating Speed: speed=$speed km/h at lat=$latitude, lon=$longitude")

        // Pošlji na strežnik
        val result = simulationRepository.submitSimulatedSpeed(speed, latitude, longitude)

        when (result) {
            is Resource.Success -> {
                Log.i(TAG, "Speed simulation sent successfully")
                broadcastData(SimulationSensorType.SPEED, latitude, longitude, speed, timestamp)
                updateNotification("Hitrost", "%.1f km/h".format(speed))
                broadcastStatus(true, "Hitrostni podatki poslani")
            }
            is Resource.Error -> {
                Log.e(TAG, "Failed to send speed simulation: ${result.message}")
                broadcastStatus(false, result.message)
            }
            is Resource.Loading -> {}
        }
    }

    private fun generateRandomValue(min: Float, max: Float): Float {
        return min + Random.nextFloat() * (max - min)
    }

    private fun broadcastData(
        sensorType: SimulationSensorType,
        latitude: Double,
        longitude: Double,
        speed: Float,
        timestamp: Long,
        pathIndex: Int = -1,
        pathTotal: Int = -1
    ) {
        val intent = Intent(ACTION_SIMULATION_DATA).apply {
            putExtra(EXTRA_SENSOR_TYPE, sensorType.name)
            putExtra(EXTRA_LATITUDE, latitude)
            putExtra(EXTRA_LONGITUDE, longitude)
            putExtra(EXTRA_SPEED, speed)
            putExtra(EXTRA_TIMESTAMP, timestamp)
            putExtra(EXTRA_PATH_INDEX, pathIndex)
            putExtra(EXTRA_PATH_TOTAL, pathTotal)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun broadcastStatus(success: Boolean, message: String?) {
        val intent = Intent(ACTION_SIMULATION_STATUS).apply {
            putExtra(EXTRA_SUCCESS, success)
            putExtra(EXTRA_MESSAGE, message)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun updateNotification(sensorType: String, lastValue: String) {
        val notification = NotificationHelper.createSimulationNotification(this, sensorType, lastValue)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NotificationHelper.SIMULATION_NOTIFICATION_ID, notification)
    }
}
