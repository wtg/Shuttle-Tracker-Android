package edu.rpi.shuttletracker.data.models

import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

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

class RouteDeserializer : JsonDeserializer<Route> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): Route {
        val obj = json.asJsonObject
        val color = obj["COLOR"].asString

        val stops: List<String> =
            context.deserialize(
                obj["STOPS"],
                object : TypeToken<List<String>>() {}.type,
            )

        val polylineStops: List<String> =
            context.deserialize(
                obj["POLYLINE_STOPS"],
                object : TypeToken<List<String>>() {}.type,
            )

        val coordinates: List<List<List<Double>>> =
            context.deserialize(
                obj["ROUTES"],
                object : TypeToken<List<List<List<Double>>>>() {}.type,
            )

        // Decode dynamic stop keys, but only those listed in STOPS
        val fixedKeys = setOf("COLOR", "STOPS", "POLYLINE_STOPS", "ROUTES")
        val validStops = stops.toSet()
        val details = mutableMapOf<String, Stop>()
        for ((key, value) in obj.entrySet()) {
            if (key !in fixedKeys && key in validStops) {
                // Safely try to parse Stop and errors are silently ignored.
                runCatching {
                    val stop = context.deserialize<Stop>(value, Stop::class.java)
                    details[key] = stop
                }
            }
        }

        return Route(
            color = color,
            stops = stops,
            polylineStops = polylineStops,
            coordinates = coordinates,
            stopDetails = details,
        )
    }
}
