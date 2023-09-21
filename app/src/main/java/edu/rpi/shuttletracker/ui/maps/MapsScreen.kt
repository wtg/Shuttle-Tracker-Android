package edu.rpi.shuttletracker.ui.maps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.Stop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(
    viewModel: MapsViewModel = hiltViewModel(),
) {
    viewModel.loadStops()
    viewModel.loadBuses()
    val state = viewModel.mapsUIState.collectAsState().value

    Scaffold(
        floatingActionButton = { BoardBusFab(state.isBoarded, viewModel::updateBoardedState) },
        floatingActionButtonPosition = FabPosition.Center,

    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(
                    LatLng(42.730426, -73.676573),
                    13.5f,
                )
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
            ) {
                state.stops.forEach {
                    StopMarker(stop = it)
                }

                state.buses.forEach {
                    BusMarker(bus = it)
                }
            }
        }
    }
}

/**
 * Creates a marker for a stop
 * */
@Composable
fun StopMarker(stop: Stop) {
    val markerState = rememberMarkerState(null, stop.latLng())
    val icon = BitmapDescriptorFactory.fromAsset("simplecircle.png")
    Marker(
        state = markerState,
        title = stop.name,
        icon = icon,
    )
}

/**
 * Creates a marker for a bus
 * */
@Composable
fun BusMarker(bus: Bus) {
    val markerState = rememberMarkerState(null, bus.latLng())

    val context = LocalContext.current

    // gets proper bus icon
    val busIcon = if (MapsActivity.colorblindMode.getMode()) {
        if (bus.type == "user") {
            context.getString(R.string.colorblind_crowdsourced_bus)
        } else {
            context.getString(R.string.colorblind_GPS_bus)
        }
    } else {
        if (bus.type == "user") {
            context.getString(R.string.crowdsourced_bus)
        } else {
            context.getString(R.string.GPS_bus)
        }
    }

    val icon = BitmapDescriptorFactory.fromAsset(busIcon)

    Marker(
        state = markerState,
        title = "Bus ${bus.id}",
        icon = icon,
        snippet = bus.getTimeAgo(),
    )
}

/**
 * The Floating Action Button for boarding the bus
 * */
@Composable
fun BoardBusFab(isBoarded: Boolean, updateBoardedState: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = updateBoardedState,
        icon = { Icon(Icons.Default.ShoppingCart, "Board Bus") },
        text = { Text(text = if (isBoarded) "Leave Bus" else "Board Bus") },
    )
}
