package edu.rpi.shuttletracker.ui.maps

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationDisabled
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.ramcosta.composedestinations.generated.destinations.ScheduleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.ui.theme.BusColors
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

    val sheetState =
        rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
        )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 160.dp,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetShadowElevation = 10.dp,
        sheetContent = {
            val showDetails by remember(scaffoldState.bottomSheetState) {
                derivedStateOf {
                    scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded ||
                        scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
                }
            }

            ScheduleSheetContent(
                schedule = mapsUiState.schedule,
                showDetails = showDetails,
            )
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

        ShuttleMap(
            mapsUiState = mapsUiState,
            padding = padding,
            onScheduleClick = { navigator.navigate(ScheduleScreenDestination()) },
            onSettingsClick = { navigator.navigate(SettingsScreenDestination()) },
            onToggleMapTypeClick = { viewModel.toggleMapType() },
        )
    }
}

/**
 * Creates the map displaying everything
 *
 * @param mapsUiState: The UI state of the view from the view-model
 * @param padding: Padding needed for the map content padding
 * to close/open the stop bottom sheet
 * @param onScheduleClick: Callback invoked when the user taps the Schedule button
 * @param onSettingsClick: Callback invoked when the user taps the Settings button
 * @param onToggleMapTypeClick: Callback invoked when user taps the MapType button
 *
 * */
@Composable
private fun ShuttleMap(
    mapsUiState: MapsUiState,
    padding: PaddingValues,
    onScheduleClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onToggleMapTypeClick: () -> Unit,
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

    // keeps track of where the camera currently is
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
                route.stopDetails.forEach { (_, stop) ->
                    StopMarker(
                        stop = stop,
                        selected = stop.name == selectedStop?.name,
                        onSelected = { selectedStop = it },
                    )
                }
            }

            // creates the bus markers
            mapsUiState.buses.values.forEach {
                BusMarker(
                    bus = it,
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
                    .statusBarsPadding()
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
 * Creates a marker for a bus
 * */
@Composable
private fun BusMarker(bus: Bus) {
    val context = LocalContext.current
    val markerState = rememberUpdatedMarkerState(position = bus.latLng())

    // every time bus changes, update the position of the marker
    LaunchedEffect(bus) {
        markerState.position = bus.latLng()
    }

    val busColor =
        when (bus.routeName) {
            "NORTH" -> BusColors.North
            "WEST" -> BusColors.West
            else -> BusColors.Default
        }

    val icon =
        remember(busColor) {
            getBusMarkerDescriptor(context, 25f, busColor.toArgb())
        }

    // gets bus speed and last time it updated
    val lastUpdatedAgoText = bus.getTimeAgo().collectAsStateWithLifecycle(initialValue = "").value
    val snippetText =
        buildString {
            append(stringResource(R.string.bus_speed, bus.speedMph))
            if (lastUpdatedAgoText.isNotBlank()) {
                append(" • ")
                append(lastUpdatedAgoText)
            }
        }

    Marker(
        state = markerState,
        title = stringResource(R.string.bus_number, bus.name),
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
