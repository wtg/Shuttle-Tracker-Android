package edu.rpi.shuttletracker.ui.settings.developerMenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.ui.settings.BaseUrlSettingItem
import edu.rpi.shuttletracker.ui.util.SettingsItem

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun DevMenuScreen(
    navigator: DestinationsNavigator,
    viewModel: DevMenuViewModel = hiltViewModel(),
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val devMenuUiState = viewModel.devMenuUiState.collectAsStateWithLifecycle().value

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings)) },
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
        Column(
            modifier = Modifier.padding(padding),
        ) {
            SettingsItem(icon = Icons.Outlined.Code, title = "Developer Options") {
                Switch(checked = true, onCheckedChange = {
                    viewModel.updateDevMenu(false)
                    navigator.popBackStack()
                })
            }

            MinStopDistItem(
                maxStopDist = devMenuUiState.maxStopDist,
                updateMaxStopDist = viewModel::updateMinStopDist,
            )

            BaseUrlSettingItem(
                currentUrl = devMenuUiState.baseUrl,
                updateBaseUrl = viewModel::updateBaseUrl,
                updateAutoBoardService = viewModel::updateAutoBoardServiceBlocking,
            )
        }
    }
}

@Composable
fun MinStopDistItem(
    maxStopDist: Float,
    updateMaxStopDist: (Float) -> Unit,
) {
    SettingsItem(
        icon = Icons.Outlined.LocationOn,
        title = stringResource(R.string.max_stop_dist),
        description = stringResource(R.string.current_meters, maxStopDist.toInt()),
        useLargeAction = true,
    ) {
        Slider(
            value = maxStopDist,
            valueRange = 10f..100f,
            steps = 8,
            onValueChange = { updateMaxStopDist(it) },
        )
    }
}
