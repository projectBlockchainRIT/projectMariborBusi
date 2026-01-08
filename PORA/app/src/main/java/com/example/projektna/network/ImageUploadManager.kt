package com.example.projektna.network

import android.content.Context
import com.example.projektna.data.CameraData
import com.example.projektna.data.repository.SimulationRepository
import com.example.projektna.util.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * Manager for uploading images to the server.
 * Uploads images via HTTP to /v1/simulation/image endpoint.
 */
class ImageUploadManager(private val context: Context) {

    private var uploadListener: UploadListener? = null
    private val repository = SimulationRepository()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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
     * Upload image to server via HTTP POST /v1/simulation/image.
     *
     * @param cameraData The camera data containing image path and metadata
     */
    fun uploadImage(cameraData: CameraData) {
        uploadListener?.onUploadStarted(cameraData)

        val imageFile = File(cameraData.imagePath)
        if (!imageFile.exists()) {
            uploadListener?.onUploadFailed(cameraData, "Slika ne obstaja")
            return
        }

        // Show indeterminate progress
        uploadListener?.onUploadProgress(cameraData, 50)

        scope.launch {
            val result = repository.uploadSimulatedImage(
                imagePath = cameraData.imagePath,
                timestamp = cameraData.timestamp,
                latitude = cameraData.latitude,
                longitude = cameraData.longitude
            )

            when (result) {
                is Resource.Success -> {
                    uploadListener?.onUploadProgress(cameraData, 100)
                    uploadListener?.onUploadSuccess(cameraData)
                }
                is Resource.Error -> {
                    uploadListener?.onUploadFailed(cameraData, result.message ?: "Neznana napaka")
                }
                is Resource.Loading -> {
                    // Not used
                }
            }
        }
    }
}
