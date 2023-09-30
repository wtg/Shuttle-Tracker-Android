package edu.rpi.shuttletracker.ui.maps

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Announcement
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.ui.destinations.AnnouncementsScreenDestination
import edu.rpi.shuttletracker.ui.errors.CheckResponseError
import edu.rpi.shuttletracker.ui.errors.Error
import edu.rpi.shuttletracker.ui.permissions.BluetoothPermissionChecker
import edu.rpi.shuttletracker.ui.permissions.LocationPermissionsChecker
import edu.rpi.shuttletracker.util.services.BeaconService
import edu.rpi.shuttletracker.util.services.LocationService

/**TODO follow thread https://github.com/googlemaps/android-maps-compose/pull/347 */
@Destination(start = true)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MapsScreen(
    navigator: DestinationsNavigator,
    viewModel: MapsViewModel = hiltViewModel(),
) {
    // makes sure the 2 flows are collected when ui is open
    val mapsUIState = viewModel.mapsUIState.collectAsStateWithLifecycle().value
    viewModel.runningBusesState.collectAsStateWithLifecycle({})

    CheckResponseError(
        mapsUIState.networkError,
        mapsUIState.serverError,
        mapsUIState.unknownError,
        ignoreErrorRequest = { viewModel.clearErrors() },
        retryErrorRequest = {
            viewModel.clearErrors()
            viewModel.loadAll()
        },
    )

    with(LocationService.error.collectAsStateWithLifecycle().value) {
        if (this != null) {
            Error(
                error = this,
                onSecondaryRequest = { LocationService.dismissError() },
                onPrimaryRequest = { LocationService.dismissError() },
                errorType = "Location service",
                errorBody = "You may be too far from a stop (50 ft) or selected an invalid bus",
                primaryButtonText = "I understand",
                showSecondaryButton = false,

            )
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AutoBoardBusFab()
                BoardBusFab(mapsUIState.allBuses, viewModel::closestDistanceToStop)
            }
        },

    ) { padding ->
        BusMap(mapsUIState = mapsUIState, padding = padding)

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Button(
                onClick = { navigator.navigate(AnnouncementsScreenDestination()) },
                modifier = Modifier
                    .padding(10.dp)
                    .size(50.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp),
            ) {
                Icon(Icons.Default.Announcement, "Announcements")
            }
        }
    }
}

