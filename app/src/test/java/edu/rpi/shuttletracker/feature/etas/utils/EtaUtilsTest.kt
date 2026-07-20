package edu.rpi.shuttletracker.feature.etas.utils

import com.google.common.truth.Truth.assertThat
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.models.Vehicle
import org.junit.Test
import java.time.Instant

class EtaUtilsTest {
    private val union = Stop(listOf(42.730, -73.680), 0, "Student Union")
    private val academy = Stop(listOf(42.731, -73.679), 5, "Academy Hall")

    private val northRoute =
        Route(
            color = "#D32F2F",
            stops = listOf("union", "academy"),
            polylineStops = emptyList(),
            coordinates = emptyList(),
            stopDetails = mapOf("union" to union, "academy" to academy),
        )

    private val westRoute =
        Route(
            color = "#1976D2",
            stops = listOf("union"),
            polylineStops = emptyList(),
            coordinates = emptyList(),
            stopDetails = mapOf("union" to union),
        )

    private fun vehicle(
        id: String,
        name: String,
        routeName: String?,
        stopTimes: Map<String, String>,
    ) = Vehicle(
        id = id,
        name = name,
        latitude = 42.730,
        longitude = -73.680,
        speedMph = 5.0,
        timestamp = "2026-07-19T12:00:00Z",
        headingDegrees = null,
        routeName = routeName,
        isAtStop = false,
        currentStop = null,
        stopTimes = stopTimes,
    )

    @Test
    fun `stops are deduplicated across routes and tagged with every serving route`() {
        val stops =
            buildStopsWithEtas(
                routes = mapOf("NORTH" to northRoute, "WEST" to westRoute),
                vehicles = emptyList(),
            )

        assertThat(stops.map { it.stopKey }).containsExactly("academy", "union")
        assertThat(stops.single { it.stopKey == "union" }.routeNames).containsExactly("NORTH", "WEST").inOrder()
    }

    @Test
    fun `route filter limits stops to that route`() {
        val stops =
            buildStopsWithEtas(
                routes = mapOf("NORTH" to northRoute, "WEST" to westRoute),
                vehicles = emptyList(),
                routeFilter = "NORTH",
            )

        assertThat(stops.map { it.stopKey }).containsExactly("academy", "union")

        val westOnly =
            buildStopsWithEtas(
                routes = mapOf("NORTH" to northRoute, "WEST" to westRoute),
                vehicles = emptyList(),
                routeFilter = "WEST",
            )

        assertThat(westOnly.map { it.stopKey }).containsExactly("union")
    }

    @Test
    fun `routes other than north and west are ignored even if present in the data`() {
        val academyShuttle =
            Route(
                color = "#00FF00",
                stops = listOf("academy"),
                polylineStops = emptyList(),
                coordinates = emptyList(),
                stopDetails = mapOf("academy" to academy),
            )

        val stops =
            buildStopsWithEtas(
                routes = mapOf("NORTH" to northRoute, "ACADEMY_SHUTTLE" to academyShuttle),
                vehicles = emptyList(),
            )

        assertThat(stops.map { it.stopKey }).containsExactly("academy", "union")
        assertThat(stops.single { it.stopKey == "academy" }.routeNames).containsExactly("NORTH")
    }

    @Test
    fun `etas for a stop are inverted from each vehicle's stop times and sorted soonest first`() {
        val bus1 = vehicle("bus-1", "North Bus", "NORTH", mapOf("union" to "2026-07-19T12:10:00Z"))
        val bus2 = vehicle("bus-2", "West Bus", "WEST", mapOf("union" to "2026-07-19T12:05:00Z"))

        val stops =
            buildStopsWithEtas(
                routes = mapOf("NORTH" to northRoute, "WEST" to westRoute),
                vehicles = listOf(bus1, bus2),
            )

        val unionEtas = stops.single { it.stopKey == "union" }.etas
        assertThat(unionEtas.map { it.vehicleId }).containsExactly("bus-2", "bus-1").inOrder()
    }

    @Test
    fun `a vehicle on a route outside the visible list is excluded from eta chips too`() {
        val strayBus = vehicle("bus-3", "Test Bus", "ACADEMY_SHUTTLE", mapOf("union" to "2026-07-19T12:01:00Z"))

        val stops =
            buildStopsWithEtas(
                routes = mapOf("NORTH" to northRoute),
                vehicles = listOf(strayBus),
            )

        assertThat(stops.single { it.stopKey == "union" }.etas).isEmpty()
    }

    @Test
    fun `a vehicle with no eta for a stop is excluded from that stop's list`() {
        val bus1 = vehicle("bus-1", "North Bus", "NORTH", emptyMap())

        val stops =
            buildStopsWithEtas(
                routes = mapOf("NORTH" to northRoute),
                vehicles = listOf(bus1),
            )

        assertThat(stops.single { it.stopKey == "union" }.etas).isEmpty()
    }

    @Test
    fun `an unparseable eta timestamp is excluded rather than crashing`() {
        val bus1 = vehicle("bus-1", "North Bus", "NORTH", mapOf("union" to "not-a-timestamp"))

        val stops =
            buildStopsWithEtas(
                routes = mapOf("NORTH" to northRoute),
                vehicles = listOf(bus1),
            )

        assertThat(stops.single { it.stopKey == "union" }.etas).isEmpty()
    }

    @Test
    fun `eta minutes from now rounds down the duration until the eta instant`() {
        val now = Instant.parse("2026-07-19T12:00:00Z")
        val eta = Instant.parse("2026-07-19T12:05:30Z")

        assertThat(etaMinutesFromNow(eta, now)).isEqualTo(5)
    }

    @Test
    fun `eta minutes from now is negative once the eta has passed`() {
        val now = Instant.parse("2026-07-19T12:10:00Z")
        val eta = Instant.parse("2026-07-19T12:05:00Z")

        assertThat(etaMinutesFromNow(eta, now)).isEqualTo(-5)
    }
}
