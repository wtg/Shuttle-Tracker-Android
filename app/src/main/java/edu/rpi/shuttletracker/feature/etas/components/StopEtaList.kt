package edu.rpi.shuttletracker.feature.etas.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Vehicle
import edu.rpi.shuttletracker.feature.etas.utils.ETA_VISIBLE_ROUTES
import edu.rpi.shuttletracker.feature.etas.utils.StopWithEtas
import edu.rpi.shuttletracker.feature.etas.utils.buildStopsWithEtas
import edu.rpi.shuttletracker.feature.etas.utils.etaMinutesFromNow

/**
 * The route picker + list of stops, each showing a preview of its soonest live etas. Has no
 * Scaffold/TopAppBar of its own so the caller controls that chrome.
 * */
@Composable
fun StopEtaList(
    routes: Map<String, Route>,
    vehicles: List<Vehicle>,
    routesLoaded: Boolean,
    selectedRouteFilter: String?,
    onRouteFilterChange: (String?) -> Unit,
    onStopClick: (String) -> Unit,
    showTitle: Boolean = true,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EtasHeader(showTitle = showTitle)

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = DividerDefaults.Thickness,
        )

        if (!routesLoaded) {
            CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            return
        }

        if (routes.isEmpty()) {
            EmptyState(R.string.etas_no_routes)
            return
        }

        RouteFilterRow(
            routeNames = routes.keys.filter { it in ETA_VISIBLE_ROUTES }.sorted(),
            selectedRoute = selectedRouteFilter,
            onSelect = onRouteFilterChange,
        )

        HorizontalDivider(Modifier, DividerDefaults.Thickness)

        val stops =
            remember(routes, vehicles, selectedRouteFilter) {
                buildStopsWithEtas(routes, vehicles, selectedRouteFilter)
            }

        if (stops.isEmpty()) {
            EmptyState(R.string.etas_no_stops)
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(stops, key = { it.stopKey }) { stop ->
                StopEtaRow(stop = stop, onClick = { onStopClick(stop.stopKey) })
            }
        }
    }
}

@Composable
private fun EtasHeader(showTitle: Boolean) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showTitle) {
            Text(
                text = stringResource(R.string.nav_etas),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        Text(
            text = stringResource(R.string.etas_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RouteFilterRow(
    routeNames: List<String>,
    selectedRoute: String?,
    onSelect: (String?) -> Unit,
) {
    val scrollState = rememberScrollState()
    val options = listOf<String?>(null) + routeNames

    SingleChoiceSegmentedButtonRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .horizontalScroll(scrollState),
    ) {
        options.forEachIndexed { index, routeName ->
            SegmentedButton(
                selected = selectedRoute == routeName,
                onClick = { onSelect(routeName) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        routeName?.let {
                            it.lowercase().replaceFirstChar { char -> char.titlecase() }
                        } ?: stringResource(R.string.etas_route_all),
                    )
                },
            )
        }
    }
}

@Composable
private fun StopEtaRow(
    stop: StopWithEtas,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stop.stop.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )

            Icon(
                painter = painterResource(R.drawable.ic_keyboard_arrow_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (stop.etas.isEmpty()) {
            Text(
                text = stringResource(R.string.etas_no_live_etas),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 6.dp),
            ) {
                stop.etas.take(3).forEach { eta ->
                    EtaChip(routeName = eta.routeName, minutes = etaMinutesFromNow(eta.etaInstant))
                }
            }
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
    )
}

@Composable
fun EtaChip(
    routeName: String?,
    minutes: Long,
) {
    val tagColor = routeAccentColor(routeName)

    Surface(
        color = tagColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = etaLabelText(minutes),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = tagColor,
        )
    }
}

@Composable
fun etaLabelText(minutes: Long): String =
    if (minutes <= 0) {
        stringResource(R.string.eta_now)
    } else {
        stringResource(R.string.eta_minutes_format, minutes)
    }

@Composable
fun routeAccentColor(routeName: String?): Color =
    when {
        routeName == null -> MaterialTheme.colorScheme.onSurfaceVariant
        "north" in routeName.lowercase() -> Color(0xFFD32F2F)
        "west" in routeName.lowercase() -> Color(0xFF1976D2)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
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
