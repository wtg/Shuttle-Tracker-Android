package edu.rpi.shuttletracker.data.models

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName
import edu.rpi.shuttletracker.util.Flatten
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

data class Bus(
    @Flatten("location::coordinate::latitude")
    val latitude: Double,

    @Flatten("location::coordinate::longitude")
    val longitude: Double,

    @SerializedName("id")
    val id: Int,

    @Flatten("location::type")
    val type: String,

    @Flatten("location::date")
    val date: String,

    @Flatten("location::id")
    val uuid: String,

) {
    /**
     * Turns the date stored into a time of a generalized time ago from current
     * updates once per second if subscribed to
     * */
    fun getTimeAgo(): Flow<String> {
        // get bus time and rounds to nearest second
        val busDate = ZonedDateTime.parse(date)
            .truncatedTo(ChronoUnit.SECONDS)

        return flow {
            while (true) {
                // gets current time and rounds to nearest second
                val currentDate: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.SECONDS)

                val duration: Duration = Duration.between(busDate.toLocalDateTime(), currentDate)

                // formats duration to h m s
                emit(
                    "~" + duration.toString()
                        .substring(2)
                        .replace("(\\d[HMS])(?!$)".toRegex(), "$1 ")
                        .lowercase(Locale.ROOT) + " ago",
                )
                delay(1000)
            }
        }
    }

    fun latLng() = LatLng(latitude, longitude)
}
