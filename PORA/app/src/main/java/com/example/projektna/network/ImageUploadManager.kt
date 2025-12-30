package com.example.projektna.network

import android.content.Context
import android.util.Base64
import com.example.projektna.data.CameraData
import java.io.File

/**
 * Manager for uploading images to the server.
 * Currently a framework/skeleton that will be integrated with MQTT client.
 */
class ImageUploadManager(private val context: Context) {

    private var uploadListener: UploadListener? = null
    private var isConnected = false

    interface UploadListener {
        fun onUploadStarted(cameraData: CameraData)
        fun onUploadProgress(cameraData: CameraData, progress: Int)
        fun onUploadSuccess(cameraData: CameraData)
        fun onUploadFailed(cameraData: CameraData, error: String)
    }

    fun setUploadListener(listener: UploadListener?) {
        this.uploadListener = listener
    }

    /**
     * Check if connected to MQTT broker.
     * TODO: Implement actual MQTT connection check
     */
    fun isConnectedToServer(): Boolean {
        return isConnected
    }

    /**
     * Set connection status (will be managed by MQTT client later)
     */
    fun setConnectionStatus(connected: Boolean) {
        isConnected = connected
    }

    /**
     * Upload image to server via MQTT.
     * Currently simulates the upload process.
     *
     * @param cameraData The camera data containing image path and metadata
     */
    fun uploadImage(cameraData: CameraData) {
        uploadListener?.onUploadStarted(cameraData)

        // TODO: Replace with actual MQTT publish when MQTT client is integrated
        // The flow will be:
        // 1. Read image file and encode to Base64
        // 2. Create JSON payload with image data, timestamp, location
        // 3. Publish to MQTT topic (e.g., "sensors/camera/image")

        Thread {
            try {
                val imageFile = File(cameraData.imagePath)
                if (!imageFile.exists()) {
                    uploadListener?.onUploadFailed(cameraData, "Slika ne obstaja")
                    return@Thread
                }

                // Prepare upload payload
                val payload = prepareUploadPayload(cameraData, imageFile)

                // Simulate upload progress
                for (progress in listOf(25, 50, 75, 100)) {
                    Thread.sleep(200)
                    uploadListener?.onUploadProgress(cameraData, progress)
                }

                // TODO: Actual MQTT publish
                // mqttClient.publish("sensors/camera/image", payload)

                // For now, simulate success after "upload"
                if (isConnected) {
                    uploadListener?.onUploadSuccess(cameraData)
                } else {
                    uploadListener?.onUploadFailed(cameraData, "Ni povezave s strežnikom")
                }

            } catch (e: Exception) {
                uploadListener?.onUploadFailed(cameraData, e.message ?: "Neznana napaka")
            }
        }.start()
    }

    /**
     * Prepare the upload payload.
     * Creates a JSON-like structure ready for MQTT publish.
     */
    private fun prepareUploadPayload(cameraData: CameraData, imageFile: File): UploadPayload {
        val imageBytes = imageFile.readBytes()
        val imageBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        return UploadPayload(
            imageBase64 = imageBase64,
            timestamp = cameraData.timestamp,
            latitude = cameraData.latitude,
            longitude = cameraData.longitude,
            fileName = imageFile.name,
            fileSize = imageFile.length()
        )
    }

    /**
     * Data class representing the upload payload structure.
     * Will be serialized to JSON for MQTT publish.
     */
    data class UploadPayload(
        val imageBase64: String,
        val timestamp: Long,
        val latitude: Double?,
        val longitude: Double?,
        val fileName: String,
        val fileSize: Long
    )

    companion object {
        const val MQTT_TOPIC_CAMERA = "sensors/camera/image"
    }
}
