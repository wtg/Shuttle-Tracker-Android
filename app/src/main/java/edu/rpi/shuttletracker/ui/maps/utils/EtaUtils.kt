package edu.rpi.shuttletracker.ui.maps.utils

import androidx.compose.runtime.Immutable
import edu.rpi.shuttletracker.data.models.DayOfWeek
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.models.Vehicle
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

private const val PREVIOUS_LOOP_ETA_MAX_AGE_SECONDS = 300L

@Immutable
data class VehicleEtaUi(
    val vehicleId: String,
    val vehicleLabel: String,
    val etaText: String,
    val etaInstant: Instant,
)

@Immutable
data class StopRowUi(
    val stopKey: String,
    val stop: Stop,
    val etaLabels: List<VehicleEtaLabel>,
)

@Immutable
data class VehicleEtaLabel(
    val vehicleLabel: String,
    val minutes: Long? = null,
    val scheduledTime: String? = null,
)

object EtaUtils {
    fun updateLastSeenStopIndices(
        vehicles: List<Vehicle>,
        routesByName: Map<String, Route>,
        previousStopIndexByVehicleId: Map<String, Int>,
    ): Map<String, Int> {
        val updatedStopIndexByVehicleId = previousStopIndexByVehicleId.toMutableMap()

        vehicles.forEach { vehicle ->
            val route = vehicle.routeName?.let(routesByName::get) ?: return@forEach
            val currentStopIndex = findCurrentStopIndex(vehicle, route) ?: return@forEach

            val previousStopIndex = updatedStopIndexByVehicleId[vehicle.id]

            when {
                previousStopIndex == null -> {
                    updatedStopIndexByVehicleId[vehicle.id] = currentStopIndex
                }

                currentStopIndex > previousStopIndex -> {
                    updatedStopIndexByVehicleId[vehicle.id] = currentStopIndex
                }

                currentStopIndex == 0 && previousStopIndex > 0 -> {
                    // Bus looped back to the beginning of the route
                    updatedStopIndexByVehicleId[vehicle.id] = 0
                }
            }
        }

        return updatedStopIndexByVehicleId
    }

    fun buildSelectedStopEtas(
        selectedStopKey: String,
        routesByName: Map<String, Route>,
        vehicles: List<Vehicle>,
        lastSeenStopIndexByVehicleId: Map<String, Int>,
        now: Instant = Instant.now(),
    ): List<VehicleEtaUi> {
        return vehicles
            .asSequence()
            .mapNotNull { vehicle ->
                val route = vehicle.routeName?.let(routesByName::get) ?: return@mapNotNull null
                val stopIndex = route.stops.indexOf(selectedStopKey)
                if (stopIndex == -1) return@mapNotNull null

                val lastSeenStopIndex = lastSeenStopIndexByVehicleId[vehicle.id]
                if (!shouldShowStopForVehicle(vehicle, route, stopIndex, lastSeenStopIndex)) {
                    return@mapNotNull null
                }

                val etaInstant = vehicle.stopTimes[selectedStopKey]?.toInstantOrNull() ?: return@mapNotNull null
                if (shouldHideOldEtaAtRouteStart(vehicle, route, etaInstant, now)) {
                    return@mapNotNull null
                }

                VehicleEtaUi(
                    vehicleId = vehicle.id,
                    vehicleLabel = vehicle.name,
                    etaText = formatEtaText(now, etaInstant),
                    etaInstant = etaInstant,
                )
            }.sortedBy { it.etaInstant }
            .toList()
    }

