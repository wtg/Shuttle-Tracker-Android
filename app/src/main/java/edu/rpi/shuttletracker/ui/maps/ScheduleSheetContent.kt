package edu.rpi.shuttletracker.ui.maps

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.DayOfWeek
import edu.rpi.shuttletracker.data.models.Schedule
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

// Header / Peak

@Composable
fun ScheduleSheetContent(
    schedule: Schedule?,
    showDetails: Boolean,
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

        if (showDetails) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = DividerDefaults.Thickness,
            )

            if (schedule == null) {
                EmptyState(R.string.schedule_no_upcoming_today)
            } else {
                ScheduleDetailsContent(schedule = schedule)
            }
        }
    }
}

@Composable
private fun ScheduleDetailsContent(schedule: Schedule) {
    var selectedDay by remember { mutableStateOf(DayOfWeek.fromToday()) }
    var selectedDirection by remember { mutableStateOf<String?>(null) }

    val directions =
        remember(selectedDay, schedule) {
            directionsForDay(selectedDay, schedule)
        }

    LaunchedEffect(selectedDay, directions) {
        selectedDirection =
            when {
                directions.isEmpty() -> null
                selectedDirection in directions -> selectedDirection
                else -> directions.first()
            }
    }

    DaySelector(
        selectedDay = selectedDay,
        onSelect = { selectedDay = it },
    )

    if (directions.isEmpty()) {
        EmptyState(R.string.schedule_none_running)
        return
    }

    RouteSelector(
        directions = directions,
        selectedDirection = selectedDirection,
        onSelect = { selectedDirection = it },
    )

    HorizontalDivider(Modifier, DividerDefaults.Thickness)

    val times =
        remember(selectedDay, selectedDirection, schedule) {
            val dir = selectedDirection ?: return@remember emptyList()
            consolidatedTimes(dir, selectedDay, schedule)
        }

    ScheduleBody(
        selectedDirection = selectedDirection,
        times = times,
    )
}

// Selectors

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
    directions: List<String>,
    selectedDirection: String?,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        directions.forEach { dir ->
            RouteTab(
                label =
                    stringResource(
                        R.string.route_label_format,
                        dir
                            .lowercase()
                            .replaceFirstChar { it.titlecase() },
                    ),
                route = dir,
                selectedRoute = selectedDirection,
                onRouteSelected = onSelect,
                modifier = Modifier.weight(1f).padding(bottom = 8.dp),
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
private fun ScheduleBody(
    selectedDirection: String?,
    times: List<TimeInfo>,
) {
    when {
        selectedDirection == null -> EmptyState(R.string.schedule_select_route)
        times.isEmpty() -> EmptyState(R.string.schedule_no_upcoming_today)
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(times, key = { it.busName + it.time + it.direction }) { item ->
                    ScheduleTimeRow(time = item.time, busName = item.busName)
                }
            }
        }
    }
}

@Composable
private fun ScheduleTimeRow(
    time: String,
    busName: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )

            val tagColor = busTagColor(busName, MaterialTheme.colorScheme.onSurfaceVariant)

            Surface(
                color = tagColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = busName,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = tagColor,
                )
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
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(textRes))
    }
}

private fun busTagColor(
    busName: String,
    defaultColor: Color,
): Color {
    val n = busName.lowercase()
    return when {
        "north" in n -> Color(0xFFD32F2F)
        "west" in n -> Color(0xFF1976D2)
        else -> defaultColor
    }
}

// Data helpers

private fun directionsForDay(
    day: DayOfWeek,
    data: Schedule,
): List<String> {
    val scheduleMap = data.scheduleMapFor(day)
    val dirs = mutableSetOf<String>()

    for ((_, times) in scheduleMap) {
        for (pair in times) {
            if (pair.size > 1) dirs.add(pair[1])
        }
    }
    return dirs.sorted()
}

private data class TimeInfo(
    val time: String,
    val direction: String,
    val busName: String,
    val minutesOfDay: Int,
)

private fun parseMinutesOfDay(timeStr: String): Int? =
    runCatching {
        val t =
            LocalTime.parse(
                timeStr.trim(),
                DateTimeFormatter.ofPattern("h:mm a", Locale.US),
            )
        t.hour * 60 + t.minute
    }.getOrNull()

private fun consolidatedTimes(
    direction: String,
    day: DayOfWeek,
    data: Schedule,
): List<TimeInfo> {
    val scheduleMap = data.scheduleMapFor(day)

    val now = Calendar.getInstance()
    val isToday = DayOfWeek.fromToday() == day
    val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

    val out = mutableListOf<TimeInfo>()

    for ((busName, times) in scheduleMap) {
        for (pair in times) {
            if (pair.size <= 1) continue

            val timeStr = pair[0]
            val dirStr = pair[1]
            if (dirStr != direction) continue

            val minutes = parseMinutesOfDay(timeStr) ?: continue
            if (isToday && minutes < nowMinutes) continue

            out +=
                TimeInfo(
                    time = timeStr,
                    direction = dirStr,
                    busName = busName,
                    minutesOfDay = minutes,
                )
        }
    }

    return out.sortedBy { it.minutesOfDay }
}
