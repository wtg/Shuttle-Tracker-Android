package edu.rpi.shuttletracker.ui.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.ui.util.CheckResponseError
import edu.rpi.shuttletracker.ui.util.CollapsableList
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Destination
@Composable
fun ScheduleScreen(
    navigator: DestinationsNavigator,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val scheduleUiState = viewModel.scheduleUiState.collectAsStateWithLifecycle().value


    val pagerState =
        rememberPagerState(
            pageCount = { scheduleUiState.schedule.size },
            initialPage = scheduleUiState.schedule.size,
        )

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
                title = { Text(text = stringResource(R.string.schedule)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxWidth(),
            reverseLayout = true,
        ) { page ->

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (page < scheduleUiState.schedule.size) {
                    SchedulePagerItem(schedule = scheduleUiState.schedule[page])
                }
            }
        }
    }
}


@Composable
fun SchedulePagerItem(schedule: Schedule) {
    val calendar = Calendar.getInstance()
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = schedule.name, style = MaterialTheme.typography.headlineLarge)

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.monday),
                    fontWeight = if (dayOfWeek == Calendar.MONDAY) FontWeight.Bold else null,
                )
                Text(
                    text = stringResource(R.string.tuesday),
                    fontWeight = if (dayOfWeek == Calendar.TUESDAY) FontWeight.Bold else null,
                )
                Text(
                    text = stringResource(R.string.wednesday),
                    fontWeight = if (dayOfWeek == Calendar.WEDNESDAY) FontWeight.Bold else null,
                )
                Text(
                    text = stringResource(R.string.thursday),
                    fontWeight = if (dayOfWeek == Calendar.THURSDAY) FontWeight.Bold else null,
                )
                Text(
                    text = stringResource(R.string.friday),
                    fontWeight = if (dayOfWeek == Calendar.FRIDAY) FontWeight.Bold else null,
                )
                Text(
                    text = stringResource(R.string.saturday),
                    fontWeight = if (dayOfWeek == Calendar.SATURDAY) FontWeight.Bold else null,
                )
                Text(
                    text = stringResource(R.string.sunday),
                    fontWeight = if (dayOfWeek == Calendar.SUNDAY) FontWeight.Bold else null,
                )
            }

            Column {
                Text(
                    text = schedule.monday.toString(),
                    fontWeight = if (dayOfWeek == Calendar.MONDAY) FontWeight.Bold else null,
                )
                Text(
                    text = schedule.tuesday.toString(),
                    fontWeight = if (dayOfWeek == Calendar.TUESDAY) FontWeight.Bold else null,
                )
                Text(
                    text = schedule.wednesday.toString(),
                    fontWeight = if (dayOfWeek == Calendar.WEDNESDAY) FontWeight.Bold else null,
                )
                Text(
                    text = schedule.thursday.toString(),
                    fontWeight = if (dayOfWeek == Calendar.THURSDAY) FontWeight.Bold else null,
                )
                Text(
                    text = schedule.friday.toString(),
                    fontWeight = if (dayOfWeek == Calendar.FRIDAY) FontWeight.Bold else null,
                )
                Text(
                    text = schedule.saturday.toString(),
                    fontWeight = if (dayOfWeek == Calendar.SATURDAY) FontWeight.Bold else null,
                )
                Text(
                    text = schedule.sunday.toString(),
                    fontWeight = if (dayOfWeek == Calendar.SUNDAY) FontWeight.Bold else null,
                )
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        Text(text = stringResource(R.string.effective_from, schedule.startTime, schedule.endTime))
    }
}
