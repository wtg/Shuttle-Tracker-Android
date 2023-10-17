package edu.rpi.shuttletracker.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BusAlert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.ui.destinations.AboutScreenDestination
import edu.rpi.shuttletracker.ui.destinations.SetupScreenDestination
import edu.rpi.shuttletracker.ui.util.SettingsItem
import edu.rpi.shuttletracker.util.services.BeaconService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun SettingsScreen(
    navigator: DestinationsNavigator,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val settingsUiState = viewModel.settingsUiState.collectAsStateWithLifecycle().value

    val snackbarHostState = remember { SnackbarHostState() }

    val coroutineScope = rememberCoroutineScope()

    val errorStartingBeaconService = BeaconService.permissionError.collectAsStateWithLifecycle().value

    val context = LocalContext.current

    // listens to beacon service if they don't have permissions or not
    LaunchedEffect(errorStartingBeaconService) {
        if (errorStartingBeaconService) {
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.service_missing_permissions),
                    actionLabel = context.getString(R.string.fix),
                    duration = SnackbarDuration.Long,
                )
                when (result) {
                    SnackbarResult.ActionPerformed -> {
                        navigator.navigate(SetupScreenDestination())
                    }
                    SnackbarResult.Dismissed -> { /* IGNORED */ }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.Outlined.ArrowBack, stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 10.dp),
        ) {
            AutoBoardBusSettingItem(
                autoBoardService = settingsUiState.autoBoardService,
                updateAutoBoardService = viewModel::updateAutoBoardService,
            )

            ColorBlindSettingItem(
                colorBlindMode = settingsUiState.colorBlindMode,
                updateColorBlindMode = viewModel::updateColorBlindMode,
            )

            MinStopDistItem(
                minStopDist = settingsUiState.minStopDist,
                updateMinStopDist = viewModel::updateMinStopDist,
            )

            SettingsItem(
                Icons.Outlined.Info,
                stringResource(R.string.about),
                onClick = { navigator.navigate(AboutScreenDestination()) },
            )
        }
    }
}

@Composable
fun AutoBoardBusSettingItem(
    autoBoardService: Boolean,
    updateAutoBoardService: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    SettingsItem(icon = Icons.Outlined.BusAlert, stringResource(R.string.auto_boarding)) {
        Switch(
            checked = autoBoardService,
            onCheckedChange = {
                if (it) {
                    context.startForegroundService(Intent(context, BeaconService::class.java))
                } else {
                    updateAutoBoardService(false)
                    context.stopService(Intent(context, BeaconService::class.java))
                }
            },
        )
    }
}

@Composable
fun ColorBlindSettingItem(
    colorBlindMode: Boolean,
    updateColorBlindMode: (Boolean) -> Unit,
) {
    SettingsItem(icon = Icons.Outlined.Visibility, stringResource(R.string.color_blind)) {
        Switch(
            checked = colorBlindMode,
            onCheckedChange = { updateColorBlindMode(it) },
        )
    }
}

@Composable
fun MinStopDistItem(
    minStopDist: Float,
    updateMinStopDist: (Float) -> Unit,
) {
    SettingsItem(icon = Icons.Outlined.LocationOn, stringResource(R.string.stopDist)) {
        Slider(
            value = minStopDist,
            valueRange = 10f..100f,
            steps = 8,
            onValueChange = { updateMinStopDist(it) },
        )
    }
}
