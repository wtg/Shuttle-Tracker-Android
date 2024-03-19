package edu.rpi.shuttletracker.ui.maps

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.rpi.shuttletracker.data.models.Departure
import edu.rpi.shuttletracker.data.models.Stop
import java.time.LocalDateTime
import java.util.Calendar

/**
 * gives you a pair of:
 * Long name : Short name
 * */
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeparturesBottomSheet(
    stop: Stop?,
    departures: List<Departure>,
    changeStopLoaded: (Stop?) -> Unit,
    addDeparture: (Departure) -> Unit,
    updateStopDestination: (String) -> Unit,
    deleteDeparture: (Departure) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    if (stop != null) {
        val departureToAdd by remember { mutableStateOf(getDefaultDeparture(stop)) }
        updateStopDestination(stop.name)

        ModalBottomSheet(
            onDismissRequest = { changeStopLoaded(null) },
            sheetState = sheetState,
        ) {
            /**
             * Contains the stop title name
             * and the departures that are scheduled
             * */
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

                item { HorizontalDivider() }

                /**
                 * contains a message on what a departure is if there are no departures for that stop
                 * or else it will show the list of departures
                 * */
                if (departures.isEmpty()) {
                    item {
                        Text(
                            text =
                                "Departures let you schedule notifications to let you know" +
                                    " what buses are approaching a stop. " +
                                    "Press the \"+\" to schedule one!",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                } else {
                    item { Text(text = "Departures:") }

                    items(
                        departures,
                        key = { it.id },
                    ) {
                        TimeDateText(departure = it, addDeparture, deleteDeparture)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 5.dp))

            /**
             * Selection to let you add a new departure
             * */
            Row(
                modifier =
                    Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(text = "New departure:", style = MaterialTheme.typography.labelSmall)

                    TimeDateText(departureToAdd)
                }

                Button(
                    modifier = Modifier.align(Alignment.Bottom),
                    onClick = { addDeparture(departureToAdd) },
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

/**
 * Shows the departure information with time and dates
 * @param departure The departure you are modifying
 * @param departureModified What to do when the departure information is modified and saved
 * This defaults to doing nothing.
 * */
@Composable
fun TimeDateText(
    departure: Departure,
    departureModified: (Departure) -> Unit = {},
    departureDeleted: (Departure) -> Unit = {},
) {
    val context = LocalContext.current
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

                val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmMgr.canScheduleExactAlarms()) {
                    Toast.makeText(context, "Enable alarms for Shuttle Tracker", Toast.LENGTH_LONG).show()
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    return@TimeDateDialog
                }

                departureModified(departure)
            },
            onDelete = {
                departureDeleted(it)
                showTimePickerDialog = false
            },
        )
    }
}

/**
 * @param stop The stop you want the departure to be based on, used for the name
 * @return Give you a departure with current time and day
 * */
fun getDefaultDeparture(stop: Stop): Departure {
    val calendar: Calendar = Calendar.getInstance()
    val day: Int = calendar.get(Calendar.DAY_OF_WEEK)
    return Departure(stop.name, stop.latitude, stop.longitude, listOf(day))
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
    onDelete: (Departure) -> Unit,
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
        dismissButton = {
            TextButton(onClick = { onDelete(departure) }) {
                Text(text = "Delete")
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
