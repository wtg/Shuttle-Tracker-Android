package edu.rpi.shuttletracker.data.models

import com.google.android.gms.maps.model.LatLng

data class Stop(
    val coordinates: List<Double>,
    val offset: Int,
    val name: String,
) {
    fun latLng() = LatLng(coordinates[0], coordinates[1])
}
