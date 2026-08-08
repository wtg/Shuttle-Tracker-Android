package edu.rpi.shuttletracker.widget

import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Vehicle
import edu.rpi.shuttletracker.feature.etas.utils.StopWithEtas
import edu.rpi.shuttletracker.feature.etas.utils.toEtaInstantOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

/** How many soonest-arriving stops the "all routes" view keeps - it only has room to show a handful. */
private const val MAX_STOPS = 8

/** How many upcoming shuttles per stop the "all routes" view keeps. */
private const val MAX_ETAS_PER_STOP = 3

/** How many vehicles the single-stop view keeps. */
private const val MAX_VEHICLES_PER_STOP = 6

private val json = Json { ignoreUnknownKeys = true }

/** One upcoming arrival - a serializable stand-in for [edu.rpi.shuttletracker.feature.etas.utils.VehicleEta]. */
@Serializable
data class WidgetEtaSnapshot(
    val routeName: String?,
    val etaEpochMillis: Long,
)

/** One stop plus its soonest upcoming arrivals - the "all routes" view's stand-in for [StopWithEtas]. */
@Serializable
data class WidgetStopSnapshot(
    val stopName: String,
    val etas: List<WidgetEtaSnapshot>,
)

/** One vehicle relevant to a single-stop view's target stop - see [edu.rpi.shuttletracker.feature.etas.utils.vehiclesForStop]. */
@Serializable
data class WidgetVehicleSnapshot(
    val name: String,
    val routeName: String?,
    val speedMph: Double,
    val isAtStop: Boolean,
    val currentStopName: String?,
    val etaEpochMillis: Long?,
)

/** Everything a single-stop widget instance needs to render one target stop. */
@Serializable
data class SingleStopSnapshot(
    val stopName: String,
    val vehicles: List<WidgetVehicleSnapshot>,
    val nextScheduledEpochMillis: Long?,
)

/**
 * Everything the widget shows, as of one successful fetch, stored as JSON in Glance state.
 * [allRoutes] backs the all-routes view; [perStop] holds every stop's own single-stop view (keyed
 * by stop key) so any instance can show whichever stop it's configured for.
 * */
@Serializable
data class WidgetSnapshot(
    val allRoutes: List<WidgetStopSnapshot> = emptyList(),
    val perStop: Map<String, SingleStopSnapshot> = emptyMap(),
) {
    fun toJson(): String = json.encodeToString(this)

    companion object {
        val Empty = WidgetSnapshot()

        fun fromJsonOrEmpty(raw: String?): WidgetSnapshot =
            if (raw.isNullOrBlank()) {
                Empty
            } else {
                runCatching { json.decodeFromString<WidgetSnapshot>(raw) }.getOrDefault(Empty)
            }
    }
}

/** Keeps only the stops with a live eta, soonest first, trimmed to what the widget has room to show. */
fun List<StopWithEtas>.toWidgetStopSnapshots(): List<WidgetStopSnapshot> =
    this
        .filter { it.etas.isNotEmpty() }
        .sortedBy { it.etas.first().etaInstant }
        .take(MAX_STOPS)
        .map { stop ->
            WidgetStopSnapshot(
                stopName = stop.stop.name,
                etas =
                    stop.etas.take(MAX_ETAS_PER_STOP).map { eta ->
                        WidgetEtaSnapshot(
                            routeName = eta.routeName,
                            etaEpochMillis = eta.etaInstant.toEpochMilli(),
                        )
                    },
            )
        }

/** Builds the single-stop view for [stopKey]: [vehicles] sorted at-stop-first then soonest eta, trimmed to [MAX_VEHICLES_PER_STOP]. [routesByName] resolves each vehicle's current stop key to a display name. */
fun buildSingleStopSnapshot(
    stopKey: String,
    stopName: String,
    vehicles: List<Vehicle>,
    routesByName: Map<String, Route>,
    nextScheduledEpochMillis: Long?,
): SingleStopSnapshot {
    fun stopDisplayName(key: String?): String? =
        key?.let { k -> routesByName.values.firstNotNullOfOrNull { it.stopDetails[k]?.name } }

    val vehicleSnapshots =
        vehicles
            .map { vehicle -> vehicle to vehicle.stopTimes[stopKey]?.toEtaInstantOrNull() }
            .sortedWith(
                compareByDescending<Pair<Vehicle, Instant?>> { (vehicle, _) ->
                    vehicle.isAtStop == true && vehicle.currentStop == stopKey
                }.thenBy { (_, etaInstant) -> etaInstant ?: Instant.MAX },
            ).take(MAX_VEHICLES_PER_STOP)
            .map { (vehicle, etaInstant) ->
                WidgetVehicleSnapshot(
                    name = vehicle.name,
                    routeName = vehicle.routeName,
                    speedMph = vehicle.speedMph,
                    isAtStop = vehicle.isAtStop == true,
                    currentStopName = stopDisplayName(vehicle.currentStop),
                    etaEpochMillis = etaInstant?.toEpochMilli(),
                )
            }

    return SingleStopSnapshot(
        stopName = stopName,
        vehicles = vehicleSnapshots,
        nextScheduledEpochMillis = nextScheduledEpochMillis,
    )
}

/** [WidgetSnapshot.allRoutes] narrowed to one route, dropping stops left with no matching etas - `null` shows every route, as-is. */
fun WidgetSnapshot.allRoutesForRoute(routeFilter: String?): List<WidgetStopSnapshot> =
    if (routeFilter == null) {
        allRoutes
    } else {
        allRoutes.mapNotNull { stop ->
            val filtered = stop.etas.filter { it.routeName == routeFilter }
            if (filtered.isEmpty()) null else stop.copy(etas = filtered)
        }
    }
