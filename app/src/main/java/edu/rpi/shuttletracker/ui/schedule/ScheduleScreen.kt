package edu.rpi.shuttletracker.ui.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.ui.util.CheckResponseError

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Destination
@Composable
fun ScheduleScreen(
    navigator: DestinationsNavigator,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val scheduleUiState = viewModel.scheduleUiState.collectAsStateWithLifecycle().value

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

    val pagerState = rememberPagerState(
        pageCount = { scheduleUiState.schedule.size },
        initialPage = scheduleUiState.schedule.size,
    )

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
    )

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.schedule)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.Outlined.ArrowBack, stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding),
            reverseLayout = true,
        ) { page ->
            if (page < scheduleUiState.schedule.size) {
                SchedulePagerItem(schedule = scheduleUiState.schedule[page])
            }
        }
    }
}

@Composable
fun SchedulePagerItem(schedule: Schedule) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(text = schedule.name, style = MaterialTheme.typography.headlineLarge)

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Text(text = stringResource(R.string.monday))
                Text(text = stringResource(R.string.tuesday))
                Text(text = stringResource(R.string.wednesday))
                Text(text = stringResource(R.string.thursday))
                Text(text = stringResource(R.string.friday))
                Text(text = stringResource(R.string.saturday))
                Text(text = stringResource(R.string.sunday))
            }

            Column {
                Text(text = schedule.monday.toString())
                Text(text = schedule.tuesday.toString())
                Text(text = schedule.wednesday.toString())
                Text(text = schedule.thursday.toString())
                Text(text = schedule.friday.toString())
                Text(text = schedule.saturday.toString())
                Text(text = schedule.sunday.toString())
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        Text(text = stringResource(R.string.effective_from, schedule.startTime, schedule.endTime))
    }
}
