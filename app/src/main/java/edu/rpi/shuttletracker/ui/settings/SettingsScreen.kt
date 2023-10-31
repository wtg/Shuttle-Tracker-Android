package edu.rpi.shuttletracker.ui.settings

import android.app.Activity
import android.content.Intent
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BusAlert
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.mutableStateOf
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
import edu.rpi.shuttletracker.ui.MainActivity
import edu.rpi.shuttletracker.ui.destinations.AboutScreenDestination
import edu.rpi.shuttletracker.ui.destinations.AnalyticsScreenDestination
import edu.rpi.shuttletracker.ui.destinations.DevMenuScreenDestination
import edu.rpi.shuttletracker.ui.destinations.SetupScreenDestination
import edu.rpi.shuttletracker.ui.util.SettingsItem
import edu.rpi.shuttletracker.util.services.BeaconService
import edu.rpi.shuttletracker.util.services.LocationService
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
                .padding(padding),
        ) {
            AutoBoardBusSettingItem(
                autoBoardService = settingsUiState.autoBoardService,
                updateAutoBoardService = viewModel::updateAutoBoardService,
            )

            ColorBlindSettingItem(
                colorBlindMode = settingsUiState.colorBlindMode,
                updateColorBlindMode = viewModel::updateColorBlindMode,
            )

            if (settingsUiState.devOptionState) {
                SettingsItem(
                    Icons.Outlined.Code,
                    "Developer Options",
                    onClick = { navigator.navigate(DevMenuScreenDestination()) },
                )
            }

            SettingsItem(
                Icons.Outlined.Timeline,
                "Analytics",
                onClick = { navigator.navigate(AnalyticsScreenDestination()) },
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

    SettingsItem(
        icon = Icons.Outlined.BusAlert,
        stringResource(R.string.auto_boarding),
        stringResource(R.string.auto_boarding_description),
    ) {
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
    SettingsItem(
        icon = Icons.Outlined.Visibility,
        stringResource(R.string.color_blind_mode),
        stringResource(R.string.color_blind_description),
    ) {
        Switch(
            checked = colorBlindMode,
            onCheckedChange = { updateColorBlindMode(it) },
        )
    }
}

@Composable
fun BaseUrlSettingItem(
    currentUrl: String,
    updateBaseUrl: (String) -> Unit,
    updateAutoBoardService: (Boolean) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var textFieldUrl by remember { mutableStateOf(currentUrl) }

    val context = LocalContext.current

    // updates to the current url whenever the dialog is shown
    LaunchedEffect(key1 = showDialog) {
        if (showDialog) {
            textFieldUrl = currentUrl
        }
    }

    SettingsItem(
        icon = Icons.Outlined.Link,
        title = stringResource(R.string.base_url),
        description = currentUrl,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = stringResource(R.string.base_url)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.change_url_warning),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = textFieldUrl,
                        onValueChange = { textFieldUrl = it },
                        label = { Text(text = stringResource(R.string.url)) },
                    )
                }
            },

            confirmButton = {
                Button(onClick = {
                    // checks for valid url
                    if (Patterns.WEB_URL.matcher(textFieldUrl).matches()) {
                        // stops all services from running
                        context.stopService(Intent(context, BeaconService::class.java))
                        context.stopService(Intent(context, LocationService::class.java))

                        // preference to use auto boarding turned off
                        updateAutoBoardService(false)

                        // updates the preferred url
                        updateBaseUrl(textFieldUrl)

                        showDialog = false

                        // restarts app
                        val intent = Intent(context, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                        if (context is Activity) {
                            context.finish()
                        }
                        Runtime.getRuntime().exit(0)
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.invalid_url),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }) {
                    Text(text = stringResource(R.string.save))
                }
            },

            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}
