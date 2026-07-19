package edu.rpi.shuttletracker.feature.map.utils

import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Vehicle
import java.time.Instant
import kotlin.math.atan2
import kotlin.math.sqrt

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
)

private const val FAKE_LOOP_DURATION_MS = 60_000L
private const val FAKE_VEHICLE_COUNT = 2
private const val FAKE_HEADING_LOOKAHEAD = 0.01

/**
 * Flattens a route's raw coordinates into an ordered loop, deliberately avoiding the Android
 * [com.google.android.gms.maps.model.LatLng] that [Route.latLng] returns so this stays usable
 * from a plain JVM unit test.
 * */
fun Route.toRoutePoints(): List<RoutePoint> =
    coordinates.flatMap { polyline ->
        polyline.mapNotNull { pair -> if (pair.size >= 2) RoutePoint(pair[0], pair[1]) else null }
    }

/**
 * Walks the closed loop formed by [points] (implicitly connecting the last point back to the
 * first) and returns the point [progress] of the way around, where progress wraps at 1.0.
 * */
fun interpolateAlongLoop(
    points: List<RoutePoint>,
    progress: Double,
): RoutePoint {
    require(points.size >= 2) { "Need at least two points to form a loop" }

    val loop = points + points.first()
    val segmentLengths = loop.zipWithNext { a, b -> a.distanceTo(b) }
    val totalLength = segmentLengths.sum()
    if (totalLength <= 0.0) return points.first()

    val targetDistance = progress.mod(1.0) * totalLength

    var traveled = 0.0
    for (i in segmentLengths.indices) {
        val segmentLength = segmentLengths[i]
        if (traveled + segmentLength >= targetDistance || i == segmentLengths.lastIndex) {
            val segmentProgress = if (segmentLength == 0.0) 0.0 else (targetDistance - traveled) / segmentLength
            return loop[i].lerp(loop[i + 1], segmentProgress.coerceIn(0.0, 1.0))
        }
        traveled += segmentLength
    }

    return points.first()
}

/**
 * Builds [FAKE_VEHICLE_COUNT] vehicles continuously looping [route], evenly spaced around it, for
 * developer-mode testing. Entirely separate from live vehicle data - the caller is responsible for
 * keeping these out of [Vehicle] lists sourced from the API.
 * */
fun buildFakeVehicles(
    routeName: String,
    route: Route,
    elapsedMs: Long,
    now: Instant = Instant.now(),
): List<Vehicle> {
    val points = route.toRoutePoints()
    if (points.size < 2) return emptyList()

    return (0 until FAKE_VEHICLE_COUNT).map { index ->
        val offset = index.toDouble() / FAKE_VEHICLE_COUNT
        val progress = (elapsedMs.toDouble() / FAKE_LOOP_DURATION_MS) + offset
        val position = interpolateAlongLoop(points, progress)
        val ahead = interpolateAlongLoop(points, progress + FAKE_HEADING_LOOKAHEAD)

        Vehicle(
            id = "fake-shuttle-${index + 1}",
            name = "Fake Shuttle ${index + 1}",
            latitude = position.latitude,
            longitude = position.longitude,
            speedMph = 12.0,
            timestamp = now.toString(),
            headingDegrees = headingBetween(position, ahead),
            routeName = routeName,
            isAtStop = false,
            currentStop = null,
            stopTimes = emptyMap(),
        )
    }
}

/**
 * Picks the first route (by name, for determinism) with enough coordinates to loop around.
 * */
fun pickFakeShuttleRoute(routes: Map<String, Route>): Pair<String, Route>? =
    routes
        .toSortedMap()
        .entries
        .firstOrNull { it.value.toRoutePoints().size >= 2 }
        ?.let { it.key to it.value }

private fun RoutePoint.distanceTo(other: RoutePoint): Double {
    val dLat = other.latitude - latitude
    val dLon = other.longitude - longitude
    return sqrt(dLat * dLat + dLon * dLon)
}

private fun RoutePoint.lerp(
    other: RoutePoint,
    fraction: Double,
): RoutePoint =
    RoutePoint(
        latitude = latitude + (other.latitude - latitude) * fraction,
        longitude = longitude + (other.longitude - longitude) * fraction,
    )

private fun headingBetween(
    from: RoutePoint,
    to: RoutePoint,
): Int {
    val dLat = to.latitude - from.latitude
    val dLon = to.longitude - from.longitude
    if (dLat == 0.0 && dLon == 0.0) return 0

    val degrees = Math.toDegrees(atan2(dLon, dLat))
    return ((degrees + 360) % 360).toInt()
}
