package edu.rpi.shuttletracker.data.database.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Im so sorry but I don't want to work with foreign keys
 * */
class DepartureConverter {
    @TypeConverter
    fun fromDaysToJSON(date: List<Int>): String {
        return Gson().toJson(date)
    }

    @TypeConverter
    fun fromJSONtoDays(json: String): List<Int> {
        val type = object : TypeToken<List<Int>>() {}.type
        return Gson().fromJson(json, type)
    }
}
