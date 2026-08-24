package edu.rpi.shuttletracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Vehicle data comes from three endpoints and is merged client-side by VehicleMerger.

@Serializable
data class VehicleLocationDto(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("speed_mph") val speedMph: Double,
    val timestamp: String,
    @SerialName("heading_degrees") val headingDegrees: Double? = null,
)

@Serializable
data class VehicleStopEtaDto(
    @SerialName("stop_times") val stopTimes: Map<String, String>,
)

@Serializable
data class VehicleVelocitiesDto(
    @SerialName("route_name") val routeName: String?,
    @SerialName("is_at_stop") val isAtStop: Boolean,
    @SerialName("current_stop") val currentStop: String? = null,
)
