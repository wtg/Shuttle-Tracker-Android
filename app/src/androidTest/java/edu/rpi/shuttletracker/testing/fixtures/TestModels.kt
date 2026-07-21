package edu.rpi.shuttletracker.testing.fixtures

import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.models.Stop

/**
 * Mirrors app/src/test's fixtures of the same name. Duplicated here because androidTest and test
 * are separate source sets with no shared fixtures module in this project.
 * */
fun testRoute() =
    Route(
        color = "#D32F2F",
        stops = listOf("union", "academy"),
        polylineStops = emptyList(),
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