/**
 * Creates the map displaying everything
 *
 * @param mapsUIState: The UI state of the view from the view-model
 * @param padding: Padding needed for the map content padding
 * */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun BusMap(
    mapsUIState: MapsUIState,
    padding: PaddingValues,
) {
    var mapLocationEnabled by remember { mutableStateOf(false) }
    LocationPermissionsChecker(onPermissionGranted = { mapLocationEnabled = true })

    val context = LocalContext.current

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(42.73068146020498, -73.67619731950525),
            14.3f,
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),

        // makes sure the items drawn (current location and compass) are clickable
        contentPadding = padding,
        cameraPositionState = cameraPositionState,

        // auto dark theme
        properties = MapProperties(
            latLngBoundsForCameraTarget = LatLngBounds(
                LatLng(42.72095724005504, -73.70196321825452),
                LatLng(42.741173465236876, -73.6543446409232),
            ),
            isBuildingEnabled = true,
            minZoomPreference = 13f,
            isMyLocationEnabled = mapLocationEnabled,
            mapStyleOptions = if (isSystemInDarkTheme()) {
                MapStyleOptions.loadRawResourceStyle(context, R.raw.map_dark)
            } else {
                MapStyleOptions("[]")
            },
        ),

        // removes the zoom control which was covered by the FAB
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
        ),
    ) {
        mapsUIState.stops.forEach {
            StopMarker(stop = it)
        }

        mapsUIState.runningBuses.forEach {
            BusMarker(bus = it)
        }

        mapsUIState.routes.forEach {
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

/**
 * Creates a marker for a stop
 * */
@Composable
fun StopMarker(stop: Stop) {
    val markerState = rememberMarkerState(stop.name, stop.latLng())
    val icon = BitmapDescriptorFactory.fromAsset("simplecircle.png")
    Marker(
        state = markerState,
        title = stop.name,
        icon = icon,
        onClick = {
            it.showInfoWindow()
            true
        },
    )
}

/**
 * Creates a marker for a bus
 * */
@Composable
fun BusMarker(bus: Bus) {
    val markerState = rememberMarkerState(position = bus.latLng())

    // every time bus changes, update the position of the marker
    LaunchedEffect(bus) {
        markerState.position = bus.latLng()
    }

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
        onClick = {
            it.showInfoWindow()
            true
        },
    )
}

/**
 * The Floating Action Button for boarding the bus
 * */
@SuppressLint("MissingPermission") // permissions checked in external composable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun BoardBusFab(
    buses: List<Int>,
    checkDistanceToStop: (location: Location) -> Float,
) {
    val locationServiceBusNumber = LocationService.busNum.collectAsStateWithLifecycle().value
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
                context.startForegroundService(intent)
            },
            onDismiss = { busPickerState = false },
        )
    }

    if (checkLocationPermissionsState) {
        LocationPermissionsChecker(
            onPermissionGranted = {
                // MissingPermission suppressed here as location is already checked
                // This receives the most recent position of the user
                LocationServices.getFusedLocationProviderClient(context).lastLocation
                    .addOnSuccessListener { location: Location? ->

                        // if they a location was found and they are 50 ft away from a stop
                        if (location != null && checkDistanceToStop(location) * 3.28084 <= 50) {
                            busPickerState = true
                        } else {
                            // not close enough to a stop
                            Toast.makeText(context, "Must be 50 ft from bus to board", Toast.LENGTH_SHORT).show()
                        }
                    }

                checkLocationPermissionsState = false
            },

            onPermissionDenied = { checkLocationPermissionsState = false },
        )
    }

    ExtendedFloatingActionButton(
        onClick = {
            if (locationServiceBusNumber != null) {
                context.stopService(Intent(context, LocationService::class.java))
            } else {
                checkLocationPermissionsState = true
            }
        },
        icon = { Icon(Icons.Default.DirectionsBus, "Board Bus") },
        text = { Text(text = if (locationServiceBusNumber != null) "Leave Bus" else "Board Bus") },
    )
}

/**
 * Creates the FAB to enable auto-boarding
 * */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AutoBoardBusFab() {
    val context = LocalContext.current

    val isBeaconServiceRunning = BeaconService.isRunning.collectAsState().value

    var checkLocationPermissionsState by remember { mutableStateOf(false) }
    var checkBluetoothPermissionsState by remember { mutableStateOf(false) }

    // checks for location permissions first, if they have check for bluetooth
    if (checkLocationPermissionsState) {
        LocationPermissionsChecker(
            onPermissionGranted = {
                checkBluetoothPermissionsState = true
                checkLocationPermissionsState = false
            },
            onPermissionDenied = { checkLocationPermissionsState = false },
        )
    }

    // if there is bluetooth permissions then start the beacon service
    if (checkBluetoothPermissionsState) {
        BluetoothPermissionChecker(
            onPermissionGranted = {
                context.startForegroundService(Intent(context, BeaconService::class.java))
                checkBluetoothPermissionsState = false
            },
            onPermissionDenied = { checkBluetoothPermissionsState = false },
        )
    }

    SmallFloatingActionButton(
        onClick = {
            if (!isBeaconServiceRunning) {
                checkLocationPermissionsState = true
            } else {
                context.stopService(Intent(context, BeaconService::class.java))
            }
        },
        containerColor = if (isBeaconServiceRunning) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
    ) {
        Icon(
            if (isBeaconServiceRunning) {
                Icons.Default.ShareLocation
            } else {
                Icons.Default.LocationDisabled
            },
            "Location",
        )
    }
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
    // -1 will be unselected
    var selectedBus by remember { mutableIntStateOf(-1) }
    val context = LocalContext.current

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
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
