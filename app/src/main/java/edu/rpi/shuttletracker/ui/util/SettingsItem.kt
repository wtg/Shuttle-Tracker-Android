package edu.rpi.shuttletracker.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * @param icon: Icon to show with the setting
 * @param title: Title of the setting
 * @param description: Any subtitle to show with the setting
 * @param onClick: What happens when the setting tile is clicked
 * @param actions: any other composable such as switches to display with the setting
 * */
@Composable
fun SettingsItem(
    icon: ImageVector? = null,
    title: String,
    description: String = "",
    onClick: () -> Unit = {},
    useLargeAction: Boolean = false,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier =
            Modifier
                .clickable { onClick() }
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                title,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title)

            if (description != "") {
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (useLargeAction) {
                actions()
            }
        }

        if (!useLargeAction) {
            actions()
        }
    }
}
