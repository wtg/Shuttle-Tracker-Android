package edu.rpi.shuttletracker.feature.announcements.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.models.AnnouncementType

@Composable
fun AnnouncementListItem(announcement: Announcement) {
    Column {
        Text(
            text = announcement.type.name,
            style = MaterialTheme.typography.labelLarge,
        )

        Text(text = announcement.message)
    }
}

@Preview(showBackground = true)
@Composable
private fun AnnouncementItemPreview() {
    ShuttleTrackerTheme(dynamicColor = false) {
        AnnouncementListItem(
            announcement =
                Announcement(
                    id = "service-update",
                    message = "Shuttle service will follow the updated schedule.",
                    type = AnnouncementType.Info,
                    active = true,
                ),
        )
    }
}
