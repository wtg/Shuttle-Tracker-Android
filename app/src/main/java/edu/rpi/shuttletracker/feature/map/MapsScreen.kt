package edu.rpi.shuttletracker.feature.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import edu.rpi.shuttletracker.core.ui.CheckResponseError
import edu.rpi.shuttletracker.feature.map.components.ScheduleSheet

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(start = true)
@Composable
fun MapsScreen(
    navigator: DestinationsNavigator,
    viewModel: MapsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.mapsUiState.collectAsStateWithLifecycle()
    var selectedScheduleRoute by rememberSaveable { mutableStateOf<String?>(null) }
    var isScheduleVisible by rememberSaveable { mutableStateOf(false) }
    val scheduleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LifecycleStartEffect(viewModel) {
        viewModel.startVehiclePolling()
        onStopOrDispose { viewModel.stopVehiclePolling() }
    }

    Scaffold(
        snackbarHost = {
            CheckResponseError(
                uiState.networkError,
                uiState.serverError,
                uiState.unknownError,
                ignoreErrorRequest = viewModel::clearErrors,
                retryErrorRequest = viewModel::retry,
            )
        },
    ) { contentPadding ->
        Box(Modifier.fillMaxSize()) {
            ShuttleMap(
                uiState = uiState,
                contentPadding = contentPadding,
                onSettingsClick = { navigator.navigate(SettingsScreenDestination()) },
                onScheduleClick = { isScheduleVisible = true },
                onToggleMapTypeClick = viewModel::toggleMapType,
            )

            ScheduleSheet(
                show = isScheduleVisible,
                sheetState = scheduleSheetState,
                schedule = uiState.schedule,
                isLoading = uiState.isScheduleLoading,
                routesByName = uiState.routes,
                selectedRoute = selectedScheduleRoute,
                onSelectedRouteChange = { selectedScheduleRoute = it },
                onDismiss = { isScheduleVisible = false },
            )
        }
    }
}
