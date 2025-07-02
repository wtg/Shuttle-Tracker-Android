package edu.rpi.shuttletracker.ui.settings.analytics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Analytics
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import edu.rpi.shuttletracker.BuildConfig
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.ui.util.SettingsItem

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AnalyticsScreen(
    navigator: DestinationsNavigator,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
        )
    val analytics = viewModel.getAnalytics()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.analytics)) },
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

        Column(modifier = Modifier.padding(padding)) {
            SettingsItem(
                icon = Icons.Outlined.Analytics,
                stringResource(R.string.analytics),
            ) {
                Switch(
                    checked = viewModel.getAnalyticsEnabled().collectAsStateWithLifecycle(initialValue = false).value,
                    onCheckedChange = { viewModel.updateAnalytics(it) },
                )
            }

            SettingsItem(
                title = stringResource(R.string.user_id),
                description = analytics.userID,
            )

            SettingsItem(
                title = stringResource(R.string.android_version),
                description = analytics.clientPlatformVersion,
            )

            SettingsItem(
                title = stringResource(R.string.board_bus),
                description = analytics.boardBusCount.toString(),
            )

            SettingsItem(
                title = stringResource(R.string.debug_mode),
                description = BuildConfig.DEBUG.toString(),
            )
        }
    }
}
