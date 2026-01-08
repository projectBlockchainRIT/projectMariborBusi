package com.example.projektna.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "projektna_prefs"

        // Authentication keys
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_USERNAME = "user_username"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"

        // Sensor keys
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
        private const val KEY_SIMULATION_ACCEL_MIN = "simulation_accel_min"
        private const val KEY_SIMULATION_ACCEL_MAX = "simulation_accel_max"
        private const val KEY_SIMULATION_MANUAL_LAT = "simulation_manual_lat"
        private const val KEY_SIMULATION_MANUAL_LON = "simulation_manual_lon"
        private const val KEY_SIMULATION_USE_MANUAL_LOCATION = "simulation_use_manual_location"

        // Simulation route following keys
        private const val KEY_SIMULATION_FOLLOW_ROUTE = "simulation_follow_route"
        private const val KEY_SIMULATION_ROUTE_ID = "simulation_route_id"
        private const val KEY_SIMULATION_ROUTE_NAME = "simulation_route_name"
        private const val KEY_SIMULATION_ROUTE_PATH = "simulation_route_path"
        private const val KEY_SIMULATION_PATH_INDEX = "simulation_path_index"
        private const val KEY_SIMULATION_ROUTE_DIRECTION = "simulation_route_direction"

        // MQTT keys
        private const val KEY_MQTT_ENABLED = "mqtt_enabled"
        private const val KEY_MQTT_BROKER_URL = "mqtt_broker_url"
        private const val KEY_MQTT_USERNAME = "mqtt_username"
        private const val KEY_MQTT_PASSWORD = "mqtt_password"

        // MQTT defaults
        const val DEFAULT_MQTT_BROKER_URL = "tcp://10.0.2.2:1883"  // Android emulator localhost

        const val DEFAULT_ACCELEROMETER_INTERVAL = 1 // sekunde
        const val DEFAULT_GPS_INTERVAL = 5 // sekunde
        const val DEFAULT_SIMULATION_INTERVAL = 10 // sekunde

        // Default simulation ranges - Maribor area
        const val DEFAULT_GPS_LAT_MIN = 46.54f
        const val DEFAULT_GPS_LAT_MAX = 46.57f
        const val DEFAULT_GPS_LON_MIN = 15.63f
        const val DEFAULT_GPS_LON_MAX = 15.67f
        const val DEFAULT_ACCEL_MIN = 0f // m/s²
        const val DEFAULT_ACCEL_MAX = 15f // m/s² (do praga ekstremnega dogodka)
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ==================== Authentication ====================

    var authToken: String?
        get() = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()

    var userId: Int
        get() = prefs.getInt(KEY_USER_ID, -1)
        set(value) = prefs.edit().putInt(KEY_USER_ID, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userUsername: String?
        get() = prefs.getString(KEY_USER_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_USER_USERNAME, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    /**
     * Shrani podatke po uspešni prijavi.
     */
    fun saveLoginData(token: String, visitorId: Int, email: String, username: String? = null) {
        authToken = token
        userId = visitorId
        userEmail = email
        userUsername = username
        isLoggedIn = true
    }

    /**
     * Počisti podatke ob odjavi.
     */
    fun clearAuthData() {
        authToken = null
        userId = -1
        userEmail = null
        userUsername = null
        isLoggedIn = false
    }

    // ==================== Sensors ====================

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

    var simulationAccelMin: Float
        get() = prefs.getFloat(KEY_SIMULATION_ACCEL_MIN, DEFAULT_ACCEL_MIN)
        set(value) = prefs.edit().putFloat(KEY_SIMULATION_ACCEL_MIN, value).apply()

    var simulationAccelMax: Float
        get() = prefs.getFloat(KEY_SIMULATION_ACCEL_MAX, DEFAULT_ACCEL_MAX)
        set(value) = prefs.edit().putFloat(KEY_SIMULATION_ACCEL_MAX, value).apply()

    var simulationManualLat: Float
        get() = prefs.getFloat(KEY_SIMULATION_MANUAL_LAT, DEFAULT_GPS_LAT_MIN)
        set(value) = prefs.edit().putFloat(KEY_SIMULATION_MANUAL_LAT, value).apply()

    var simulationManualLon: Float
        get() = prefs.getFloat(KEY_SIMULATION_MANUAL_LON, DEFAULT_GPS_LON_MIN)
        set(value) = prefs.edit().putFloat(KEY_SIMULATION_MANUAL_LON, value).apply()

    var useManualLocation: Boolean
        get() = prefs.getBoolean(KEY_SIMULATION_USE_MANUAL_LOCATION, false)
        set(value) = prefs.edit().putBoolean(KEY_SIMULATION_USE_MANUAL_LOCATION, value).apply()

    // Route following simulation properties
    var simulationFollowRoute: Boolean
        get() = prefs.getBoolean(KEY_SIMULATION_FOLLOW_ROUTE, false)
        set(value) = prefs.edit().putBoolean(KEY_SIMULATION_FOLLOW_ROUTE, value).apply()

    var simulationRouteId: Long
        get() = prefs.getLong(KEY_SIMULATION_ROUTE_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_SIMULATION_ROUTE_ID, value).apply()

    var simulationRouteName: String?
        get() = prefs.getString(KEY_SIMULATION_ROUTE_NAME, null)
        set(value) = prefs.edit().putString(KEY_SIMULATION_ROUTE_NAME, value).apply()

    var simulationRoutePath: String?
        get() = prefs.getString(KEY_SIMULATION_ROUTE_PATH, null)
        set(value) = prefs.edit().putString(KEY_SIMULATION_ROUTE_PATH, value).apply()

    var simulationPathIndex: Int
        get() = prefs.getInt(KEY_SIMULATION_PATH_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_SIMULATION_PATH_INDEX, value).apply()

    var simulationRouteDirection: Int
        get() = prefs.getInt(KEY_SIMULATION_ROUTE_DIRECTION, 1) // 1 = naprej, -1 = nazaj
        set(value) = prefs.edit().putInt(KEY_SIMULATION_ROUTE_DIRECTION, value).apply()

    /**
     * Počisti podatke o poti ob koncu simulacije ali spremembi linije.
     */
    fun resetRouteProgress() {
        simulationPathIndex = 0
        simulationRouteDirection = 1
    }

    // ==================== MQTT ====================

    var isMqttEnabled: Boolean
        get() = prefs.getBoolean(KEY_MQTT_ENABLED, true)  // MQTT privzeto omogočen
        set(value) = prefs.edit().putBoolean(KEY_MQTT_ENABLED, value).apply()

    var mqttBrokerUrl: String
        get() = prefs.getString(KEY_MQTT_BROKER_URL, DEFAULT_MQTT_BROKER_URL) ?: DEFAULT_MQTT_BROKER_URL
        set(value) = prefs.edit().putString(KEY_MQTT_BROKER_URL, value).apply()

    var mqttUsername: String
        get() = prefs.getString(KEY_MQTT_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MQTT_USERNAME, value).apply()

    var mqttPassword: String
        get() = prefs.getString(KEY_MQTT_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MQTT_PASSWORD, value).apply()
}

enum class SimulationSensorType {
    GPS,
    ACCELEROMETER;

    companion object {
        fun fromOrdinal(ordinal: Int): SimulationSensorType {
            return entries.getOrElse(ordinal) { GPS }
        }
    }
}
