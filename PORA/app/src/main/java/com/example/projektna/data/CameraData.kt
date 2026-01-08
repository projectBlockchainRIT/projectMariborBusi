package com.example.projektna.data

data class CameraData(
    val imagePath: String,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val isPendingUpload: Boolean = true
)
