package edu.rpi.shuttletracker.ui.maps

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.ui.permissions.LocationPermissionsChecker
import edu.rpi.shuttletracker.util.services.LocationService

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(
    viewModel: MapsViewModel = hiltViewModel(),
) {
    val state = viewModel.mapsUIState.collectAsState().value

    var mapLocationEnabled by remember { mutableStateOf(false) }

    LocationPermissionsChecker(onPermissionGranted = { mapLocationEnabled = true })

    Scaffold(
        floatingActionButton = { BoardBusFab(state.allBuses) },
        floatingActionButtonPosition = FabPosition.Center,

    ) {
        Box {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(
                    LatLng(42.730426, -73.676573),
                    13.5f,
                )
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = mapLocationEnabled),
            ) {
                state.stops.forEach {
                    StopMarker(stop = it)
                }

                state.runningBuses.forEach {
                    BusMarker(bus = it)
                }

                state.routes.forEach {
                    Polyline(
                        points = it.latLng(),
                        color = Color(
                            android.graphics.Color.valueOf(
                                android.graphics.Color.parseColor(it.colorName),
                            ).toArgb(),
                        ),
                    )
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
    val busIcon = if (bus.type == "user") {
        context.getString(R.string.crowdsourced_bus)
    } else {
        context.getString(R.string.GPS_bus)
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
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun BoardBusFab(buses: List<Int>) {
    val isLocationServiceRunning = LocationService.isRunning.collectAsState().value
    val context = LocalContext.current

    var busPickerState by remember { mutableStateOf(false) }
    var checkLocationPermissionsState by remember { mutableStateOf(false) }

    // launches the bus picker dialog
    if (busPickerState) {
        BusPicker(
            buses = buses,
            onBusChosen = {
                val intent = Intent(context, LocationService::class.java).apply {
                    putExtra(LocationService.BUNDLE_BUS_ID, it)
                }
                context.startService(intent)
            },
            onDismiss = { busPickerState = false },
        )
    }

    if (checkLocationPermissionsState) {
        LocationPermissionsChecker(
            onPermissionGranted = {
                busPickerState = true
                checkLocationPermissionsState = false
            },
            onPermissionDenied = { checkLocationPermissionsState = false },
        )
    }

    ExtendedFloatingActionButton(
        onClick = {
            if (isLocationServiceRunning) {
                context.stopService(Intent(context, LocationService::class.java))
            } else {
                checkLocationPermissionsState = true
            }
        },
        icon = { Icon(Icons.Default.ShoppingCart, "Board Bus") },
        text = { Text(text = if (isLocationServiceRunning) "Leave Bus" else "Board Bus") },
    )
}

/**
 * Dialog that appears to choose which bus to board
 * */
@Composable
fun BusPicker(
    buses: List<Int>,
    onBusChosen: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedBus by remember { mutableIntStateOf(-1) }
    val context = LocalContext.current

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "Choose a Bus",
                    style = MaterialTheme.typography.headlineLarge,
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                        .padding(10.dp),
                ) {
                    items(items = buses, itemContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedBus == it,
                                onClick = { selectedBus = it },
                            )

                            Text(text = it.toString())
                        }
                    })
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(onClick = {
                        if (selectedBus == -1) {
                            Toast.makeText(context, "No Bus Chosen", Toast.LENGTH_LONG).show()
                        } else {
                            onBusChosen(selectedBus)
                        }

                        onDismiss()
                    }) {
                        Text(text = "Select")
                    }
                    Button(onClick = { onDismiss() }) {
                        Text(text = "Cancel")
                    }
                }
            }
        }
    }
}
