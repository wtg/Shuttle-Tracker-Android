package edu.rpi.shuttletracker.data.models

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class Departure(
    val stop: String,
    // based on Calendar class, 1 is saturday, 7 is sunday
    var days: List<Int>,
    var time: String = LocalDateTime.now().toString(),
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
