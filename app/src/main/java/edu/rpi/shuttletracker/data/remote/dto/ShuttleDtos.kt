package edu.rpi.shuttletracker.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AnnouncementDto(
    @SerializedName("subject") val subject: String,
    @SerializedName("body") val body: String,
    @SerializedName("start") val rawStartTime: String,
    @SerializedName("end") val rawEndTime: String,
)

data class ScheduleDto(
    @SerializedName("MONDAY") val monday: String,
    @SerializedName("TUESDAY") val tuesday: String,
    @SerializedName("WEDNESDAY") val wednesday: String,
    @SerializedName("THURSDAY") val thursday: String,
    @SerializedName("FRIDAY") val friday: String,
    @SerializedName("SATURDAY") val saturday: String,
    @SerializedName("SUNDAY") val sunday: String,
    @SerializedName("weekday") val weekday: Map<String, List<List<String>>>,
    @SerializedName("saturday") val saturdaySchedule: Map<String, List<List<String>>>,
    @SerializedName("sunday") val sundaySchedule: Map<String, List<List<String>>>,
)

data class ErrorResponse(
    @SerializedName("error") val error: Boolean,
    @SerializedName("reason") val reason: String,
)
