package edu.rpi.shuttletracker.ui.schedule

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Route
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
    val routes = scheduleUiState.routes

    // Checks if height < 480 dp
    val useHorizontalLayout =
        !windowSizeClass
            .isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)

    val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val allowedRoutes = setOf("NORTH", "WEST")

    // Route dropdown values
    val routeDropdownItems = remember(routes) { allowedRoutes.toList() }
    var selectedRoute by remember(routeDropdownItems) { mutableStateOf(routeDropdownItems.first()) }
    if (selectedRoute !in routeDropdownItems) selectedRoute = routeDropdownItems.first()

    val selectedStop = "All Stops"

    // Weekday dropdown values
    val todayName = remember { days[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1] }
    var selectedDay by remember { mutableStateOf(todayName) }
    val dayIndex = remember(selectedDay) { days.indexOf(selectedDay).takeIf { it >= 0 } ?: 0 }

    // Gets schedule base times for the selected day and route (north/west)
    val selectedRouteTimes: List<String> =
        remember(selectedDay, selectedRoute, scheduleUiState.schedule) {
            val routeSchedule = scheduleUiState.schedule.getOrNull(dayIndex)
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
                    )
                }

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
                        routeData = routes[selectedRoute],
                    )
                }
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
                        routeData = routes[selectedRoute],
                    )
                }
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
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            routeItems.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = selectedRoute == option,
                    onClick = { onRoute(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, routeItems.size),
                    label = { Text(option) },
                )
            }
        }
        LabeledDropdown(
            label = "Weekday",
            items = days,
            selectedItem = selectedDay,
            onItemSelected = onDay,
        )
    }
}

@Composable
private fun ScheduleScroll(
    selectedRouteTimes: List<String>,
    selectedStop: String,
    routeData: Route?,
    centered: Boolean = false,
) {
    val listState = rememberLazyListState()
    val stops = routeData?.stopDetails?.values?.toList() ?: emptyList()

    val columnModifier = if (centered) Modifier.fillMaxSize() else Modifier
    val columnHorizontal = if (centered) Alignment.CenterHorizontally else Alignment.Start
    val columnVertical = if (centered) Arrangement.Center else Arrangement.Top
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start

    val reliableStops = listOf("All Stops", "Student Union")

    Column(
        modifier = columnModifier,
        horizontalAlignment = columnHorizontal,
        verticalArrangement = columnVertical,
    ) {
        Text(
            text =
                if (selectedStop in reliableStops) {
                    stringResource(R.string.time_estimated_reliable)
                } else {
                    stringResource(R.string.time_estimated_unreliable)
                },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )

        if (selectedRouteTimes.isEmpty() || stops.isEmpty()) {
            Text(
                text = "Loading...",
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            )
            return
        }

        val formatterIn = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()) }
        val formatterOut = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }

        fun parseTime(timeStr: String): LocalTime = LocalTime.parse(timeStr.uppercase(Locale.getDefault()), formatterIn)

        val rowTimes =
            remember(selectedRouteTimes, selectedStop, stops) {
                if (selectedStop == "All Stops") {
                    selectedRouteTimes.map { baseStr ->
                        parseTime(baseStr)
                    }
                } else {
                    val offset = stops.find { it.name == selectedStop }?.offset ?: 0
                    selectedRouteTimes.map { baseStr -> parseTime(baseStr).plusMinutes(offset.toLong()) }
                }
            }

        val rowDisplay =
            remember(rowTimes, selectedStop) {
                rowTimes.map { time -> time.format(formatterOut) }
            }

        // Auto-scroll to user time
        LaunchedEffect(rowTimes, selectedStop) {
            if (rowTimes.isEmpty()) return@LaunchedEffect
            val now = LocalTime.now()
            val firstUpcomingIndex = rowTimes.indexOfFirst { !it.isBefore(now) }
            val scrollToIndex = if (firstUpcomingIndex >= 0) firstUpcomingIndex else rowTimes.lastIndex
            if (scrollToIndex >= 0) listState.scrollToItem(scrollToIndex)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(rowDisplay) { line ->
                if (selectedStop == "All Stops") {
                    Text(
                        text = "$line Student Union",
                        textAlign = textAlign,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 0.dp,
                                    bottom = 4.dp,
                                ),
                    )
                    stops
                        .filter { it.name != "Student Union" }
                        .forEach { stop ->
                            Text(
                                text = stop.name,
                                textAlign = textAlign,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = if (!centered) 16.dp else 0.dp,
                                            bottom = 2.dp,
                                        ),
                            )
                        }
                } else {
                    Text(
                        text = line,
                        textAlign = textAlign,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    bottom = 4.dp,
                                ),
                    )
                }
            }
        }
    }
}
