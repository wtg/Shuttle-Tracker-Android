package edu.rpi.shuttletracker.data.models

import com.google.gson.annotations.SerializedName

data class AggregatedSchedule(
    @SerializedName("NORTH")
    val north: List<String>,
    @SerializedName("WEST")
    val west: List<String>,
)
