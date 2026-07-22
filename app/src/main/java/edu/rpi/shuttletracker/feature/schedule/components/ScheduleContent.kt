package edu.rpi.shuttletracker.feature.schedule.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.DayOfWeek
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.feature.schedule.utils.StopTimeInfo
import edu.rpi.shuttletracker.feature.schedule.utils.consolidatedTimes
import edu.rpi.shuttletracker.feature.schedule.utils.routesForDay
import edu.rpi.shuttletracker.feature.schedule.utils.scrollIndexFor
import java.util.Calendar
import kotlin.text.lowercase

/**
 * The full schedule picker + times list, filling whatever container hosts it. Has no
 * Scaffold/TopAppBar of its own so callers control that chrome.
 * */
@Composable
fun ScheduleContent(
    schedule: Schedule?,
    isLoading: Boolean,
    routesByName: Map<String, Route>,
    selectedRoute: String?,
    onSelectedRouteChange: (String) -> Unit,
    showTitle: Boolean = true,
    isWideLayout: Boolean = false,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScheduleHeader(showTitle = showTitle)

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = DividerDefaults.Thickness,
        )

        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            schedule == null -> EmptyState(R.string.no_schedule_found)
            else ->
                ScheduleDetailsContent(
                    schedule = schedule,
                    routesByName = routesByName,
                    selectedRoute = selectedRoute,
                    onSelectedRouteChange = onSelectedRouteChange,
                    isWideLayout = isWideLayout,
                )
        }
    }
}

@Composable
private fun ScheduleHeader(showTitle: Boolean) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showTitle) {
            Text(
                text = stringResource(R.string.schedule_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

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
    selectedRoute: String?,
    onSelectedRouteChange: (String) -> Unit,
    isWideLayout: Boolean,
) {
    var selectedDay by remember { mutableStateOf(DayOfWeek.fromToday()) }

    val routes =
        remember(selectedDay, schedule) {
            routesForDay(selectedDay, schedule)
        }

    val activeRoute =
        when {
            selectedRoute in routes -> selectedRoute
            routes.isNotEmpty() -> routes.first()
            else -> null
        }

    if (isWideLayout) {
        // Wide enough that the two selectors don't need to compete for the same row - saves the
        // vertical space the stacked layout would otherwise spend on a second row.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DaySelector(
                selectedDay = selectedDay,
                onSelect = { selectedDay = it },
                modifier = Modifier.weight(1f),
            )

            if (routes.isNotEmpty()) {
                RouteSelector(
                    routes = routes,
                    selectedRoute = activeRoute,
                    onSelect = onSelectedRouteChange,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                )
            }
        }
    } else {
        DaySelector(
            selectedDay = selectedDay,
            onSelect = { selectedDay = it },
        )
    }

    if (routes.isEmpty()) {
        EmptyState(R.string.schedule_none_running)
        return
    }

    if (!isWideLayout) {
        RouteSelector(
            routes = routes,
            selectedRoute = activeRoute,
            onSelect = onSelectedRouteChange,
        )
    }

    HorizontalDivider(Modifier, DividerDefaults.Thickness)

    val times =
        remember(selectedDay, activeRoute, schedule, routesByName) {
            val routeName = activeRoute ?: return@remember emptyList()
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

    var expandedRowIndex by remember(selectedDay, activeRoute) {
        mutableStateOf<Int?>(null)
    }

    val listState = rememberLazyListState()

    val now = Calendar.getInstance()
    val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

    val scrollIndex = remember(times) { scrollIndexFor(times, nowMinutes) }

    LaunchedEffect(times, scrollIndex) {
        if (times.isNotEmpty()) {
            expandedRowIndex = scrollIndex
            listState.scrollToItem(scrollIndex)
        } else {
            expandedRowIndex = null
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        itemsIndexed(
            items = times,
            key = { index, item ->
                "$index|${item.vehicleName}|${item.departureTime}|${item.routeName}"
            },
        ) { index, item ->
            ScheduleTimeRow(
                time = item.departureTime,
                vehicleName = item.vehicleName,
                expanded = expandedRowIndex == index,
                stopTimes = item.stopTimes,
                onToggleExpanded = {
                    expandedRowIndex = if (expandedRowIndex == index) null else index
                },
            )
        }
    }
}

@Composable
private fun DaySelector(
    selectedDay: DayOfWeek,
    onSelect: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val days = DayOfWeek.entries

    SingleChoiceSegmentedButtonRow(
        modifier =
            modifier
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
    modifier: Modifier = Modifier.fillMaxWidth(0.9f),
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        routes.forEach { dir ->
            RouteTab(
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
    route: String,
    selectedRoute: String?,
    onRouteSelected: (String) -> Unit,
    modifier: Modifier,
) {
    val selected = route == selectedRoute

    val selectedColor =
        when {
            "north" in route.lowercase() -> Color(0xFFD32F2F)
            "west" in route.lowercase() -> Color(0xFF1976D2)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(
        onClick = { onRouteSelected(route) },
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (selected) 2.dp else 0.dp,
        color =
            if (selected) {
                selectedColor.copy(alpha = 0.30f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        modifier = modifier,
    ) {
        Text(
            text =
                stringResource(
                    R.string.route_label_format,
                    route.lowercase().replaceFirstChar { it.titlecase() },
                ),
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
    val expandedDescription = stringResource(R.string.schedule_row_expanded)
    val collapsedDescription = stringResource(R.string.schedule_row_collapsed)
    val tagColor =
        when {
            "north" in vehicleName.lowercase() -> Color(0xFFD32F2F)
            "west" in vehicleName.lowercase() -> Color(0xFF1976D2)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.schedule_toggle_row),
                        onClick = onToggleExpanded,
                    ).semantics {
                        stateDescription =
                            if (expanded) {
                                expandedDescription
                            } else {
                                collapsedDescription
                            }
                    }.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )

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

            Icon(
                painter =
                    if (expanded) {
                        painterResource(R.drawable.ic_keyboard_arrow_down)
                    } else {
                        painterResource(R.drawable.ic_keyboard_arrow_right)
                    },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        if (expanded) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter =
                expandVertically(
                    animationSpec = tween(durationMillis = 220),
                ) +
                    fadeIn(
                        animationSpec = tween(durationMillis = 180),
                    ),
            exit =
                shrinkVertically(
                    animationSpec = tween(durationMillis = 200),
                ) +
                    fadeOut(
                        animationSpec = tween(durationMillis = 150),
                    ),
        ) {
            if (stopTimes.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_stop_times),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    stopTimes.forEach { stopTime ->
                        StopTimeItem(
                            stopName = stopTime.stopName,
                            time = stopTime.time,
                            accentColor = tagColor,
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
        )
    }
}

@Composable
private fun StopTimeItem(
    stopName: String,
    time: String,
    accentColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .width(2.dp)
                    .height(28.dp)
                    .background(accentColor, RoundedCornerShape(999.dp)),
        )

        Text(
            text = stopName,
            style = MaterialTheme.typography.bodyMedium,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
        )

        Text(
            text = time,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp),
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
