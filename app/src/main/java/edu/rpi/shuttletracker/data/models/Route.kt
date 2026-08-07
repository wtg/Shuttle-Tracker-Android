package edu.rpi.shuttletracker.data.models

import com.google.android.gms.maps.model.LatLng

/**
 * One shuttle route (e.g. "NORTH"). [coordinates] is the polyline to draw on the map - a list of
 * line segments, each a list of `[latitude, longitude]` pairs. [stops] is the ordered list of stop
 * keys on this route; look each one up in [stopDetails] for its [Stop] (name, position, offset).
 * */
data class Route(
    val color: String,
    val stops: List<String>,
    val coordinates: List<List<List<Double>>>,
    val stopDetails: Map<String, Stop>,
) {
    /** [coordinates] flattened into map-ready points, in order. */
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
