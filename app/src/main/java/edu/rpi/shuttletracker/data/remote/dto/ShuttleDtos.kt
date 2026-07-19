package edu.rpi.shuttletracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnnouncementDto(
    val subject: String,
    val body: String,
    @SerialName("start") val rawStartTime: String,
    @SerialName("end") val rawEndTime: String,
)

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
