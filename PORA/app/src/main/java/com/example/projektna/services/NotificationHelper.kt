package com.example.projektna.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.projektna.MainActivity
import com.example.projektna.R
import com.example.projektna.data.schedule.SensorType

object NotificationHelper {
    const val CHANNEL_ID = "accelerometer_channel"
    const val NOTIFICATION_ID = 1001

    const val GPS_CHANNEL_ID = "gps_channel"
    const val GPS_NOTIFICATION_ID = 1002

    const val SCHEDULE_CHANNEL_ID = "schedule_reminders"
    private const val SCHEDULE_NOTIFICATION_BASE_ID = 2000

    const val SIMULATION_CHANNEL_ID = "simulation_channel"
    const val SIMULATION_NOTIFICATION_ID = 1003

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                setShowBadge(false)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createForegroundNotification(context: Context, magnitude: Float): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_text, magnitude))
            .setSmallIcon(R.drawable.ic_accelerometer)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun createGpsNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                GPS_CHANNEL_ID,
                context.getString(R.string.gps_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.gps_notification_channel_description)
                setShowBadge(false)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createGpsNotification(context: Context, latitude: Double, longitude: Double, speedKmh: Float): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, GPS_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.gps_notification_title))
            .setContentText(context.getString(R.string.gps_notification_text, latitude, longitude, speedKmh))
            .setSmallIcon(R.drawable.ic_location)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun createScheduleNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SCHEDULE_CHANNEL_ID,
                context.getString(R.string.schedule_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.schedule_notification_channel_description)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createSimulationNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SIMULATION_CHANNEL_ID,
                context.getString(R.string.simulation_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.simulation_notification_channel_description)
                setShowBadge(false)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createSimulationNotification(context: Context, sensorType: String, lastValue: String): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, SIMULATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.simulation_notification_title))
            .setContentText(context.getString(R.string.simulation_notification_text, sensorType, lastValue))
            .setSmallIcon(R.drawable.ic_simulation)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun showScheduleReminderNotification(context: Context, sensorType: SensorType, scheduleId: Long) {
        createScheduleNotificationChannel(context)

        val title = when (sensorType) {
            SensorType.CAMERA -> context.getString(R.string.camera_reminder_title)
            SensorType.GPS -> context.getString(R.string.gps_reminder_title)
            SensorType.ACCELEROMETER -> context.getString(R.string.accelerometer_reminder_title)
        }

        val text = when (sensorType) {
            SensorType.CAMERA -> context.getString(R.string.camera_reminder_text)
            SensorType.GPS -> context.getString(R.string.gps_reminder_text)
            SensorType.ACCELEROMETER -> context.getString(R.string.accelerometer_reminder_text)
        }

        val icon = when (sensorType) {
            SensorType.CAMERA -> R.drawable.ic_camera
            SensorType.GPS -> R.drawable.ic_location
            SensorType.ACCELEROMETER -> R.drawable.ic_accelerometer
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "sensors")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            SCHEDULE_NOTIFICATION_BASE_ID + scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SCHEDULE_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(icon)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(
            SCHEDULE_NOTIFICATION_BASE_ID + scheduleId.toInt(),
            notification
        )
    }
}
