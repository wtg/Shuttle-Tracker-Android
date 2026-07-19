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
    fun `a route with fewer than two points has no fake vehicles`() {
        val tooShort =
            loopRoute.copy(coordinates = listOf(listOf(listOf(0.0, 0.0))))

        assertThat(buildFakeVehicles("NORTH", tooShort, elapsedMs = 0L)).isEmpty()
    }

    @Test
    fun `building fake vehicles produces exactly two vehicles offset around the loop`() {
        val now = Instant.parse("2026-07-19T12:00:00Z")

        val vehicles = buildFakeVehicles("NORTH", loopRoute, elapsedMs = 0L, now = now)

        assertThat(vehicles).hasSize(2)
        assertThat(vehicles.map { it.id }).containsExactly("fake-shuttle-1", "fake-shuttle-2")
        assertThat(vehicles.all { it.routeName == "NORTH" }).isTrue()

        val first = vehicles[0]
        val second = vehicles[1]
        assertThat(first.latitude).isWithin(1e-9).of(0.0)
        assertThat(first.longitude).isWithin(1e-9).of(0.0)
        assertThat(second.latitude).isWithin(1e-9).of(1.0)
        assertThat(second.longitude).isWithin(1e-9).of(1.0)
    }

    @Test
    fun `fake vehicles keep moving as elapsed time advances`() {
        val atStart = buildFakeVehicles("NORTH", loopRoute, elapsedMs = 0L).first()
        val later = buildFakeVehicles("NORTH", loopRoute, elapsedMs = 15_000L).first()

        assertThat(later.latitude != atStart.latitude || later.longitude != atStart.longitude).isTrue()
    }

    @Test
    fun `fake vehicles complete a full loop and return to the start`() {
        val atStart = buildFakeVehicles("NORTH", loopRoute, elapsedMs = 0L).first()
        val afterFullLoop = buildFakeVehicles("NORTH", loopRoute, elapsedMs = 60_000L).first()

        assertThat(afterFullLoop.latitude).isWithin(1e-9).of(atStart.latitude)
        assertThat(afterFullLoop.longitude).isWithin(1e-9).of(atStart.longitude)
    }

    @Test
    fun `pick fake shuttle route chooses the alphabetically first route with a loop`() {
        val short = loopRoute.copy(coordinates = listOf(listOf(listOf(5.0, 5.0))))
        val routes = mapOf("WEST" to loopRoute, "ACADEMY_SHUTTLE" to short, "NORTH" to loopRoute)

        val picked = pickFakeShuttleRoute(routes)

        assertThat(picked?.first).isEqualTo("NORTH")
    }

    @Test
    fun `pick fake shuttle route returns null when no route has a usable loop`() {
        val short = loopRoute.copy(coordinates = listOf(listOf(listOf(5.0, 5.0))))

        assertThat(pickFakeShuttleRoute(mapOf("NORTH" to short))).isNull()
    }
}
