package edu.rpi.shuttletracker.data.models

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.String

data class Vehicle(
    val id: String,
    val name: String,
    // from locations
    val latitude: Double,
    val longitude: Double,
    val speedMph: Double,
    val timestamp: String,
    val headingDegrees: Int?,
    // from velocities
    val routeName: String?,
    val isAtStop: Boolean?,
    val currentStop: String?,
    // from etas
    val stopTimes: Map<String, String>,
) {
    /**
     * Turns the date stored into a time of a generalized time ago from current
     * updates once per second if subscribed to
     * */
    fun getTimeAgo(): Flow<String> {
        val busInstant =
            OffsetDateTime.parse(timestamp).toInstant()

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

data class VehicleLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val speedMph: Double,
    val timestamp: String,
    val headingDegrees: Int?,
)

data class VehicleStopEta(
    val stopTimes: Map<String, String>,
    val timestamp: String,
)

data class VehicleVelocities(
    val routeName: String,
    val isAtStop: Boolean,
    val currentStop: String?,
)

object VehicleMerger {
    fun merge(
        locations: Map<String, VehicleLocation>,
        velocities: Map<String, VehicleVelocities> = emptyMap(),
        etas: Map<String, VehicleStopEta> = emptyMap(),
    ): List<Vehicle> =
        locations
            .map { (vehicleId, location) ->
                val velocity = velocities[vehicleId]
                val eta = etas[vehicleId]

                Vehicle(
                    id = vehicleId,
                    name = location.name,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    speedMph = location.speedMph,
                    timestamp = location.timestamp,
                    headingDegrees = location.headingDegrees,
                    routeName = velocity?.routeName,
                    isAtStop = velocity?.isAtStop,
                    currentStop = velocity?.currentStop,
                    stopTimes = eta?.stopTimes ?: emptyMap(),
                )
            }.sortedBy { it.name }
}
