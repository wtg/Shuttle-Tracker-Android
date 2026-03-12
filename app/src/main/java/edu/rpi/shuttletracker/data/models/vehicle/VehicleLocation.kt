package edu.rpi.shuttletracker.data.models.vehicle

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

data class VehicleLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("speed_mph") val speedMph: Double,
    @SerializedName("route_name") val routeName: String,
    @SerializedName("timestamp") val date: String,
) {
    /**
     * Turns the date stored into a time of a generalized time ago from current
     * updates once per second if subscribed to
     * */
    fun getTimeAgo(): Flow<String> {
        val busInstant =
            OffsetDateTime.parse(date).toInstant()

        return flow {
            while (true) {
                val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
                val duration = Duration.between(busInstant, now)

                emit(formatDuration(duration) + " ago")
                delay(1000)
            }
        }
    }

    // Pretty "xh ym zs" formatter (avoids relying on Duration.toString())
    private fun formatDuration(d: Duration): String {
        var secs = d.seconds
        val h = secs / 3600
        secs %= 3600
        val m = secs / 60
        secs %= 60
        val s = secs
        return buildString {
            if (h > 0) append("${h}h ")
            if (m > 0 || h > 0) append("${m}m ")
            append("${s}s")
        }.trim().lowercase(Locale.ROOT)
    }

    fun latLng() = LatLng(latitude, longitude)
}
