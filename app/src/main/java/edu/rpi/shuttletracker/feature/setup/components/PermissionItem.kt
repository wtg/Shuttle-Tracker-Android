package edu.rpi.shuttletracker.feature.setup.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme

/** A name/description row with a Grant button that becomes disabled and relabeled once granted. */
@Composable
fun PermissionItem(
    name: String,
    description: String,
    isGranted: Boolean,
    grantLabel: String,
    grantedLabel: String,
    onRequestPermission: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(text = description, style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.padding(20.dp))

        Button(onClick = onRequestPermission, enabled = !isGranted) {
            Text(text = if (isGranted) grantedLabel else grantLabel)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionItemPreview() {
    ShuttleTrackerTheme(dynamicColor = false) {
        PermissionItem(
            name = "Location",
            description = "Access your location to display your position on the map.",
            isGranted = false,
            grantLabel = "Grant",
            grantedLabel = "Granted",
            onRequestPermission = {},
        )
    }
}
