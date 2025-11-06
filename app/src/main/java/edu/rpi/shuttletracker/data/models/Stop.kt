package edu.rpi.shuttletracker.data.models

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName

data class Stop(
    @SerializedName("COORDINATES") val coordinates: List<Double>,
    @SerializedName("OFFSET") val offset: Int,
    @SerializedName("NAME") val name: String,
) {
    fun latLng() = LatLng(coordinates[0], coordinates[1])
}
