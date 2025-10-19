package edu.rpi.shuttletracker.ui.schedule

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import edu.rpi.shuttletracker.ui.util.LabeledDropdown
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ScheduleScreen(
    navigator: DestinationsNavigator,
    //    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val loops = listOf("NORTH", "WEST")
    val stops = listOf("All Stops", "Union", "DCC", "JEC", "West Hall")

    var selectedDay by remember {
        mutableStateOf(days[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1])
    }
    var selectedLoop by remember { mutableStateOf(loops.first()) }
    var selectedStop by remember { mutableStateOf(stops.first()) }

    Scaffold(
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
                Modifier.padding(padding)
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
                items = loops,
                selectedItem = selectedLoop,
                onItemSelected = { selectedLoop = it },
            )

            LabeledDropdown(
                label = "Stop",
                items = stops,
                selectedItem = selectedStop,
                onItemSelected = { selectedStop = it },
            )

            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .border(width = 1.dp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        .padding(8.dp),
            ) {
                ScheduleHardcodedList()
            }
        }
    }
}

@Composable
private fun ScheduleHardcodedList() {
    val scheduleLines =
        listOf(
            "9:00 AM Student Union",
            "9:03 AM Colonie",
            "9:05 AM Georgian",
            "9:06 AM Bryckwyck",
            "9:07 AM Stacwyck",
            "9:08 AM E-Lot",
            "9:11 AM ECAV",
            "9:13 AM Houston Field House",
            "9:16 AM Student Union (Return)",
            "9:20 AM Student Union",
            "9:23 AM Colonie",
            "Filler",
            "Filler",
            "Filler",
            "Filler",
            "Filler",
            "Filler",
            "Filler",
            "Filler",
            "Filler",
            "Filler",
        )

    Column {
        Text(
            text = "Time (estimated)",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(scheduleLines) { line ->
                val isOutdented = line.contains("Student Union")
                Text(
                    text = line,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(
                                start = if (isOutdented) 0.dp else 16.dp,
                                bottom = 4.dp,
                            ),
                )
            }
        }
    }
}
