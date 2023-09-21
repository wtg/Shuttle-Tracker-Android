package edu.rpi.shuttletracker.data.models

import com.google.gson.annotations.SerializedName
import edu.rpi.shuttletracker.util.Flatten
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class BoardBus(

    @SerializedName("id")
    val uuid: String,

    @Flatten("coordinate::latitude")
    val latitude: Double,

    @Flatten("coordinate::longitude")
    val longitude: Double,

    @SerializedName("type")
    val type: String,

    @SerializedName("date")
    val date: String = getCurrentFormattedDate(),

) {
    companion object {
        /**
         *  Get the current date time in the format of ISO-8601 (e.g. 2021-11-12T22:44:55+00:00), excluding milliseconds.
         *  @return An ISO-8601 date string.
         */
        private fun getCurrentFormattedDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC") // use UTC as default time zone

            return sdf.format(Date())
        }
    }
}
