package edu.rpi.shuttletracker.ui.maps.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.ui.maps.utils.VehicleEtaUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

@Composable
fun StopEtaCard(
    modifier: Modifier = Modifier,
    title: String,
    selectedStopEtas: List<VehicleEtaUi>,
    lastEtasUpdatedAt: Instant?,
    stopSelected: Boolean,
    onClearStop: () -> Unit,
    onEtaChipClick: (vehicleId: String) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StopEtaHeader(
            title = title,
            onClearStop = onClearStop,
            stopSelected = stopSelected,
            lastEtasUpdatedAt = lastEtasUpdatedAt,
        )

        if (!stopSelected) return@Column

        if (selectedStopEtas.isEmpty()) {
            Text(
                text = stringResource(R.string.no_etas),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(
                    items = selectedStopEtas,
                    key = { it.vehicleId },
                ) { eta ->
                    EtaChip(
                        eta = eta,
                        onClick = { onEtaChipClick(eta.vehicleId) },
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.eta_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp),
        )
    }
}

@Composable
private fun StopEtaHeader(
    title: String,
    onClearStop: () -> Unit,
    stopSelected: Boolean,
    lastEtasUpdatedAt: Instant?,
) {
    val updatedText =
        updatedAgoFlow(lastEtasUpdatedAt)
            .collectAsStateWithLifecycle(initialValue = "")
            .value

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )

        if (stopSelected) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (updatedText.isNotBlank()) {
                    Text(
                        text = updatedText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .size(28.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(50),
                            ).clickable { onClearStop() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕")
                }
            }
        }
    }
}

@Composable
private fun EtaChip(
    eta: VehicleEtaUi,
    onClick: () -> Unit,
) {
    Text(
        text = stringResource(R.string.shuttle_eta_chip, eta.vehicleLabel, eta.etaText),
        style = MaterialTheme.typography.bodyMedium,
        modifier =
            Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(999.dp),
                ).clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

fun updatedAgoFlow(lastUpdatedAt: Instant?): Flow<String> =
    flow {
        if (lastUpdatedAt == null) {
            emit("")
            return@flow
        }

        while (true) {
            val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
            val duration = Duration.between(lastUpdatedAt, now)

            val secs = duration.seconds.coerceAtLeast(0)
            val text =
                when {
                    secs < 5 -> ""
                    secs < 60 -> "Updated ${secs}s ago"
                    else -> "Updated ${secs / 60}m ago"
                }

            emit(text)
            delay(1000)
        }
    }
