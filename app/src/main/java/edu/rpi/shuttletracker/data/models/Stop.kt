package edu.rpi.shuttletracker.data.models

import com.google.android.gms.maps.model.LatLng

/**
 * One stop on a route. [coordinates] is `[latitude, longitude]`. [offset] is minutes from that
 * route's departure time until a shuttle is expected here - used to build the printed schedule's
 * per-stop times (see `feature/schedule/utils/buildStopTimesForDeparture`).
 * */
data class Stop(
    val coordinates: List<Double>,
    val offset: Int,
    val name: String,
) {
    fun latLng() = LatLng(coordinates[0], coordinates[1])
}
