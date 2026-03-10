package edu.rpi.shuttletracker.ui.maps

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationDisabled
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.graphics.toColorInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ScheduleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.models.Vehicle
import edu.rpi.shuttletracker.ui.maps.components.StopEtaContent
import edu.rpi.shuttletracker.ui.maps.components.StopSheetContent
import edu.rpi.shuttletracker.ui.maps.components.getVehicleMarkerDescriptor
import edu.rpi.shuttletracker.ui.maps.utils.VehicleEtaUi
import edu.rpi.shuttletracker.ui.theme.VehicleColors
import edu.rpi.shuttletracker.ui.util.CheckResponseError
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(start = true)
@Composable
fun MapsScreen(
    navigator: DestinationsNavigator,
    viewModel: MapsViewModel = hiltViewModel(),
) {
    val mapsUiState = viewModel.mapsUiState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedStop by remember { mutableStateOf<Stop?>(null) }
    var selectedVehicleId by remember { mutableStateOf<String?>(null) }

    var showSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val cameraPositionState =
        rememberCameraPositionState {
            position =
                CameraPosition.fromLatLngZoom(
                    LatLng(42.73068146020498, -73.67619731950525),
                    14.3f,
                )
        }

    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { showSheet = true },
                    icon = { Icon(Icons.Outlined.StopCircle, contentDescription = null) },
                    label = { Text("Stops") },
                )

                NavigationBarItem(
                    selected = false,
                    onClick = { navigator.navigate(ScheduleScreenDestination()) },
                    icon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                    label = { Text("Schedule") },
                )
            }
        },
        snackbarHost = {
            // finds errors when requesting data to server
            CheckResponseError(
                mapsUiState.networkError,
                mapsUiState.serverError,
                mapsUiState.unknownError,
                ignoreErrorRequest = { viewModel.clearErrors() },
                retryErrorRequest = { viewModel.retry() },
            )
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            ShuttleMap(
                mapsUiState = mapsUiState,
                padding = padding,
                onScheduleClick = { navigator.navigate(ScheduleScreenDestination()) },
                onSettingsClick = { navigator.navigate(SettingsScreenDestination()) },
                onToggleMapTypeClick = { viewModel.toggleMapType() },
                cameraPositionState = cameraPositionState,
                selectedStopKey = mapsUiState.selectedStopKey,
                selectedStop = selectedStop,
                selectedVehicleId = selectedVehicleId,
                onStopSelected = { stopKey, stop ->
                    viewModel.setSelectedStop(stopKey)
                    selectedStop = stop
                },
            )

            EtaOverlayCard(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(padding)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                title = selectedStop?.name ?: "Tap a stop to see etas",
                selectedStopEtas = mapsUiState.selectedStopEtas,
                lastEtasUpdatedAt = mapsUiState.lastEtasUpdatedAt,
                stopSelected = selectedStop != null,
                onClearStop = {
                    viewModel.setSelectedStop(null)
                    selectedStop = null
                    selectedVehicleId = null
                },
                onEtaChipClick = { vehicleId ->
                    selectedVehicleId = vehicleId
                    val vehicle = mapsUiState.vehicles.firstOrNull { it.id == vehicleId } ?: return@EtaOverlayCard
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition
                                    .Builder()
                                    .target(vehicle.latLng())
                                    .zoom(maxOf(cameraPositionState.position.zoom, 16f))
                                    .tilt(0f)
                                    .build(),
                            ),
                            700,
                        )
                    }
                },
            )

            if (showSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSheet = false },
                    sheetState = sheetState,
                ) {
                    StopSheetContent(
                        routeKeys = mapsUiState.allowedRouteKeys,
                        selectedRouteKey = mapsUiState.selectedRouteKey,
                        stopRows = mapsUiState.stopRows,
                        showDetails = true,
                        onRouteSelected = { routeKey ->
                            viewModel.setSelectedRoute(routeKey)
                        },
                        onStopClick = { stopKey, stop ->
                            viewModel.setSelectedStop(stopKey)
                            selectedStop = stop
                            showSheet = false
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newCameraPosition(
                                        CameraPosition
                                            .Builder()
                                            .target(stop.latLng())
                                            .zoom(maxOf(cameraPositionState.position.zoom, 16f))
                                            .tilt(0f)
                                            .build(),
                                    ),
                                    1000,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShuttleMap(
    mapsUiState: MapsUiState,
    padding: PaddingValues,
    onScheduleClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onToggleMapTypeClick: () -> Unit,
    cameraPositionState: CameraPositionState,
    selectedStopKey: String?,
    selectedStop: Stop?,
    selectedVehicleId: String?,
    onStopSelected: (stopKey: String, stop: Stop) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // can't show current location without location
    val isLocationPermissionGranted by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val isDark = mapsUiState.themeMode.isDarkTheme(isSystemInDarkTheme())

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            // makes sure the items drawn (current location and compass) are clickable
            contentPadding = padding,
            cameraPositionState = cameraPositionState,
            properties =
                MapProperties(
                    latLngBoundsForCameraTarget =
                        LatLngBounds(
                            LatLng(42.72095724005504, -73.70196321825452),
                            LatLng(42.741173465236876, -73.6543446409232),
                        ),
                    mapType = mapsUiState.mapType,
                    isBuildingEnabled = true,
                    minZoomPreference = 14f,
                    isMyLocationEnabled = isLocationPermissionGranted,
                    mapStyleOptions =
                        if (isDark) {
                            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_dark)
                        } else {
                            null
                        },
                ),
            // removes the zoom control which was covered by the FAB
            uiSettings =
                MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                ),
        ) {
            // creates the stops
            mapsUiState.routes.forEach { (_, route) ->
                route.stopDetails.forEach { (stopKey, stop) ->
                    StopMarker(
                        stop = stop,
                        selected = stopKey == selectedStopKey || stop.name == selectedStop?.name,
                        onSelected = { onStopSelected(stopKey, stop) },
                    )
                }
            }

            // creates the vehicle markers
            mapsUiState.vehicles.forEach { vehicle ->
                VehicleMarker(
                    vehicle = vehicle,
                    selected = vehicle.id == selectedVehicleId,
                )
            }

            // draws the paths
            mapsUiState.routes.forEach { (_, route) ->
                val points = route.latLng()
                if (points.isNotEmpty()) {
                    Polyline(
                        points = points,
                        color =
                            Color(
                                android.graphics.Color
                                    .valueOf(
                                        route.color.toColorInt(),
                                    ).toArgb(),
                            ),
                    )
                }
            }
        }

        val mapTypeIcon =
            if (mapsUiState.mapType == MapType.NORMAL) {
                Icons.Outlined.Layers
            } else {
                Icons.Filled.Layers
            }

        MapButtonsOverlay(
            modifier =
                Modifier
                    .padding(padding)
                    .padding(horizontal = 10.dp),
            isMyLocationEnabled = isLocationPermissionGranted,
            mapTypeIcon = mapTypeIcon,
            onScheduleClick = onScheduleClick,
            onSettingsClick = onSettingsClick,
            onRecenterClick = {
                LocationServices
                    .getFusedLocationProviderClient(context)
                    .lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location == null) return@addOnSuccessListener

                        coroutineScope.launch {
                            cameraPositionState.animate(
                                update =
                                    CameraUpdateFactory.newCameraPosition(
                                        CameraPosition
                                            .builder()
                                            .target(
                                                LatLng(
                                                    location.latitude,
                                                    location.longitude,
                                                ),
                                            ).tilt(0f)
                                            .zoom(cameraPositionState.position.zoom)
                                            .build(),
                                    ),
                                durationMs = 1000,
                            )
                        }
                    }
            },
            onToggleMapTypeClick = onToggleMapTypeClick,
        )
    }
}

