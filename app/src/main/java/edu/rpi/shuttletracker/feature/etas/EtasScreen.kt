package edu.rpi.shuttletracker.feature.etas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.rpi.shuttletracker.core.ui.CheckResponseError
import edu.rpi.shuttletracker.feature.etas.components.StopEtaList
import edu.rpi.shuttletracker.feature.etas.components.StopEtaSheet
import edu.rpi.shuttletracker.feature.etas.utils.buildStopsWithEtas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EtasScreen(viewModel: EtasViewModel = hiltViewModel()) {
    val uiState by viewModel.etasUiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LifecycleStartEffect(viewModel) {
        viewModel.startVehiclePolling()
        onStopOrDispose {
            viewModel.stopVehiclePolling()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
        Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            StopEtaList(
                routes = uiState.routes,
                vehicles = uiState.vehicles,
                selectedRouteFilter = uiState.selectedRouteFilter,
                onRouteFilterChange = viewModel::selectRouteFilter,
                onStopClick = viewModel::selectStop,
            )

            val selectedStop =
                uiState.selectedStopKey?.let { stopKey ->
                    buildStopsWithEtas(uiState.routes, uiState.vehicles).find { it.stopKey == stopKey }
                }

            StopEtaSheet(
                stop = selectedStop,
                sheetState = sheetState,
                onDismiss = { viewModel.selectStop(null) },
            )
        }
    }
}
