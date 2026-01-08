package com.example.projektna.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektna.data.AppDatabase
import com.example.projektna.data.schedule.SensorSchedule
import com.example.projektna.data.schedule.SensorType
import com.example.projektna.scheduling.ScheduleManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val scheduleManager = ScheduleManager(application)

    fun getSchedulesForSensor(sensorType: SensorType): Flow<List<SensorSchedule>> {
        return database.scheduleDao().getSchedulesForSensor(sensorType)
    }

    fun getActiveScheduleCount(sensorType: SensorType): Flow<Int> {
        return database.scheduleDao().getActiveScheduleCount(sensorType)
    }

    fun saveSchedule(schedule: SensorSchedule) {
        viewModelScope.launch {
            val id = if (schedule.id == 0L) {
                database.scheduleDao().insert(schedule)
            } else {
                database.scheduleDao().update(schedule)
                schedule.id
            }

            if (schedule.isEnabled) {
                scheduleManager.scheduleAlarm(schedule.copy(id = id))
            } else {
                scheduleManager.cancelAlarm(id)
            }
        }
    }

    fun deleteSchedule(schedule: SensorSchedule) {
        viewModelScope.launch {
            scheduleManager.cancelAlarm(schedule.id)
            database.scheduleDao().delete(schedule)
        }
    }

    fun toggleScheduleEnabled(schedule: SensorSchedule) {
        viewModelScope.launch {
            val newEnabled = !schedule.isEnabled
            database.scheduleDao().setEnabled(schedule.id, newEnabled)

            if (newEnabled) {
                scheduleManager.scheduleAlarm(schedule.copy(isEnabled = true))
            } else {
                scheduleManager.cancelAlarm(schedule.id)
            }
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return scheduleManager.canScheduleExactAlarms()
    }
}
