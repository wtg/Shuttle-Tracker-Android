package edu.rpi.shuttletracker.data.remote.dto

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

data class RouteDto(
    @SerializedName("COLOR") val color: String,
    @SerializedName("STOPS") val stops: List<String>,
    @SerializedName("POLYLINE_STOPS") val polylineStops: List<String>,
    @SerializedName("ROUTES") val coordinates: List<List<List<Double>>>,
    val stopDetails: Map<String, StopDto>,
)

data class StopDto(
    @SerializedName("COORDINATES") val coordinates: List<Double>,
    @SerializedName("OFFSET") val offset: Int,
    @SerializedName("NAME") val name: String,
)

class RouteDtoDeserializer : JsonDeserializer<RouteDto> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): RouteDto {
        val obj = json.asJsonObject
        val color = obj["COLOR"].asString
        val stops: List<String> = context.deserialize(obj["STOPS"], object : TypeToken<List<String>>() {}.type)
        val polylineStops: List<String> =
            context.deserialize(obj["POLYLINE_STOPS"], object : TypeToken<List<String>>() {}.type)
        val coordinates: List<List<List<Double>>> =
            context.deserialize(obj["ROUTES"], object : TypeToken<List<List<List<Double>>>>() {}.type)

        val fixedKeys = setOf("COLOR", "STOPS", "POLYLINE_STOPS", "ROUTES")
        val validStops = stops.toSet()
        val details = mutableMapOf<String, StopDto>()
        for ((key, value) in obj.entrySet()) {
            if (key !in fixedKeys && key in validStops) {
                runCatching {
                    details[key] = context.deserialize(value, StopDto::class.java)
                }
            }
        }

        return RouteDto(color, stops, polylineStops, coordinates, details)
    }
}
