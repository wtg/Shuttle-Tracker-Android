package edu.rpi.shuttletracker.data.models

import com.google.gson.annotations.SerializedName

data class VehicleETAData(
    @SerializedName("stop_times") val stopTimes: Map<String, String>,
    @SerializedName("timestamp") val timestamp: String,
)
