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

private const val MAX_STOPS = 8

private const val MAX_ETAS_PER_STOP = 3

private const val MAX_VEHICLES_PER_STOP = 6

private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class WidgetEtaSnapshot(
    val routeName: String?,
    val etaEpochMillis: Long,
)

@Serializable
data class WidgetStopSnapshot(
    val stopName: String,
    val etas: List<WidgetEtaSnapshot>,
)

@Serializable
data class WidgetVehicleSnapshot(
    val name: String,
    val routeName: String?,
    val speedMph: Double,
    val isAtStop: Boolean,
    val currentStopName: String?,
    val etaEpochMillis: Long?,
)

@Serializable
data class SingleStopSnapshot(
    val stopName: String,
    val vehicles: List<WidgetVehicleSnapshot>,
    val nextScheduledEpochMillis: Long?,
)

/** Serializable data shared by all widget instances after one successful fetch. */
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

/** Keeps the soonest live stops that fit in the all-routes widget. */
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

/** Builds a single-stop view with at-stop vehicles first, then by ETA. */
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

/** Filters the all-routes snapshot; null keeps every route. */
fun WidgetSnapshot.allRoutesForRoute(routeFilter: String?): List<WidgetStopSnapshot> =
    if (routeFilter == null) {
        allRoutes
    } else {
        allRoutes.mapNotNull { stop ->
            val filtered = stop.etas.filter { it.routeName == routeFilter }
            if (filtered.isEmpty()) null else stop.copy(etas = filtered)
        }
    }
