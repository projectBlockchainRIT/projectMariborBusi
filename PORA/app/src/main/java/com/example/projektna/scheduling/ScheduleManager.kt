package com.example.projektna.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.projektna.data.AppDatabase
import com.example.projektna.data.schedule.ScheduleType
import com.example.projektna.data.schedule.SensorSchedule
import java.util.Calendar

class ScheduleManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val database = AppDatabase.getDatabase(context)

    companion object {
        private const val TAG = "ScheduleManager"
        const val ACTION_TRIGGER_SCHEDULE = "com.example.projektna.TRIGGER_SCHEDULE"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
    }

    suspend fun scheduleAlarm(schedule: SensorSchedule) {
        val triggerTime = calculateNextTriggerTime(schedule)
        if (triggerTime <= System.currentTimeMillis()) {
            Log.w(TAG, "Trigger time is in the past, skipping schedule ${schedule.id}")
            return
        }

        val intent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ACTION_TRIGGER_SCHEDULE
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.w(TAG, "Exact alarm permission not granted, using inexact alarm")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm for ${schedule.id} at ${java.util.Date(triggerTime)}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to schedule exact alarm: ${e.message}")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(scheduleId: Long) {
        val intent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ACTION_TRIGGER_SCHEDULE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled alarm for schedule $scheduleId")
    }

    suspend fun rescheduleAllAlarms() {
        val schedules = database.scheduleDao().getAllEnabledSchedulesSync()
        schedules.forEach { schedule ->
            scheduleAlarm(schedule)
        }
        Log.d(TAG, "Rescheduled ${schedules.size} alarms")
    }

    fun calculateNextTriggerTime(schedule: SensorSchedule): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, schedule.hour)
            set(Calendar.MINUTE, schedule.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (schedule.scheduleType) {
            ScheduleType.DAILY -> {
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            ScheduleType.WEEKLY -> {
                val now = System.currentTimeMillis()
                var found = false

                for (i in 0..7) {
                    val checkCalendar = calendar.clone() as Calendar
                    checkCalendar.add(Calendar.DAY_OF_YEAR, i)
                    val dayOfWeek = checkCalendar.get(Calendar.DAY_OF_WEEK)
                    val dayBit = getDayBit(dayOfWeek)

                    if (schedule.isDayEnabled(dayBit) && checkCalendar.timeInMillis > now) {
                        calendar.add(Calendar.DAY_OF_YEAR, i)
                        found = true
                        break
                    }
                }

                if (!found) {
                    calendar.add(Calendar.DAY_OF_YEAR, 7)
                }
            }
            ScheduleType.ONE_TIME -> {
                schedule.scheduledDateMillis?.let { dateMillis ->
                    val dateCalendar = Calendar.getInstance().apply {
                        timeInMillis = dateMillis
                    }
                    calendar.set(Calendar.YEAR, dateCalendar.get(Calendar.YEAR))
                    calendar.set(Calendar.MONTH, dateCalendar.get(Calendar.MONTH))
                    calendar.set(Calendar.DAY_OF_MONTH, dateCalendar.get(Calendar.DAY_OF_MONTH))
                }
            }
        }

        return calendar.timeInMillis
    }

    private fun getDayBit(calendarDay: Int): Int {
        return when (calendarDay) {
            Calendar.SUNDAY -> SensorSchedule.SUNDAY
            Calendar.MONDAY -> SensorSchedule.MONDAY
            Calendar.TUESDAY -> SensorSchedule.TUESDAY
            Calendar.WEDNESDAY -> SensorSchedule.WEDNESDAY
            Calendar.THURSDAY -> SensorSchedule.THURSDAY
            Calendar.FRIDAY -> SensorSchedule.FRIDAY
            Calendar.SATURDAY -> SensorSchedule.SATURDAY
            else -> 0
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
