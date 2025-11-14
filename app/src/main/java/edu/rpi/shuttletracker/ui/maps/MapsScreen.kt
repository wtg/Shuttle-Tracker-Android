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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationDisabled
import androidx.compose.material.icons.outlined.MyLocation
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
import com.ramcosta.composedestinations.generated.destinations.SetupScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Bus
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.ui.schedule.ScheduleScroll
import edu.rpi.shuttletracker.ui.theme.BusColors
import edu.rpi.shuttletracker.ui.util.CheckResponseError
import edu.rpi.shuttletracker.util.services.BeaconService
import edu.rpi.shuttletracker.util.services.LocationService
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
    viewModel.runningBusesState.collectAsStateWithLifecycle({})

    val snackbarHostState = remember { SnackbarHostState() }

    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    var bottomSheetLoaded by remember { mutableStateOf<Stop?>(null) }

    val errorStartingBeaconService = BeaconService.permissionError.collectAsStateWithLifecycle().value
    val errorStartingLocationService = LocationService.permissionError.collectAsStateWithLifecycle().value

    // shows a snackbar whenever the service isn't able to run, usually because of lack of permissions
    LaunchedEffect(errorStartingBeaconService, errorStartingLocationService) {
        if (errorStartingBeaconService || errorStartingLocationService) {
            coroutineScope.launch {
                val result =
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.service_missing_permissions),
                        actionLabel = context.getString(R.string.fix),
                        duration = SnackbarDuration.Long,
                    )
                when (result) {
                    SnackbarResult.ActionPerformed -> {
                        navigator.navigate(SetupScreenDestination())
                    }

                    SnackbarResult.Dismissed -> { // IGNORED
                    }
                }
            }
        }
    }

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
//        floatingActionButton = {
//            Column(horizontalAlignment = Alignment.End) {
//                RefreshFab {
//                    viewModel.refreshRunningBusses()
//                    viewModel.loadAll()
//                }
//                BoardBusFab(
//                    mapsUiState.allBuses,
//                    viewModel::closestDistanceToStop,
//                    mapsUiState.minStopDist,
//                    viewModel::leaveBusPressed,
//                    viewModel::boardBusPressed,
//                    viewModel::busSelectionCanceled,
//                )
//            }
//        },
    ) { padding ->

        BusMap(mapsUIState = mapsUiState, padding = padding, bottomSheetOnChange = {
            bottomSheetLoaded = it
        })

        StopInfoBottomSheet(
            stop = bottomSheetLoaded,
            mapsUIState = mapsUiState,
            onDismiss = { bottomSheetLoaded = null },
        )

        Box(
            modifier =
                Modifier
                    .padding(padding)
                    .padding(horizontal = 10.dp)
                    .fillMaxSize(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // navigates to announcements
//                ActionButton(
//                    icon = Icons.Outlined.Notifications,
//                    badgeCount = mapsUiState.totalAnnouncements - mapsUiState.notificationsRead,
//                ) {
//                    navigator.navigate(AnnouncementsScreenDestination())
//                }

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
    bottomSheetOnChange: (Stop?) -> Unit,
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
        mapsUIState.runningBuses.forEach {
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

    // Icon to recenter the user on the map to their location
    // makes sure its in the top right
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 10.dp),
        contentAlignment = Alignment.TopEnd,
    ) {
        Column(
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
                // finds current position and moves to there
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
            }
            ActionButton(icon = Icons.Outlined.Layers) {
                mapType =
                    if (mapType == MapType.NORMAL) {
                        MapType.HYBRID
                    } else {
                        MapType.NORMAL
                    }
            }
        }
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
 * The Floating Action Button for boarding the bus
 * */
@SuppressLint("MissingPermission") // permissions checked in external composable
@Composable
fun BoardBusFab(
    buses: List<Int>,
    checkDistanceToStop: (location: Location) -> Float,
    minStopDist: Float,
    leaveBusPressed: () -> Unit,
    boardBusPressed: () -> Unit,
    busSectionCanceled: () -> Unit,
) {
    val locationServiceBusNumber = LocationService.busNum.collectAsStateWithLifecycle().value
    val context = LocalContext.current

    var busPickerState by remember { mutableStateOf(false) }

    // launches the bus picker dialog
    if (busPickerState) {
        BusPicker(
            buses = buses,
            onBusChosen = {
                val intent =
                    Intent(context, LocationService::class.java).apply {
                        putExtra(LocationService.BUNDLE_BUS_ID, it)
                    }
                context.startForegroundService(intent)
                boardBusPressed()
            },
            onDismiss = {
                busPickerState = false
                busSectionCanceled()
            },
        )
    }

    ExtendedFloatingActionButton(
        onClick = {
            if (locationServiceBusNumber != null) {
                context.stopService(Intent(context, LocationService::class.java))
                leaveBusPressed()
            } else {
                LocationServices.getFusedLocationProviderClient(context).lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location == null) return@addOnSuccessListener

                        // if they a location was found and they are 50 m away from a stop
                        if (checkDistanceToStop(location) <= minStopDist) {
                            busPickerState = true
                        } else {
                            // not close enough to a stop
                            Toast.makeText(
                                context,
                                context.getString(
                                    R.string.distance_warning,
//                                    minStopDist.toInt(),
                                ),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                    .addOnFailureListener {
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
                text =
                    if (locationServiceBusNumber != null) {
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
fun RefreshFab(refresh: () -> Unit) {
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
            modifier =
                Modifier
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
                    modifier =
                        Modifier
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
    var selectedDay by remember { mutableStateOf(todayName) }
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
