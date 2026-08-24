package edu.rpi.shuttletracker.data.models

import java.time.DayOfWeek

/** Weekly schedule whose days select a weekday, Saturday, or Sunday departure table. */
data class Schedule(
    val monday: String,
    val tuesday: String,
    val wednesday: String,
    val thursday: String,
    val friday: String,
    val saturday: String,
    val sunday: String,
    // Each bus maps to [departure time, route] pairs.
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
