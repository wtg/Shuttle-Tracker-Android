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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.rpi.shuttletracker.core.ui.CheckResponseError
import edu.rpi.shuttletracker.feature.etas.components.StopEtaList
import edu.rpi.shuttletracker.feature.etas.components.StopEtaSheet
import edu.rpi.shuttletracker.feature.etas.utils.buildStopsWithEtas

/**
 * The ETAs tab: a [StopEtaList] of stops with live arrival previews, and a [StopEtaSheet] with the
 * full details for whichever stop is tapped. Vehicle polling starts/stops with this composable's
 * own lifecycle via [LifecycleStartEffect], so it only runs while this tab is actually visible.
 * */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EtasScreen(
    viewModel: EtasViewModel = hiltViewModel(),
    showTitle: Boolean = true,
) {
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
            val allVehicles = uiState.vehicles + uiState.fakeVehicles

            StopEtaList(
                routes = uiState.routes,
                vehicles = allVehicles,
                selectedRouteFilter = uiState.selectedRouteFilter,
                onRouteFilterChange = viewModel::selectRouteFilter,
                onStopClick = viewModel::selectStop,
                showTitle = showTitle,
            )

            val stops =
                remember(uiState.routes, allVehicles) {
                    buildStopsWithEtas(uiState.routes, allVehicles)
                }
            val selectedStop = uiState.selectedStopKey?.let { stopKey -> stops.find { it.stopKey == stopKey } }

            StopEtaSheet(
                stop = selectedStop,
                sheetState = sheetState,
                onDismiss = { viewModel.selectStop(null) },
            )
        }
    }
}
