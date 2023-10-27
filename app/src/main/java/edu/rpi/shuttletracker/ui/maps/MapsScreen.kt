package edu.rpi.shuttletracker.ui.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.outlined.LocationDisabled
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
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
import edu.rpi.shuttletracker.ui.destinations.ScheduleScreenDestination
import edu.rpi.shuttletracker.ui.destinations.SettingsScreenDestination
import edu.rpi.shuttletracker.ui.destinations.SetupScreenDestination
import edu.rpi.shuttletracker.ui.util.CheckResponseError
import edu.rpi.shuttletracker.util.services.BeaconService
import edu.rpi.shuttletracker.util.services.LocationService
import kotlinx.coroutines.launch

@Destination
@Composable
fun MapsScreen(
    navigator: DestinationsNavigator,
    viewModel: MapsViewModel = hiltViewModel(),
) {
    // makes sure the 2 flows are collected when ui is open
    val mapsUiState = viewModel.mapsUiState.collectAsStateWithLifecycle().value
    viewModel.runningBusesState.collectAsStateWithLifecycle({})

    val snackbarHostState = remember { SnackbarHostState() }

    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    // finds errors when requesting data to server
    CheckResponseError(
        mapsUiState.networkError,
        mapsUiState.serverError,
        mapsUiState.unknownError,
        ignoreErrorRequest = { viewModel.clearErrors() },
        retryErrorRequest = {
            viewModel.clearErrors()
            viewModel.loadAll()
        },
    )

    val errorStartingBeaconService = BeaconService.permissionError.collectAsStateWithLifecycle().value
    val errorStartingLocationService = LocationService.permissionError.collectAsStateWithLifecycle().value

    // shows a snackbar whenever the service isn't able to run, usually because of lack of permissions
    LaunchedEffect(errorStartingBeaconService, errorStartingLocationService) {
        if (errorStartingBeaconService || errorStartingLocationService) {
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.service_missing_permissions),
                    actionLabel = context.getString(R.string.fix),
                    duration = SnackbarDuration.Long,
                )
                when (result) {
                    SnackbarResult.ActionPerformed -> {
                        navigator.navigate(SetupScreenDestination())
                    }
                    SnackbarResult.Dismissed -> { /* IGNORED */ }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                RefreshFab {
                    viewModel.refreshRunningBusses()
                    viewModel.loadAll()
                }
                BoardBusFab(
                    mapsUiState.allBuses,
                    viewModel::closestDistanceToStop,
                    mapsUiState.minStopDist,
                )
            }
        },

    ) { padding ->

        BusMap(mapsUIState = mapsUiState, padding = padding)

        Box(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 10.dp)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // navigates to announcements
                ActionButton(
                    icon = Icons.Outlined.Notifications,
                    badgeCount = mapsUiState.totalAnnouncements - mapsUiState.notificationsRead,
                ) {
                    navigator.navigate(AnnouncementsScreenDestination())
                }

                // navigates to the schedule
                ActionButton(icon = Icons.Outlined.Schedule) {
                    navigator.navigate(ScheduleScreenDestination())
                }

                // navigates to settings
                ActionButton(icon = Icons.Outlined.Settings) {
                    navigator.navigate(SettingsScreenDestination())
                }
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
@Composable
fun BusMap(
    mapsUIState: MapsUIState,
    padding: PaddingValues,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // can't show current location without location
    val mapLocationEnabled by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    // keeps track of where the camera currently is
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

        myLocationButton = {
            // makes sure its in the top left
            Box(
                modifier = Modifier.fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 10.dp),

                contentAlignment = Alignment.TopEnd,
            ) {
                ActionButton(
                    icon = if (mapLocationEnabled) {
                        Icons.Outlined.MyLocation
                    } else {
                        Icons.Outlined.LocationDisabled
                    },
                ) {
                    // finds current position and moves to there
                    LocationServices.getFusedLocationProviderClient(context).lastLocation
                        .addOnSuccessListener { location: Location ->
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    update = CameraUpdateFactory.newLatLng(
                                        LatLng(
                                            location.latitude,
                                            location.longitude,
                                        ),
                                    ),
                                    durationMs = 1000,
                                )
                            }
                        }
                }
            }
        },
    ) {
        // creates the stops
        mapsUIState.stops.forEach {
            StopMarker(stop = it)
        }

        // creates the bus markers
        mapsUIState.runningBuses.forEach {
            BusMarker(
                bus = it,
                colorBlindMode = mapsUIState.colorBlindMode,
            )
        }

        // draws the paths
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
    val icon = BitmapDescriptorFactory.fromAsset(stringResource(R.string.simple_circle))
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
fun BusMarker(bus: Bus, colorBlindMode: Boolean) {
    val markerState = rememberMarkerState(position = bus.latLng())

    // every time bus changes, update the position of the marker
    LaunchedEffect(bus) {
        markerState.position = bus.latLng()
    }

    // gets proper bus icon
    val busIcon = if (bus.type == "user") {
        if (colorBlindMode) {
            stringResource(R.string.colorblind_crowdsourced_bus)
        } else {
            stringResource(R.string.crowdsourced_bus)
        }
    } else {
        if (colorBlindMode) {
            stringResource(R.string.colorblind_GPS_bus)
        } else {
            stringResource(R.string.GPS_bus)
        }
    }

    val icon = BitmapDescriptorFactory.fromAsset(busIcon)

    Marker(
        state = markerState,
        title = stringResource(R.string.bus_number, bus.id),
        icon = icon,
        snippet = bus.getTimeAgo().collectAsStateWithLifecycle(initialValue = "").value,
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
@Composable
fun BoardBusFab(
    buses: List<Int>,
    checkDistanceToStop: (location: Location) -> Float,
    minStopDist: Float,
) {
    val locationServiceBusNumber = LocationService.busNum.collectAsStateWithLifecycle().value
    val context = LocalContext.current

    var busPickerState by remember { mutableStateOf(false) }

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

    ExtendedFloatingActionButton(
        onClick = {
            if (locationServiceBusNumber != null) {
                context.stopService(Intent(context, LocationService::class.java))
            } else {
                LocationServices.getFusedLocationProviderClient(context).lastLocation
                    .addOnSuccessListener { location: Location ->

                        // if they a location was found and they are 50 m away from a stop
                        if (checkDistanceToStop(location) <= minStopDist) {
                            busPickerState = true
                        } else {
                            // not close enough to a stop
                            Toast.makeText(
                                context,
                                context.getString(
                                    R.string.distance_warning,
                                    minStopDist.toInt(),
                                ),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(
                            context,
                            context.getText(R.string.no_location_warning),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
            }
        },
        icon = { Icon(Icons.Default.DirectionsBus, stringResource(R.string.board_bus)) },
        text = {
            Text(
                text = if (locationServiceBusNumber != null) {
                    stringResource(R.string.leave_bus)
                } else {
                    stringResource(R.string.board_bus)
                },
            )
        },
    )
}

/**
 * A FAB that refreshes server items on click
 * */
@Composable
fun RefreshFab(
    refresh: () -> Unit,
) {
    val refreshAnimation = remember { Animatable(0F) }
    val coroutineScope = rememberCoroutineScope()

    SmallFloatingActionButton(
        onClick = {
            refresh()
            coroutineScope.launch {
                refreshAnimation.animateTo(
                    targetValue = 360F,
                    animationSpec = tween(500, easing = LinearEasing),
                )
                refreshAnimation.snapTo(0F)
            }
        },
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Icon(
            Icons.Outlined.Refresh,
            stringResource(R.string.refresh),
            modifier = Modifier.rotate(refreshAnimation.value),
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
                    text = stringResource(R.string.choose_bus),
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
                            Toast.makeText(
                                context,
                                context.getText(R.string.no_bus_chosen),
                                Toast.LENGTH_LONG,
                            ).show()
                        } else {
                            onBusChosen(selectedBus)
                        }

                        onDismiss()
                    }) {
                        Text(text = stringResource(R.string.select))
                    }
                    Button(onClick = { onDismiss() }) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

/**
 * Buttons that let you do things that is displayed on the map
 * @param badgeCount: if a badge is needed for a item, it will display
 * @param action: what to do on button click
 * */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionButton(
    icon: ImageVector,
    badgeCount: Int = 0,
    action: () -> Unit,
) {
    BadgedBox(
        badge = {
            if (badgeCount > 0) {
                // moves the badge on top of the circle
                Badge(modifier = Modifier.offset((-11).dp, 11.dp)) {
                    Text(text = badgeCount.toString())
                }
            }
        },
    ) {
        Button(
            onClick = { action() },
            modifier = Modifier
                .size(50.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp),
        ) {
            Icon(icon, icon.name)
        }
    }
}
