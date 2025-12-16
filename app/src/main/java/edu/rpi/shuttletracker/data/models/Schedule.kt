package edu.rpi.shuttletracker.data.models

import com.google.gson.annotations.SerializedName
import java.util.Calendar

data class Schedule(
    @SerializedName("MONDAY") val monday: String,
    @SerializedName("TUESDAY") val tuesday: String,
    @SerializedName("WEDNESDAY") val wednesday: String,
    @SerializedName("THURSDAY") val thursday: String,
    @SerializedName("FRIDAY") val friday: String,
    @SerializedName("SATURDAY") val saturday: String,
    @SerializedName("SUNDAY") val sunday: String,
    // busName -> list of [time, direction]
    @SerializedName("weekday") val weekday: Map<String, List<List<String>>>,
    @SerializedName("saturday") val saturdaySchedule: Map<String, List<List<String>>>,
    @SerializedName("sunday") val sundaySchedule: Map<String, List<List<String>>>,
)

enum class DayOfWeek(val displayName: String, val calendarConst: Int) {
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
