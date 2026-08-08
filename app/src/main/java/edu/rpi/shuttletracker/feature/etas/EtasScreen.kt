package edu.rpi.shuttletracker.feature.etas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Vehicle
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
    routes: Map<String, Route>,
    vehicles: List<Vehicle>,
    routesLoaded: Boolean,
    showTitle: Boolean = true,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedRouteFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedStopKey by rememberSaveable { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        StopEtaList(
            routes = routes,
            vehicles = vehicles,
            routesLoaded = routesLoaded,
            selectedRouteFilter = selectedRouteFilter,
            onRouteFilterChange = { selectedRouteFilter = it },
            onStopClick = { selectedStopKey = it },
            showTitle = showTitle,
        )

        val stops = remember(routes, vehicles) { buildStopsWithEtas(routes, vehicles) }
        val selectedStop = selectedStopKey?.let { stopKey -> stops.find { it.stopKey == stopKey } }

        StopEtaSheet(
            stop = selectedStop,
            sheetState = sheetState,
            onDismiss = { selectedStopKey = null },
        )
    }
}
