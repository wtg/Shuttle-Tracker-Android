package edu.rpi.shuttletracker.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import edu.rpi.shuttletracker.data.database.converters.DepartureConverter
import edu.rpi.shuttletracker.data.database.daos.DepartureDao
import edu.rpi.shuttletracker.data.models.Departure

@TypeConverters(DepartureConverter::class)
@Database(entities = [Departure::class], version = 1, exportSchema = false)
abstract class ShuttleTrackerDatabase : RoomDatabase() {
    abstract fun departureDao(): DepartureDao
}
