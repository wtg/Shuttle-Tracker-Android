package edu.rpi.shuttletracker.data.models

/** A route with ordered stop keys and polyline `[latitude, longitude]` pairs. */
data class Route(
    val color: String,
    val stops: List<String>,
    val coordinates: List<List<List<Double>>>,
    val stopDetails: Map<String, Stop>,
)
