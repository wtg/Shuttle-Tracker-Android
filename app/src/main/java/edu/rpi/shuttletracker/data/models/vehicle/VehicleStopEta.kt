package edu.rpi.shuttletracker.data.models.vehicle

import com.google.gson.annotations.SerializedName

data class VehicleStopEta(
    @SerializedName("stop_times") val stopTimes: Map<String, String>,
    @SerializedName("timestamp") val timestamp: String,
)
