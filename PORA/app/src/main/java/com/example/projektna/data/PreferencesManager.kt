package com.example.projektna.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "projektna_prefs"
        private const val KEY_ACCELEROMETER_ENABLED = "accelerometer_enabled"
        private const val KEY_CAMERA_ENABLED = "camera_enabled"
        private const val KEY_GPS_ENABLED = "gps_enabled"
        private const val KEY_ACCELEROMETER_INTERVAL = "accelerometer_interval"
        private const val KEY_GPS_INTERVAL = "gps_interval"

        const val DEFAULT_ACCELEROMETER_INTERVAL = 1 // sekunde
        const val DEFAULT_GPS_INTERVAL = 5 // sekunde
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isAccelerometerEnabled: Boolean
        get() = prefs.getBoolean(KEY_ACCELEROMETER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ACCELEROMETER_ENABLED, value).apply()

    var isCameraEnabled: Boolean
        get() = prefs.getBoolean(KEY_CAMERA_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_CAMERA_ENABLED, value).apply()

    var isGpsEnabled: Boolean
        get() = prefs.getBoolean(KEY_GPS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_GPS_ENABLED, value).apply()

    var accelerometerIntervalSeconds: Int
        get() = prefs.getInt(KEY_ACCELEROMETER_INTERVAL, DEFAULT_ACCELEROMETER_INTERVAL)
        set(value) = prefs.edit().putInt(KEY_ACCELEROMETER_INTERVAL, value).apply()

    var gpsIntervalSeconds: Int
        get() = prefs.getInt(KEY_GPS_INTERVAL, DEFAULT_GPS_INTERVAL)
        set(value) = prefs.edit().putInt(KEY_GPS_INTERVAL, value).apply()
}
