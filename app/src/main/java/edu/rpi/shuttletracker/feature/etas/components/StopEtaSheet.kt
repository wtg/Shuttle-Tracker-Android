package edu.rpi.shuttletracker.feature.etas.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.feature.etas.utils.StopWithEtas
import edu.rpi.shuttletracker.feature.etas.utils.VehicleEta
import edu.rpi.shuttletracker.feature.etas.utils.etaMinutesFromNow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Shows every live vehicle relevant to the selected stop. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopEtaSheet(
    stop: StopWithEtas?,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onVehicleClick: (String) -> Unit,
) {
    if (stop == null) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stop.stop.name,
                style = MaterialTheme.typography.titleLarge,
            )

            if (stop.etas.isEmpty()) {
                Text(
                    text = stringResource(R.string.etas_no_live_etas),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                stop.etas.forEach { eta ->
                    StopEtaDetailRow(eta, onVehicleClick)
                }

                Text(
                    text = stringResource(R.string.etas_sheet_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StopEtaDetailRow(
    eta: VehicleEta,
    onVehicleClick: (String) -> Unit,
) {
    val tagColor = routeAccentColor(eta.routeName)

    Surface(
        onClick = { onVehicleClick(eta.vehicleId) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = tagColor.copy(alpha = 0.10f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = eta.vehicleName,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = eta.etaInstant.toLocalTimeText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            EtaChip(
                vehicleName = eta.vehicleName,
                routeName = eta.routeName,
                minutes = etaMinutesFromNow(eta.etaInstant),
            )
        }
    }
}

private val LOCAL_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

private fun Instant.toLocalTimeText(): String = LOCAL_TIME_FORMATTER.withZone(ZoneId.systemDefault()).format(this)
