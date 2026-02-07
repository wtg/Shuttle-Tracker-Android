package edu.rpi.shuttletracker.data.models

import com.google.gson.annotations.SerializedName

data class VehicleStopEta(
    @SerializedName("stop_times") val stopTimes: Map<String, String>,
    @SerializedName("timestamp") val timestamp: String,
)
