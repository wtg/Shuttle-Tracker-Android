package edu.rpi.shuttletracker.feature.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.feature.map.components.AnnouncementStrip
import edu.rpi.shuttletracker.feature.map.components.DeveloperVehicleView
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.LocationIndicatorLayer
import org.maplibre.compose.location.rememberLocationState
import org.maplibre.compose.map.CameraConstraints
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.StyleLoadState
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.overlay.AttributionLinks
import org.maplibre.compose.overlay.ExpandingAttributionButton
import org.maplibre.compose.overlay.MaplibreLogo
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position

private val CampusCenter = Position(longitude = -73.67619731950525, latitude = 42.73068146020498)
private val CampusBounds =
    BoundingBox(
        west = -73.70196321825452,
        south = 42.72095724005504,
        east = -73.6543446409232,
        north = 42.741173465236876,
    )

// MapLibre uses 512-dp tiles.
private const val VEHICLE_FOCUS_ZOOM = 16.0

/** Location button states: free camera, north-up follow, and tilted follow. */
private enum class LocationFollowMode { NotFollowing, Following, FollowingTilted }

@Composable
internal fun ShuttleMap(
    uiState: MapsUiState,
    contentPadding: PaddingValues,
    onSettingsClick: () -> Unit,
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
    val locationState = rememberLocationState(enabled = hasLocationPermission)
    var selectedStop by remember { mutableStateOf<Stop?>(null) }
    var selectedVehicleId by remember { mutableStateOf<String?>(null) }
    var isDevPanelOpen by remember { mutableStateOf(false) }
    var followMode by remember { mutableStateOf(LocationFollowMode.NotFollowing) }
    val useDarkMap = uiState.themeMode.isDarkTheme(isSystemInDarkTheme())
    var styleAttempt by remember(useDarkMap) { mutableIntStateOf(0) }
    val styleUrl = openFreeMapStyle(useDarkMap) + if (styleAttempt == 0) "" else "?retry=$styleAttempt"
    val fallbackRouteColor = MaterialTheme.colorScheme.primary
    val stopColor = MaterialTheme.colorScheme.onPrimaryContainer
    val vehicles = uiState.vehicles + uiState.fakeVehicles
    val vehiclePositions =
        vehicles.associate { vehicle ->
            vehicle.id to key(vehicle.id) { rememberVehiclePosition(vehicle, uiState.shuttleAnimationsEnabled) }
        }
    val uniqueStops =
        remember(uiState.routes) {
            uiState.routes.values
                .flatMap { it.stopDetails.values }
                .filter { it.coordinates.size >= 2 && it.coordinates.take(2).all(Double::isFinite) }
                .distinctBy { it.coordinates.take(2) }
        }
    val mapState =
        rememberMapState(
            baseStyle = BaseStyle.Uri(styleUrl),
            initialCameraPosition = CameraPosition(center = CampusCenter, zoom = 13.3),
        ) {
            uiState.routes.forEach { (routeId, route) ->
                key(routeId) {
                    val geometry = remember(route.coordinates) { route.mapGeometry() }
                    if (geometry.coordinates.isNotEmpty()) {
                        val source = rememberGeoJsonSource(GeoJsonData.Features(geometry))
                        LineLayer(
                            id = "route-$routeId",
                            source = source,
                            color = const(route.color.toComposeColorOrNull() ?: fallbackRouteColor),
                            width = const(3.dp),
                        )
                    }
                }
            }
            uniqueStops.forEachIndexed { index, stop ->
                key(stop.coordinates.take(2)) {
                    StopMarker(
                        id = "stop-$index",
                        stop = stop,
                        color = stopColor,
                        selected = stop == selectedStop,
                        onSelected = {
                            selectedStop = it
                            selectedVehicleId = null
                        },
                    )
                }
            }
            if (hasLocationPermission) LocationIndicatorLayer(id = "user-location", locationState = locationState)
            vehicles.forEach { vehicle ->
                key(vehicle.id) {
                    VehicleMarker(
                        vehicle = vehicle,
                        position = vehiclePositions.getValue(vehicle.id),
                        rotationEnabled = uiState.shuttleRotationEnabled,
                        onSelected = {
                            selectedVehicleId = vehicle.id
                            selectedStop = null
                        },
                    )
                }
            }
        }

    LaunchedEffect(focusedVehicleId) {
        val vehicleId = focusedVehicleId ?: return@LaunchedEffect
        val vehicle = vehicles.firstOrNull { it.id == vehicleId }
        if (vehicle != null) {
            selectedVehicleId = null
            withFrameNanos {}
            selectedVehicleId = vehicle.id
            selectedStop = null
            followMode = LocationFollowMode.NotFollowing
            mapState.animateCamera(CameraUpdate(center = vehicle.mapPosition(), zoom = VEHICLE_FOCUS_ZOOM))
        }
        onFocusedVehicleHandled()
    }
    LaunchedEffect(locationState.lastLocation?.position, followMode, hasLocationPermission) {
        if (!hasLocationPermission) followMode = LocationFollowMode.NotFollowing
        if (followMode == LocationFollowMode.NotFollowing) return@LaunchedEffect
        val position = locationState.lastLocation?.position ?: return@LaunchedEffect
        mapState.animateCamera(
            CameraUpdate(
                center = position,
                pitch = if (followMode == LocationFollowMode.FollowingTilted) 60.0 else 0.0,
                bearing = 0.0,
                zoom =
                    if (followMode ==
                        LocationFollowMode.FollowingTilted
                    ) {
                        maxOf(mapState.cameraPosition.zoom, 17.0)
                    } else {
                        null
                    },
            ),
        )
    }

    Box(Modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            state = mapState,
            viewportInsets = contentPadding,
            cameraConstraints =
                CameraConstraints {
                    minZoom = 13.0
                    boundingBox = CampusBounds
                },
            interactions =
                MapInteractions {
                    camera { pan { onStart { followMode = LocationFollowMode.NotFollowing } } }
                    callbacks {
                        click {
                            onUnhandled {
                                selectedStop = null
                                selectedVehicleId = null
                                ClickResult.Pass
                            }
                        }
                    }
                },
        ) {
            Row(
                modifier =
                    Modifier
                        .align(
                            Alignment.BottomStart,
                        ).padding(contentPadding)
                        .padding(start = 8.dp, end = 80.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MaplibreLogo()
                ExpandingAttributionButton(
                    contentAlignment = Alignment.BottomStart,
                    expandedContent = { attributions, textStyle ->
                        AttributionLinks(attributions, textStyle = textStyle, breakWithinAttribution = true)
                    },
                )
            }
            selectedStop?.let { stop ->
                MapInfoCard(
                    title = stop.name,
                    modifier = Modifier.placedAt(stop.mapPosition(), Alignment.BottomCenter).padding(bottom = 20.dp),
                )
            }
            vehicles.firstOrNull { it.id == selectedVehicleId }?.let { vehicle ->
                VehicleInfoCard(
                    vehicle = vehicle,
                    modifier =
                        Modifier
                            .placedAt(
                                vehiclePositions.getValue(vehicle.id),
                                Alignment.BottomCenter,
                            ).padding(bottom = 20.dp),
                )
            }
        }

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
            if (mapState.style.loadState is StyleLoadState.Failed) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.errorContainer) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.map_load_failed), modifier = Modifier.weight(1f))
                        TextButton(onClick = { styleAttempt++ }) { Text(stringResource(R.string.retry)) }
                    }
                }
            }
            Box(Modifier.fillMaxWidth()) {
                LastUpdatedChip(updatedAt = uiState.vehiclesUpdatedAt, modifier = Modifier.align(Alignment.TopStart))
                Column(modifier = Modifier.align(Alignment.TopEnd), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MapActionButton(
                        icon = R.drawable.ic_settings,
                        contentDescription = stringResource(R.string.map_open_settings),
                        onClick = onSettingsClick,
                    )
                    if (uiState.isDevModeEnabled) {
                        MapActionButton(
                            icon = R.drawable.ic_bug_report,
                            contentDescription =
                                stringResource(
                                    R.string.dev_shuttle_inspector,
                                ),
                            onClick = {
                                isDevPanelOpen =
                                    !isDevPanelOpen
                            },
                        )
                    }
                }
            }
        }
        if (uiState.isDevModeEnabled && isDevPanelOpen) {
            DeveloperVehicleView(
                vehicles = vehicles,
                onClose = { isDevPanelOpen = false },
                onZoomToVehicle = { vehicle ->
                    selectedVehicleId = vehicle.id
                    selectedStop = null
                    followMode = LocationFollowMode.NotFollowing
                    coroutineScope.launch {
                        mapState.animateCamera(CameraUpdate(center = vehicle.mapPosition(), zoom = VEHICLE_FOCUS_ZOOM))
                    }
                },
                modifier = Modifier.align(Alignment.CenterStart).padding(contentPadding).padding(16.dp),
            )
        }
        val (fabContainerColor, fabContentColor) = mapButtonColors()
        FloatingActionButton(
            onClick = {
                if (hasLocationPermission) {
                    followMode =
                        when (followMode) {
                            LocationFollowMode.NotFollowing -> LocationFollowMode.Following
                            LocationFollowMode.Following -> LocationFollowMode.FollowingTilted
                            LocationFollowMode.FollowingTilted -> LocationFollowMode.Following
                        }
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(contentPadding).padding(16.dp),
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

internal fun openFreeMapStyle(dark: Boolean): String =
    "https://tiles.openfreemap.org/styles/" + if (dark) "dark" else "liberty"

private fun String.toComposeColorOrNull(): Color? = runCatching { Color(toColorInt()) }.getOrNull()

private fun Context.hasLocationPermission(): Boolean =
    listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ).any { ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
