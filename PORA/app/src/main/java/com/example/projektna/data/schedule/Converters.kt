package com.example.projektna.data.schedule

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromSensorType(value: SensorType): String = value.name

    @TypeConverter
    fun toSensorType(value: String): SensorType = SensorType.valueOf(value)

    @TypeConverter
    fun fromScheduleType(value: ScheduleType): String = value.name

    @TypeConverter
    fun toScheduleType(value: String): ScheduleType = ScheduleType.valueOf(value)

    @TypeConverter
    fun fromActionType(value: ActionType): String = value.name

    @TypeConverter
    fun toActionType(value: String): ActionType = ActionType.valueOf(value)
}
