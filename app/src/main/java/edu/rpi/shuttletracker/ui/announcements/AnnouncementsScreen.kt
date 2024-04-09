package edu.rpi.shuttletracker.ui.announcements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.ui.util.CheckResponseError

@OptIn(ExperimentalMaterial3Api::class)
@Destination(
    deepLinks = [
        DeepLink(
            uriPattern = "https://shuttletracker.app/analytics/",
        ),
    ],
)
@Composable
fun AnnouncementsScreen(
    navigator: DestinationsNavigator,
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

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
        )

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.announcements)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .padding(padding)
                    .padding(horizontal = 10.dp),
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
