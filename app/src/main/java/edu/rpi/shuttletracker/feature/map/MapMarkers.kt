package edu.rpi.shuttletracker.feature.map

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.core.ui.theme.VehicleColors
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.models.Vehicle
import edu.rpi.shuttletracker.feature.map.components.getVehicleMarkerBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.exponential
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.interpolate
import org.maplibre.compose.expressions.dsl.zoom
import org.maplibre.compose.expressions.value.CirclePitchAlignment
import org.maplibre.compose.expressions.value.IconPitchAlignment
import org.maplibre.compose.expressions.value.IconRotationAlignment
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import java.time.Duration
import java.time.Instant
import kotlin.math.PI
import kotlin.math.cos

@Composable
@MaplibreComposable
internal fun StopMarker(
    id: String,
    stop: Stop,
    color: Color,
    selected: Boolean,
    onSelected: (Stop) -> Unit,
) {
    val source = rememberGeoJsonSource(GeoJsonData.Features(Point(stop.mapPosition())))
    // Keep the stop radius at 15 metres.
    val radiusAtZoomZero = (15.0 * 512.0 / (2.0 * PI * 6378137.0 * cos(stop.coordinates[0] * PI / 180.0))).toFloat()
    CircleLayer(
        id = id,
        source = source,
        color = const(Color.Transparent),
        radius =
            interpolate(
                type = exponential(2f),
                input = zoom(),
                0 to const(radiusAtZoomZero.dp),
                24 to const((radiusAtZoomZero * 16_777_216f).dp),
            ),
        pitchAlignment = const(CirclePitchAlignment.Map),
        strokeColor = const(if (selected) Color(0xFF6699FF) else color),
        strokeWidth = const(2.dp),
        hitPadding = 12.dp,
        onClick = {
            onSelected(stop)
            ClickResult.Consume
        },
    )
}

@Composable
internal fun rememberVehiclePosition(
    vehicle: Vehicle,
    animationsEnabled: Boolean,
): Position {
    val latitude = remember { Animatable(vehicle.latitude.toFloat()) }
    val longitude = remember { Animatable(vehicle.longitude.toFloat()) }
    LaunchedEffect(vehicle.latitude, vehicle.longitude, animationsEnabled) {
        if (animationsEnabled) {
            launch { latitude.animateTo(vehicle.latitude.toFloat(), tween(2000)) }
            launch { longitude.animateTo(vehicle.longitude.toFloat(), tween(2000)) }
        } else {
            latitude.snapTo(vehicle.latitude.toFloat())
            longitude.snapTo(vehicle.longitude.toFloat())
        }
    }
    return Position(longitude = longitude.value.toDouble(), latitude = latitude.value.toDouble())
}

@Composable
@MaplibreComposable
internal fun VehicleMarker(
    vehicle: Vehicle,
    position: Position,
    rotationEnabled: Boolean,
    onSelected: () -> Unit,
) {
    val context = LocalContext.current
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
    val painter =
        remember(context, markerColor) {
            BitmapPainter(getVehicleMarkerBitmap(context, 25f, markerColor.toArgb()).asImageBitmap())
        }
    val source = rememberGeoJsonSource(GeoJsonData.Features(Point(position)))
    SymbolLayer(
        id = "vehicle-${vehicle.id}",
        source = source,
        iconImage = image(painter, size = DpSize(25.dp, 25.dp)),
        iconAllowOverlap = const(true),
        iconIgnorePlacement = const(true),
        iconRotate = const(if (rotationEnabled) vehicle.headingDegrees?.toFloat() ?: 0f else 0f),
        iconRotationAlignment =
            const(
                if (rotationEnabled) IconRotationAlignment.Map else IconRotationAlignment.Viewport,
            ),
        iconPitchAlignment = const(if (rotationEnabled) IconPitchAlignment.Map else IconPitchAlignment.Viewport),
        hitPadding = 12.dp,
        onClick = {
            onSelected()
            ClickResult.Consume
        },
    )
}

@Composable
internal fun VehicleInfoCard(
    vehicle: Vehicle,
    modifier: Modifier = Modifier,
) {
    val timeAgoFlow = remember(vehicle.timestamp) { vehicle.getTimeAgo() }
    val lastUpdated by timeAgoFlow.collectAsStateWithLifecycle(initialValue = "")
    val speed = stringResource(R.string.vehicle_speed, vehicle.speedMph)
    MapInfoCard(
        title = stringResource(R.string.vehicle_number, vehicle.name),
        subtitle = if (lastUpdated.isBlank()) speed else "$speed • $lastUpdated",
        modifier = modifier,
    )
}

@Composable
internal fun MapInfoCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val (containerColor, contentColor) = mapButtonColors()
    Surface(
        modifier = modifier.widthIn(max = 260.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 4.dp,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
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
