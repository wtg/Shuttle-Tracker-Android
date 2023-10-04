package edu.rpi.shuttletracker.ui.settings

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BusAlert
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import edu.rpi.shuttletracker.ui.destinations.AboutScreenDestination
import edu.rpi.shuttletracker.ui.util.BackgroundLocationPermissionChecker
import edu.rpi.shuttletracker.ui.util.BluetoothPermissionChecker
import edu.rpi.shuttletracker.ui.util.SettingsItem
import edu.rpi.shuttletracker.util.services.BeaconService

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun SettingsScreen(
    navigator: DestinationsNavigator,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val settingsUiState = viewModel.settingsUiState.collectAsStateWithLifecycle().value

    var checkLocationPermissionsState by remember { mutableStateOf(false) }
    var checkBluetoothPermissionsState by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // checks for location permissions first, if they have check for bluetooth
    if (checkLocationPermissionsState) {
        BackgroundLocationPermissionChecker(
            onPermissionGranted = {
                checkBluetoothPermissionsState = true
                checkLocationPermissionsState = false
            },
            onPermissionDenied = { checkLocationPermissionsState = false },
        )
    }

    // if there is bluetooth permissions then start the beacon service
    if (checkBluetoothPermissionsState) {
        BluetoothPermissionChecker(
            onPermissionGranted = {
                context.startForegroundService(Intent(context, BeaconService::class.java))
                checkBluetoothPermissionsState = false
            },
            onPermissionDenied = { checkBluetoothPermissionsState = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Settings") },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.Outlined.ArrowBack, "back")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 10.dp)) {
            SettingsItem(icon = Icons.Outlined.BusAlert, "Auto board bus") {
                Switch(
                    checked = settingsUiState.autoBoardService,
                    onCheckedChange = {
                        if (it) {
                            checkLocationPermissionsState = true
                        } else {
                            viewModel.updateAutoBoardService(false)
                            context.stopService(Intent(context, BeaconService::class.java))
                        }
                    },
                )
            }

            SettingsItem(
                Icons.Outlined.Info,
                "About",
                onClick = { navigator.navigate(AboutScreenDestination()) },
            )
        }
    }
}
