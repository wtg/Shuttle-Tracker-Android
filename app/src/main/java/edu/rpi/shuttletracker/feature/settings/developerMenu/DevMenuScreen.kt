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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.feature.settings.components.SettingsItem
import kotlinx.coroutines.launch

@Composable
fun DevMenuScreen(
    onBack: () -> Unit,
    viewModel: DevMenuViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()

    DevMenuContent(
        onBack = onBack,
        onDisable = {
            scope.launch {
                viewModel.setDeveloperOptions(false)
                onBack()
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevMenuContent(
    onBack: () -> Unit,
    onDisable: () -> Unit,
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
        )
    }
}
