package edu.rpi.shuttletracker.ui.settings

import android.content.Intent
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
import edu.rpi.shuttletracker.ui.util.AutoBoardingPermissionsChecker
import edu.rpi.shuttletracker.ui.util.SettingsItem
import edu.rpi.shuttletracker.util.services.BeaconService

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun SettingsScreen(
    navigator: DestinationsNavigator,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val settingsUiState = viewModel.settingsUiState.collectAsStateWithLifecycle().value

    val context = LocalContext.current

    var checkAutoBoardPermissions by remember { mutableStateOf(false) }

    if (checkAutoBoardPermissions) {
        AutoBoardingPermissionsChecker(
            onPermissionGranted = {
                context.startForegroundService(Intent(context, BeaconService::class.java))
                checkAutoBoardPermissions = false
            },
            onPermissionDenied = { checkAutoBoardPermissions = false },
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
                            checkAutoBoardPermissions = !checkAutoBoardPermissions
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
