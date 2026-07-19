package edu.rpi.shuttletracker.feature.announcements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.core.ui.CheckResponseError
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.feature.announcements.components.AnnouncementListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen(
    onBack: () -> Unit,
    viewModel: AnnouncementsViewModel = hiltViewModel(),
) {
    val uiState = viewModel.announcementsUiState.collectAsStateWithLifecycle().value

    AnnouncementsContent(
        uiState = uiState,
        onBack = onBack,
        onDismissError = viewModel::clearErrors,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnnouncementsContent(
    uiState: AnnouncementsUiState,
    onBack: () -> Unit,
    onDismissError: () -> Unit,
    onRetry: () -> Unit,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
        )

    Scaffold(
        snackbarHost = {
            CheckResponseError(
                uiState.networkError,
                uiState.serverError,
                uiState.unknownError,
                ignoreErrorRequest = onDismissError,
                retryErrorRequest = onRetry,
            )
        },
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.announcements)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        when {
            uiState.isLoading ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            uiState.announcements.isEmpty() ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Text(stringResource(R.string.no_announcements))
                }
            else ->
                LazyColumn(
                    modifier =
                        Modifier
                            .padding(padding)
                            .padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                ) {
                    items(uiState.announcements) { item ->
                        AnnouncementItem(announcement = item)
                    }
                }
        }
    }
}

@Composable
fun AnnouncementItem(announcement: Announcement) {
    AnnouncementListItem(announcement)
}

@Preview(showBackground = true)
@Composable
private fun AnnouncementsContentPreview() {
    ShuttleTrackerTheme(dynamicColor = false) {
        AnnouncementsContent(
            uiState =
                AnnouncementsUiState(
                    announcements =
                        listOf(
                            Announcement(
                                subject = "Service update",
                                body = "Shuttle service will follow the updated schedule.",
                                rawStartTime = "2026-01-15T08:00:00-05:00",
                                rawEndTime = "2026-01-15T18:00:00-05:00",
                            ),
                        ),
                    isLoading = false,
                ),
            onBack = {},
            onDismissError = {},
            onRetry = {},
        )
    }
}
