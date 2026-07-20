package edu.rpi.shuttletracker.feature.map

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberUpdatedMarkerState
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.core.ui.theme.VehicleColors
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.models.Vehicle
import edu.rpi.shuttletracker.feature.map.components.getVehicleMarkerDescriptor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A stop's circle on the map. The actual [Marker] is invisible (`alpha = 0f`) - it only exists to catch taps and show the stop's name in an info window. */
@Composable
internal fun StopMarker(
    stop: Stop,
    selected: Boolean,
    onSelected: (Stop) -> Unit,
) {
    val markerState = rememberUpdatedMarkerState(position = stop.latLng())

    Circle(
        center = stop.latLng(),
        radius = 15.0,
        strokeColor = if (selected) Color(0xFF6699FF) else MaterialTheme.colorScheme.onPrimaryContainer,
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
 * A shuttle's marker, colored by [Vehicle.routeName] (falls back to the last known color for a
 * while if the route briefly drops out, so the marker doesn't flash gray). Works for real vehicles
 * and fake ones alike - both are just [Vehicle] instances.
 * */
@Composable
internal fun VehicleMarker(
    vehicle: Vehicle,
    animationsEnabled: Boolean,
    rotationEnabled: Boolean,
) {
    val context = LocalContext.current
    val target = vehicle.latLng()
    val latitude = remember { Animatable(target.latitude.toFloat()) }
    val longitude = remember { Animatable(target.longitude.toFloat()) }
    val markerState =
        rememberUpdatedMarkerState(
            position = LatLng(latitude.value.toDouble(), longitude.value.toDouble()),
        )

    LaunchedEffect(target, animationsEnabled) {
        if (animationsEnabled) {
            launch { latitude.animateTo(target.latitude.toFloat(), tween(2000)) }
            launch { longitude.animateTo(target.longitude.toFloat(), tween(2000)) }
        } else {
            latitude.snapTo(target.latitude.toFloat())
            longitude.snapTo(target.longitude.toFloat())
        }
    }
    LaunchedEffect(latitude.value, longitude.value) {
        markerState.position = LatLng(latitude.value.toDouble(), longitude.value.toDouble())
    }

    val routeColor =
        when (vehicle.routeName) {
            "NORTH" -> VehicleColors.North
            "WEST" -> VehicleColors.West
            else -> null
        }
    var lastKnownRouteColor by remember { mutableStateOf(routeColor) }
    LaunchedEffect(routeColor) {
        if (routeColor != null) {
            lastKnownRouteColor = routeColor
        } else {
            delay(30_000)
            if (lastKnownRouteColor == null) lastKnownRouteColor = VehicleColors.Default
        }
    }
    val markerColor = routeColor ?: lastKnownRouteColor ?: VehicleColors.Default
    val icon =
        remember(markerColor) {
            getVehicleMarkerDescriptor(context, 25f, markerColor.toArgb())
        }
    val timeAgoFlow = remember(vehicle.timestamp) { vehicle.getTimeAgo() }
    val lastUpdated by timeAgoFlow.collectAsStateWithLifecycle(initialValue = "")
    val snippet =
        buildString {
            append(stringResource(R.string.vehicle_speed, vehicle.speedMph))
            if (lastUpdated.isNotBlank()) {
                append(" • ")
                append(lastUpdated)
            }
        }

    Marker(
        state = markerState,
        title = stringResource(R.string.vehicle_number, vehicle.name),
        icon = icon,
        snippet = snippet,
        anchor = Offset(0.5f, 0.5f),
        zIndex = 3f,
        rotation = if (rotationEnabled) vehicle.headingDegrees?.toFloat() ?: 0f else 0f,
        flat = rotationEnabled,
        onClick = {
            it.showInfoWindow()
            true
        },
    )
}

/** A small round icon button that floats over the map (settings, map-type toggle, etc). */
@Composable
internal fun MapActionButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(50.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp),
    ) {
        Icon(painterResource(icon), contentDescription)
    }
}
