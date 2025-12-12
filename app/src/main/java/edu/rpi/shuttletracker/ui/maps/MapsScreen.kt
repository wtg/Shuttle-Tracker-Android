package edu.rpi.shuttletracker.ui.maps

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.hilt.navigation.compose.hiltViewModel
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
import edu.rpi.shuttletracker.ui.schedule.ScheduleScroll
import edu.rpi.shuttletracker.ui.theme.BusColors
import edu.rpi.shuttletracker.ui.util.CheckResponseError
import kotlinx.coroutines.launch
import java.util.Calendar

@Destination<RootGraph>(start = true)
@Composable
fun MapsScreen(
    navigator: DestinationsNavigator,
    viewModel: MapsViewModel = hiltViewModel(),
) {
    // makes sure the 2 flows are collected when ui is open
    val mapsUiState = viewModel.mapsUiState.collectAsStateWithLifecycle().value
    viewModel.busesState.collectAsStateWithLifecycle({})

    val snackbarHostState = remember { SnackbarHostState() }

    var bottomSheetLoaded by remember { mutableStateOf<Stop?>(null) }

    Scaffold(
        snackbarHost = {
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

            SnackbarHost(hostState = snackbarHostState)
        },
    ) { padding ->

        BusMap(
            mapsUIState = mapsUiState,
            padding = padding,
            bottomSheetOnChange = { bottomSheetLoaded = it },
            onScheduleClick = {
                navigator.navigate(ScheduleScreenDestination())
            },
            onSettingsClick = {
                navigator.navigate(SettingsScreenDestination())
            },
        )

        StopInfoBottomSheet(
            stop = bottomSheetLoaded,
            mapsUIState = mapsUiState,
            onDismiss = { bottomSheetLoaded = null },
        )
    }
}

/**
 * Creates the map displaying everything
 *
 * @param mapsUIState: The UI state of the view from the view-model
 * @param padding: Padding needed for the map content padding
 * @param bottonSheetOnChange: Callback invoked when a stop is selected/deselected
 * to close/open the stop bottom sheet
 * @param onScheduleClick: Callback invoked when the user taps the Schedule button.
 * @param onSettingsClick: Callback invoked when the user taps the Settings button.
 * */
