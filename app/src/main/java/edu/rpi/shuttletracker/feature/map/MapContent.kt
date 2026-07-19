package edu.rpi.shuttletracker.feature.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
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
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.feature.map.components.AnnouncementStrip
import kotlinx.coroutines.launch

private val CampusCenter = LatLng(42.73068146020498, -73.67619731950525)
private val CampusBounds =
    LatLngBounds(
        LatLng(42.72095724005504, -73.70196321825452),
        LatLng(42.741173465236876, -73.6543446409232),
    )

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                }
            }
        }

        // Schedule is reached via the bottom nav bar now, so Recenter is the map's only FAB.
        FloatingActionButton(
            onClick = recenter@{
                if (!hasLocationPermission) return@recenter

                LocationServices
                    .getFusedLocationProviderClient(context)
                    .lastLocation
                    .addOnSuccessListener { location: Location? ->
                        location ?: return@addOnSuccessListener
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(location.latitude, location.longitude),
                                    cameraPositionState.position.zoom,
                                ),
                                durationMs = 1000,
                            )
                        }
                    }
            },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(contentPadding)
                    .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Icon(
                painter =
                    painterResource(
                        if (hasLocationPermission) {
                            R.drawable.ic_my_location
                        } else {
                            R.drawable.ic_location_disabled
                        },
                    ),
                contentDescription =
                    stringResource(
                        if (hasLocationPermission) {
                            R.string.map_recenter
                        } else {
                            R.string.map_location_unavailable
                        },
                    ),
            )
        }
    }
}

private fun String.toComposeColorOrNull(): Color? = runCatching { Color(toColorInt()) }.getOrNull()
