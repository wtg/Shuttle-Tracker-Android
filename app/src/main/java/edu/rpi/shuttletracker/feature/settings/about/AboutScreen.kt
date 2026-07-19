package edu.rpi.shuttletracker.feature.settings.about

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import edu.rpi.shuttletracker.BuildConfig
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.feature.settings.components.SettingsItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenLibraries: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val uriHandler = LocalUriHandler.current

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
        )

    val context = LocalContext.current
    val repositoryUrl = stringResource(R.string.url_repository)
    val repositoryIssuesUrl = stringResource(R.string.url_repository_issues)
    val privacyPolicyUrl = stringResource(R.string.url_private_policy)
    val devOptionsActivatedMessage = stringResource(R.string.dev_options_activated)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.about)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                Text(text = stringResource(R.string.about_page))
            }

            Spacer(modifier = Modifier.height(10.dp))

            SettingsItem(
                icon = Icons.Outlined.Code,
                title = stringResource(R.string.check_out_repository),
                onClick = { uriHandler.openUri(repositoryUrl) },
            )

            SettingsItem(
                icon = Icons.Outlined.BugReport,
                title = stringResource(R.string.report_problem),
                onClick = { uriHandler.openUri(repositoryIssuesUrl) },
            )

            SettingsItem(
                icon = Icons.Outlined.Shield,
                title = stringResource(R.string.view_privacy_policy),
                onClick = { uriHandler.openUri(privacyPolicyUrl) },
            )

            SettingsItem(
                icon = Icons.Outlined.Description,
                title = stringResource(R.string.libraries_used),
                onClick = onOpenLibraries,
            )

            var timesClicked by remember { mutableIntStateOf(10) }
            var toast by remember { mutableStateOf<Toast?>(null) }
            val devOptionsStateMessage = stringResource(R.string.dev_options_state, timesClicked - 1)
            SettingsItem(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.version),
                BuildConfig.VERSION_NAME,
                onClick = {
                    if (timesClicked > 0) {
                        toast?.cancel()
                    }

                    timesClicked--
                    if (timesClicked == 0) {
                        toast =
                            Toast.makeText(
                                context,
                                devOptionsActivatedMessage,
                                Toast.LENGTH_SHORT,
                            )
                        viewModel.activateDevOptions()
                    } else if (timesClicked in 1..3) {
                        toast =
                            Toast.makeText(
                                context,
                                devOptionsStateMessage,
                                Toast.LENGTH_SHORT,
                            )
                    }

                    toast?.show()
                },
            )

            SettingsItem(
                icon = Icons.Outlined.DirectionsBus,
                title = stringResource(R.string.shuttle_tracker_version),
                stringResource(R.string.api_key),
            )
        }
    }
}
