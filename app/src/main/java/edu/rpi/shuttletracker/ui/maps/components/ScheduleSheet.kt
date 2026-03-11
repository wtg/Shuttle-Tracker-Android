package edu.rpi.shuttletracker.ui.maps.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.DayOfWeek
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.ui.maps.utils.StopTimeInfo
import edu.rpi.shuttletracker.ui.maps.utils.consolidatedTimes
import edu.rpi.shuttletracker.ui.maps.utils.routesForDay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSheet(
    show: Boolean,
    sheetState: SheetState,
    schedule: Schedule?,
    routesByName: Map<String, Route>,
    onDismiss: () -> Unit,
) {
    if (!show) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        ScheduleSheetContent(
            schedule = schedule,
            routesByName = routesByName,
        )
    }
}

@Composable
private fun ScheduleSheetContent(
    schedule: Schedule?,
    routesByName: Map<String, Route>,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(.86f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScheduleHeader()

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = DividerDefaults.Thickness,
        )

        when (schedule) {
            null -> EmptyState(R.string.no_schedule_found)
            else -> ScheduleDetailsContent(schedule = schedule, routesByName = routesByName)
        }
    }
}

@Composable
private fun ScheduleHeader() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.schedule_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        Text(
            text = stringResource(R.string.schedule_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ScheduleDetailsContent(
    schedule: Schedule,
    routesByName: Map<String, Route>,
) {
    var selectedDay by remember { mutableStateOf(DayOfWeek.fromToday()) }

    val routes =
        remember(selectedDay, schedule) {
            routesForDay(selectedDay, schedule)
        }

    var selectedRoute by remember(selectedDay, routes) {
        mutableStateOf(routes.firstOrNull())
    }

    DaySelector(
        selectedDay = selectedDay,
        onSelect = { selectedDay = it },
    )

    if (routes.isEmpty()) {
        EmptyState(R.string.schedule_none_running)
        return
    }

    RouteSelector(
        routes = routes,
        selectedRoute = selectedRoute,
        onSelect = { selectedRoute = it },
    )

    HorizontalDivider(Modifier, DividerDefaults.Thickness)

    val times =
        remember(selectedDay, selectedRoute, schedule, routesByName) {
            val routeName = selectedRoute ?: return@remember emptyList()
            consolidatedTimes(
                routeName = routeName,
                day = selectedDay,
                schedule = schedule,
                routesByName = routesByName,
            )
        }

    if (times.isEmpty()) {
        EmptyState(R.string.no_schedule_found)
        return
    }

    var expandedRowKey by remember(selectedDay, selectedRoute) {
        mutableStateOf<String?>(null)
    }

    val listState = rememberLazyListState()

    val now = Calendar.getInstance()
    val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

    val scrollIndex =
        remember(times) {
            times.indexOfFirst { it.minutesOfDay >= nowMinutes }.let { index ->
                if (index == -1) 0 else index
            }
        }

    LaunchedEffect(times, scrollIndex) {
        if (times.isNotEmpty()) {
            listState.scrollToItem(scrollIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(times, key = { it.vehicleName + it.departureTime + it.routeName }) { item ->
            val rowKey = item.vehicleName + item.departureTime + item.routeName

            ScheduleTimeRow(
                time = item.departureTime,
                vehicleName = item.vehicleName,
                expanded = expandedRowKey == rowKey,
                stopTimes = item.stopTimes,
                onToggleExpanded = {
                    expandedRowKey = if (expandedRowKey == rowKey) null else rowKey
                },
            )
        }
    }
}

@Composable
private fun DaySelector(
    selectedDay: DayOfWeek,
    onSelect: (DayOfWeek) -> Unit,
) {
    val scrollState = rememberScrollState()
    val days = DayOfWeek.entries

    SingleChoiceSegmentedButtonRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .horizontalScroll(scrollState),
    ) {
        days.forEachIndexed { index, day ->
            SegmentedButton(
                selected = selectedDay == day,
                onClick = { onSelect(day) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = days.size),
                label = { Text(day.displayName) },
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
private fun ScheduleTimeRow(
    time: String,
    vehicleName: String,
    expanded: Boolean,
    stopTimes: List<StopTimeInfo>,
    onToggleExpanded: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )

            val tagColor = vehicleTagColor(vehicleName, MaterialTheme.colorScheme.onSurfaceVariant)

            Surface(
                color = tagColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = vehicleName,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = tagColor,
                )
            }

            Text(
                text = if (expanded) "∨" else ">",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        if (expanded) {
            if (stopTimes.isEmpty()) {
                Text(
                    text = "No stop times available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
            } else {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    stopTimes.forEach { stopTime ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stopTime.stopName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )

                            Text(
                                text = stopTime.time,
                                style = MaterialTheme.typography.bodyMedium,
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

private fun vehicleTagColor(
    vehicleName: String,
    defaultColor: Color,
): Color {
    val n = vehicleName.lowercase()
    return when {
        "north" in n -> Color(0xFFD32F2F)
        "west" in n -> Color(0xFF1976D2)
        else -> defaultColor
    }
}
