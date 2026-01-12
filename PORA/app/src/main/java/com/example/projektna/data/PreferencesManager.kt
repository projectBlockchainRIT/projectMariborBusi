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

        // Simulation mode keys
        private const val KEY_SIMULATION_ENABLED = "simulation_enabled"
        private const val KEY_SIMULATION_SENSOR_TYPE = "simulation_sensor_type"
        private const val KEY_SIMULATION_INTERVAL = "simulation_interval"
        private const val KEY_SIMULATION_GPS_LAT_MIN = "simulation_gps_lat_min"
        private const val KEY_SIMULATION_GPS_LAT_MAX = "simulation_gps_lat_max"
        private const val KEY_SIMULATION_GPS_LON_MIN = "simulation_gps_lon_min"
        private const val KEY_SIMULATION_GPS_LON_MAX = "simulation_gps_lon_max"
        private const val KEY_SIMULATION_SPEED_MIN = "simulation_speed_min"
        private const val KEY_SIMULATION_SPEED_MAX = "simulation_speed_max"
        private const val KEY_SIMULATION_MANUAL_LAT = "simulation_manual_lat"
        private const val KEY_SIMULATION_MANUAL_LON = "simulation_manual_lon"
        private const val KEY_SIMULATION_USE_MANUAL_LOCATION = "simulation_use_manual_location"

        const val DEFAULT_ACCELEROMETER_INTERVAL = 1 // sekunde
        const val DEFAULT_GPS_INTERVAL = 5 // sekunde
        const val DEFAULT_SIMULATION_INTERVAL = 10 // sekunde

        // Default simulation ranges - Maribor area
        const val DEFAULT_GPS_LAT_MIN = 46.54f
        const val DEFAULT_GPS_LAT_MAX = 46.57f
        const val DEFAULT_GPS_LON_MIN = 15.63f
        const val DEFAULT_GPS_LON_MAX = 15.67f
        const val DEFAULT_SPEED_MIN = 0f // km/h
        const val DEFAULT_SPEED_MAX = 50f // km/h
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

    // Simulation mode properties
    var isSimulationEnabled: Boolean
        get() = prefs.getBoolean(KEY_SIMULATION_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SIMULATION_ENABLED, value).apply()

    var simulationSensorType: SimulationSensorType
        get() = SimulationSensorType.fromOrdinal(prefs.getInt(KEY_SIMULATION_SENSOR_TYPE, 0))
        set(value) = prefs.edit().putInt(KEY_SIMULATION_SENSOR_TYPE, value.ordinal).apply()

    var simulationIntervalSeconds: Int
        get() = prefs.getInt(KEY_SIMULATION_INTERVAL, DEFAULT_SIMULATION_INTERVAL)
        set(value) = prefs.edit().putInt(KEY_SIMULATION_INTERVAL, value).apply()

    var simulationGpsLatMin: Float
        get() = prefs.getFloat(KEY_SIMULATION_GPS_LAT_MIN, DEFAULT_GPS_LAT_MIN)
        set(value) = prefs.edit().putFloat(KEY_SIMULATION_GPS_LAT_MIN, value).apply()

    var simulationGpsLatMax: Float
        get() = prefs.getFloat(KEY_SIMULATION_GPS_LAT_MAX, DEFAULT_GPS_LAT_MAX)
        set(value) = prefs.edit().putFloat(KEY_SIMULATION_GPS_LAT_MAX, value).apply()

    var simulationGpsLonMin: Float
        get() = prefs.getFloat(KEY_SIMULATION_GPS_LON_MIN, DEFAULT_GPS_LON_MIN)
        set(value) = prefs.edit().putFloat(KEY_SIMULATION_GPS_LON_MIN, value).apply()

    var simulationGpsLonMax: Float
        get() = prefs.getFloat(KEY_SIMULATION_GPS_LON_MAX, DEFAULT_GPS_LON_MAX)
        set(value) = prefs.edit().putFloat(KEY_SIMULATION_GPS_LON_MAX, value).apply()

    var simulationSpeedMin: Float
        get() = prefs.getFloat(KEY_SIMULATION_SPEED_MIN, DEFAULT_SPEED_MIN)
        set(value) = prefs.edit().putFloat(KEY_SIMULATION_SPEED_MIN, value).apply()

    var simulationSpeedMax: Float
        get() = prefs.getFloat(KEY_SIMULATION_SPEED_MAX, DEFAULT_SPEED_MAX)
        set(value) = prefs.edit().putFloat(KEY_SIMULATION_SPEED_MAX, value).apply()

    var simulationManualLat: Float
        get() = prefs.getFloat(KEY_SIMULATION_MANUAL_LAT, DEFAULT_GPS_LAT_MIN)
        set(value) = prefs.edit().putFloat(KEY_SIMULATION_MANUAL_LAT, value).apply()

    var simulationManualLon: Float
        get() = prefs.getFloat(KEY_SIMULATION_MANUAL_LON, DEFAULT_GPS_LON_MIN)
        set(value) = prefs.edit().putFloat(KEY_SIMULATION_MANUAL_LON, value).apply()

    var useManualLocation: Boolean
        get() = prefs.getBoolean(KEY_SIMULATION_USE_MANUAL_LOCATION, false)
        set(value) = prefs.edit().putBoolean(KEY_SIMULATION_USE_MANUAL_LOCATION, value).apply()
}

enum class SimulationSensorType {
    GPS,
    SPEED;

    companion object {
        fun fromOrdinal(ordinal: Int): SimulationSensorType {
            return entries.getOrElse(ordinal) { GPS }
        }
    }
}
