package edu.rpi.shuttletracker.ui.maps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.models.Vehicle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Composable
fun StopEtaContent(
    modifier: Modifier = Modifier,
    selectedStopKey: String?,
    selectedStop: Stop?,
    routes: Map<String, Route>,
    vehicles: List<Vehicle>,
    lastEtasUpdatedAt: Instant?,
    onClearStop: () -> Unit,
    onEtaChipClick: (vehicleId: String) -> Unit,
) {
    val stopTitle = selectedStop?.name ?: "Tap a stop to see etas"

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StopEtaHeader(
            title = stopTitle,
            onClearStop = onClearStop,
            stopSelected = selectedStop,
            lastEtasUpdatedAt = lastEtasUpdatedAt,
        )

        if (selectedStopKey == null) return@Column

        val lastSeenStopIndexByVehicle = remember { mutableStateMapOf<String, Int>() }

        LaunchedEffect(vehicles, routes) {
            vehicles.forEach { vehicle ->
                val routeKey = vehicle.routeName ?: return@forEach
                val route = routes[routeKey] ?: return@forEach
                val currentStopName = vehicle.currentStop ?: return@forEach

                val matchedIndex =
                    route.stops.indexOfFirst { stopKey ->
                        route.stopDetails[stopKey]?.name.equals(currentStopName, ignoreCase = true)
                    }

                if (matchedIndex == -1) return@forEach

                val previousIndex = lastSeenStopIndexByVehicle[vehicle.id]
                if (previousIndex == null || matchedIndex > previousIndex) {
                    lastSeenStopIndexByVehicle[vehicle.id] = matchedIndex
                }
            }
        }

        val etas =
            remember(selectedStopKey, vehicles, routes, lastSeenStopIndexByVehicle.toMap()) {
                buildVehicleEtas(
                    stopKey = selectedStopKey,
                    routes = routes,
                    vehicles = vehicles,
                    lastSeenStopIndexByVehicle = lastSeenStopIndexByVehicle,
                )
            }

        val now = Instant.now()

        val visibleEtas =
            etas.filter { Duration.between(now, it.etaInstant).toMinutes() >= -5 }

        if (visibleEtas.isEmpty()) {
            Text(
                text = "No ETAs found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(
                    items = visibleEtas,
                    key = { it.vehicleId },
                ) { eta ->
                    EtaChip(
                        eta = eta,
                        now = now,
                        onClick = { onEtaChipClick(eta.vehicleId) },
                    )
                }
            }
        }

        Text(
            text = "Note: ETAs may be off by a few minutes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp),
        )
    }
}

@Composable
private fun StopEtaHeader(
    title: String,
    onClearStop: () -> Unit,
    stopSelected: Stop?,
    lastEtasUpdatedAt: Instant?,
) {
    val updatedText =
        updatedAgoFlow(lastEtasUpdatedAt)
            .collectAsStateWithLifecycle(initialValue = "")
            .value

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )

        if (stopSelected != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (updatedText.isNotBlank()) {
                    Text(
                        text = updatedText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .size(28.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(50),
                            ).clickable { onClearStop() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕")
                }
            }
        }
    }
}

@Composable
private fun EtaChip(
    eta: VehicleEta,
    now: Instant,
    onClick: () -> Unit,
) {
    val mins = Duration.between(now, eta.etaInstant).toMinutes()
    val etaText =
        when {
            mins <= 0 -> "${mins}m"
            else -> "${mins}m"
        }

    Text(
        text = "Shuttle ${eta.vehicleLabel} • $etaText",
        style = MaterialTheme.typography.bodyMedium,
        modifier =
            Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(999.dp),
                ).clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

// Data

private data class VehicleEta(
    val vehicleId: String,
    val vehicleLabel: String,
    val etaInstant: Instant,
)

private fun buildVehicleEtas(
    stopKey: String,
    routes: Map<String, Route>,
    vehicles: List<Vehicle>,
    lastSeenStopIndexByVehicle: Map<String, Int>,
): List<VehicleEta> {
    val now = Instant.now()

    return vehicles
        .asSequence()
        .mapNotNull { vehicle ->
            val routeKey = vehicle.routeName
            val route = routeKey?.let(routes::get)

            val rawTime = vehicle.stopTimes[stopKey] ?: return@mapNotNull null
            val etaInstant = rawTime.toInstantOrNull() ?: return@mapNotNull null

            if (route != null) {
                val candidateIndex = route.stops.indexOf(stopKey)
                if (candidateIndex != -1) {
                    val lastSeenIndex = lastSeenStopIndexByVehicle[vehicle.id]

                    // Only show ETAs for stops after the last recorded stop index
                    if (lastSeenIndex != null && candidateIndex <= lastSeenIndex) {
                        return@mapNotNull null
                    }
                }

                // If the bus is currently at the first stop, hide old ETAs
                val firstStopKey = route.stops.firstOrNull()
                val firstStopName = firstStopKey?.let { route.stopDetails[it]?.name }
                val isCurrentlyAtFirstStop =
                    firstStopName != null && vehicle.currentStop == firstStopName

                if (isCurrentlyAtFirstStop && etaInstant.isBefore(now)) {
                    return@mapNotNull null
                }
            }

            VehicleEta(
                vehicleId = vehicle.id,
                vehicleLabel = vehicle.name,
                etaInstant = etaInstant,
            )
        }.sortedBy { it.etaInstant }
        .toList()
}

private fun String.toInstantOrNull(): Instant? = runCatching { OffsetDateTime.parse(trim()).toInstant() }.getOrNull()

fun updatedAgoFlow(lastUpdatedAt: Instant?): Flow<String> =
    flow {
        if (lastUpdatedAt == null) {
            emit("")
            return@flow
        }

        while (true) {
            val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
            val duration = Duration.between(lastUpdatedAt, now)

            val secs = duration.seconds.coerceAtLeast(0)
            val text =
                when {
                    secs < 5 -> ""
                    secs < 60 -> "Updated ${secs}s ago"
                    else -> "Updated ${secs / 60}m ago"
                }

            emit(text)
            delay(1000)
        }
    }
