package com.example.projektna.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "projektna_prefs"
        private const val KEY_ACCELEROMETER_ENABLED = "accelerometer_enabled"
        private const val KEY_CAMERA_ENABLED = "camera_enabled"
        private const val KEY_GPS_ENABLED = "gps_enabled"
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
}
