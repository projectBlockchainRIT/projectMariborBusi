package com.example.projektna.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.projektna.data.AccelerometerData
import com.example.projektna.data.PreferencesManager
import com.example.projektna.data.mqtt.MqttManager
import com.example.projektna.data.repository.CollisionRepository
import com.example.projektna.util.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AccelerometerService : Service(), SensorEventListener, MqttManager.MqttConnectionCallback {

    companion object {
        const val TAG = "AccelerometerService"
        const val ACTION_ACCELEROMETER_DATA = "com.example.projektna.ACCELEROMETER_DATA"
        const val ACTION_COLLISION_REPORTED = "com.example.projektna.COLLISION_REPORTED"
        const val EXTRA_MAGNITUDE = "magnitude"
        const val EXTRA_TIMESTAMP = "timestamp"
        const val EXTRA_IS_EXTREME = "is_extreme"
        const val EXTRA_IS_COLLISION = "is_collision"
        const val EXTRA_COLLISION_SUCCESS = "collision_success"
        const val EXTRA_COLLISION_MESSAGE = "collision_message"

        // Cooldown med zaporednimi poročili o trčenju (5 sekund)
        private const val COLLISION_COOLDOWN_MS = 5000L
    }

    private lateinit var sensorManager: SensorManager
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var collisionRepository: CollisionRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var accelerometer: Sensor? = null
    private var lastUpdateTime = 0L
    private var lastCollisionReportTime = 0L
    private var updateIntervalMs = 1000L // privzeto 1 sekunda

    // Zadnja znana GPS lokacija
    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null

    // MQTT
    private var isMqttEnabled = false
    private var isMqttConnected = false

    // Receiver za GPS podatke
    private val gpsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                lastLatitude = it.getDoubleExtra(GpsService.EXTRA_LATITUDE, 0.0).takeIf { lat -> lat != 0.0 }
                lastLongitude = it.getDoubleExtra(GpsService.EXTRA_LONGITUDE, 0.0).takeIf { lon -> lon != 0.0 }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        NotificationHelper.createNotificationChannel(this)

        preferencesManager = PreferencesManager(this)
        collisionRepository = CollisionRepository()
        updateIntervalMs = preferencesManager.accelerometerIntervalSeconds * 1000L
        isMqttEnabled = preferencesManager.isMqttEnabled
        Log.d(TAG, "Update interval: ${updateIntervalMs}ms, MQTT enabled: $isMqttEnabled")

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            Log.e(TAG, "Accelerometer not available on this device")
            stopSelf()
            return
        }

        // Vzpostavi MQTT povezavo
        if (isMqttEnabled) {
            Log.d(TAG, "MQTT enabled, connecting to broker...")
            MqttManager.connect(this, this)
        }

        // Registriraj receiver za GPS podatke
        val gpsFilter = IntentFilter(GpsService.ACTION_GPS_DATA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(gpsReceiver, gpsFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(gpsReceiver, gpsFilter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")

        val notification = NotificationHelper.createForegroundNotification(this, 0f)
        startForeground(NotificationHelper.NOTIFICATION_ID, notification)

        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")
        sensorManager.unregisterListener(this)
        try {
            unregisterReceiver(gpsReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "GPS receiver already unregistered")
        }

        // Prekini MQTT povezavo
        if (isMqttEnabled) {
            MqttManager.disconnect()
        }

        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastUpdateTime >= updateIntervalMs) {
                    lastUpdateTime = currentTime

                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]

                    val magnitude = sqrt(x * x + y * y + z * z)
                    val isExtreme = magnitude > AccelerometerData.EXTREME_THRESHOLD
                    val isCollision = magnitude > AccelerometerData.COLLISION_THRESHOLD

                    if (isExtreme) {
                        Log.w(TAG, "EXTREME EVENT DETECTED! Magnitude: $magnitude m/s²")
                    }

                    if (isCollision) {
                        Log.w(TAG, "COLLISION DETECTED! Magnitude: $magnitude m/s²")
                        // Pošlji na API če je pretekel cooldown
                        if (currentTime - lastCollisionReportTime >= COLLISION_COOLDOWN_MS) {
                            lastCollisionReportTime = currentTime
                            reportCollisionToApi(x, y, z, magnitude)
                        } else {
                            Log.d(TAG, "Collision cooldown active, skipping report")
                        }
                    }

                    // Pošlji preko MQTT
                    if (isMqttEnabled) {
                        val success = MqttManager.publishAccelerometer(
                            x = x,
                            y = y,
                            z = z,
                            magnitude = magnitude,
                            latitude = lastLatitude ?: 0.0,
                            longitude = lastLongitude ?: 0.0,
                            timestamp = currentTime,
                            isSimulated = false
                        )
                        if (success) {
                            Log.d(TAG, "Accelerometer data published to MQTT")
                        }
                    }

                    broadcastData(magnitude, currentTime, isExtreme, isCollision, x, y, z)
                    updateNotification(magnitude)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor accuracy changed: $accuracy")
    }

    private fun broadcastData(
        magnitude: Float,
        timestamp: Long,
        isExtreme: Boolean,
        isCollision: Boolean,
        x: Float,
        y: Float,
        z: Float
    ) {
        val intent = Intent(ACTION_ACCELEROMETER_DATA).apply {
            putExtra(EXTRA_MAGNITUDE, magnitude)
            putExtra(EXTRA_TIMESTAMP, timestamp)
            putExtra(EXTRA_IS_EXTREME, isExtreme)
            putExtra(EXTRA_IS_COLLISION, isCollision)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun reportCollisionToApi(x: Float, y: Float, z: Float, magnitude: Float) {
        serviceScope.launch {
            Log.d(TAG, "Reporting collision to API: magnitude=$magnitude, lat=$lastLatitude, lon=$lastLongitude")

            val result = collisionRepository.reportCollision(
                magnitude = magnitude,
                accelerometerX = x,
                accelerometerY = y,
                accelerometerZ = z,
                latitude = lastLatitude,
                longitude = lastLongitude
            )

            when (result) {
                is Resource.Success -> {
                    Log.i(TAG, "Collision reported successfully: ${result.data?.message}")
                    broadcastCollisionResult(true, result.data?.message)
                }
                is Resource.Error -> {
                    Log.e(TAG, "Failed to report collision: ${result.message}")
                    broadcastCollisionResult(false, result.message)
                }
                is Resource.Loading -> {
                    // Ne pričakujemo tega stanja tukaj
                }
            }
        }
    }

    private fun broadcastCollisionResult(success: Boolean, message: String?) {
        val intent = Intent(ACTION_COLLISION_REPORTED).apply {
            putExtra(EXTRA_COLLISION_SUCCESS, success)
            putExtra(EXTRA_COLLISION_MESSAGE, message)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun updateNotification(magnitude: Float) {
        val notification = NotificationHelper.createForegroundNotification(this, magnitude)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NotificationHelper.NOTIFICATION_ID, notification)
    }

    // MQTT Callback implementacija
    override fun onConnected() {
        Log.d(TAG, "MQTT connected")
        isMqttConnected = true
    }

    override fun onDisconnected(cause: Throwable?) {
        Log.d(TAG, "MQTT disconnected: ${cause?.message}")
        isMqttConnected = false
    }

    override fun onConnectionFailed(error: String) {
        Log.e(TAG, "MQTT connection failed: $error")
        isMqttConnected = false
    }

    override fun onMessagePublished(topic: String) {
        Log.d(TAG, "MQTT message published to $topic")
    }

    override fun onPublishFailed(topic: String, error: String) {
        Log.e(TAG, "MQTT publish failed to $topic: $error")
    }
}
