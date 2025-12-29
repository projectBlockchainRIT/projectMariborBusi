package com.example.projektna.services

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import com.example.projektna.data.AccelerometerData
import kotlin.math.sqrt

class AccelerometerService : Service(), SensorEventListener {

    companion object {
        const val TAG = "AccelerometerService"
        const val ACTION_ACCELEROMETER_DATA = "com.example.projektna.ACCELEROMETER_DATA"
        const val EXTRA_MAGNITUDE = "magnitude"
        const val EXTRA_TIMESTAMP = "timestamp"
        const val EXTRA_IS_EXTREME = "is_extreme"
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastUpdateTime = 0L
    private val updateInterval = 200L // ms

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        NotificationHelper.createNotificationChannel(this)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            Log.e(TAG, "Accelerometer not available on this device")
            stopSelf()
            return
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
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastUpdateTime >= updateInterval) {
                    lastUpdateTime = currentTime

                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]

                    val magnitude = sqrt(x * x + y * y + z * z)
                    val isExtreme = magnitude > AccelerometerData.EXTREME_THRESHOLD

                    if (isExtreme) {
                        Log.w(TAG, "EXTREME EVENT DETECTED! Magnitude: $magnitude m/s²")
                    }

                    broadcastData(magnitude, currentTime, isExtreme)
                    updateNotification(magnitude)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor accuracy changed: $accuracy")
    }

    private fun broadcastData(magnitude: Float, timestamp: Long, isExtreme: Boolean) {
        val intent = Intent(ACTION_ACCELEROMETER_DATA).apply {
            putExtra(EXTRA_MAGNITUDE, magnitude)
            putExtra(EXTRA_TIMESTAMP, timestamp)
            putExtra(EXTRA_IS_EXTREME, isExtreme)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun updateNotification(magnitude: Float) {
        val notification = NotificationHelper.createForegroundNotification(this, magnitude)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NotificationHelper.NOTIFICATION_ID, notification)
    }
}
