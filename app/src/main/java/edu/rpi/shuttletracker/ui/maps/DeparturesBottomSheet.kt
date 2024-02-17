package edu.rpi.shuttletracker.ui.maps

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.rpi.shuttletracker.data.models.Departure
import edu.rpi.shuttletracker.data.models.Stop
import java.time.LocalDateTime
import java.util.Calendar

private val daysOfWeek =
    listOf(
        "Sunday" to "U",
        "Monday" to "M",
        "Tuesday" to "T",
        "Wednesday" to "W",
        "Thursday" to "R",
        "Friday" to "F",
        "Saturday" to "S",
    )

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DeparturesBottomSheet(
    stop: Stop?,
    changeStopLoaded: (Stop?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    if (stop != null) {
        ModalBottomSheet(
            onDismissRequest = { changeStopLoaded(null) },
            sheetState = sheetState,
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .padding(horizontal = 10.dp)
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        text = stop.name,
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                    )
                }

                item { Divider() }

                // TimePicker(state = timeState)
                item {
                    Text(text = "Departures")

                    Text(
                        text =
                            "Departures let you schedule notifications to let you know" +
                                " what buses are approaching a stop. " +
                                "Press the \"+\" to schedule one!",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                item { Divider() }
            }

            Row(
                modifier =
                    Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(text = "New departure:", style = MaterialTheme.typography.labelSmall)

                    TimeDateText(stop)
                }

                Button(
                    modifier = Modifier.align(Alignment.Bottom),
                    onClick = { /*TODO*/ },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "New Departure",
                    )
                    Text(text = "Add")
                }
            }
        }
    }
}

@Composable
fun TimeDateText(departure: Departure) {
    var timeChosen by remember { mutableStateOf(departure.getReadableTime()) }

    LaunchedEffect(departure.time) {
        timeChosen = departure.getReadableTime()
    }

    val days =
        departure.days.joinToString(", ") {
            return@joinToString daysOfWeek[it - 1].second
        }

    var showTimePickerDialog by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier.clickable {
                showTimePickerDialog = true
            },
    ) {
        Text(
            text = timeChosen,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = "Days: $days", style = MaterialTheme.typography.bodyLarge)
    }

    if (showTimePickerDialog) {
        TimeDateDialog(
            departure = departure,
            onDismiss = { showTimePickerDialog = it },
            onSave = { hour, minute, daysSelected ->
                departure.setTime(hour, minute)
                departure.days =
                    daysSelected.mapIndexedNotNull { index, selected ->
                        if (selected) return@mapIndexedNotNull index + 1
                        null
                    }
            },
        )
    }
}

@Composable
fun TimeDateText(stop: Stop) {
    val calendar: Calendar = Calendar.getInstance()
    val day: Int = calendar.get(Calendar.DAY_OF_WEEK)
    val departure = Departure(stop.name, listOf(day))
    TimeDateText(departure = departure)
}

/**
 * @param departure the initial departure time you want to modify
 * @param onDismiss callback when dialog is closed
 * @param onSave callback when save button is pressed. It gets called with the format: hour, minute
 * */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimeDateDialog(
    departure: Departure,
    onDismiss: (Boolean) -> Unit,
    onSave: (Int, Int, List<Boolean>) -> Unit,
) {
    val daysSelected = remember { mutableStateListOf(false, false, false, false, false, false, false) }

    LaunchedEffect(true) {
        departure.days.forEach { date ->
            daysSelected[date - 1] = true
        }
    }

    val timePickerState =
        rememberTimePickerState(
            initialHour = LocalDateTime.parse(departure.time).hour,
            initialMinute = LocalDateTime.parse(departure.time).minute,
        )

    AlertDialog(
        onDismissRequest = { onDismiss(false) },
        confirmButton = {
            Button(onClick = {
                onSave(timePickerState.hour, timePickerState.minute, daysSelected)
                onDismiss(false)
            }) {
                Text(text = "Save")
            }
        },
        title = { Text(text = "Set a departure time") },
        text = {
            Column {
                TimePicker(state = timePickerState)

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    repeat(7) {
                        FilterChip(
                            selected = daysSelected[it],
                            onClick = { daysSelected[it] = !daysSelected[it] },
                            label = { Text(text = daysOfWeek[it].first) },
                        )
                    }
                }
            }
        },
    )
}
