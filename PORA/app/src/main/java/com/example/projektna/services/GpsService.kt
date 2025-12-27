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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class GpsService : Service() {

    companion object {
        const val TAG = "GpsService"
        const val ACTION_GPS_DATA = "com.example.projektna.GPS_DATA"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_TIMESTAMP = "timestamp"
        const val EXTRA_IS_EXTREME = "is_extreme"

        private const val UPDATE_INTERVAL = 5000L // 5 sekund
        private const val FASTEST_INTERVAL = 3000L
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var lastLatitude = 0.0
    private var lastLongitude = 0.0
    private var lastSpeedKmh = 0f

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        NotificationHelper.createGpsNotificationChannel(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val speed = location.speed // m/s
                    val isExtreme = speed > GpsData.SPEED_THRESHOLD

                    lastLatitude = location.latitude
                    lastLongitude = location.longitude
                    lastSpeedKmh = speed * 3.6f

                    if (isExtreme) {
                        Log.w(TAG, "EXTREME EVENT DETECTED! Speed: ${lastSpeedKmh} km/h")
                    }

                    broadcastData(
                        location.latitude,
                        location.longitude,
                        speed,
                        System.currentTimeMillis(),
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
    }

    override fun onBind(intent: Intent?): IBinder? = null

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

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
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
