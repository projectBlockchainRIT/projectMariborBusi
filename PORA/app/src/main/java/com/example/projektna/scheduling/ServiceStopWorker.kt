package com.example.projektna.scheduling

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.projektna.data.PreferencesManager
import com.example.projektna.data.schedule.SensorType
import com.example.projektna.services.AccelerometerService
import com.example.projektna.services.GpsService

class ServiceStopWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ServiceStopWorker"
        const val KEY_SENSOR_TYPE = "sensor_type"
        const val KEY_SCHEDULE_ID = "schedule_id"
    }

    override suspend fun doWork(): Result {
        val sensorTypeName = inputData.getString(KEY_SENSOR_TYPE)
        val scheduleId = inputData.getLong(KEY_SCHEDULE_ID, -1)

        if (sensorTypeName == null) {
            Log.e(TAG, "Sensor type not provided")
            return Result.failure()
        }

        val sensorType = try {
            SensorType.valueOf(sensorTypeName)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid sensor type: $sensorTypeName")
            return Result.failure()
        }

        val preferencesManager = PreferencesManager(applicationContext)

        when (sensorType) {
            SensorType.ACCELEROMETER -> {
                preferencesManager.isAccelerometerEnabled = false
                applicationContext.stopService(
                    Intent(applicationContext, AccelerometerService::class.java)
                )
                Log.d(TAG, "Stopped AccelerometerService (schedule $scheduleId)")
            }
            SensorType.GPS -> {
                preferencesManager.isGpsEnabled = false
                applicationContext.stopService(
                    Intent(applicationContext, GpsService::class.java)
                )
                Log.d(TAG, "Stopped GpsService (schedule $scheduleId)")
            }
            SensorType.CAMERA -> {
                Log.d(TAG, "Camera has no service to stop")
            }
        }

        return Result.success()
    }
}
