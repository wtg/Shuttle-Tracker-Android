package edu.rpi.shuttletracker.feature.settings.about

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.android.gms.oss.licenses.v2.OssLicensesMenuActivity
import edu.rpi.shuttletracker.BuildConfig
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.core.ui.theme.shuttleTrackerColorScheme
import edu.rpi.shuttletracker.feature.settings.components.SettingsItem

/** Shows project details; tapping the version ten times unlocks developer options. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
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
    val librariesTitle = stringResource(R.string.libraries_used)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.about)) },
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
                icon = R.drawable.ic_code,
                title = stringResource(R.string.check_out_repository),
                onClick = { uriHandler.openUri(repositoryUrl) },
            )

            SettingsItem(
                icon = R.drawable.ic_bug_report,
                title = stringResource(R.string.report_problem),
                onClick = { uriHandler.openUri(repositoryIssuesUrl) },
            )

            SettingsItem(
                icon = R.drawable.ic_shield,
                title = stringResource(R.string.view_privacy_policy),
                onClick = { uriHandler.openUri(privacyPolicyUrl) },
            )

            SettingsItem(
                icon = R.drawable.ic_description,
                title = librariesTitle,
                onClick = {
                    OssLicensesMenuActivity.setActivityTitle(librariesTitle)
                    OssLicensesMenuActivity.setTheme(
                        shuttleTrackerColorScheme(context, darkTheme = false),
                        shuttleTrackerColorScheme(context, darkTheme = true),
                        Typography(),
                    )
                    context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
                },
            )

            var timesClicked by remember { mutableIntStateOf(10) }
            var toast by remember { mutableStateOf<Toast?>(null) }
            val devOptionsStateMessage = stringResource(R.string.dev_options_state, timesClicked - 1)
            SettingsItem(
                icon = R.drawable.ic_info,
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
                icon = R.drawable.ic_directions_bus,
                title = stringResource(R.string.shuttle_tracker_version),
                stringResource(R.string.api_key),
            )
        }
    }
}
