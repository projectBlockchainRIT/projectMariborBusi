package com.example.projektna.data

data class AccelerometerData(
    val magnitude: Float,
    val timestamp: Long,
    val isExtreme: Boolean
) {
    companion object {
        const val EXTREME_THRESHOLD = 15f // m/s² (~1.5G)
    }
}
