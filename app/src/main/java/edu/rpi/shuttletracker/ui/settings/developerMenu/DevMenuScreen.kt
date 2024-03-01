package edu.rpi.shuttletracker.ui.settings.developerMenu

import android.app.Activity
import android.content.Intent
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import edu.rpi.shuttletracker.ui.util.SettingsItem
import edu.rpi.shuttletracker.util.services.BeaconService
import edu.rpi.shuttletracker.util.services.LocationService

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

    val context = LocalContext.current

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

            SettingsItem(
                icon = Icons.Outlined.Alarm,
                title = "Restart Departures",
                onClick = {
                    viewModel.restartAllDepartures(context)
                },
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
