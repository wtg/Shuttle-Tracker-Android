package edu.rpi.shuttletracker.widget

import com.google.common.truth.Truth.assertThat
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.models.Vehicle
import org.junit.Test

class WidgetSnapshotTest {
    private val union = Stop(listOf(42.730, -73.680), 0, "Student Union")
    private val academy = Stop(listOf(42.731, -73.679), 5, "Academy Hall")

    private val northRoute =
        Route(
            color = "#D32F2F",
            stops = listOf("union", "academy"),
            coordinates = emptyList(),
            stopDetails = mapOf("union" to union, "academy" to academy),
        )

    private fun vehicle(
        id: String,
        name: String,
        routeName: String? = "NORTH",
        isAtStop: Boolean = false,
        currentStop: String? = null,
        stopTimes: Map<String, String> = emptyMap(),
    ) = Vehicle(
        id = id,
        name = name,
        latitude = 42.730,
        longitude = -73.680,
        speedMph = 8.0,
        timestamp = "2026-07-19T12:00:00Z",
        headingDegrees = null,
        routeName = routeName,
        isAtStop = isAtStop,
        currentStop = currentStop,
        stopTimes = stopTimes,
    )

    @Test
    fun `single-stop snapshot resolves a vehicle's current stop to a display name`() {
        val bus =
            vehicle(
                "bus-1",
                "North Bus",
                isAtStop = true,
                currentStop = "academy",
                stopTimes = mapOf("union" to "2026-07-19T12:05:00Z"),
            )

        val snapshot =
            buildSingleStopSnapshot(
                stopKey = "union",
                stopName = "Student Union",
                vehicles = listOf(bus),
                routesByName = mapOf("NORTH" to northRoute),
                nextScheduledEpochMillis = null,
            )

        assertThat(snapshot.vehicles.single().currentStopName).isEqualTo("Academy Hall")
        assertThat(snapshot.vehicles.single().etaEpochMillis).isNotNull()
    }

    @Test
    fun `single-stop snapshot pins at-stop vehicles ahead of ones still en route`() {
        val enRoute = vehicle("bus-1", "North Bus", stopTimes = mapOf("union" to "2026-07-19T12:05:00Z"))
        val atStop = vehicle("bus-2", "West Bus", isAtStop = true, currentStop = "union")

        val snapshot =
            buildSingleStopSnapshot(
                stopKey = "union",
                stopName = "Student Union",
                vehicles = listOf(enRoute, atStop),
                routesByName = mapOf("NORTH" to northRoute),
                nextScheduledEpochMillis = null,
            )

        assertThat(snapshot.vehicles.map { it.name }).containsExactly("West Bus", "North Bus").inOrder()
    }

    @Test
    fun `all-routes snapshot narrows to one route and drops stops left with no matching etas`() {
        val snapshot =
            WidgetSnapshot(
                allRoutes =
                    listOf(
                        WidgetStopSnapshot(
                            stopName = "Student Union",
                            etas =
                                listOf(
                                    WidgetEtaSnapshot(routeName = "NORTH", etaEpochMillis = 1000L),
                                    WidgetEtaSnapshot(routeName = "WEST", etaEpochMillis = 2000L),
                                ),
                        ),
                        WidgetStopSnapshot(
                            stopName = "Academy Hall",
                            etas = listOf(WidgetEtaSnapshot(routeName = "WEST", etaEpochMillis = 3000L)),
                        ),
                    ),
            )

        val filtered = snapshot.allRoutesForRoute("NORTH")

        assertThat(filtered.map { it.stopName }).containsExactly("Student Union")
        assertThat(filtered.single().etas.map { it.routeName }).containsExactly("NORTH")
    }
}
