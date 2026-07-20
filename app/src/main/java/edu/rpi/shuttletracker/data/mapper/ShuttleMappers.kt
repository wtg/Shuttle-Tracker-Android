package edu.rpi.shuttletracker.data.mapper

import edu.rpi.shuttletracker.data.models.AggregatedSchedule
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.models.VehicleLocation
import edu.rpi.shuttletracker.data.models.VehicleStopEta
import edu.rpi.shuttletracker.data.models.VehicleVelocities
import edu.rpi.shuttletracker.data.remote.dto.AggregatedScheduleDto
import edu.rpi.shuttletracker.data.remote.dto.AnnouncementDto
import edu.rpi.shuttletracker.data.remote.dto.RouteDto
import edu.rpi.shuttletracker.data.remote.dto.ScheduleDto
import edu.rpi.shuttletracker.data.remote.dto.StopDto
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
