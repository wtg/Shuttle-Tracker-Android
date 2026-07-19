package edu.rpi.shuttletracker.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class AnnouncementDto(
    val id: String,
    val message: String,
    val type: String = "info",
    val active: Boolean = false,
    val expiresAt: String? = null,
    val createdAt: String? = null,
)

@Serializable(with = AnnouncementsResponseDtoSerializer::class)
data class AnnouncementsResponseDto(
    val announcements: List<AnnouncementDto> = emptyList(),
)

/**
 * The API is documented to return a wrapped `{"announcements": [...]}` object, but the live
 * endpoint has been observed returning a bare JSON array instead. Tolerate both shapes so a
 * mismatch between the documented and actual response never crashes decoding.
 * */
object AnnouncementsResponseDtoSerializer : KSerializer<AnnouncementsResponseDto> {
    private val listSerializer = ListSerializer(AnnouncementDto.serializer())

    override val descriptor: SerialDescriptor = listSerializer.descriptor

    override fun deserialize(decoder: Decoder): AnnouncementsResponseDto {
        require(decoder is JsonDecoder) { "AnnouncementsResponseDto can only be decoded from JSON" }

        val array =
            when (val element = decoder.decodeJsonElement()) {
                is JsonArray -> element
                is JsonObject -> element["announcements"] as? JsonArray ?: JsonArray(emptyList())
                else -> JsonArray(emptyList())
            }

        return AnnouncementsResponseDto(decodeAnnouncements(decoder.json, array))
    }

    /**
     * Decodes each entry independently so one malformed announcement can't sink the whole list.
     * */
    private fun decodeAnnouncements(
        json: Json,
        array: JsonArray,
    ): List<AnnouncementDto> =
        array.mapNotNull { element ->
            runCatching { json.decodeFromJsonElement(AnnouncementDto.serializer(), element) }.getOrNull()
        }

    override fun serialize(
        encoder: Encoder,
        value: AnnouncementsResponseDto,
    ) {
        require(encoder is JsonEncoder) { "AnnouncementsResponseDto can only be encoded to JSON" }

        val announcementsElement = encoder.json.encodeToJsonElement(listSerializer, value.announcements)
        encoder.encodeJsonElement(JsonObject(mapOf("announcements" to announcementsElement)))
    }
}

@Serializable
data class ScheduleDto(
    @SerialName("MONDAY") val monday: String,
    @SerialName("TUESDAY") val tuesday: String,
    @SerialName("WEDNESDAY") val wednesday: String,
    @SerialName("THURSDAY") val thursday: String,
    @SerialName("FRIDAY") val friday: String,
    @SerialName("SATURDAY") val saturday: String,
    @SerialName("SUNDAY") val sunday: String,
    val weekday: Map<String, List<List<String>>>,
    @SerialName("saturday") val saturdaySchedule: Map<String, List<List<String>>>,
    @SerialName("sunday") val sundaySchedule: Map<String, List<List<String>>>,
)

@Serializable
data class ErrorResponse(
    val error: Boolean = false,
    val reason: String? = null,
)
