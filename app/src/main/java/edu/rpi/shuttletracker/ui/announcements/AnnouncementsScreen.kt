package edu.rpi.shuttletracker.ui.announcements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.ui.errors.CheckResponseError

@Destination
@Composable
fun AnnouncementsScreen(
    viewModel: AnnouncementsViewModel = hiltViewModel(),
) {
    val announcementsUIState = viewModel.announcementsUiState.collectAsStateWithLifecycle().value

    CheckResponseError(
        announcementsUIState.networkError,
        announcementsUIState.serverError,
        announcementsUIState.unknownError,
        ignoreErrorRequest = { viewModel.clearErrors() },
        retryErrorRequest = {
            viewModel.clearErrors()
            viewModel.loadAll()
        },
    )

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            itemsIndexed(announcementsUIState.announcements) { _, item ->
                AnnouncementItem(announcement = item)
            }
        }
    }
}

@Composable
fun AnnouncementItem(announcement: Announcement) {
    Column {
        Text(
            text = "Effective from ${announcement.startTime} to ${announcement.endTime}",
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(text = announcement.body)
    }
}
