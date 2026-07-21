package edu.rpi.shuttletracker.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * A route's JSON looks like `{"COLOR": ..., "STOPS": [...], "union": {...}, "academy": {...}}` -
 * fixed route fields and per-stop objects are siblings in the same JSON object, keyed by stop
 * name. That's not something `@Serializable` can express directly, hence [RouteDtoSerializer]
 * below doing it by hand.
 * */
@Serializable(with = RouteDtoSerializer::class)
data class RouteDto(
    val color: String,
    val stops: List<String>,
    val polylineStops: List<String>,
    val coordinates: List<List<List<Double>>>,
    val stopDetails: Map<String, StopDto>,
)

@Serializable
data class StopDto(
    @SerialName("COORDINATES") val coordinates: List<Double>,
    @SerialName("OFFSET") val offset: Int,
    @SerialName("NAME") val name: String,
)

/**
 * Splits a route's JSON object into the fixed [RouteFields] (decoded normally) and everything else,
 * treating each remaining key as a stop name mapping to a [StopDto]. Serializing does the reverse:
 * flatten [RouteFields] and the stop map back into one JSON object.
 * */
object RouteDtoSerializer : KSerializer<RouteDto> {
    private val fixedKeys = setOf("COLOR", "STOPS", "POLYLINE_STOPS", "ROUTES")

    override val descriptor: SerialDescriptor = RouteFields.serializer().descriptor

    override fun deserialize(decoder: Decoder): RouteDto {
        require(decoder is JsonDecoder) { "RouteDto can only be decoded from JSON" }

        val jsonObject = decoder.decodeJsonElement().jsonObject
        val fixedFields =
            decoder.json.decodeFromJsonElement<RouteFields>(
                JsonObject(jsonObject.filterKeys { it in fixedKeys }),
            )
        val validStops = fixedFields.stops.toSet()
        val stopDetails =
            jsonObject
                .filterKeys { it in validStops }
                .mapNotNull { (name, element) ->
                    runCatching {
                        name to decoder.json.decodeFromJsonElement<StopDto>(element)
                    }.getOrNull()
                }.toMap()

        return RouteDto(
            color = fixedFields.color,
            stops = fixedFields.stops,
            polylineStops = fixedFields.polylineStops,
            coordinates = fixedFields.coordinates,
            stopDetails = stopDetails,
        )
    }

    override fun serialize(
        encoder: Encoder,
        value: RouteDto,
    ) {
        require(encoder is JsonEncoder) { "RouteDto can only be encoded to JSON" }

        val fixedFields =
            encoder.json
                .encodeToJsonElement(
                    RouteFields(
                        color = value.color,
                        stops = value.stops,
                        polylineStops = value.polylineStops,
                        coordinates = value.coordinates,
                    ),
                ).jsonObject
        val stopDetails =
            value.stopDetails.mapValues { (_, stop) ->
                encoder.json.encodeToJsonElement(stop)
            }

        encoder.encodeJsonElement(JsonObject(fixedFields + stopDetails))
    }
}

@Serializable
private data class RouteFields(
    @SerialName("COLOR") val color: String,
    @SerialName("STOPS") val stops: List<String>,
    @SerialName("POLYLINE_STOPS") val polylineStops: List<String>,
    @SerialName("ROUTES") val coordinates: List<List<List<Double>>>,
)
