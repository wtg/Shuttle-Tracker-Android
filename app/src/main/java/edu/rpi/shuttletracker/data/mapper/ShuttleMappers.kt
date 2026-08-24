package edu.rpi.shuttletracker.data.mapper

import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.AnnouncementType
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.models.VehicleLocation
import edu.rpi.shuttletracker.data.models.VehicleStopEta
import edu.rpi.shuttletracker.data.models.VehicleVelocities
import edu.rpi.shuttletracker.data.remote.dto.AnnouncementDto
import edu.rpi.shuttletracker.data.remote.dto.AnnouncementsResponseDto
import edu.rpi.shuttletracker.data.remote.dto.RouteDto
import edu.rpi.shuttletracker.data.remote.dto.ScheduleDto
import edu.rpi.shuttletracker.data.remote.dto.StopDto
import edu.rpi.shuttletracker.data.remote.dto.VehicleLocationDto
import edu.rpi.shuttletracker.data.remote.dto.VehicleStopEtaDto
import edu.rpi.shuttletracker.data.remote.dto.VehicleVelocitiesDto
import java.time.OffsetDateTime

// One `toModel()` extension per DTO, converting the raw network shape (data/remote/dto/) into the
// app's own model (data/models/). Some also validate the data (e.g. lat/lng ranges) and throw if
// the API sent something malformed, so a bad response fails fast instead of silently corrupting
// the UI - callers see this surface as a NetworkError.Unknown via ShuttleRemoteDataSource.

fun VehicleLocationDto.toModel(): VehicleLocation {
    require(latitude.isFinite() && latitude in -90.0..90.0) { "Invalid vehicle latitude" }
    require(longitude.isFinite() && longitude in -180.0..180.0) { "Invalid vehicle longitude" }
    OffsetDateTime.parse(timestamp)
    return VehicleLocation(name, latitude, longitude, speedMph, timestamp, headingDegrees?.toInt())
}

fun VehicleStopEtaDto.toModel() = VehicleStopEta(stopTimes)

fun VehicleVelocitiesDto.toModel() = VehicleVelocities(routeName, isAtStop, currentStop)

fun StopDto.toModel(): Stop {
    require(coordinates.size >= 2) { "Stop coordinates must contain latitude and longitude" }
    require(coordinates[0].isFinite() && coordinates[0] in -90.0..90.0) { "Invalid stop latitude" }
    require(coordinates[1].isFinite() && coordinates[1] in -180.0..180.0) { "Invalid stop longitude" }
    return Stop(coordinates, offset, name)
}

fun RouteDto.toModel() = Route(color, stops, coordinates, stopDetails.mapValues { it.value.toModel() })

fun AnnouncementDto.toModel(): Announcement =
    Announcement(
        id = id,
        message = message,
        type = AnnouncementType.fromApiValue(type),
        active = active,
        expiresAt = parseAnnouncementInstant(expiresAt),
        createdAt = parseAnnouncementInstant(createdAt),
    )

fun AnnouncementsResponseDto.toModel(): List<Announcement> = announcements.map { it.toModel() }

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
