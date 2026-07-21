package edu.rpi.shuttletracker.feature.map.utils

import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Vehicle
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.sqrt

/** A plain lat/lng pair, used instead of Android's `LatLng` so this file is unit-testable on the JVM. */
data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
)

private const val FAKE_LOOP_DURATION_MS = 60_000L
private const val FAKE_HEADING_LOOKAHEAD = 0.01

/**
 * Real routes to run fake shuttles on. Hardcoded rather than picked dynamically so each fake
 * vehicle always follows a real, named route loop instead of whichever route happened to sort
 * first.
 * */
private val FAKE_SHUTTLE_ROUTE_NAMES = listOf("NORTH", "WEST")

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
 * Builds one vehicle per [FAKE_SHUTTLE_ROUTE_NAMES] route present in [routes], each continuously
 * looping that route's own coordinates, for developer-mode testing. Entirely separate from live
 * vehicle data - the caller is responsible for keeping these out of [Vehicle] lists sourced from
 * the API.
 * */
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
            // Just the route name ("North"/"West") - the id already marks it as fake.
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

/** Synthesizes an eta for every stop on [route], based on how far the fake vehicle ([vehicleProgress] around [points]) still has to travel to reach it. */
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

/** How far around the loop (0.0-1.0, same convention as [interpolateAlongLoop]) the point nearest [target] sits. */
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
