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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationDisabled
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.models.Vehicle
import edu.rpi.shuttletracker.ui.maps.components.ScheduleSheet
import edu.rpi.shuttletracker.ui.maps.components.getVehicleMarkerDescriptor
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

    var selectedScheduleRoute by rememberSaveable { mutableStateOf<String?>(null) }

    var showScheduleSheet by rememberSaveable { mutableStateOf(false) }
    val scheduleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        snackbarHost = {
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
                onSettingsClick = { navigator.navigate(SettingsScreenDestination()) },
                onScheduleClick = { showScheduleSheet = true },
                onToggleMapTypeClick = { viewModel.toggleMapType() },
            )

            ScheduleSheet(
                show = showScheduleSheet,
                sheetState = scheduleSheetState,
                schedule = mapsUiState.schedule,
                routesByName = mapsUiState.routes,
                selectedRoute = selectedScheduleRoute,
                onSelectedRouteChange = { selectedScheduleRoute = it },
                onDismiss = { showScheduleSheet = false },
            )
        }
    }
}

@Composable
private fun ShuttleMap(
    mapsUiState: MapsUiState,
    padding: PaddingValues,
    onSettingsClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onToggleMapTypeClick: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isLocationPermissionGranted by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val cameraPositionState =
        rememberCameraPositionState {
            position =
                CameraPosition.fromLatLngZoom(
                    LatLng(42.73068146020498, -73.67619731950525),
                    14.3f,
                )
        }

    var selectedStop by remember { mutableStateOf<Stop?>(null) }
    val isDark = mapsUiState.themeMode.isDarkTheme(isSystemInDarkTheme())

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
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
            uiSettings =
                MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                ),
        ) {
            mapsUiState.routes.forEach { (_, route) ->
                route.stopDetails.forEach { (_, stop) ->
                    StopMarker(
                        stop = stop,
                        selected = stop.name == selectedStop?.name,
                        onSelected = { selectedStop = it },
                    )
                }
            }

            mapsUiState.vehicles.forEach { vehicle ->
                VehicleMarker(
                    vehicle = vehicle,
                )
            }

            mapsUiState.routes.forEach { (_, route) ->
                val points = route.latLng()
                if (points.isNotEmpty()) {
                    Polyline(
                        points = points,
                        color =
                            Color(
                                android.graphics.Color
                                    .valueOf(route.color.toColorInt())
                                    .toArgb(),
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
            onSettingsClick = onSettingsClick,
            onScheduleClick = onScheduleClick,
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
private fun VehicleMarker(vehicle: Vehicle) {
    val context = LocalContext.current
    val markerState = rememberUpdatedMarkerState(position = vehicle.latLng())

    // every time the vehicle changes, update the position of the marker
    LaunchedEffect(vehicle) {
        markerState.position = vehicle.latLng()
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
    onSettingsClick: () -> Unit,
    onScheduleClick: () -> Unit,
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
            ActionButton(icon = Icons.Outlined.Settings) {
                onSettingsClick()
            }
            ActionButton(icon = Icons.Outlined.Schedule) {
                onScheduleClick()
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
