package edu.rpi.shuttletracker.feature.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Vehicle

/** Developer panel for inspecting vehicles and selecting one on the map. */
@Composable
internal fun DeveloperVehicleView(
    vehicles: List<Vehicle>,
    onClose: () -> Unit,
    onZoomToVehicle: (Vehicle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .width(300.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    shape = MaterialTheme.shapes.medium,
                ).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.dev_shuttles_title, vehicles.size),
                style = MaterialTheme.typography.titleSmall,
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        HorizontalDivider()

        if (vehicles.isEmpty()) {
            Text(
                text = stringResource(R.string.dev_no_active_shuttles),
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            Column(
                modifier =
                    Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                vehicles.sortedBy { it.name }.forEach { vehicle ->
                    DevVehicleCard(vehicle = vehicle, onZoomClick = { onZoomToVehicle(vehicle) })
                }
            }
        }
    }
}

@Composable
private fun DevVehicleCard(
    vehicle: Vehicle,
    onZoomClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                ).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.vehicle_number, vehicle.name),
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.weight(1f))

            IconButton(onClick = onZoomClick, modifier = Modifier.size(28.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_my_location),
                    contentDescription = stringResource(R.string.dev_zoom_to_shuttle),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        DevInfoRow(
            label = stringResource(R.string.dev_route),
            value = vehicle.routeName ?: stringResource(R.string.dev_unknown),
        )
        DevInfoRow(
            label = stringResource(R.string.dev_speed),
            value = stringResource(R.string.vehicle_speed, vehicle.speedMph),
        )
        DevInfoRow(
            label = stringResource(R.string.dev_current_stop),
            value = vehicle.currentStop ?: stringResource(R.string.dev_none),
        )
    }
}

@Composable
private fun DevInfoRow(
    label: String,
    value: String,
) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodySmall,
    )
}
