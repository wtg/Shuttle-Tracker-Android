package edu.rpi.shuttletracker.feature.map.utils

import com.google.common.truth.Truth.assertThat
import edu.rpi.shuttletracker.data.models.DayOfWeek
import edu.rpi.shuttletracker.testing.fixtures.testRoute
import edu.rpi.shuttletracker.testing.fixtures.testSchedule
import org.junit.Test
import java.time.LocalTime

class ScheduleUtilsTest {
    @Test
    fun `routes for day returns distinct sorted route names`() {
        val schedule =
            testSchedule(
                weekday =
                    mapOf(
                        "Bus 1" to listOf(listOf("7:00 AM", "WEST"), listOf("8:00 AM", "NORTH")),
                        "Bus 2" to listOf(listOf("9:00 AM", "WEST"), listOf("invalid")),
                    ),
            )

        assertThat(routesForDay(DayOfWeek.MONDAY, schedule)).containsExactly("NORTH", "WEST").inOrder()
    }

    @Test
    fun `consolidated times filters route and sorts after-midnight service last`() {
        val schedule =
            testSchedule(
                weekday =
                    mapOf(
                        "Bus 1" to
                            listOf(
                                listOf("12:30 AM", "NORTH"),
                                listOf("6:30 AM", "WEST"),
                                listOf("7:00 AM", "NORTH"),
                            ),
                    ),
            )

        val result = consolidatedTimes("NORTH", DayOfWeek.MONDAY, schedule, mapOf("NORTH" to testRoute()))

        assertThat(result.map { it.departureTime }).containsExactly("7:00 AM", "12:30 AM").inOrder()
    }

    @Test
    fun `stop times apply each stop offset`() {
        val result = buildStopTimesForDeparture("NORTH", "7:00 AM", mapOf("NORTH" to testRoute()))

        assertThat(result.map { it.time }).containsExactly("7:00 AM", "7:05 AM").inOrder()
    }

    @Test
    fun `unknown route has no stop times`() {
        assertThat(buildStopTimesForDeparture("WEST", "7:00 AM", mapOf("NORTH" to testRoute()))).isEmpty()
    }

    @Test
    fun `normal departure converts to minutes since midnight`() {
        assertThat(parseMinutesOfDay("7:15 AM")).isEqualTo(435)
    }

    @Test
    fun `after-midnight departure sorts as next service day`() {
        assertThat(parseMinutesOfDay("12:30 AM")).isEqualTo(1_470)
    }

    @Test
    fun `invalid time returns null`() {
        assertThat(parseLocalTime("25:70 PM")).isNull()
    }

    @Test
    fun `time formatting uses the schedule format`() {
        assertThat(formatLocalTime(LocalTime.of(19, 5))).isEqualTo("7:05 PM")
    }
}
