package com.example.projektna.services

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.projektna.data.GpsData
import com.example.projektna.data.PreferencesManager
import com.example.projektna.data.mqtt.MqttManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class GpsService : Service(), MqttManager.MqttConnectionCallback {

    companion object {
        const val TAG = "GpsService"
        const val ACTION_GPS_DATA = "com.example.projektna.GPS_DATA"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_TIMESTAMP = "timestamp"
        const val EXTRA_IS_EXTREME = "is_extreme"
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var preferencesManager: PreferencesManager

    private var updateIntervalMs = 5000L // privzeto 5 sekund
    private var lastLatitude = 0.0
    private var lastLongitude = 0.0
    private var lastSpeedKmh = 0f

    // MQTT
    private var isMqttEnabled = false
    private var isMqttConnected = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        NotificationHelper.createGpsNotificationChannel(this)

        preferencesManager = PreferencesManager(this)
        updateIntervalMs = preferencesManager.gpsIntervalSeconds * 1000L
        isMqttEnabled = preferencesManager.isMqttEnabled
        Log.d(TAG, "Update interval: ${updateIntervalMs}ms, MQTT enabled: $isMqttEnabled")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Vzpostavi MQTT povezavo
        if (isMqttEnabled) {
            Log.d(TAG, "MQTT enabled, connecting to broker...")
            MqttManager.connect(this, this)
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val speed = location.speed // m/s
                    val isExtreme = speed > GpsData.SPEED_THRESHOLD
                    val timestamp = System.currentTimeMillis()

                    lastLatitude = location.latitude
                    lastLongitude = location.longitude
                    lastSpeedKmh = speed * 3.6f

                    if (isExtreme) {
                        Log.w(TAG, "EXTREME EVENT DETECTED! Speed: ${lastSpeedKmh} km/h")
                    }

                    // Pošlji preko MQTT
                    if (isMqttEnabled) {
                        val success = MqttManager.publishGps(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            timestamp = timestamp,
                            isSimulated = false
                        )
                        if (success) {
                            Log.d(TAG, "GPS data published to MQTT")
                        }
                    }

                    broadcastData(
                        location.latitude,
                        location.longitude,
                        speed,
                        timestamp,
                        isExtreme
                    )
                    updateNotification()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")

        val notification = NotificationHelper.createGpsNotification(this, 0.0, 0.0, 0f)
        startForeground(NotificationHelper.GPS_NOTIFICATION_ID, notification)

        startLocationUpdates()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")
        stopLocationUpdates()

        // Prekini MQTT povezavo
        if (isMqttEnabled) {
            MqttManager.disconnect()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

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

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permission not granted")
            stopSelf()
            return
        }

        val fastestInterval = (updateIntervalMs * 0.6).toLong() // 60% od glavnega intervala
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, updateIntervalMs)
            .setMinUpdateIntervalMillis(fastestInterval)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun broadcastData(
        latitude: Double,
        longitude: Double,
        speed: Float,
        timestamp: Long,
        isExtreme: Boolean
    ) {
        val intent = Intent(ACTION_GPS_DATA).apply {
            putExtra(EXTRA_LATITUDE, latitude)
            putExtra(EXTRA_LONGITUDE, longitude)
            putExtra(EXTRA_SPEED, speed)
            putExtra(EXTRA_TIMESTAMP, timestamp)
            putExtra(EXTRA_IS_EXTREME, isExtreme)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun updateNotification() {
        val notification = NotificationHelper.createGpsNotification(
            this,
            lastLatitude,
            lastLongitude,
            lastSpeedKmh
        )
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NotificationHelper.GPS_NOTIFICATION_ID, notification)
    }
}
