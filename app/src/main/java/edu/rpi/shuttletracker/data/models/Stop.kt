package edu.rpi.shuttletracker.data.models

import com.google.android.gms.maps.model.LatLng

data class Stop(
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val offset: Int,
    val route: String,
) {
    fun latLng() = LatLng(latitude, longitude)
}
