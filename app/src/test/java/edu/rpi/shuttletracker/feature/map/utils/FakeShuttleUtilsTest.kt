package edu.rpi.shuttletracker.feature.map.utils

import com.google.common.truth.Truth.assertThat
import edu.rpi.shuttletracker.data.models.Route
import org.junit.Test
import java.time.Instant

class FakeShuttleUtilsTest {
    private val squareLoop =
        listOf(
            RoutePoint(0.0, 0.0),
            RoutePoint(0.0, 1.0),
            RoutePoint(1.0, 1.0),
            RoutePoint(1.0, 0.0),
        )

    private val loopRoute =
        Route(
            color = "#D32F2F",
            stops = emptyList(),
            polylineStops = emptyList(),
            coordinates =
                listOf(
                    listOf(
                        listOf(0.0, 0.0),
                        listOf(0.0, 1.0),
                        listOf(1.0, 1.0),
                        listOf(1.0, 0.0),
                    ),
                ),
            stopDetails = emptyMap(),
        )

    @Test
    fun `interpolating at progress zero returns the first point`() {
        assertThat(interpolateAlongLoop(squareLoop, 0.0)).isEqualTo(RoutePoint(0.0, 0.0))
    }

    @Test
    fun `interpolating at a quarter progress lands on the second point`() {
        val result = interpolateAlongLoop(squareLoop, 0.25)

        assertThat(result.latitude).isWithin(1e-9).of(0.0)
        assertThat(result.longitude).isWithin(1e-9).of(1.0)
    }

    @Test
    fun `progress wraps around the loop past 1_0`() {
        val atStart = interpolateAlongLoop(squareLoop, 0.0)
        val afterFullLoop = interpolateAlongLoop(squareLoop, 1.0)
        val afterOneAndAQuarterLoops = interpolateAlongLoop(squareLoop, 1.25)
        val atQuarter = interpolateAlongLoop(squareLoop, 0.25)

        assertThat(afterFullLoop).isEqualTo(atStart)
        assertThat(afterOneAndAQuarterLoops).isEqualTo(atQuarter)
    }

    @Test
    fun `interpolating midway between two points averages their coordinates`() {
        val result = interpolateAlongLoop(squareLoop, 0.125)

        assertThat(result.latitude).isWithin(1e-9).of(0.0)
        assertThat(result.longitude).isWithin(1e-9).of(0.5)
    }

    @Test
    fun `route coordinates flatten in order into route points`() {
        assertThat(loopRoute.toRoutePoints()).isEqualTo(squareLoop)
    }

    @Test
    fun `a route with fewer than two points produces no fake vehicle for that route`() {
        val tooShort = loopRoute.copy(coordinates = listOf(listOf(listOf(0.0, 0.0))))

        val vehicles = buildFakeVehicles(mapOf("NORTH" to tooShort, "WEST" to loopRoute), elapsedMs = 0L)

        assertThat(vehicles.map { it.routeName }).containsExactly("WEST")
    }

    @Test
    fun `routes other than north and west are ignored`() {
        val vehicles =
            buildFakeVehicles(mapOf("NORTH" to loopRoute, "ACADEMY_SHUTTLE" to loopRoute), elapsedMs = 0L)

        assertThat(vehicles.map { it.routeName }).containsExactly("NORTH")
    }

    @Test
    fun `building fake vehicles produces one vehicle per configured route, each on its own route`() {
        val now = Instant.parse("2026-07-19T12:00:00Z")

        val vehicles = buildFakeVehicles(mapOf("NORTH" to loopRoute, "WEST" to loopRoute), elapsedMs = 0L, now = now)

        assertThat(vehicles).hasSize(2)
        assertThat(vehicles.map { it.id }).containsExactly("fake-shuttle-NORTH", "fake-shuttle-WEST")
        assertThat(vehicles.map { it.routeName }).containsExactly("NORTH", "WEST")

        vehicles.forEach { vehicle ->
            assertThat(vehicle.latitude).isWithin(1e-9).of(0.0)
            assertThat(vehicle.longitude).isWithin(1e-9).of(0.0)
        }
    }

    @Test
    fun `fake vehicles keep moving as elapsed time advances`() {
        val routes = mapOf("NORTH" to loopRoute)
        val atStart = buildFakeVehicles(routes, elapsedMs = 0L).first()
        val later = buildFakeVehicles(routes, elapsedMs = 15_000L).first()

        assertThat(later.latitude != atStart.latitude || later.longitude != atStart.longitude).isTrue()
    }

    @Test
    fun `fake vehicles complete a full loop and return to the start`() {
        val routes = mapOf("NORTH" to loopRoute)
        val atStart = buildFakeVehicles(routes, elapsedMs = 0L).first()
        val afterFullLoop = buildFakeVehicles(routes, elapsedMs = 60_000L).first()

        assertThat(afterFullLoop.latitude).isWithin(1e-9).of(atStart.latitude)
        assertThat(afterFullLoop.longitude).isWithin(1e-9).of(atStart.longitude)
    }
}
