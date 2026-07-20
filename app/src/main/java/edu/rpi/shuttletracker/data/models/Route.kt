package edu.rpi.shuttletracker.data.models

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName

data class Route(
    @SerializedName("COLOR") val color: String,
    @SerializedName("STOPS") val stops: List<String>,
    @SerializedName("POLYLINE_STOPS") val polylineStops: List<String>,
    @SerializedName("ROUTES") val coordinates: List<List<List<Double>>>,
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
