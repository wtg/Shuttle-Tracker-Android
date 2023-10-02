package edu.rpi.shuttletracker.data.models

import com.google.android.gms.maps.model.LatLng

data class Route(
    val colorName: String,

    val coordinates: List<Coordinate>,

) {
    fun latLng(): List<LatLng> =
        coordinates.map {
            LatLng(it.latitude, it.longitude)
        }
}

data class Coordinate(
    val latitude: Double,
    val longitude: Double,
)
