package edu.rpi.shuttletracker.feature.schedule.utils

import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

/** One stop's expected time for a single departure - a row inside an expanded [TimeInfo]. */
data class StopTimeInfo(
    val stopName: String,
    val time: String,
)

/** One scheduled departure: a vehicle leaving at a time, with every stop's estimated time along the way. */
data class TimeInfo(
    val departureTime: String,
    val vehicleName: String,
    val minutesOfDay: Int,
    val stopTimes: List<StopTimeInfo>,
)

/** Every route with at least one scheduled departure on [day], sorted alphabetically. */
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

/** Every stop on [routeName], with its estimated time computed as [departureTime] plus that stop's [edu.rpi.shuttletracker.data.models.Stop.offset]. */
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

/** The soonest scheduled arrival at [stopKey] on [day], across every route that serves it. An after-midnight departure (e.g. 12:30 AM) counts as later tonight, not earlier today. */
fun nextScheduledArrival(
    stopKey: String,
    schedule: Schedule,
    routesByName: Map<String, Route>,
    day: DayOfWeek = LocalDate.now().dayOfWeek,
    now: LocalDateTime = LocalDateTime.now(),
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

/** Minutes since midnight, for sorting departures - after-midnight times (12-3am) sort last, as the next day's early service rather than the earliest. */
fun parseMinutesOfDay(timeText: String): Int? =
    parseLocalTime(timeText)?.let { time ->
        val minutes = time.hour * 60 + time.minute

        // Moves after midnight departures at the end of the list
        if (time.hour in 0..3) {
            minutes + 24 * 60
        } else {
            minutes
        }
    }

/** Parses a schedule time like "7:00 AM"; returns null instead of throwing on a bad value. */
fun parseLocalTime(timeText: String): LocalTime? =
    runCatching {
        LocalTime.parse(timeText.trim(), TIME_FORMATTER)
    }.getOrNull()

/** Formats a time back to the schedule's display style, e.g. "7:00 AM". */
fun formatLocalTime(time: LocalTime): String = time.format(TIME_FORMATTER)

/**
 * Which row of [times] to auto-scroll/expand to: the departure just before the next upcoming one,
 * so the user sees "you just missed this one, next is this one" context. If every departure is
 * still upcoming, that's row 0; if every departure has already happened, it's the last row (the
 * most recent one) rather than looping back to the top of the morning schedule.
 * */
fun scrollIndexFor(
    times: List<TimeInfo>,
    nowMinutes: Int,
): Int =
    when (val index = times.indexOfFirst { it.minutesOfDay >= nowMinutes }) {
        -1 -> (times.size - 1).coerceAtLeast(0)
        0 -> 0
        else -> index - 1
    }
