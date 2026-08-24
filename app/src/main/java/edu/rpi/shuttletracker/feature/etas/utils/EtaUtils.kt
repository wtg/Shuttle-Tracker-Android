package edu.rpi.shuttletracker.feature.etas.utils

import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.models.Vehicle
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

data class VehicleEta(
    val vehicleId: String,
    val vehicleName: String,
    val routeName: String?,
    val etaInstant: Instant,
)

/** A stop and its approaching vehicles, in display order. */
data class StopWithEtas(
    val stopKey: String,
    val stop: Stop,
    val routeNames: List<String>,
    val etas: List<VehicleEta>,
)

/** Public routes shown in ETA filters; unexpected API routes stay hidden. */
val ETA_VISIBLE_ROUTES = listOf("NORTH", "WEST")

/** Grace period before a passed arrival disappears from the ETA list. */
internal const val ETA_PAST_GRACE_PERIOD_MINUTES = 1L

/** Builds the route-ordered stop list by inverting each vehicle's stop-to-ETA map. */
fun buildStopsWithEtas(
    routes: Map<String, Route>,
    vehicles: List<Vehicle>,
    routeFilter: String? = null,
    now: Instant = Instant.now(),
): List<StopWithEtas> {
    val stopsByKey = linkedMapOf<String, Pair<Stop, MutableSet<String>>>()
    val oldestVisibleEta = now.minusSeconds(ETA_PAST_GRACE_PERIOD_MINUTES * 60)

    for ((routeName, route) in routes) {
        if (routeName !in ETA_VISIBLE_ROUTES) continue
        if (routeFilter != null && routeName != routeFilter) continue

        for (stopKey in route.stops) {
            val stop = route.stopDetails[stopKey] ?: continue
            val entry = stopsByKey.getOrPut(stopKey) { stop to mutableSetOf() }
            entry.second += routeName
        }
    }

    return stopsByKey
        .map { (stopKey, stopAndRoutes) ->
            val (stop, routeNames) = stopAndRoutes

            val etas =
                vehicles
                    .mapNotNull { vehicle ->
                        if (vehicle.routeName !in ETA_VISIBLE_ROUTES) return@mapNotNull null
                        if (routeFilter != null && vehicle.routeName != routeFilter) return@mapNotNull null
                        val rawEta = vehicle.stopTimes[stopKey] ?: return@mapNotNull null
                        val etaInstant = rawEta.toEtaInstantOrNull() ?: return@mapNotNull null
                        if (etaInstant.isBefore(oldestVisibleEta)) return@mapNotNull null

                        VehicleEta(
                            vehicleId = vehicle.id,
                            vehicleName = vehicle.name,
                            routeName = vehicle.routeName,
                            etaInstant = etaInstant,
                        )
                    }.sortedBy { it.etaInstant }

            StopWithEtas(
                stopKey = stopKey,
                stop = stop,
                routeNames = routeNames.sorted(),
                etas = etas,
            )
        }
}

fun String.toEtaInstantOrNull(): Instant? = runCatching { OffsetDateTime.parse(trim()).toInstant() }.getOrNull()

fun etaMinutesFromNow(
    etaInstant: Instant,
    now: Instant = Instant.now(),
): Long = Duration.between(now, etaInstant).toMinutes()

/** Extra tolerance for stale ETAs in the selected stop's live vehicle list. */
private const val ETA_PAST_TOLERANCE_MINUTES = 2L

/** Vehicles currently at [stopKey] or still within its ETA tolerance. */
fun vehiclesForStop(
    vehicles: List<Vehicle>,
    stopKey: String,
    now: Instant = Instant.now(),
): List<Vehicle> =
    vehicles.filter { vehicle ->
        val isAtThisStop = vehicle.isAtStop == true && vehicle.currentStop == stopKey
        val etaInstant = vehicle.stopTimes[stopKey]?.toEtaInstantOrNull()
        val hasRelevantEta = etaInstant != null && etaMinutesFromNow(etaInstant, now) >= -ETA_PAST_TOLERANCE_MINUTES

        isAtThisStop || hasRelevantEta
    }
