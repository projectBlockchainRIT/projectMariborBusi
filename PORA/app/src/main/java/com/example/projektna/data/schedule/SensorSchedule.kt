package com.example.projektna.data.schedule

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_schedules")
data class SensorSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sensorType: SensorType,
    val scheduleType: ScheduleType,
    val actionType: ActionType,
    val hour: Int,
    val minute: Int,
    val weekDays: Int = 0,
    val scheduledDateMillis: Long? = null,
    val durationMinutes: Int = 30,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastTriggeredAt: Long? = null
) {
    companion object {
        const val SUNDAY = 1
        const val MONDAY = 2
        const val TUESDAY = 4
        const val WEDNESDAY = 8
        const val THURSDAY = 16
        const val FRIDAY = 32
        const val SATURDAY = 64

        const val WEEKDAYS = MONDAY or TUESDAY or WEDNESDAY or THURSDAY or FRIDAY
        const val WEEKEND = SATURDAY or SUNDAY
        const val ALL_DAYS = WEEKDAYS or WEEKEND
    }

    fun isDayEnabled(dayBit: Int): Boolean = (weekDays and dayBit) != 0

    fun getFormattedTime(): String = String.format("%02d:%02d", hour, minute)
}
