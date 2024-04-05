package edu.rpi.shuttletracker.data.models

import com.google.gson.annotations.SerializedName
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.GregorianCalendar

data class Announcement(
    @SerializedName("subject")
    val subject: String,
    @SerializedName("body")
    val body: String,
    @SerializedName("start")
    private val rawStartTime: String,
    @SerializedName("end")
    private val rawEndTime: String,
) {
    private fun getReadableTime(date: String): String {
        val outputFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

        val busDate =
            ZonedDateTime.parse(date)
                .truncatedTo(ChronoUnit.SECONDS)
                .toLocalDate()

        return busDate.format(outputFormatter)
    }

    private fun getCalendar(date: String): Calendar {
        val zdt = ZonedDateTime.parse(date)
        return GregorianCalendar.from(zdt)
    }

    val startCalendar: Calendar
        get() = getCalendar(rawStartTime)

    val endCalendar: Calendar
        get() = getCalendar(rawEndTime)

    val startTime: String
        get() = getReadableTime(rawStartTime)

    val endTime: String
        get() = getReadableTime(rawEndTime)
}
