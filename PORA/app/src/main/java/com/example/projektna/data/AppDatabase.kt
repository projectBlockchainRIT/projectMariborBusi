package com.example.projektna.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.projektna.data.schedule.Converters
import com.example.projektna.data.schedule.ScheduleDao
import com.example.projektna.data.schedule.SensorSchedule

@Database(
    entities = [SensorSchedule::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "projektna_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
