package edu.rpi.shuttletracker.data.remote.dto

import com.google.gson.annotations.SerializedName

data class VehicleLocationDto(
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("speed_mph") val speedMph: Double,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("heading_degrees") val headingDegrees: Int?,
)

data class VehicleStopEtaDto(
    @SerializedName("stop_times") val stopTimes: Map<String, String>,
    @SerializedName("timestamp") val timestamp: String,
)

data class VehicleVelocitiesDto(
    @SerializedName("route_name") val routeName: String,
    @SerializedName("is_at_stop") val isAtStop: Boolean,
    @SerializedName("current_stop") val currentStop: String?,
)
