package com.example.projektna.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.projektna.data.AppDatabase
import com.example.projektna.data.PreferencesManager
import com.example.projektna.data.schedule.ActionType
import com.example.projektna.data.schedule.ScheduleType
import com.example.projektna.data.schedule.SensorSchedule
import com.example.projektna.data.schedule.SensorType
import com.example.projektna.services.AccelerometerService
import com.example.projektna.services.GpsService
import com.example.projektna.services.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ScheduleReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScheduleReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: ${intent.action}")

        when (intent.action) {
            ScheduleManager.ACTION_TRIGGER_SCHEDULE -> {
                val scheduleId = intent.getLongExtra(ScheduleManager.EXTRA_SCHEDULE_ID, -1)
                if (scheduleId != -1L) {
                    handleScheduleTrigger(context, scheduleId)
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Device booted, rescheduling all alarms")
                CoroutineScope(Dispatchers.IO).launch {
                    ScheduleManager(context).rescheduleAllAlarms()
                }
            }
        }
    }

    private fun handleScheduleTrigger(context: Context, scheduleId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val schedule = database.scheduleDao().getScheduleById(scheduleId)

            if (schedule == null) {
                Log.w(TAG, "Schedule $scheduleId not found")
                return@launch
            }

            if (!schedule.isEnabled) {
                Log.d(TAG, "Schedule $scheduleId is disabled, skipping")
                return@launch
            }

            Log.d(TAG, "Triggering schedule: $schedule")

            when (schedule.actionType) {
                ActionType.NOTIFICATION_ONLY -> {
                    showReminderNotification(context, schedule)
                }
                ActionType.AUTO_START -> {
                    startSensorService(context, schedule)
                    scheduleServiceStop(context, schedule)
                }
            }

            database.scheduleDao().updateLastTriggered(scheduleId, System.currentTimeMillis())

            if (schedule.scheduleType == ScheduleType.ONE_TIME) {
                database.scheduleDao().setEnabled(scheduleId, false)
                Log.d(TAG, "One-time schedule $scheduleId disabled after trigger")
            } else {
                ScheduleManager(context).scheduleAlarm(schedule)
                Log.d(TAG, "Rescheduled recurring alarm for $scheduleId")
            }
        }
    }

    private fun showReminderNotification(context: Context, schedule: SensorSchedule) {
        NotificationHelper.showScheduleReminderNotification(
            context,
            schedule.sensorType,
            schedule.id
        )
    }

    private fun startSensorService(context: Context, schedule: SensorSchedule) {
        val preferencesManager = PreferencesManager(context)

        when (schedule.sensorType) {
            SensorType.ACCELEROMETER -> {
                preferencesManager.isAccelerometerEnabled = true
                val intent = Intent(context, AccelerometerService::class.java)
                ContextCompat.startForegroundService(context, intent)
                Log.d(TAG, "Started AccelerometerService via schedule")
            }
            SensorType.GPS -> {
                preferencesManager.isGpsEnabled = true
                val intent = Intent(context, GpsService::class.java)
                ContextCompat.startForegroundService(context, intent)
                Log.d(TAG, "Started GpsService via schedule")
            }
            SensorType.CAMERA -> {
                showReminderNotification(context, schedule)
            }
        }
    }

    private fun scheduleServiceStop(context: Context, schedule: SensorSchedule) {
        val stopRequest = OneTimeWorkRequestBuilder<ServiceStopWorker>()
            .setInitialDelay(schedule.durationMinutes.toLong(), TimeUnit.MINUTES)
            .setInputData(
                workDataOf(
                    ServiceStopWorker.KEY_SENSOR_TYPE to schedule.sensorType.name,
                    ServiceStopWorker.KEY_SCHEDULE_ID to schedule.id
                )
            )
            .addTag("service_stop_${schedule.id}")
            .build()

        WorkManager.getInstance(context).enqueue(stopRequest)
        Log.d(TAG, "Scheduled service stop in ${schedule.durationMinutes} minutes")
    }
}
