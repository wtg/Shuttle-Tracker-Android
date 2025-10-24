package edu.rpi.shuttletracker.ui.schedule

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
) {
    val scheduleUiState = viewModel.scheduleUiState.collectAsStateWithLifecycle().value

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
    val dayIndex = remember(selectedDay) { if (days.indexOf(selectedDay) >= 0) days.indexOf(selectedDay) else 0 }

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
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LabeledDropdown(
                label = "Weekday",
                items = days,
                selectedItem = selectedDay,
                onItemSelected = { selectedDay = it },
            )

            LabeledDropdown(
                label = "Loop",
                items = routeDropdownItems,
                selectedItem = selectedRoute,
                onItemSelected = { selectedRoute = it },
            )

            LabeledDropdown(
                label = "Stop",
                items = stopDropdownItems,
                selectedItem = selectedStop,
                onItemSelected = { selectedStop = it },
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .border(width = 1.dp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        .padding(8.dp),
            ) {
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
            fontWeight = FontWeight.Bold,
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

        // Auto-scroll to user time
        LaunchedEffect(rowTimes, isToday, selectedStop) {
            if (!isToday || rowTimes.isEmpty()) return@LaunchedEffect
            val now = LocalTime.now()
            val firstUpcomingIndex = rowTimes.indexOfFirst { !it.isBefore(now) }
            val scrollToIndex = if (firstUpcomingIndex >= 0) firstUpcomingIndex else rowTimes.lastIndex
            if (scrollToIndex >= 0) listState.scrollToItem(scrollToIndex)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
        ) {
            items(rowDisplay) { line ->
                val isOutdented =
                    selectedStop == "All Stops" &&
                        line.contains("Student Union") &&
                        !line.contains("(Return)")

                Text(
                    text = line,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = if (isOutdented) 0.dp else 16.dp,
                                bottom = 4.dp,
                            ),
                )
            }
        }
    }
}

private fun buildRouteMetaFromStops(stops: List<Stop>): Map<String, RouteStops> {
    val byRoute = stops.groupBy { it.route }
    return byRoute.mapValues { (_, routeStops) ->
        RouteStops(stops = routeStops)
    }
}
