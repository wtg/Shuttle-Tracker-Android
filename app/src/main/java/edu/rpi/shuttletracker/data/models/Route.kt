package edu.rpi.shuttletracker.data.models

import com.google.android.gms.maps.model.LatLng

data class Route(
    val color: String,
    val stops: List<String>,
    val polylineStops: List<String>,
    val coordinates: List<List<List<Double>>>,
    val stopDetails: Map<String, Stop>,
) {
    fun latLng(): List<LatLng> =
        buildList {
            coordinates.forEach { polyline ->
                polyline.forEach { pair ->
                    if (pair.size >= 2) {
                        add(LatLng(pair[0], pair[1]))
                    }
                }
            }
        }
}
