package edu.rpi.shuttletracker.feature.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.feature.map.components.AnnouncementStrip
import edu.rpi.shuttletracker.feature.map.components.DeveloperVehicleView
import kotlinx.coroutines.launch

private val CampusCenter = LatLng(42.73068146020498, -73.67619731950525)
private val CampusBounds =
    LatLngBounds(
        LatLng(42.72095724005504, -73.70196321825452),
        LatLng(42.741173465236876, -73.6543446409232),
    )
private const val TILTED_DEGREES = 60f
private const val TILT_ZOOM = 18f

// Ignore small target drift caused by zooming or rotating while following the user.
private const val PAN_DETECTION_THRESHOLD_METERS = 20f
private const val VEHICLE_FOCUS_ZOOM = 17f

/** Location button states: free camera, north-up follow, and tilted follow. */
private enum class LocationFollowMode {
    NotFollowing,
    Following,
    FollowingTilted,
}

/** Draws routes, stops, vehicles, announcements, and map controls. */
@Composable
internal fun ShuttleMap(
    uiState: MapsUiState,
    contentPadding: PaddingValues,
    onSettingsClick: () -> Unit,
    onToggleMapTypeClick: () -> Unit,
    onAnnouncementsClick: () -> Unit,
    focusedVehicleId: String?,
    onFocusedVehicleHandled: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var hasLocationPermission by remember(context) { mutableStateOf(context.hasLocationPermission()) }
    LifecycleResumeEffect(context) {
        hasLocationPermission = context.hasLocationPermission()
        onPauseOrDispose {}
    }
    val cameraPositionState =
        rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(CampusCenter, 14.3f)
        }
    var selectedStop by remember { mutableStateOf<Stop?>(null) }
    var isDevPanelOpen by remember { mutableStateOf(false) }
    var selectedDevVehicleId by remember { mutableStateOf<String?>(null) }
    var followMode by remember { mutableStateOf(LocationFollowMode.NotFollowing) }
    var gestureStartTarget by remember { mutableStateOf<LatLng?>(null) }
    val useDarkMap = uiState.themeMode.isDarkTheme(isSystemInDarkTheme())
    val fallbackRouteColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(focusedVehicleId) {
        val vehicleId = focusedVehicleId ?: return@LaunchedEffect
        val vehicle =
            (uiState.vehicles + uiState.fakeVehicles).firstOrNull { it.id == vehicleId }
                ?: run {
                    onFocusedVehicleHandled()
                    return@LaunchedEffect
                }

        // Drop selection for one frame so repeatedly choosing the same shuttle reopens its window.
        selectedDevVehicleId = null
        withFrameNanos {}
        selectedDevVehicleId = vehicle.id
        followMode = LocationFollowMode.NotFollowing
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(vehicle.latLng(), VEHICLE_FOCUS_ZOOM),
            durationMs = 1000,
        )
        onFocusedVehicleHandled()
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving) {
            if (cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
                gestureStartTarget = cameraPositionState.position.target
            }
            return@LaunchedEffect
        }

        val start = gestureStartTarget ?: return@LaunchedEffect
        gestureStartTarget = null
        val end = cameraPositionState.position.target
        val distanceMeters = FloatArray(1)
        Location.distanceBetween(
            start.latitude,
            start.longitude,
            end.latitude,
            end.longitude,
            distanceMeters,
        )
        if (distanceMeters[0] > PAN_DETECTION_THRESHOLD_METERS) {
            followMode = LocationFollowMode.NotFollowing
        }
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            cameraPositionState = cameraPositionState,
            properties =
                MapProperties(
                    latLngBoundsForCameraTarget = CampusBounds,
                    mapType = uiState.mapType,
                    isBuildingEnabled = true,
                    minZoomPreference = 14f,
                    isMyLocationEnabled = hasLocationPermission,
                    mapStyleOptions =
                        if (useDarkMap) {
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
            val uniqueStops =
                uiState.routes.values
                    .flatMap { it.stopDetails.values }
                    .filter { it.coordinates.size >= 2 }
                    .distinctBy { it.coordinates.take(2) }

            uniqueStops.forEach { stop ->
                key(stop.coordinates.take(2)) {
                    StopMarker(
                        stop = stop,
                        selected = stop.name == selectedStop?.name,
                        onSelected = { selectedStop = it },
                    )
                }
            }

            uiState.routes.values.forEach { route ->
                route.latLng().takeIf { it.isNotEmpty() }?.let { points ->
                    Polyline(
                        points = points,
                        color = route.color.toComposeColorOrNull() ?: fallbackRouteColor,
                    )
                }
            }

            uiState.vehicles.forEach { vehicle ->
                key(vehicle.id) {
                    VehicleMarker(
                        vehicle = vehicle,
                        animationsEnabled = uiState.shuttleAnimationsEnabled,
                        rotationEnabled = uiState.shuttleRotationEnabled,
                        selected = vehicle.id == selectedDevVehicleId,
                    )
                }
            }

            uiState.fakeVehicles.forEach { vehicle ->
                key(vehicle.id) {
                    VehicleMarker(
                        vehicle = vehicle,
                        animationsEnabled = uiState.shuttleAnimationsEnabled,
                        rotationEnabled = uiState.shuttleRotationEnabled,
                        selected = vehicle.id == selectedDevVehicleId,
                    )
                }
            }
        }

        // Keep status information above the lower-priority map controls.
        Column(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(contentPadding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnnouncementStrip(
                announcements = uiState.announcements,
                onClick = onAnnouncementsClick,
                modifier = Modifier.fillMaxWidth(),
            )

            // Independent alignment keeps controls fixed when the update chip disappears.
            Box(modifier = Modifier.fillMaxWidth()) {
                LastUpdatedChip(
                    updatedAt = uiState.vehiclesUpdatedAt,
                    modifier = Modifier.align(Alignment.TopStart),
                )

                Column(
                    modifier = Modifier.align(Alignment.TopEnd),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MapActionButton(
                        icon = R.drawable.ic_settings,
                        contentDescription = stringResource(R.string.map_open_settings),
                        onClick = onSettingsClick,
                    )
                    MapActionButton(
                        icon =
                            if (uiState.mapType == MapType.NORMAL) {
                                R.drawable.ic_layers
                            } else {
                                R.drawable.ic_layers_filled
                            },
                        contentDescription = stringResource(R.string.map_toggle_type),
                        onClick = onToggleMapTypeClick,
                    )
                    if (uiState.isDevModeEnabled) {
                        MapActionButton(
                            icon = R.drawable.ic_bug_report,
                            contentDescription = stringResource(R.string.dev_shuttle_inspector),
                            onClick = { isDevPanelOpen = !isDevPanelOpen },
                        )
                    }
                }
            }
        }

        if (uiState.isDevModeEnabled && isDevPanelOpen) {
            DeveloperVehicleView(
                vehicles = uiState.vehicles + uiState.fakeVehicles,
                onClose = { isDevPanelOpen = false },
                onZoomToVehicle = { vehicle ->
                    selectedDevVehicleId = vehicle.id
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(vehicle.latLng(), VEHICLE_FOCUS_ZOOM),
                            durationMs = 1000,
                        )
                    }
                },
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(contentPadding)
                        .padding(16.dp),
            )
        }

        val (fabContainerColor, fabContentColor) = mapButtonColors()
        FloatingActionButton(
            onClick = handleClick@{
                if (!hasLocationPermission) return@handleClick

                when (followMode) {
                    LocationFollowMode.NotFollowing -> {
                        try {
                            LocationServices
                                .getFusedLocationProviderClient(context)
                                .lastLocation
                                .addOnSuccessListener { location: Location? ->
                                    location ?: return@addOnSuccessListener
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newCameraPosition(
                                                CameraPosition(
                                                    LatLng(location.latitude, location.longitude),
                                                    cameraPositionState.position.zoom,
                                                    0f,
                                                    0f,
                                                ),
                                            ),
                                            durationMs = 1000,
                                        )
                                    }
                                    followMode = LocationFollowMode.Following
                                }
                        } catch (_: SecurityException) {
                            hasLocationPermission = false
                        }
                    }

                    // A second tap switches north-up following to a tilted view.
                    LocationFollowMode.Following -> {
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition(
                                        cameraPositionState.position.target,
                                        maxOf(cameraPositionState.position.zoom, TILT_ZOOM),
                                        TILTED_DEGREES,
                                        cameraPositionState.position.bearing,
                                    ),
                                ),
                                durationMs = 500,
                            )
                        }
                        followMode = LocationFollowMode.FollowingTilted
                    }

                    // Flatten the view without leaving follow mode.
                    LocationFollowMode.FollowingTilted -> {
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition(
                                        cameraPositionState.position.target,
                                        cameraPositionState.position.zoom,
                                        0f,
                                        cameraPositionState.position.bearing,
                                    ),
                                ),
                                durationMs = 500,
                            )
                        }
                        followMode = LocationFollowMode.Following
                    }
                }
            },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(contentPadding)
                    .padding(16.dp),
            containerColor = fabContainerColor,
            contentColor = fabContentColor,
        ) {
            Icon(
                painter =
                    painterResource(
                        when {
                            !hasLocationPermission -> R.drawable.ic_location_disabled
                            followMode == LocationFollowMode.NotFollowing -> R.drawable.ic_near_me
                            followMode == LocationFollowMode.Following -> R.drawable.ic_near_me_filled
                            else -> R.drawable.ic_navigation_filled
                        },
                    ),
                contentDescription =
                    stringResource(
                        when {
                            !hasLocationPermission -> R.string.map_location_unavailable
                            followMode == LocationFollowMode.NotFollowing -> R.string.map_recenter
                            followMode == LocationFollowMode.Following -> R.string.map_following
                            else -> R.string.map_following_tilted
                        },
                    ),
            )
        }
    }
}

private fun String.toComposeColorOrNull(): Color? = runCatching { Color(toColorInt()) }.getOrNull()

private fun Context.hasLocationPermission(): Boolean =
    listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ).any { permission ->
        ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
