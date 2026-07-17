package edu.rpi.shuttletracker.data.mapper

import edu.rpi.shuttletracker.data.models.AggregatedSchedule
import edu.rpi.shuttletracker.data.models.Analytics
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.Event
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.models.VehicleLocation
import edu.rpi.shuttletracker.data.models.VehicleStopEta
import edu.rpi.shuttletracker.data.models.VehicleVelocities
import edu.rpi.shuttletracker.data.remote.dto.AggregatedScheduleDto
import edu.rpi.shuttletracker.data.remote.dto.AnalyticsDto
import edu.rpi.shuttletracker.data.remote.dto.AnnouncementDto
import edu.rpi.shuttletracker.data.remote.dto.AnnouncementViewedEventDto
import edu.rpi.shuttletracker.data.remote.dto.EmptyEventDto
import edu.rpi.shuttletracker.data.remote.dto.EnabledEventDto
import edu.rpi.shuttletracker.data.remote.dto.EventDto
import edu.rpi.shuttletracker.data.remote.dto.LocationAccuracyAuthorizationEventDto
import edu.rpi.shuttletracker.data.remote.dto.LocationAuthorizationStatusEventDto
import edu.rpi.shuttletracker.data.remote.dto.RouteDto
import edu.rpi.shuttletracker.data.remote.dto.ScheduleDto
import edu.rpi.shuttletracker.data.remote.dto.ServerBaseUrlChangedEventDto
import edu.rpi.shuttletracker.data.remote.dto.StopDto
import edu.rpi.shuttletracker.data.remote.dto.UserSettingsDto
import edu.rpi.shuttletracker.data.remote.dto.VehicleLocationDto
import edu.rpi.shuttletracker.data.remote.dto.VehicleStopEtaDto
import edu.rpi.shuttletracker.data.remote.dto.VehicleVelocitiesDto

fun VehicleLocationDto.toModel() = VehicleLocation(name, latitude, longitude, speedMph, timestamp, headingDegrees)

fun VehicleStopEtaDto.toModel() = VehicleStopEta(stopTimes, timestamp)

fun VehicleVelocitiesDto.toModel() = VehicleVelocities(routeName, isAtStop, currentStop)

fun StopDto.toModel() = Stop(coordinates, offset, name)

fun RouteDto.toModel() = Route(color, stops, polylineStops, coordinates, stopDetails.mapValues { it.value.toModel() })

fun AnnouncementDto.toModel() = Announcement(subject, body, rawStartTime, rawEndTime)

fun ScheduleDto.toModel() =
    Schedule(
        monday,
        tuesday,
        wednesday,
        thursday,
        friday,
        saturday,
        sunday,
        weekday,
        saturdaySchedule,
        sundaySchedule,
    )

fun AggregatedScheduleDto.toModel() = AggregatedSchedule(north, west)

fun Analytics.toDto() =
    AnalyticsDto(
        id,
        userID,
        date,
        clientPlatform,
        clientPlatformVersion,
        appVersion,
        UserSettingsDto(colorBlindMode, logging, serverBaseURL),
        event?.toDto(),
    )

private fun Event.toDto() =
    EventDto(
        colorBlindModeToggled = colorBlindModeToggled?.let(::EnabledEventDto),
        announcementViewed = announcementViewed?.let(::AnnouncementViewedEventDto),
        debugModeToggled = debugModeTogged?.let(::EnabledEventDto),
        serverBaseURLChanged = serverBaseURL?.let(::ServerBaseUrlChangedEventDto),
        locationAuthorizationStatusDidChange =
            locationAuthorizationStatusChanged?.let(::LocationAuthorizationStatusEventDto),
        locationAccuracyAuthorizationDidChange =
            locationAccuracyAuthorizationDidChange?.let(::LocationAccuracyAuthorizationEventDto),
        coldLaunch = coldLaunch?.let { EmptyEventDto() },
        announcementsListOpened = announcementsListOpened?.let { EmptyEventDto() },
        permissionsSheetOpened = permissionsSheetOpened?.let { EmptyEventDto() },
    )
