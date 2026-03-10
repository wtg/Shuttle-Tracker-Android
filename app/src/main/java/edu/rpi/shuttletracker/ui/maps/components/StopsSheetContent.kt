package edu.rpi.shuttletracker.ui.maps.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.ui.maps.utils.StopRowUi

@Composable
fun StopSheetContent(
    routeKeys: List<String>,
    selectedRouteKey: String?,
    stopRows: List<StopRowUi>,
    showDetails: Boolean,
    onRouteSelected: (String) -> Unit,
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

        if (routeKeys.isEmpty()) {
            EmptyState(R.string.no_schedule_found)
            return@Column
        }

        StopsDetailsContent(
            routeKeys = routeKeys,
            selectedRouteKey = selectedRouteKey,
            stopRows = stopRows,
            onRouteSelected = onRouteSelected,
            onStopClick = onStopClick,
        )
    }
}

@Composable
private fun StopsDetailsContent(
    routeKeys: List<String>,
    selectedRouteKey: String?,
    stopRows: List<StopRowUi>,
    onRouteSelected: (String) -> Unit,
    onStopClick: (stopKey: String, stop: Stop) -> Unit,
) {
    if (routeKeys.isEmpty() || selectedRouteKey == null) {
        EmptyState(R.string.no_schedule_found)
        return
    }

    RouteSelector(
        routes = routeKeys,
        selectedRoute = selectedRouteKey,
        onSelect = onRouteSelected,
    )

    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = DividerDefaults.Thickness,
    )

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
                onClick = {
                    onStopClick(row.stopKey, row.stop)
                },
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
