package edu.rpi.shuttletracker.data.models

import java.util.Calendar

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

/** Wraps [java.util.Calendar]'s day constants so the rest of the app never has to touch them directly. */
enum class DayOfWeek(
    val displayName: String,
    val calendarConst: Int,
) {
    MONDAY("Mon", Calendar.MONDAY),
    TUESDAY("Tue", Calendar.TUESDAY),
    WEDNESDAY("Wed", Calendar.WEDNESDAY),
    THURSDAY("Thu", Calendar.THURSDAY),
    FRIDAY("Fri", Calendar.FRIDAY),
    SATURDAY("Sat", Calendar.SATURDAY),
    SUNDAY("Sun", Calendar.SUNDAY),
    ;

    companion object {
        fun fromToday(): DayOfWeek {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            return entries.firstOrNull { it.calendarConst == today } ?: MONDAY
        }
    }
}
