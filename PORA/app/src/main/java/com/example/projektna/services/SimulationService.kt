package com.example.projektna.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.projektna.data.PreferencesManager
import com.example.projektna.data.SimulationSensorType
import com.example.projektna.data.mqtt.MqttManager
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

class SimulationService : Service(), MqttManager.MqttConnectionCallback {

    companion object {
        const val TAG = "SimulationService"
        const val ACTION_SIMULATION_DATA = "com.example.projektna.SIMULATION_DATA"
        const val ACTION_SIMULATION_STATUS = "com.example.projektna.SIMULATION_STATUS"
        const val ACTION_MQTT_STATUS = "com.example.projektna.MQTT_STATUS"
        const val EXTRA_SENSOR_TYPE = "sensor_type"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_TIMESTAMP = "timestamp"
        const val EXTRA_SUCCESS = "success"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_PATH_INDEX = "path_index"
        const val EXTRA_PATH_TOTAL = "path_total"
        const val EXTRA_MQTT_CONNECTED = "mqtt_connected"
        const val EXTRA_ACCEL_X = "accel_x"
        const val EXTRA_ACCEL_Y = "accel_y"
        const val EXTRA_ACCEL_Z = "accel_z"
        const val EXTRA_ACCEL_MAGNITUDE = "accel_magnitude"
    }

    // Route path data
    private var routePath: List<List<Double>> = emptyList()
    private var currentPathIndex: Int = 0
    private var routeDirection: Int = 1 // 1 = naprej, -1 = nazaj

    // MQTT state
    private var isMqttEnabled: Boolean = false
    private var isMqttConnected: Boolean = false

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

