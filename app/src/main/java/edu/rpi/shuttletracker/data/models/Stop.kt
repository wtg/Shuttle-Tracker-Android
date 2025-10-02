package edu.rpi.shuttletracker.data.models

import com.google.android.gms.maps.model.LatLng

data class Stop(
    val latitude: Double,
    val longitude: Double,
    val name: String,
) {
    fun latLng() = LatLng(latitude, longitude)
}
