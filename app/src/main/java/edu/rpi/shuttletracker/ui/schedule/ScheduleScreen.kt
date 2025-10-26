package edu.rpi.shuttletracker.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.RouteStops
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.ui.util.CheckResponseError
import edu.rpi.shuttletracker.ui.util.LabeledDropdown
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ScheduleScreen(
    navigator: DestinationsNavigator,
    viewModel: ScheduleViewModel = hiltViewModel(),
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
) {
    val scheduleUiState = viewModel.scheduleUiState.collectAsStateWithLifecycle().value

    // Checks if height < 480 dp
    val useHorizontalLayout =
        !windowSizeClass
            .isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)

    val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val allowedRoutes = setOf("NORTH", "WEST")

    val routeMeta =
        remember(scheduleUiState.stops) {
            buildRouteMetaFromStops(scheduleUiState.stops)
                .filterKeys { it in allowedRoutes }
        }

    // Route dropdown values
    val routeDropdownItems = remember(routeMeta) { routeMeta.keys.ifEmpty { allowedRoutes }.toList() }
    var selectedRoute by remember(routeDropdownItems) { mutableStateOf(routeDropdownItems.first()) }
    if (selectedRoute !in routeDropdownItems) selectedRoute = routeDropdownItems.first()

    // Stop dropdown values
    val stopDropdownItems =
        remember(selectedRoute, routeMeta) {
            listOf("All Stops") + (routeMeta[selectedRoute]?.stops?.map { it.name } ?: emptyList())
        }
    var selectedStop by remember(selectedRoute) { mutableStateOf(stopDropdownItems.first()) }
    if (selectedStop !in stopDropdownItems) selectedStop = "All Stops"

    // Weekday dropdown values
    val todayName = remember { days[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1] }
    var selectedDay by remember { mutableStateOf(todayName) }
    val dayIndex = remember(selectedDay) { days.indexOf(selectedDay).takeIf { it >= 0 } ?: 0 }

    // Base times for the selected day + route (strings like "9:00 AM")
    val selectedRouteTimes: List<String> =
        remember(selectedDay, selectedRoute, scheduleUiState.schedule) {
            val routeSchedule: Schedule? = scheduleUiState.schedule.getOrNull(dayIndex)
            when (selectedRoute) {
                "NORTH" -> routeSchedule?.north ?: emptyList()
                "WEST" -> routeSchedule?.west ?: emptyList()
                else -> emptyList()
            }
        }

    Scaffold(
        snackbarHost = {
            CheckResponseError(
                scheduleUiState.networkError,
                scheduleUiState.serverError,
                scheduleUiState.unknownError,
                ignoreErrorRequest = { viewModel.clearErrors() },
                retryErrorRequest = {
                    viewModel.clearErrors()
                    viewModel.loadAll()
                },
            )
        },
        topBar = {
            TopAppBar(
                title = { Text("Schedule") },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (useHorizontalLayout) {
            Row(
                modifier =
                    Modifier
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier =
                        Modifier
                            .widthIn(min = 260.dp, max = 360.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Controls(
                        days = days,
                        selectedDay = selectedDay,
                        onDay = { selectedDay = it },
                        routeItems = routeDropdownItems,
                        selectedRoute = selectedRoute,
                        onRoute = { selectedRoute = it },
                        stopItems = stopDropdownItems,
                        selectedStop = selectedStop,
                        onStop = { selectedStop = it },
                    )
                }

                ScheduleScroll(
                    selectedRouteTimes = selectedRouteTimes,
                    selectedStop = selectedStop,
                    routeInfo = routeMeta[selectedRoute],
                    isToday = (selectedDay == todayName),
                )
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Controls(
                    days = days,
                    selectedDay = selectedDay,
                    onDay = { selectedDay = it },
                    routeItems = routeDropdownItems,
                    selectedRoute = selectedRoute,
                    onRoute = { selectedRoute = it },
                    stopItems = stopDropdownItems,
                    selectedStop = selectedStop,
                    onStop = { selectedStop = it },
                )

                ScheduleScroll(
                    selectedRouteTimes = selectedRouteTimes,
                    selectedStop = selectedStop,
                    routeInfo = routeMeta[selectedRoute],
                    isToday = (selectedDay == todayName),
                )
            }
        }
    }
}

@Composable
private fun Controls(
    days: List<String>,
    selectedDay: String,
    onDay: (String) -> Unit,
    routeItems: List<String>,
    selectedRoute: String,
    onRoute: (String) -> Unit,
    stopItems: List<String>,
    selectedStop: String,
    onStop: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LabeledDropdown(
            label = "Weekday",
            items = days,
            selectedItem = selectedDay,
            onItemSelected = onDay,
        )
        LabeledDropdown(
            label = "Loop",
            items = routeItems,
            selectedItem = selectedRoute,
            onItemSelected = onRoute,
        )
        LabeledDropdown(
            label = "Stop",
            items = stopItems,
            selectedItem = selectedStop,
            onItemSelected = onStop,
        )
    }
}

