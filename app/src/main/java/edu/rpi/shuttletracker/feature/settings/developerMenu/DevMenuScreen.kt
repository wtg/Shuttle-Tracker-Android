package edu.rpi.shuttletracker.feature.settings.developerMenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.feature.settings.components.SettingsItem
import kotlinx.coroutines.launch

/** Developer toggles unlocked from About; disabling the top switch locks this screen again. */
@Composable
fun DevMenuScreen(
    onBack: () -> Unit,
    viewModel: DevMenuViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val simulateAnnouncements by viewModel.simulateAnnouncements.collectAsStateWithLifecycle()
    val fakeShuttlesEnabled by viewModel.fakeShuttlesEnabled.collectAsStateWithLifecycle()

    DevMenuContent(
        onBack = onBack,
        onDisable = {
            scope.launch {
                viewModel.setDeveloperOptions(false)
                onBack()
            }
        },
        simulateAnnouncements = simulateAnnouncements,
        onSimulateAnnouncementsChange = viewModel::setSimulateAnnouncements,
        fakeShuttlesEnabled = fakeShuttlesEnabled,
        onFakeShuttlesEnabledChange = viewModel::setFakeShuttlesEnabled,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevMenuContent(
    onBack: () -> Unit,
    onDisable: () -> Unit,
    simulateAnnouncements: Boolean,
    onSimulateAnnouncementsChange: (Boolean) -> Unit,
    fakeShuttlesEnabled: Boolean,
    onFakeShuttlesEnabledChange: (Boolean) -> Unit,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings)) },
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
        Column(modifier = Modifier.padding(padding)) {
            SettingsItem(
                icon = R.drawable.ic_code,
                title = stringResource(R.string.dev_options),
            ) {
                Switch(
                    checked = true,
                    onCheckedChange = { enabled -> if (!enabled) onDisable() },
                )
            }

            SettingsItem(
                icon = R.drawable.ic_bug_report,
                title = stringResource(R.string.simulate_announcements),
                description = stringResource(R.string.simulate_announcements_description),
            ) {
                Switch(
                    checked = simulateAnnouncements,
                    onCheckedChange = onSimulateAnnouncementsChange,
                )
            }

            SettingsItem(
                icon = R.drawable.ic_directions_bus,
                title = stringResource(R.string.fake_shuttles),
                description = stringResource(R.string.fake_shuttles_description),
            ) {
                Switch(
                    checked = fakeShuttlesEnabled,
                    onCheckedChange = onFakeShuttlesEnabledChange,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DevMenuContentPreview() {
    ShuttleTrackerTheme(dynamicColor = false) {
        DevMenuContent(
            onBack = {},
            onDisable = {},
            simulateAnnouncements = false,
            onSimulateAnnouncementsChange = {},
            fakeShuttlesEnabled = false,
            onFakeShuttlesEnabledChange = {},
        )
    }
}
