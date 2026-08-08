package edu.rpi.shuttletracker.testing.fixtures

import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.AnnouncementType
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.models.VehicleLocation
import edu.rpi.shuttletracker.data.models.VehicleStopEta
import edu.rpi.shuttletracker.data.models.VehicleVelocities
import java.time.Instant

fun testRoute() =
    Route(
        color = "#D32F2F",
        stops = listOf("union", "academy"),
        coordinates = listOf(listOf(listOf(42.730, -73.680), listOf(42.731, -73.679))),
        stopDetails =
            mapOf(
                "union" to Stop(listOf(42.730, -73.680), 0, "Student Union"),
                "academy" to Stop(listOf(42.731, -73.679), 5, "Academy Hall"),
            ),
    )

fun testSchedule(
    weekday: Map<String, List<List<String>>> =
        mapOf(
            "North Bus" to
                listOf(
                    listOf("7:00 AM", "NORTH"),
                    listOf("12:30 AM", "NORTH"),
                ),
        ),
) = Schedule(
    monday = "weekday",
    tuesday = "weekday",
    wednesday = "weekday",
    thursday = "weekday",
    friday = "weekday",
    saturday = "saturday",
    sunday = "sunday",
    weekday = weekday,
    saturdaySchedule = emptyMap(),
    sundaySchedule = emptyMap(),
)

fun testAnnouncement(
    id: String,
    message: String = "Service update",
    type: AnnouncementType = AnnouncementType.Info,
    active: Boolean = true,
    expiresAt: Instant? = null,
    createdAt: Instant? = Instant.parse("2026-01-15T08:00:00Z"),
) = Announcement(
    id = id,
    message = message,
    type = type,
    active = active,
    expiresAt = expiresAt,
    createdAt = createdAt,
)

fun testVehicleLocation(name: String = "North Bus") =
    VehicleLocation(
        name = name,
        latitude = 42.730,
        longitude = -73.680,
        speedMph = 12.0,
        timestamp = "2026-01-15T08:00:00-05:00",
        headingDegrees = 90,
    )

fun testVehicleEta() = VehicleStopEta(mapOf("union" to "2 min"))

fun testVehicleVelocity() = VehicleVelocities("NORTH", false, null)
