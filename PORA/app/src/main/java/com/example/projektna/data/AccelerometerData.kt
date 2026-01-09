package com.example.projektna.data

data class AccelerometerData(
    val magnitude: Float,
    val timestamp: Long,
    val isExtreme: Boolean,
    val isCollision: Boolean = false
) {
    companion object {
        const val EXTREME_THRESHOLD = 15f // m/s² (~1.5G)
        const val COLLISION_THRESHOLD = 25f // m/s² (~2.5G) - prag za zaznavo trčenja
    }
}
