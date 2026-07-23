package edu.rpi.shuttletracker.feature.map

import android.Manifest
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.graphics.toColorInt
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
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
import com.google.android.gms.maps.GoogleMap as AndroidGoogleMap

private val CampusCenter = LatLng(42.73068146020498, -73.67619731950525)
private val CampusBounds =
    LatLngBounds(
        LatLng(42.72095724005504, -73.70196321825452),
        LatLng(42.741173465236876, -73.6543446409232),
    )
private const val TILTED_DEGREES = 60f

// Matches the classic Google-Maps "blue dot" user-location marker color.
private val LocationDotColor = Color(0xFF4285F4)

/**
 * Mirrors the stock Google Maps app's location FAB: [NotFollowing] until tapped, then
 * [Following] the user north-up, then [FollowingTilted] into a 3D perspective on a second tap.
 * Any user-driven camera gesture drops back to [NotFollowing] (see the `MapEffect` in
 * [ShuttleMap]) so the button never claims to be following a camera the user just took over.
 * */
private enum class LocationFollowMode {
    NotFollowing,
    Following,
    FollowingTilted,
}

/**
 * The actual Google Map: draws stops ([StopMarker]), route polylines, and vehicles
 * ([VehicleMarker] - both real and, in dev mode, fake), plus the overlaid announcement strip,
 * settings/map-type buttons, and a recenter FAB. Used by [MapsScreen]'s Map tab.
 * */
@Composable
internal fun ShuttleMap(
    uiState: MapsUiState,
    contentPadding: PaddingValues,
    onSettingsClick: () -> Unit,
    onToggleMapTypeClick: () -> Unit,
    onAnnouncementsClick: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hasLocationPermission =
        remember {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ).any { permission ->
                ActivityCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
            }
        }
    val cameraPositionState =
        rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(CampusCenter, 14.3f)
        }
    var selectedStop by remember { mutableStateOf<Stop?>(null) }
    var isDevPanelOpen by remember { mutableStateOf(false) }
    var selectedDevVehicleId by remember { mutableStateOf<String?>(null) }
    var followMode by remember { mutableStateOf(LocationFollowMode.NotFollowing) }
    val useDarkMap = uiState.themeMode.isDarkTheme(isSystemInDarkTheme())
    val fallbackRouteColor = MaterialTheme.colorScheme.primary

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
            // A user-initiated pan/zoom means they've taken the camera back over, so the FAB
            // shouldn't keep claiming to follow them - only REASON_GESTURE resets this, not our
            // own recenter/tilt animations below (REASON_API_ANIMATION/DEVELOPER_ANIMATION).
            MapEffect(Unit) { map ->
                map.setOnCameraMoveStartedListener { reason ->
                    if (reason == AndroidGoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                        followMode = LocationFollowMode.NotFollowing
                    }
                }
            }

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

            // Kept in a separate loop over its own uiState field so developer-mode fake shuttles
            // never mix with real vehicle data from the API.
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

        // Announcements are status info and belong at the very top, first thing seen; settings
        // and map-type are low-frequency controls that sit below in their own full-width row so
        // they never collide structurally with the strip above them.
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

            // A Box with independently-aligned children, not a Row with Arrangement.SpaceBetween -
            // the chip can disappear/reappear on its own schedule, and SpaceBetween would shove the
            // FAB stack over to the start edge whenever it's the Row's only remaining child.
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
                            CameraUpdateFactory.newLatLngZoom(vehicle.latLng(), 17f),
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

        // Schedule is reached via the bottom nav bar now, so Recenter is the map's only FAB.
        val (fabContainerColor, fabContentColor) = mapButtonColors()
        FloatingActionButton(
            onClick = handleClick@{
                if (!hasLocationPermission) return@handleClick

                when (followMode) {
                    LocationFollowMode.NotFollowing -> {
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
                    }

                    // Already centered north-up: tilt into a 3D perspective, like the stock app's
                    // compass button does on a second tap.
                    LocationFollowMode.Following -> {
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition(
                                        cameraPositionState.position.target,
                                        cameraPositionState.position.zoom,
                                        TILTED_DEGREES,
                                        cameraPositionState.position.bearing,
                                    ),
                                ),
                                durationMs = 500,
                            )
                        }
                        followMode = LocationFollowMode.FollowingTilted
                    }

                    // Tilted: flatten back to north-up rather than dropping out of follow mode -
                    // only an actual map drag (the MapEffect above) should do that.
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
                            followMode == LocationFollowMode.NotFollowing -> R.drawable.ic_location_dot
                            else -> R.drawable.ic_explore
                        },
                    ),
                tint =
                    if (hasLocationPermission && followMode == LocationFollowMode.NotFollowing) {
                        LocationDotColor
                    } else {
                        fabContentColor
                    },
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