        // Preveri ali je MQTT omogočen in vzpostavi povezavo
        isMqttEnabled = preferencesManager.isMqttEnabled
        if (isMqttEnabled) {
            Log.d(TAG, "MQTT enabled, connecting to broker...")
            MqttManager.connect(this, this)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")

        val sensorType = preferencesManager.simulationSensorType
        val sensorName = when (sensorType) {
            SimulationSensorType.GPS -> "GPS"
            SimulationSensorType.ACCELEROMETER -> "Pospeškometer"
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

        // Prekini MQTT povezavo
        if (isMqttEnabled) {
            MqttManager.disconnect()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ==================== MQTT Callbacks ====================

    override fun onConnected() {
        Log.i(TAG, "MQTT connected")
        isMqttConnected = true
        broadcastMqttStatus(true, "MQTT povezan")
    }

    override fun onDisconnected(cause: Throwable?) {
        Log.w(TAG, "MQTT disconnected: ${cause?.message}")
        isMqttConnected = false
        broadcastMqttStatus(false, cause?.message ?: "MQTT prekinjen")
    }

    override fun onConnectionFailed(error: String) {
        Log.e(TAG, "MQTT connection failed: $error")
        isMqttConnected = false
        broadcastMqttStatus(false, "Povezava neuspešna: $error")
    }

    override fun onMessagePublished(topic: String) {
        Log.d(TAG, "MQTT message published to $topic")
    }

    override fun onPublishFailed(topic: String, error: String) {
        Log.e(TAG, "MQTT publish failed to $topic: $error")
        broadcastStatus(false, "MQTT objava neuspešna: $error")
    }

    private fun broadcastMqttStatus(connected: Boolean, message: String?) {
        val intent = Intent(ACTION_MQTT_STATUS).apply {
            putExtra(EXTRA_MQTT_CONNECTED, connected)
            putExtra(EXTRA_MESSAGE, message)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

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
                        SimulationSensorType.ACCELEROMETER -> simulateAccelerometer()
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

        // Pošlji na MQTT ali HTTP glede na nastavitve
        if (isMqttEnabled) {
            // MQTT objava
            val success = MqttManager.publishGps(latitude, longitude, timestamp, true)
            if (success || isMqttConnected) {
                Log.i(TAG, "GPS simulation published to MQTT")
                broadcastData(SimulationSensorType.GPS, latitude, longitude, timestamp, pathIndex, pathTotal)
                val notificationText = if (pathIndex >= 0) {
                    "MQTT: %.5f, %.5f (%d/%d)".format(latitude, longitude, pathIndex + 1, pathTotal)
                } else {
                    "MQTT: %.5f, %.5f".format(latitude, longitude)
                }
                updateNotification("GPS", notificationText)
                broadcastStatus(true, "GPS podatki objavljeni na MQTT")
            } else {
                Log.e(TAG, "Failed to publish GPS to MQTT - not connected")
                broadcastStatus(false, "MQTT ni povezan")
            }
        } else {
            // HTTP pošiljanje (stara metoda)
            val result = simulationRepository.submitSimulatedGps(latitude, longitude)

            when (result) {
                is Resource.Success -> {
                    Log.i(TAG, "GPS simulation sent successfully via HTTP")
                    broadcastData(SimulationSensorType.GPS, latitude, longitude, timestamp, pathIndex, pathTotal)
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

    private suspend fun simulateAccelerometer() {
        // Generiraj naključne vrednosti za x, y, z
        val minMag = preferencesManager.simulationAccelMin
        val maxMag = preferencesManager.simulationAccelMax

        // Generiraj magnitude v razponu
        val magnitude = generateRandomValue(minMag, maxMag)

        // Razdeli magnitude na x, y, z komponente naključno
        // Uporabimo enostavno distribucijo
        val x = generateRandomValue(-magnitude, magnitude)
        val remainingForYZ = kotlin.math.sqrt((magnitude * magnitude - x * x).coerceAtLeast(0f))
        val y = generateRandomValue(-remainingForYZ, remainingForYZ)
        val z = kotlin.math.sqrt((magnitude * magnitude - x * x - y * y).coerceAtLeast(0f)) *
                (if (Random.nextBoolean()) 1 else -1)

        // Pridobi lokacijo (ročna ali privzeta)
        val latitude: Double
        val longitude: Double
        if (preferencesManager.useManualLocation) {
            latitude = preferencesManager.simulationManualLat.toDouble()
            longitude = preferencesManager.simulationManualLon.toDouble()
        } else {
            // Privzete koordinate (center Maribora)
            latitude = 46.5547
            longitude = 15.6459
        }

        val timestamp = System.currentTimeMillis()

        Log.d(TAG, "Simulating Accelerometer: x=%.2f, y=%.2f, z=%.2f, mag=%.2f m/s², loc=%.5f,%.5f".format(
            x, y, z, magnitude, latitude, longitude))

        // Pošlji na MQTT ali HTTP glede na nastavitve
        if (isMqttEnabled) {
            // MQTT objava z lokacijo
            val success = MqttManager.publishAccelerometer(x, y, z, magnitude, latitude, longitude, timestamp, true)
            if (success || isMqttConnected) {
                Log.i(TAG, "Accelerometer simulation published to MQTT")
                broadcastAccelerometerData(x, y, z, magnitude, latitude, longitude, timestamp)
                updateNotification("Pospeškometer", "MQTT: %.2f m/s²".format(magnitude))
                broadcastStatus(true, "Pospeškometer podatki objavljeni na MQTT")
            } else {
                Log.e(TAG, "Failed to publish accelerometer to MQTT - not connected")
                broadcastStatus(false, "MQTT ni povezan")
            }
        } else {
            // HTTP pošiljanje ni implementirano za accelerometer simulacijo
            // Samo broadcast za UI
            Log.i(TAG, "Accelerometer simulation (HTTP not implemented, MQTT disabled)")
            broadcastAccelerometerData(x, y, z, magnitude, latitude, longitude, timestamp)
            updateNotification("Pospeškometer", "%.2f m/s²".format(magnitude))
            broadcastStatus(true, "Pospeškometer simulacija (samo lokalno)")
        }
    }

    private fun broadcastAccelerometerData(
        x: Float,
        y: Float,
        z: Float,
        magnitude: Float,
        latitude: Double,
        longitude: Double,
        timestamp: Long
    ) {
        val intent = Intent(ACTION_SIMULATION_DATA).apply {
            putExtra(EXTRA_SENSOR_TYPE, SimulationSensorType.ACCELEROMETER.name)
            putExtra(EXTRA_ACCEL_X, x)
            putExtra(EXTRA_ACCEL_Y, y)
            putExtra(EXTRA_ACCEL_Z, z)
            putExtra(EXTRA_ACCEL_MAGNITUDE, magnitude)
            putExtra(EXTRA_LATITUDE, latitude)
            putExtra(EXTRA_LONGITUDE, longitude)
            putExtra(EXTRA_TIMESTAMP, timestamp)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun generateRandomValue(min: Float, max: Float): Float {
        return min + Random.nextFloat() * (max - min)
    }

    private fun broadcastData(
        sensorType: SimulationSensorType,
        latitude: Double,
        longitude: Double,
        timestamp: Long,
        pathIndex: Int = -1,
        pathTotal: Int = -1
    ) {
        val intent = Intent(ACTION_SIMULATION_DATA).apply {
            putExtra(EXTRA_SENSOR_TYPE, sensorType.name)
            putExtra(EXTRA_LATITUDE, latitude)
            putExtra(EXTRA_LONGITUDE, longitude)
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
