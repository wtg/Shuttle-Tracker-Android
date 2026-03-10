package edu.rpi.shuttletracker.ui.maps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.models.Vehicle
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

@Composable
fun StopSheetContent(
    routes: Map<String, Route>,
    vehicles: List<Vehicle>,
    showDetails: Boolean,
    onStopClick: (stopKey: String, stop: Stop) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(.86f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.bottom_sheet_peek_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        Text(
            text = stringResource(R.string.bottom_sheet_peek_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        if (!showDetails) return@Column

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = DividerDefaults.Thickness,
        )

        if (routes.isEmpty()) {
            EmptyState(R.string.no_schedule_found)
            return@Column
        }

        StopsDetailsContent(
            routes = routes,
            vehicles = vehicles,
            onStopClick = onStopClick,
        )
    }
}

@Composable
private fun StopsDetailsContent(
    routes: Map<String, Route>,
    vehicles: List<Vehicle>,
    onStopClick: (stopKey: String, stop: Stop) -> Unit,
) {
    val allowedRoutes = setOf("NORTH", "WEST")

    val routeKeys =
        remember(routes) {
            routes.keys
                .filter { it in allowedRoutes }
                .sorted()
        }

    var selectedRouteKey by remember(routeKeys) {
        mutableStateOf(routeKeys.firstOrNull())
    }

    if (routeKeys.isEmpty() || selectedRouteKey == null) {
        EmptyState(R.string.no_schedule_found)
        return
    }

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
            val lastRouteIndex = route.stops.lastIndex

            when {
                previousIndex == null -> {
                    lastSeenStopIndexByVehicle[vehicle.id] = matchedIndex
                }

                matchedIndex > previousIndex -> {
                    lastSeenStopIndexByVehicle[vehicle.id] = matchedIndex
                }

                previousIndex >= lastRouteIndex - 1 && matchedIndex <= 1 -> {
                    lastSeenStopIndexByVehicle[vehicle.id] = matchedIndex
                }
            }
        }
    }

    RouteSelector(
        routes = routeKeys,
        selectedRoute = selectedRouteKey,
        onSelect = { selectedRouteKey = it },
    )

    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = DividerDefaults.Thickness,
    )

    val stopRows =
        remember(selectedRouteKey, routes, vehicles, lastSeenStopIndexByVehicle.toMap()) {
            val route = routes[selectedRouteKey] ?: return@remember emptyList()
            buildStopRowsForRoute(
                route = route,
                vehicles = vehicles,
                routeKey = selectedRouteKey!!,
                lastSeenStopIndexByVehicle = lastSeenStopIndexByVehicle,
            )
        }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(
            items = stopRows,
            key = { it.stopKey },
        ) { row ->
            StopEtaRow(
                stopName = row.stop.name,
                etaLabels = row.etaLabels,
                onClick = { onStopClick(row.stopKey, row.stop) },
            )
        }
    }
}

@Composable
private fun RouteSelector(
    routes: List<String>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        routes.forEach { dir ->
            RouteTab(
                label =
                    stringResource(
                        R.string.route_label_format,
                        dir.lowercase().replaceFirstChar { it.titlecase() },
                    ),
                route = dir,
                selectedRoute = selectedRoute,
                onRouteSelected = onSelect,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun RouteTab(
    label: String,
    route: String,
    selectedRoute: String?,
    onRouteSelected: (String) -> Unit,
    modifier: Modifier,
) {
    val selected = route == selectedRoute

    Surface(
        onClick = { onRouteSelected(route) },
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (selected) 2.dp else 0.dp,
        color =
            if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        modifier = modifier,
    ) {
        Text(
            text = label,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

// List content

@Composable
private fun StopEtaRow(
    stopName: String,
    etaLabels: List<String>,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stopName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (etaLabels.isEmpty()) {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    etaLabels.forEach { label ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
            thickness = 0.5.dp,
        )
    }
}

@Composable
private fun EmptyState(textRes: Int) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(textRes))
    }
}

private data class StopRow(
    val stopKey: String,
    val stop: Stop,
    val etaLabels: List<String>,
)

private fun buildStopRowsForRoute(
    route: Route,
    vehicles: List<Vehicle>,
    routeKey: String,
    lastSeenStopIndexByVehicle: Map<String, Int>,
): List<StopRow> {
    val now = Instant.now()

    return route.stopDetails.map { (stopKey, stop) ->
        val candidateIndex = route.stops.indexOf(stopKey)

        val nextMins =
            vehicles
                .asSequence()
                .filter { it.routeName == routeKey }
                .mapNotNull { vehicle ->
                    if (candidateIndex != -1) {
                        val lastSeenIndex = lastSeenStopIndexByVehicle[vehicle.id]
                        if (lastSeenIndex != null && candidateIndex <= lastSeenIndex) {
                            return@mapNotNull null
                        }
                    }

                    val rawEta = vehicle.stopTimes[stopKey] ?: return@mapNotNull null
                    val etaInstant = rawEta.toInstantOrNull() ?: return@mapNotNull null

                    val firstStopKey = route.stops.firstOrNull()
                    val firstStopName = firstStopKey?.let { route.stopDetails[it]?.name }
                    val isCurrentlyAtFirstStop =
                        firstStopName != null &&
                            vehicle.currentStop.equals(firstStopName, ignoreCase = true)

                    if (isCurrentlyAtFirstStop && etaInstant.isBefore(now)) {
                        return@mapNotNull null
                    }

                    Duration.between(now, etaInstant).toMinutes()
                }.filter { it >= -1 }
                .sorted()
                .take(2)
                .toList()

        val labels =
            nextMins.map { mins ->
                when {
                    mins <= 0 -> "now"
                    else -> "${mins}m"
                }
            }

        StopRow(
            stopKey = stopKey,
            stop = stop,
            etaLabels = labels,
        )
    }
}

private fun String.toInstantOrNull(): Instant? = runCatching { OffsetDateTime.parse(trim()).toInstant() }.getOrNull()
