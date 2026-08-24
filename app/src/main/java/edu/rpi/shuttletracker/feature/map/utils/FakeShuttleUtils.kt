package edu.rpi.shuttletracker.feature.map.utils

import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Vehicle
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.sqrt

/** Platform-independent coordinates keep this simulation JVM-testable. */
data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
)

private const val FAKE_LOOP_DURATION_MS = 60_000L
private const val FAKE_HEADING_LOOKAHEAD = 0.01

/** Stable route names keep simulated vehicles predictable. */
private val FAKE_SHUTTLE_ROUTE_NAMES = listOf("NORTH", "WEST")

/** Flattens route coordinates without depending on Android's `LatLng`. */
fun Route.toRoutePoints(): List<RoutePoint> =
    coordinates.flatMap { polyline ->
        polyline.mapNotNull { pair -> if (pair.size >= 2) RoutePoint(pair[0], pair[1]) else null }
    }

/** Interpolates around a closed loop, wrapping [progress] at 1.0. */
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

/** Builds one looping developer-mode vehicle for each configured route. */
fun buildFakeVehicles(
    routes: Map<String, Route>,
    elapsedMs: Long,
    now: Instant = Instant.now(),
): List<Vehicle> =
    FAKE_SHUTTLE_ROUTE_NAMES.mapNotNull { routeName ->
        val route = routes[routeName] ?: return@mapNotNull null
        val points = route.toRoutePoints()
        if (points.size < 2) return@mapNotNull null

        val progress = elapsedMs.toDouble() / FAKE_LOOP_DURATION_MS
        val position = interpolateAlongLoop(points, progress)
        val ahead = interpolateAlongLoop(points, progress + FAKE_HEADING_LOOKAHEAD)

        Vehicle(
            id = "fake-shuttle-$routeName",
            name = routeName.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) },
            latitude = position.latitude,
            longitude = position.longitude,
            speedMph = 12.0,
            timestamp = now.toString(),
            headingDegrees = headingBetween(position, ahead),
            routeName = routeName,
            isAtStop = false,
            currentStop = null,
            stopTimes = buildFakeStopTimes(route, points, progress, now),
        )
    }

/** Estimates each stop ETA from the fake vehicle's remaining route distance. */
private fun buildFakeStopTimes(
    route: Route,
    points: List<RoutePoint>,
    vehicleProgress: Double,
    now: Instant,
): Map<String, String> =
    route.stops
        .mapNotNull { stopKey ->
            val stop = route.stopDetails[stopKey] ?: return@mapNotNull null
            if (stop.coordinates.size < 2) return@mapNotNull null

            val stopProgress = progressOfPoint(points, RoutePoint(stop.coordinates[0], stop.coordinates[1]))
            val remainingProgress = (stopProgress - vehicleProgress).mod(1.0)
            val etaInstant = now.plusMillis((remainingProgress * FAKE_LOOP_DURATION_MS).toLong())

            stopKey to etaInstant.atOffset(ZoneOffset.UTC).toString()
        }.toMap()

/** Returns the loop progress nearest [target]. */
private fun progressOfPoint(
    points: List<RoutePoint>,
    target: RoutePoint,
): Double {
    val loop = points + points.first()
    val segmentLengths = loop.zipWithNext { a, b -> a.distanceTo(b) }
    val totalLength = segmentLengths.sum()
    if (totalLength <= 0.0) return 0.0

    val nearestIndex = points.indices.minBy { index -> points[index].distanceTo(target) }

    return segmentLengths.take(nearestIndex).sum() / totalLength
}

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