    fun buildStopRowsForRoute(
        route: Route,
        vehicles: List<Vehicle>,
        routeName: String,
        lastSeenStopIndexByVehicleId: Map<String, Int>,
        schedule: Schedule?,
        day: DayOfWeek,
        now: Instant = Instant.now(),
        maxEtasPerStop: Int = 2,
    ): List<StopRowUi> {
        return route.stops.mapNotNull { stopKey ->
            val stop = route.stopDetails[stopKey] ?: return@mapNotNull null
            val stopIndex = route.stops.indexOf(stopKey)

            val etaLabels =
                vehicles
                    .asSequence()
                    .filter { it.routeName == routeName }
                    .mapNotNull { vehicle ->
                        val lastSeenStopIndex = lastSeenStopIndexByVehicleId[vehicle.id]
                        if (!shouldShowStopForVehicle(vehicle, route, stopIndex, lastSeenStopIndex)) {
                            return@mapNotNull null
                        }

                        val etaInstant =
                            vehicle.stopTimes[stopKey]?.toInstantOrNull() ?: return@mapNotNull null

                        if (shouldHideOldEtaAtRouteStart(vehicle, route, etaInstant, now)) {
                            return@mapNotNull null
                        }

                        val minutesUntilArrival = Duration.between(now, etaInstant).toMinutes()

                        VehicleEtaLabel(
                            vehicleLabel = vehicle.name,
                            minutes = minutesUntilArrival,
                        )
                    }.sortedBy { it.minutes ?: Long.MAX_VALUE }
                    .take(maxEtasPerStop)
                    .toMutableList()

            val firstStopKey = route.stops.firstOrNull()
            val isFirstStop = stopKey == firstStopKey

            if (isFirstStop && etaLabels.isEmpty() && schedule != null) {
                etaLabels +=
                    nextScheduledDepartures(
                        routeName = routeName,
                        day = day,
                        schedule = schedule,
                        maxCount = 2,
                    )
            }

            StopRowUi(
                stopKey = stopKey,
                stop = stop,
                etaLabels = etaLabels,
            )
        }
    }

    fun shouldShowStopForVehicle(
        vehicle: Vehicle,
        route: Route,
        candidateStopIndex: Int,
        lastSeenStopIndex: Int?,
    ): Boolean {
        val currentStopIndex = findCurrentStopIndex(vehicle, route)
        val isCurrentStop = currentStopIndex != null && candidateStopIndex == currentStopIndex

        if (lastSeenStopIndex != null) {
            if (candidateStopIndex < lastSeenStopIndex) return false

            // If this is the same stop as the last seen stop, only show it
            // when the vehicle still reports that stop as current.
            if (candidateStopIndex == lastSeenStopIndex && !isCurrentStop) {
                return false
            }
        }

        return true
    }

    fun findCurrentStopIndex(
        vehicle: Vehicle,
        route: Route,
    ): Int? {
        val currentStopKey = vehicle.currentStop ?: return null

        val stopIndex =
            route.stops.indexOfFirst { routeStopKey ->
                routeStopKey.equals(currentStopKey, ignoreCase = true)
            }

        return stopIndex.takeIf { it != -1 }
    }

    fun formatEtaText(
        now: Instant,
        etaInstant: Instant,
    ): String {
        val minutesUntilArrival = Duration.between(now, etaInstant).toMinutes()
        return formatEtaMinutes(minutesUntilArrival)
    }

    private fun formatEtaMinutes(minutesUntilArrival: Long): String =
        when {
            minutesUntilArrival <= 0 -> "${minutesUntilArrival}m"
            else -> "${minutesUntilArrival}m"
        }

    private fun shouldHideOldEtaAtRouteStart(
        vehicle: Vehicle,
        route: Route,
        etaInstant: Instant,
        now: Instant,
    ): Boolean {
        val firstStopKey = route.stops.firstOrNull() ?: return false
        val isCurrentlyAtFirstStop =
            vehicle.currentStop?.equals(firstStopKey, ignoreCase = true) == true

        return isCurrentlyAtFirstStop &&
            etaInstant.isBefore(now.minusSeconds(PREVIOUS_LOOP_ETA_MAX_AGE_SECONDS))
    }

    private fun String.toInstantOrNull(): Instant? =
        runCatching { OffsetDateTime.parse(trim()).toInstant() }.getOrNull()

    private fun nextScheduledDepartures(
        routeName: String,
        day: DayOfWeek,
        schedule: Schedule,
        maxCount: Int,
    ): List<VehicleEtaLabel> {
        val scheduleMap = schedule.scheduleMapFor(day)

        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

        val departures =
            buildList {
                for ((vehicleName, times) in scheduleMap) {
                    for (pair in times) {
                        if (pair.size <= 1) continue

                        val timeStr = pair[0]
                        val scheduledRouteName = pair[1]
                        if (scheduledRouteName != routeName) continue

                        val localTime =
                            runCatching { LocalTime.parse(timeStr.trim(), formatter) }.getOrNull()
                                ?: continue

                        val minutesOfDay = localTime.hour * 60 + localTime.minute
                        if (minutesOfDay < nowMinutes) continue

                        add(
                            Triple(
                                vehicleName,
                                timeStr,
                                minutesOfDay,
                            ),
                        )
                    }
                }
            }.sortedBy { it.third }
                .take(maxCount)

        return departures.map { (vehicleName, timeStr, _) ->
            VehicleEtaLabel(
                vehicleLabel = vehicleName,
                scheduledTime = timeStr,
            )
        }
    }
}
