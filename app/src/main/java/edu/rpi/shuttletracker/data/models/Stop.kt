package edu.rpi.shuttletracker.data.models

/** A route stop; [offset] is its minutes after the route's departure time. */
data class Stop(
    val coordinates: List<Double>,
    val offset: Int,
    val name: String,
)
