package edu.rpi.shuttletracker.feature.map

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import java.time.Duration
import java.time.Instant

/** Draws a stop circle plus an invisible marker that handles taps and its info window. */
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

/** Draws a shuttle, briefly retaining its route color if route data drops out. */
@Composable
internal fun VehicleMarker(
    vehicle: Vehicle,
    animationsEnabled: Boolean,
    rotationEnabled: Boolean,
    selected: Boolean = false,
) {
    val context = LocalContext.current
    val target = vehicle.latLng()
    val latitude = remember { Animatable(target.latitude.toFloat()) }
    val longitude = remember { Animatable(target.longitude.toFloat()) }
    val markerState =
        rememberUpdatedMarkerState(
            position = LatLng(latitude.value.toDouble(), longitude.value.toDouble()),
        )

    LaunchedEffect(selected) {
        if (selected) markerState.showInfoWindow()
    }

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

@Composable
internal fun MapActionButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val (containerColor, contentColor) = mapButtonColors()

    Button(
        onClick = onClick,
        modifier = Modifier.size(50.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp),
    ) {
        Icon(painterResource(icon), contentDescription)
    }
}

private val FLASH_WINDOW: Duration = Duration.ofSeconds(3)
private val STALE_THRESHOLD: Duration = Duration.ofSeconds(30)

/** Shows stale status, then briefly confirms recovery after polling succeeds again. */
@Composable
internal fun LastUpdatedChip(
    updatedAt: Instant?,
    modifier: Modifier = Modifier,
) {
    val mountedAt = remember { Instant.now() }
    var now by remember { mutableStateOf(Instant.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            now = Instant.now()
        }
    }

    val isStale = Duration.between(updatedAt ?: mountedAt, now) >= STALE_THRESHOLD
    var wasStale by remember { mutableStateOf(false) }
    var showRecoveryFlash by remember { mutableStateOf(false) }

    LaunchedEffect(isStale) {
        if (isStale) wasStale = true
    }

    LaunchedEffect(updatedAt) {
        if (updatedAt != null && wasStale) {
            wasStale = false
            showRecoveryFlash = true
            delay(FLASH_WINDOW.toMillis())
            showRecoveryFlash = false
        }
    }

    val text =
        when {
            showRecoveryFlash -> "Updated"
            isStale -> "Not updated in ${Duration.between(updatedAt ?: mountedAt, now).seconds}s"
            else -> null
        } ?: return
    val (containerColor, contentColor) = mapButtonColors()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor.copy(alpha = 0.55f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** Shared colors keep floating map controls visible in both themes. */
@Composable
internal fun mapButtonColors(): Pair<Color, Color> =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
        MaterialTheme.colorScheme.background to MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
