package edu.rpi.shuttletracker.ui.maps

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.rpi.shuttletracker.data.models.Stop
import edu.rpi.shuttletracker.data.models.vehicle.VehicleLocation
import edu.rpi.shuttletracker.data.models.vehicle.VehicleStopEta
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Composable
fun StopEtaContent(
    modifier: Modifier = Modifier,
    selectedStopKey: String?,
    selectedStop: Stop?,
    vehicleStopEtas: Map<String, VehicleStopEta>,
    vehicleLocations: Map<String, VehicleLocation>,
    lastEtasUpdatedAt: Instant?,
    onClearStop: () -> Unit,
    onEtaChipClick: (vehicleId: String) -> Unit,
) {
    val stopTitle = selectedStop?.name ?: "Tap a stop to see etas"

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StopEtaHeader(stopTitle, onClearStop, selectedStop, lastEtasUpdatedAt)

        if (selectedStopKey == null) {
            return
        }

        val etas =
            remember(selectedStopKey, vehicleStopEtas, vehicleLocations) {
                buildVehicleEtas(
                    selectedStopKey,
                    vehicleStopEtas,
                    vehicleLocations,
                )
            }

        val now = Instant.now()

        val visibleEtas =
            etas
                .filter { Duration.between(now, it.etaInstant).toMinutes() >= -5 }

        if (visibleEtas.isEmpty() || stopTitle == "Blitman") {
            Text(
                text = "No ETAs found",
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
                items(visibleEtas, key = { it.vehicleId }) { eta ->
                    EtaChip(
                        eta = eta,
                        now = now,
                        onClick = { onEtaChipClick(eta.vehicleId) },
                    )
                }
            }
        }

        Text(
            text = "Note: ETAs may be off by a few minutes.",
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
    stopSelected: Stop?,
    lastEtasUpdatedAt: Instant?,
) {
    val updatedText =
        updatedAgoFlow(lastEtasUpdatedAt)
            .collectAsStateWithLifecycle(initialValue = "")
            .value

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )

        if (stopSelected != null) {
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
    eta: VehicleEta,
    now: Instant,
    onClick: () -> Unit,
) {
    val mins = Duration.between(now, eta.etaInstant).toMinutes()
    val etaText =
        when {
            mins <= 0 -> "now"
            else -> "${mins}m"
        }

    Text(
        text = "Shuttle ${eta.vehicleLabel} • $etaText",
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

// Data

private data class VehicleEta(
    val vehicleId: String,
    val vehicleLabel: String,
    val etaInstant: Instant,
)

private fun buildVehicleEtas(
    stopKey: String,
    vehicleStopEtas: Map<String, VehicleStopEta>,
    vehicleLocations: Map<String, VehicleLocation>,
): List<VehicleEta> =
    vehicleStopEtas
        .mapNotNull { (vehicleId, etaData) ->
            val rawTime = etaData.stopTimes[stopKey] ?: return@mapNotNull null
            val instant = rawTime.toInstantOrNull() ?: return@mapNotNull null

            val vehicle = vehicleLocations[vehicleId]

            VehicleEta(
                vehicleId = vehicleId,
                vehicleLabel = vehicle?.name.orEmpty(),
                etaInstant = instant,
            )
        }.sortedBy { it.etaInstant }

private fun String.toInstantOrNull(): Instant? = runCatching { OffsetDateTime.parse(trim()).toInstant() }.getOrNull()

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
