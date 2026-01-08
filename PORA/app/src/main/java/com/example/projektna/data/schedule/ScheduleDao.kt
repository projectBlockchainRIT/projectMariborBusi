package com.example.projektna.data.schedule

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM sensor_schedules WHERE isEnabled = 1")
    fun getEnabledSchedules(): Flow<List<SensorSchedule>>

    @Query("SELECT * FROM sensor_schedules WHERE sensorType = :sensorType ORDER BY hour, minute")
    fun getSchedulesForSensor(sensorType: SensorType): Flow<List<SensorSchedule>>

    @Query("SELECT * FROM sensor_schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): SensorSchedule?

    @Insert
    suspend fun insert(schedule: SensorSchedule): Long

    @Update
    suspend fun update(schedule: SensorSchedule)

    @Delete
    suspend fun delete(schedule: SensorSchedule)

    @Query("UPDATE sensor_schedules SET lastTriggeredAt = :timestamp WHERE id = :id")
    suspend fun updateLastTriggered(id: Long, timestamp: Long)

    @Query("UPDATE sensor_schedules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("SELECT * FROM sensor_schedules WHERE isEnabled = 1")
    suspend fun getAllEnabledSchedulesSync(): List<SensorSchedule>

    @Query("SELECT COUNT(*) FROM sensor_schedules WHERE sensorType = :sensorType AND isEnabled = 1")
    fun getActiveScheduleCount(sensorType: SensorType): Flow<Int>
}
