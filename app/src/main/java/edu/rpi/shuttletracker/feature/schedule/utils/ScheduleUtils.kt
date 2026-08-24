package edu.rpi.shuttletracker.feature.schedule.utils

import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
val RPI_ZONE_ID: ZoneId = ZoneId.of("America/New_York")

data class StopTimeInfo(
    val stopName: String,
    val time: String,
)

data class TimeInfo(
    val departureTime: String,
    val vehicleName: String,
    val minutesOfDay: Int,
    val stopTimes: List<StopTimeInfo>,
)

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

/** Returns one route's departures in service-day order. */
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

/** Adds each stop's route offset to [departureTime]. */
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

/** Finds the soonest scheduled arrival, treating after-midnight service as later that night. */
fun nextScheduledArrival(
    stopKey: String,
    schedule: Schedule,
    routesByName: Map<String, Route>,
    day: DayOfWeek = LocalDate.now(RPI_ZONE_ID).dayOfWeek,
    now: LocalDateTime = LocalDateTime.now(RPI_ZONE_ID),
): LocalDateTime? {
    val scheduleMap = schedule.scheduleMapFor(day)
    var next: LocalDateTime? = null

    for ((_, times) in scheduleMap) {
        for (pair in times) {
            if (pair.size < 2) continue
            val routeName = pair[1]
            val route = routesByName[routeName] ?: continue
            val stop = route.stopDetails[stopKey] ?: continue
            val departureLocalTime = parseLocalTime(pair[0]) ?: continue

            var arrival = now.toLocalDate().atTime(departureLocalTime).plusMinutes(stop.offset.toLong())
            if (arrival.hour < 4 && now.hour >= 18) {
                arrival = arrival.plusDays(1)
            }

            if (arrival.isAfter(now) && (next == null || arrival.isBefore(next))) {
                next = arrival
            }
        }
    }

    return next
}

/** Sort key that places midnight-to-3 AM service at the end of the service day. */
fun parseMinutesOfDay(timeText: String): Int? =
    parseLocalTime(timeText)?.let { time ->
        val minutes = time.hour * 60 + time.minute

        if (time.hour in 0..3) {
            minutes + 24 * 60
        } else {
            minutes
        }
    }

/** Parses schedule display times, returning null for invalid input. */
fun parseLocalTime(timeText: String): LocalTime? =
    runCatching {
        LocalTime.parse(timeText.trim(), TIME_FORMATTER)
    }.getOrNull()

fun formatLocalTime(time: LocalTime): String = time.format(TIME_FORMATTER)

/** Chooses the previous departure so the next one remains visible with context. */
fun scrollIndexFor(
    times: List<TimeInfo>,
    nowMinutes: Int,
): Int =
    when (val index = times.indexOfFirst { it.minutesOfDay >= nowMinutes }) {
        -1 -> (times.size - 1).coerceAtLeast(0)
        0 -> 0
        else -> index - 1
    }