/**
 * Creates a marker for a stop
 * */
@Composable
private fun StopMarker(
    stop: Stop,
    selected: Boolean,
    onSelected: (Stop) -> Unit,
) {
    val markerState = rememberUpdatedMarkerState(position = stop.latLng())

    Circle(
        center = stop.latLng(),
        radius = 15.0,
        strokeColor =
            if (selected) {
                Color(0xFF6699FF)
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
        strokeWidth = 8f,
        zIndex = 1f,
        fillColor = Color.Transparent,
    )

    Marker(
        state = markerState,
        title = stop.name,
        anchor = Offset(0.5f, 0.5f),
        alpha = 0f,
        zIndex = 2f,
        onClick = {
            onSelected(stop)
            markerState.showInfoWindow()
            true
        },
    )
}

/**
 * Creates a marker for a vehicle
 * */
@Composable
private fun VehicleMarker(
    vehicle: Vehicle,
    selected: Boolean,
) {
    val context = LocalContext.current
    val markerState = rememberUpdatedMarkerState(position = vehicle.latLng())

    // every time the vehicle changes, update the position of the marker
    LaunchedEffect(vehicle) {
        markerState.position = vehicle.latLng()
    }

    LaunchedEffect(selected) {
        if (selected) {
            markerState.showInfoWindow()
        } else {
            markerState.hideInfoWindow()
        }
    }

    val vehicleColor =
        when (vehicle.routeName) {
            "NORTH" -> VehicleColors.North
            "WEST" -> VehicleColors.West
            else -> VehicleColors.Default
        }

    val icon =
        remember(vehicleColor) {
            getVehicleMarkerDescriptor(context, 25f, vehicleColor.toArgb())
        }

    // gets vehicle speed and last time it updated
    val timeAgoFlow = remember(vehicle.timestamp) { vehicle.getTimeAgo() }
    val lastUpdatedAgoText =
        timeAgoFlow.collectAsStateWithLifecycle(initialValue = "").value

    val snippetText =
        buildString {
            append(stringResource(R.string.vehicle_speed, vehicle.speedMph))
            if (lastUpdatedAgoText.isNotBlank()) {
                append(" • ")
                append(lastUpdatedAgoText)
            }
        }

    Marker(
        state = markerState,
        title = stringResource(R.string.vehicle_number, vehicle.name),
        icon = icon,
        snippet = snippetText,
        anchor = Offset(0.5f, 0.5f),
        zIndex = 3f,
        onClick = {
            it.showInfoWindow()
            true
        },
    )
}

@Composable
private fun MapButtonsOverlay(
    modifier: Modifier = Modifier,
    isMyLocationEnabled: Boolean,
    mapTypeIcon: ImageVector,
    onScheduleClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRecenterClick: () -> Unit,
    onToggleMapTypeClick: () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        // Left side
        Column(
            modifier =
                Modifier
                    .align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
//            ActionButton(icon = Icons.Outlined.Schedule) {
//                onScheduleClick()
//            }
//
            ActionButton(icon = Icons.Outlined.Settings) {
                onSettingsClick()
            }
        }
        // Right side
        Column(
            modifier =
                Modifier
                    .align(Alignment.TopEnd),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ActionButton(
                icon =
                    if (isMyLocationEnabled) {
                        Icons.Outlined.MyLocation
                    } else {
                        Icons.Outlined.LocationDisabled
                    },
            ) {
                onRecenterClick()
            }
            ActionButton(icon = mapTypeIcon) {
                onToggleMapTypeClick()
            }
        }
    }
}

/**
 * Buttons that let you do things that is displayed on the map
 * @param badgeCount: if a badge is needed for a item, it will display
 * @param action: what to do on button click
 * */
@Composable
private fun ActionButton(
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
            modifier =
                Modifier
                    .size(50.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp),
        ) {
            Icon(icon, icon.name)
        }
    }
}

@Composable
fun EtaOverlayCard(
    modifier: Modifier = Modifier,
    title: String,
    selectedStopEtas: List<VehicleEtaUi>,
    lastEtasUpdatedAt: java.time.Instant?,
    stopSelected: Boolean,
    onClearStop: () -> Unit,
    onEtaChipClick: (vehicleId: String) -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        StopEtaContent(
            modifier = Modifier.padding(vertical = 2.dp),
            title = title,
            selectedStopEtas = selectedStopEtas,
            lastEtasUpdatedAt = lastEtasUpdatedAt,
            stopSelected = stopSelected,
            onClearStop = onClearStop,
            onEtaChipClick = onEtaChipClick,
        )
    }
}