@Composable
private fun ScheduleScroll(
    selectedRouteTimes: List<String>,
    selectedStop: String,
    routeInfo: RouteStops?,
    isToday: Boolean,
) {
    val listState = rememberLazyListState()

    Column {
        Text(
            text = stringResource(R.string.time_estimated),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )

        if (selectedRouteTimes.isEmpty() || routeInfo == null) {
            Text(
                text = "Loading...",
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            )
            return
        }

        val formatterIn = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()) }
        val formatterOut = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
        val stops = routeInfo.stops

        fun parseTime(timeStr: String): LocalTime = LocalTime.parse(timeStr.uppercase(Locale.getDefault()), formatterIn)

        val rowTimes: List<LocalTime> =
            remember(selectedRouteTimes, selectedStop, routeInfo) {
                if (selectedStop == "All Stops") {
                    // For each base time, include every stop offset
                    selectedRouteTimes.flatMap { baseStr ->
                        stops.map { stop -> parseTime(baseStr).plusMinutes(stop.offset.toLong()) }
                    }
                } else {
                    val offset = routeInfo.stopByName[selectedStop]?.offset ?: 0
                    selectedRouteTimes.map { baseStr ->
                        parseTime(baseStr).plusMinutes(offset.toLong())
                    }
                }
            }

        val rowDisplay =
            remember(rowTimes, selectedStop) {
                if (selectedStop == "All Stops") {
                    val stopNames = selectedRouteTimes.flatMap { _ -> stops.map { it.name } }
                    rowTimes.mapIndexed { index, time ->
                        val stopName = stopNames[index]
                        "${time.format(formatterOut)} $stopName"
                    }
                } else {
                    rowTimes.map { time -> time.format(formatterOut) }
                }
            }

        val scheduleItems = remember(rowDisplay, selectedStop) {
            if (selectedStop != "All Stops") {
                rowDisplay.map { ScheduleItem.Single(it) }
            } else {
                val items = mutableListOf<ScheduleItem>()
                val groupChildren = mutableListOf<String>()
                var groupTitle: String? = null

                fun flushGroup() {
                    if (groupTitle != null) {
                        items.add(ScheduleItem.Group(groupTitle!!, groupChildren.toList()))
                        groupChildren.clear()
                    }
                }

                for (line in rowDisplay) {
                    val isTitle = line.contains("Student Union") && !line.contains("(Return)")
                    if (isTitle) {
                        flushGroup()
                        groupTitle = line
                    } else {
                        if (groupTitle != null) {
                            groupChildren.add(line)
                        } else {
                            items.add(ScheduleItem.Single(line))
                        }
                    }
                }
                flushGroup()
                items
            }
        }

        // Auto-scroll to user time
        LaunchedEffect(rowTimes, isToday, selectedStop) {
            if (!isToday || rowTimes.isEmpty()) return@LaunchedEffect
            val now = LocalTime.now()
            val firstUpcomingIndex = rowTimes.indexOfFirst { !it.isBefore(now) }
            val scrollToIndex = if (firstUpcomingIndex >= 0) firstUpcomingIndex else rowTimes.lastIndex
            if (scrollToIndex >= 0) listState.scrollToItem(scrollToIndex)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(scheduleItems) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    when (item) {
                        is ScheduleItem.Single -> {
                            Text(
                                text = item.line,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                        is ScheduleItem.Group -> {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                item.children.forEach { child ->
                                    Text(
                                        text = child
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed class ScheduleItem {
    data class Single(val line: String) : ScheduleItem()
    data class Group(val title: String, val children: List<String>) : ScheduleItem()
}

private fun buildRouteMetaFromStops(stops: List<Stop>): Map<String, RouteStops> {
    val byRoute = stops.groupBy { it.route }
    return byRoute.mapValues { (_, routeStops) ->
        RouteStops(stops = routeStops)
    }
}
