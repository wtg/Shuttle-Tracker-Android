package edu.rpi.shuttletracker.data.models

import com.google.gson.annotations.SerializedName

data class Schedule(
    @SerializedName("NORTH")
    val north: List<String>,
    @SerializedName("WEST")
    val west: List<String>,
)

data class RouteStops(
    val stops: List<Stop>,
    val stopByName: Map<String, Stop> = stops.associateBy { it.name },
)
