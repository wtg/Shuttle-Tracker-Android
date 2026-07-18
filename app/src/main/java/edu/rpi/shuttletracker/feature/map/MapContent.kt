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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationDisabled
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
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
    onScheduleClick: () -> Unit,
    onToggleMapTypeClick: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hasLocationPermission =
        remember {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        }
    val cameraPositionState =
        rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(CampusCenter, 14.3f)
        }
    var selectedStop by remember { mutableStateOf<Stop?>(null) }
    val useDarkMap = uiState.themeMode.isDarkTheme(isSystemInDarkTheme())

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
            uiState.routes.values.forEach { route ->
                route.stopDetails.values.forEach { stop ->
                    StopMarker(
                        stop = stop,
                        selected = stop.name == selectedStop?.name,
                        onSelected = { selectedStop = it },
                    )
                }

                route.latLng().takeIf { it.isNotEmpty() }?.let { points ->
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

            uiState.vehicles.forEach { vehicle ->
                VehicleMarker(
                    vehicle = vehicle,
                    animationsEnabled = uiState.shuttleAnimationsEnabled,
                    rotationEnabled = uiState.shuttleRotationEnabled,
                )
            }
        }

        MapControls(
            modifier =
                Modifier
                    .padding(contentPadding)
                    .padding(horizontal = 10.dp),
            hasLocationPermission = hasLocationPermission,
            isNormalMapType = uiState.mapType == MapType.NORMAL,
            onSettingsClick = onSettingsClick,
            onScheduleClick = onScheduleClick,
            onRecenterClick = {
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
            onToggleMapTypeClick = onToggleMapTypeClick,
        )
    }
}

@Composable
private fun MapControls(
    modifier: Modifier,
    hasLocationPermission: Boolean,
    isNormalMapType: Boolean,
    onSettingsClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onRecenterClick: () -> Unit,
    onToggleMapTypeClick: () -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MapActionButton(
                icon = Icons.Outlined.Settings,
                onClick = onSettingsClick,
            )
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MapActionButton(
                icon =
                    if (hasLocationPermission) {
                        Icons.Outlined.MyLocation
                    } else {
                        Icons.Outlined.LocationDisabled
                    },
                onClick = onRecenterClick,
            )
            MapActionButton(
                icon = if (isNormalMapType) Icons.Outlined.Layers else Icons.Filled.Layers,
                onClick = onToggleMapTypeClick,
            )
        }

        FloatingActionButton(
            onClick = onScheduleClick,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = "Open schedule",
            )
        }
    }
}