@Composable
fun BusMap(
    mapsUIState: MapsUIState,
    padding: PaddingValues,
    bottomSheetOnChange: (Stop?) -> Unit,
    onScheduleClick: () -> Unit,
    onSettingsClick: () -> Unit,
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
    val cameraPositionState =
        rememberCameraPositionState {
            position =
                CameraPosition.fromLatLngZoom(
                    LatLng(42.73068146020498, -73.67619731950525),
                    14.3f,
                )
        }

    var selectedStop by remember { mutableStateOf<Stop?>(null) }
    val isDark = mapsUIState.themeMode.isDarkTheme(isSystemInDarkTheme())
    var mapType by remember { mutableStateOf(MapType.NORMAL) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            // makes sure the items drawn (current location and compass) are clickable
            contentPadding = padding,
            cameraPositionState = cameraPositionState,
            // auto dark theme
            properties =
                MapProperties(
                    latLngBoundsForCameraTarget =
                        LatLngBounds(
                            LatLng(42.72095724005504, -73.70196321825452),
                            LatLng(42.741173465236876, -73.6543446409232),
                        ),
                    mapType = mapType,
                    isBuildingEnabled = true,
                    minZoomPreference = 14f,
                    isMyLocationEnabled = mapLocationEnabled,
                    mapStyleOptions =
                        if (isDark) {
                            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_dark)
                        } else {
                            MapStyleOptions("[]")
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
            mapsUIState.routes.forEach { (_, route) ->
                route.stopDetails.forEach { (_, stop) ->
                    StopCircle(
                        stop = stop,
                        selected = stop.name == selectedStop?.name,
                        onSelected = { s ->
                            selectedStop = s
                            bottomSheetOnChange(s)
                        },
                    )
                }
            }

            // creates the bus markers
            mapsUIState.buses.forEach {
                BusMarker(
                    bus = it,
                    colorBlindMode = mapsUIState.colorBlindMode,
                )
            }

            // draws the paths
            mapsUIState.routes.forEach { (_, route) ->
                val points = route.latLng()
                if (points.isNotEmpty()) {
                    Polyline(
                        points = points,
                        color =
                            Color(
                                android.graphics.Color.valueOf(
                                    route.color.toColorInt(),
                                ).toArgb(),
                            ),
                    )
                }
            }
        }

        val mapTypeIcon =
            if (mapType == MapType.NORMAL) {
                Icons.Outlined.Layers
            } else {
                Icons.Filled.Layers
            }

        MapButtonsOverlay(
            modifier =
                Modifier
                    .padding(padding)
                    .padding(horizontal = 10.dp),
            mapLocationEnabled = mapLocationEnabled,
            mapTypeIcon = mapTypeIcon,
            onScheduleClick = onScheduleClick,
            onSettingsClick = onSettingsClick,
            onRecenterClick = {
                LocationServices.getFusedLocationProviderClient(context).lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location == null) return@addOnSuccessListener

                        coroutineScope.launch {
                            cameraPositionState.animate(
                                update =
                                    CameraUpdateFactory.newCameraPosition(
                                        CameraPosition.builder()
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
            onToggleMapTypeClick = {
                mapType =
                    if (mapType == MapType.NORMAL) {
                        MapType.HYBRID
                    } else {
                        MapType.NORMAL
                    }
            },
        )
    }
}

/**
 * Creates a marker for a stop
 * */
@Composable
private fun StopCircle(
    stop: Stop,
    selected: Boolean,
    onSelected: (Stop) -> Unit,
) {
    Circle(
        center = stop.latLng(),
        radius = 15.0,
        strokeColor = if (selected) Color(0xFF6699FF) else MaterialTheme.colorScheme.onPrimaryContainer,
        strokeWidth = 8f,
        zIndex = 1f,
        fillColor = Color.Transparent,
        clickable = true,
        onClick = {
            onSelected(stop)
            true
        },
    )
}

/**
 * Creates a marker for a bus
 * */
@Composable
fun BusMarker(
    bus: Bus,
    colorBlindMode: Boolean,
) {
    val context = LocalContext.current
    val markerState = rememberUpdatedMarkerState(position = bus.latLng())

    // every time bus changes, update the position of the marker
    LaunchedEffect(bus) {
        markerState.position = bus.latLng()
    }

    val busColor =
        when {
            colorBlindMode -> BusColors.Default
            bus.routeName == "NORTH" -> BusColors.North
            bus.routeName == "WEST" -> BusColors.West
            else -> BusColors.Default
        }

    val icon =
        remember(busColor) {
            getBusMarkerDescriptor(context, 25f, busColor.toArgb())
        }

    // gets bus speed and last time it updated
    val timeBusUpdate = bus.getTimeAgo().collectAsStateWithLifecycle(initialValue = "").value
    val snippetText =
        buildString {
            append(stringResource(R.string.bus_speed, bus.speedMph))
            if (timeBusUpdate.isNotBlank()) {
                append(" • ")
                append(timeBusUpdate)
            }
        }

    Marker(
        state = markerState,
        title = stringResource(R.string.bus_number, bus.id),
        icon = icon,
        snippet = snippetText,
        anchor = Offset(0.5f, 0.5f),
        onClick = {
            it.showInfoWindow()
            true
        },
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopInfoBottomSheet(
    stop: Stop?,
    mapsUIState: MapsUIState,
    onDismiss: () -> Unit,
) {
    if (stop == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val routes = mapsUIState.routes
    val schedule = mapsUIState.schedule

    val allowedRoutes = setOf("NORTH", "WEST")

    val routeNames =
        remember(stop, routes) {
            routes
                .filter { (routeName, route) ->
                    routeName in allowedRoutes &&
                        route.stopDetails.values.any { it.name == stop.name }
                }
                .keys
                .sorted()
        }

    val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val todayName = remember { days[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1] }
//    var selectedDay by remember { mutableStateOf(todayName) }
    val dayIndex = remember { Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 }

    var selectedRoute by remember(routeNames) { mutableStateOf(routeNames.firstOrNull()) }

    val selectedRouteTimes: List<String> =
        remember(todayName, selectedRoute, schedule) {
            val routeSchedule = schedule.getOrNull(dayIndex)
            when (selectedRoute) {
                "NORTH" -> routeSchedule?.north ?: emptyList()
                "WEST" -> routeSchedule?.west ?: emptyList()
                else -> emptyList()
            }
        }

    ModalBottomSheet(
        modifier = Modifier.fillMaxHeight(),
        sheetState = sheetState,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth(0.7f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    stop.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 5.dp),
                )

                if (routeNames.size > 1) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        routeNames.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = selectedRoute == option,
                                onClick = { selectedRoute = option },
                                shape = SegmentedButtonDefaults.itemShape(index, routeNames.size),
                                label = { Text(option) },
                            )
                        }
                    }
                }

                ScheduleScroll(
                    selectedRouteTimes = selectedRouteTimes,
                    selectedStop = stop.name,
                    routeData = routes[selectedRoute],
                    centered = true,
                )
            }
        }
    }
}

@Composable
fun MapButtonsOverlay(
    modifier: Modifier = Modifier,
    mapLocationEnabled: Boolean,
    mapTypeIcon: ImageVector,
    onScheduleClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRecenterClick: () -> Unit,
    onToggleMapTypeClick: () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        // Left side (Schedule, Settings)
        Column(
            modifier =
                Modifier
                    .align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ActionButton(icon = Icons.Outlined.Schedule) {
                onScheduleClick()
            }

            ActionButton(icon = Icons.Outlined.Settings) {
                onSettingsClick()
            }
        }

        // Right side (Location, Layers)
        Column(
            modifier =
                Modifier
                    .align(Alignment.TopEnd),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ActionButton(
                icon =
                    if (mapLocationEnabled) {
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
