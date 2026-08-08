package edu.rpi.shuttletracker.data.models

import java.time.DayOfWeek

/**
 * The weekly shuttle schedule. Each day of the week names a schedule *type* ("weekday",
 * "saturday", or "sunday" - see [scheduleTypeFor]), and each type maps to its own set of
 * departures ([weekday]/[saturdaySchedule]/[sundaySchedule]); [scheduleMapFor] resolves a day
 * straight to its departures in one call.
 * */
data class Schedule(
    val monday: String,
    val tuesday: String,
    val wednesday: String,
    val thursday: String,
    val friday: String,
    val saturday: String,
    val sunday: String,
    // Map<busName, List<[time, direction]>>
    // ex. AM WEST Bus 1 -> list of ["7:00 AM", "WEST"]
    val weekday: Map<String, List<List<String>>>,
    val saturdaySchedule: Map<String, List<List<String>>>,
    val sundaySchedule: Map<String, List<List<String>>>,
) {
    fun scheduleTypeFor(day: DayOfWeek): String =
        when (day) {
            DayOfWeek.MONDAY -> monday
            DayOfWeek.TUESDAY -> tuesday
            DayOfWeek.WEDNESDAY -> wednesday
            DayOfWeek.THURSDAY -> thursday
            DayOfWeek.FRIDAY -> friday
            DayOfWeek.SATURDAY -> saturday
            DayOfWeek.SUNDAY -> sunday
        }

    fun scheduleMapFor(day: DayOfWeek): Map<String, List<List<String>>> =
        when (scheduleTypeFor(day).lowercase()) {
            "weekday" -> weekday
            "saturday" -> saturdaySchedule
            "sunday" -> sundaySchedule
            else -> emptyMap()
        }
}
