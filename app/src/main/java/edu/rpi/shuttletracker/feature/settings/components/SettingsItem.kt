package edu.rpi.shuttletracker.feature.settings.components

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
 * @param hasBottomSpacing: Adds bottom padding if true, else no padding
 * @param onClick: What happens when the setting tile is clicked
 * @param actions: any other composable such as switches to display with the setting
 * */
@Composable
fun SettingsItem(
    icon: ImageVector? = null,
    title: String,
    description: String = "",
    hasBottomSpacing: Boolean = true,
    onClick: (() -> Unit)? = null,
    useLargeAction: Boolean = false,
    actions: @Composable () -> Unit = {},
) {
    val clickModifier =
        if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        }

    Row(
        modifier =
            clickModifier
                .fillMaxWidth()
                .padding(
                    top = 10.dp,
                    bottom = if (hasBottomSpacing) 10.dp else 0.dp,
                    start = 20.dp,
                    end = 20.dp,
                ),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
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
