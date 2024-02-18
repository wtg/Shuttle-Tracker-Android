package edu.rpi.shuttletracker.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Entity(tableName = "departures")
data class Departure(
    @ColumnInfo(name = "stop_name")
    val stop: String,
    // based on Calendar class, 1 is saturday, 7 is sunday
    @ColumnInfo(name = "days_active")
    var days: List<Int>,
    @ColumnInfo(name = "time")
    var time: String = LocalDateTime.now().toString(),
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
) {
    fun getReadableTime(): String {
        val outputFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

        val busDate = LocalDateTime.parse(time)

        return busDate.format(outputFormatter)
    }

    fun setTime(
        hour: Int,
        minute: Int,
    ) {
        time = LocalDateTime.now().with(LocalTime.of(hour, minute)).toString()
    }
}
