package edu.rpi.shuttletracker.feature.announcements.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.data.models.Announcement

@Composable
fun AnnouncementListItem(announcement: Announcement) {
    Column {
        Text(
            text = announcement.subject,
            style = MaterialTheme.typography.headlineLarge,
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text =
                stringResource(
                    R.string.effective_from,
                    announcement.startTime,
                    announcement.endTime,
                ),
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(text = announcement.body)
    }
}

@Preview(showBackground = true)
@Composable
private fun AnnouncementItemPreview() {
    ShuttleTrackerTheme(dynamicColor = false) {
        AnnouncementListItem(
            announcement =
                Announcement(
                    subject = "Service update",
                    body = "Shuttle service will follow the updated schedule.",
                    rawStartTime = "2026-01-15T08:00:00-05:00",
                    rawEndTime = "2026-01-15T18:00:00-05:00",
                ),
        )
    }
}
