package edu.rpi.shuttletracker.data.remote.dto

import com.google.gson.annotations.SerializedName
import edu.rpi.shuttletracker.data.models.Event
import edu.rpi.shuttletracker.util.Flatten

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

data class AggregatedScheduleDto(
    @SerializedName("NORTH") val north: List<String>,
    @SerializedName("WEST") val west: List<String>,
)

data class ErrorResponse(
    @SerializedName("error") val error: Boolean,
    @SerializedName("reason") val reason: String,
)

data class AnalyticsDto(
    @SerializedName("id") val id: String,
    @SerializedName("userID") val userID: String,
    @SerializedName("date") val date: String,
    @SerializedName("clientPlatform") val clientPlatform: String,
    @SerializedName("clientPlatformVersion") val clientPlatformVersion: String,
    @SerializedName("appVersion") val appVersion: String,
    @Flatten("userSettings::colorBlindMode") val colorBlindMode: Boolean,
    @Flatten("userSettings::logging") val logging: Boolean,
    @Flatten("userSettings::serverBaseURL") val serverBaseURL: String,
    @SerializedName("eventType") val event: Event?,
)
