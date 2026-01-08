package com.example.projektna.data

data class GpsData(
    val latitude: Double,
    val longitude: Double,
    val speed: Float, // m/s
    val timestamp: Long,
    val isExtreme: Boolean
) {
    companion object {
        const val SPEED_THRESHOLD = 33.33f // 120 km/h v m/s
    }

    fun getSpeedKmh(): Float = speed * 3.6f
}
