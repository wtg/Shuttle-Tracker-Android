package edu.rpi.shuttletracker.feature.schedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.rpi.shuttletracker.core.ui.CheckResponseError
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.feature.schedule.components.ScheduleContent

@Composable
fun ScheduleScreen(
    routesByName: Map<String, Route>,
    viewModel: ScheduleViewModel = hiltViewModel(),
    showTitle: Boolean = true,
    isWideLayout: Boolean = false,
) {
    val uiState by viewModel.scheduleUiState.collectAsStateWithLifecycle()
    var selectedRoute by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            CheckResponseError(
                uiState.error,
                ignoreErrorRequest = viewModel::clearErrors,
                retryErrorRequest = viewModel::retry,
            )
        },
    ) { contentPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            ScheduleContent(
                schedule = uiState.schedule,
                isLoading = uiState.isScheduleLoading,
                routesByName = routesByName,
                selectedRoute = selectedRoute,
                onSelectedRouteChange = { selectedRoute = it },
                showTitle = showTitle,
                isWideLayout = isWideLayout,
            )
        }
    }
}
