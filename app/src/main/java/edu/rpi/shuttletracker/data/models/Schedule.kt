package edu.rpi.shuttletracker.data.models

import com.google.gson.annotations.SerializedName
import edu.rpi.shuttletracker.util.Flatten
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

data class Schedule(
    @SerializedName("name")
    val name: String,
    @SerializedName("start")
    val _start: String,
    @SerializedName("end")
    val _end: String,
    @Flatten("content::monday")
    val monday: Day,
    @Flatten("content::tuesday")
    val tuesday: Day,
    @Flatten("content::wednesday")
    val wednesday: Day,
    @Flatten("content::thursday")
    val thursday: Day,
    @Flatten("content::friday")
    val friday: Day,
    @Flatten("content::saturday")
    val saturday: Day,
    @Flatten("content::sunday")
    val sunday: Day,
) {
    private fun getReadableTime(date: String): String {
        val outputFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

        val busDate =
            ZonedDateTime.parse(date)
                .truncatedTo(ChronoUnit.SECONDS)
                .toLocalDate()

        return busDate.format(outputFormatter)
    }

    val startTime: String
        get() = getReadableTime(_start)

    val endTime: String
        get() = getReadableTime(_end)
}

data class Day(
    val start: String,
    val end: String,
) {
    override fun toString(): String = "$start - $end"
}
