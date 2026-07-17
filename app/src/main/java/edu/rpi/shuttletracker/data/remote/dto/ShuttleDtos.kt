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
    @SerializedName("userSettings") val userSettings: UserSettingsDto,
    @SerializedName("eventType") val event: EventDto?,
)

data class UserSettingsDto(
    @SerializedName("colorBlindMode") val colorBlindMode: Boolean,
    @SerializedName("logging") val logging: Boolean,
    @SerializedName("serverBaseURL") val serverBaseURL: String,
)

data class EventDto(
    @SerializedName("colorBlindModeToggled") val colorBlindModeToggled: EnabledEventDto? = null,
    @SerializedName("announcementViewed") val announcementViewed: AnnouncementViewedEventDto? = null,
    @SerializedName("debugModeToggled") val debugModeToggled: EnabledEventDto? = null,
    @SerializedName("serverBaseURLChanged") val serverBaseURLChanged: ServerBaseUrlChangedEventDto? = null,
    @SerializedName("locationAuthorizationStatusDidChange")
    val locationAuthorizationStatusDidChange: LocationAuthorizationStatusEventDto? = null,
    @SerializedName("locationAccuracyAuthorizationDidChange")
    val locationAccuracyAuthorizationDidChange: LocationAccuracyAuthorizationEventDto? = null,
    @SerializedName("coldLaunch") val coldLaunch: EmptyEventDto? = null,
    @SerializedName("announcementsListOpened") val announcementsListOpened: EmptyEventDto? = null,
    @SerializedName("permissionsSheetOpened") val permissionsSheetOpened: EmptyEventDto? = null,
)

data class EnabledEventDto(
    @SerializedName("enabled") val enabled: Boolean,
)

data class AnnouncementViewedEventDto(
    @SerializedName("id") val id: String,
)

data class ServerBaseUrlChangedEventDto(
    @SerializedName("url") val url: String,
)

data class LocationAuthorizationStatusEventDto(
    @SerializedName("authorizationStatus") val authorizationStatus: Int,
)

data class LocationAccuracyAuthorizationEventDto(
    @SerializedName("accuracyAuthorization") val accuracyAuthorization: Int,
)

class EmptyEventDto
