package edu.rpi.shuttletracker.ui.maps.utils

import edu.rpi.shuttletracker.data.models.DayOfWeek
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

fun routesForDay(
    day: DayOfWeek,
    schedule: Schedule,
): List<String> {
    val scheduleMap = schedule.scheduleMapFor(day)
    val directions = mutableSetOf<String>()

    for ((_, times) in scheduleMap) {
        for (pair in times) {
            if (pair.size > 1) directions.add(pair[1])
        }
    }

    return directions.sorted()
}

data class StopTimeInfo(
    val stopName: String,
    val time: String,
)

data class TimeInfo(
    val departureTime: String,
    val routeName: String,
    val vehicleName: String,
    val minutesOfDay: Int,
    val stopTimes: List<StopTimeInfo>,
)

fun parseLocalTime(timeText: String): LocalTime? =
    runCatching {
        LocalTime.parse(
            timeText.trim(),
            DateTimeFormatter.ofPattern("h:mm a", Locale.US),
        )
    }.getOrNull()

fun parseMinutesOfDay(timeText: String): Int? = parseLocalTime(timeText)?.let { it.hour * 60 + it.minute }

fun formatLocalTime(time: LocalTime): String = time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))

fun buildStopTimesForDeparture(
    routeName: String,
    departureTime: String,
    routesByName: Map<String, Route>,
): List<StopTimeInfo> {
    val route = routesByName[routeName] ?: return emptyList()
    val departureLocalTime = parseLocalTime(departureTime) ?: return emptyList()

    return route.stops.mapNotNull { stopKey ->
        val stop = route.stopDetails[stopKey] ?: return@mapNotNull null
        val stopTime = departureLocalTime.plusMinutes(stop.offset.toLong())

        StopTimeInfo(
            stopName = stop.name,
            time = formatLocalTime(stopTime),
        )
    }
}

/**
 * Flattens, filters, and sorts upcoming departures for one route on a given day.
 */
fun consolidatedTimes(
    routeName: String,
    day: DayOfWeek,
    schedule: Schedule,
    routesByName: Map<String, Route>,
): List<TimeInfo> {
    val scheduleMap = schedule.scheduleMapFor(day)
    val out = mutableListOf<TimeInfo>()

    for ((vehicleName, times) in scheduleMap) {
        for (pair in times) {
            if (pair.size <= 1) continue

            val departureTime = pair[0]
            val scheduledRouteName = pair[1]
            if (scheduledRouteName != routeName) continue

            val minutesOfDay = parseMinutesOfDay(departureTime) ?: continue

            out +=
                TimeInfo(
                    departureTime = departureTime,
                    routeName = scheduledRouteName,
                    vehicleName = vehicleName,
                    minutesOfDay = minutesOfDay,
                    stopTimes =
                        buildStopTimesForDeparture(
                            routeName = scheduledRouteName,
                            departureTime = departureTime,
                            routesByName = routesByName,
                        ),
                )
        }
    }

    return out.sortedBy { it.minutesOfDay }
}
