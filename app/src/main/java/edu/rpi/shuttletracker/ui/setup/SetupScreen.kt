package edu.rpi.shuttletracker.ui.setup

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.Intent.CATEGORY_DEFAULT
import android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_NO_HISTORY
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.LocationDisabled
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.NearbyError
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.popUpTo
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.ui.destinations.MapsScreenDestination
import edu.rpi.shuttletracker.ui.destinations.SetupScreenDestination
import edu.rpi.shuttletracker.util.services.BeaconService
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

const val TOTAL_PAGES = 4

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@RootNavGraph(start = true)
@Destination
@Composable
fun SetupScreen(
    navigator: DestinationsNavigator,
    viewModel: SetupScreenViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { TOTAL_PAGES })

    var sectionHeader by remember { mutableStateOf("") }

    val skipSetupDialog = remember { mutableStateOf(false) }

    val setupUiState = viewModel.setupUiState.collectAsStateWithLifecycle().value

    val context = LocalContext.current

    // a dialog will show if they want to skip the setup
    BackHandler {
        skipSetupDialog.value = true
    }

    // dialog shown when skipping setup
    if (skipSetupDialog.value) {
        SkipSetup(showDialog = skipSetupDialog) {
            navigator.navigate(MapsScreenDestination()) {
                popUpTo(SetupScreenDestination) {
                    inclusive = true
                }
            }
        }
    }

    BeaconService.isRunning.collectAsStateWithLifecycle().value.let {
        LaunchedEffect(it) {
            if (it) {
                navigator.navigate(MapsScreenDestination()) {
                    popUpTo(SetupScreenDestination) {
                        inclusive = true
                    }
                }
            }
        }
    }

    // when pages change, change title name
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collect { page ->
            when (page) {
                0 -> sectionHeader = context.getString(R.string.about)
                1 -> sectionHeader = context.getString(R.string.privacy_policy)
                2 -> sectionHeader = context.getString(R.string.permissions)
                3 -> sectionHeader = context.getString(R.string.automatic_board_bus)
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()

    /**
     * Navigates to the next page
     * */
    fun toNextPage(current: Int = pagerState.targetPage) {
        if (current == TOTAL_PAGES - 1) {
            navigator.navigate(MapsScreenDestination()) {
                popUpTo(SetupScreenDestination) {
                    inclusive = true
                }
            }
        } else {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.targetPage + 1)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(text = sectionHeader) }) },
        bottomBar = {
            Column {
                // shows how far you are in setup screen
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = (pagerState.currentPage) / (TOTAL_PAGES - 1).toFloat(),
                )
                BottomAppBar(
                    actions = {
                        IconButton(onClick = { skipSetupDialog.value = true }) {
                            Icon(Icons.Outlined.SkipNext, stringResource(R.string.skip_setup))
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { toNextPage() },

                        ) {
                            if (pagerState.currentPage == TOTAL_PAGES - 1) {
                                Icon(Icons.Outlined.Done, stringResource(R.string.complete_setup))
                            } else {
                                Icon(
                                    Icons.Outlined.ArrowForward,
                                    stringResource(R.string.next_page),
                                )
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (it) {
                0 -> TextScreen(
                    onAccept = { toNextPage(0) },
                    acceptedState = setupUiState.aboutAccepted,
                    updateState = viewModel::updateAboutAccepted,
                    text = stringResource(R.string.info_intro),
                    title = stringResource(R.string.about),
                )

                1 -> TextScreen(
                    onAccept = { toNextPage(1) },
                    acceptedState = setupUiState.privacyPolicyAccepted,
                    updateState = viewModel::updatePrivacyPolicyAccepted,
                    text = stringResource(R.string.privacy),
                    title = stringResource(R.string.privacy_policy),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = setupUiState.allowAnalytics,
                            onCheckedChange = { checked -> viewModel.updateAllowAnalytics(checked) },
                        )

                        Text(text = "Agree to share analytics")
                    }
                }

                2 -> PermissionPage { toNextPage(2) }
                3 -> AutoBoardingPage { toNextPage(3) }
            }
        }
    }
}

@Composable
fun TextScreen(
    onAccept: () -> Unit,
    acceptedState: Boolean,
    updateState: () -> Unit,
    text: String,
    title: String,
    extra: @Composable () -> Unit = {},
) {
    SideEffect {
        if (acceptedState) onAccept()
    }

    Column(modifier = Modifier.padding(20.dp)) {
        Text(text = text)

        Spacer(modifier = Modifier.height(10.dp))

        if (!acceptedState) {
            Button(onClick = { updateState() }) {
                Text(text = stringResource(R.string.accept, title.lowercase()))
            }
        } else {
            Text(text = stringResource(R.string.acknowledged, title))
        }

        extra()
    }
}

/**
 * Page asking for general permissions
 * */
@Composable
fun PermissionPage(
    allPermissionsGranted: () -> Unit,
) {
    val hasLocationPermissions = remember { mutableStateOf(false) }
    val hasNotificationPermissions = remember { mutableStateOf(false) }

    // when all the permissions are granted, call function
    SideEffect {
        if (hasLocationPermissions.value &&
            hasNotificationPermissions.value
        ) {
            allPermissionsGranted()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ask for notification permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = arrayOf(
                Manifest.permission.POST_NOTIFICATIONS,
            )

            PermissionItem(
                permission = notificationPermission,
                state = hasNotificationPermissions,
                title = stringResource(R.string.notifications),
                description = stringResource(R.string.notification_rational),
                deniedIcon = Icons.Outlined.NotificationsOff,
            )
        } else {
            hasNotificationPermissions.value = true
        }

        // ask for location permissions
        val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        PermissionItem(
            permission = locationPermissions,
            state = hasLocationPermissions,
            title = stringResource(R.string.location),
            description = stringResource(R.string.location_rational),
            deniedIcon = Icons.Outlined.LocationOff,
        )
    }
}

/**
 * Page asking for auto boarder permissions
 * */
@Composable
fun AutoBoardingPage(
    allPermissionsGranted: () -> Unit,
) {
    val context = LocalContext.current
    val hasBluetoothPermissions = remember { mutableStateOf(false) }
    val hasBackgroundLocationPermissions = remember { mutableStateOf(false) }
    val isAutoBoardingServiceRunning = BeaconService.isRunning.collectAsStateWithLifecycle().value

    // when all the permissions/auto boarding is granted, call function
    SideEffect {
        if (hasBluetoothPermissions.value &&
            hasBackgroundLocationPermissions.value &&
            isAutoBoardingServiceRunning
        ) {
            allPermissionsGranted()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ask for bluetooth permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bluetoothPermissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            )

            item {
                PermissionItem(
                    permission = bluetoothPermissions,
                    state = hasBluetoothPermissions,
                    title = stringResource(R.string.bluetooth),
                    description = stringResource(R.string.bluetooth_rational),
                    deniedIcon = Icons.Outlined.NearbyError,
                )
            }
        } else {
            hasBluetoothPermissions.value = true
        }

        // ask for background location permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val backgroundLocationPermissions = arrayOf(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            )

            item {
                PermissionItem(
                    permission = backgroundLocationPermissions,
                    state = hasBackgroundLocationPermissions,
                    title = stringResource(R.string.background_location),
                    description = stringResource(R.string.background_location_rational),
                    deniedIcon = Icons.Outlined.LocationDisabled,
                )
            }
        } else {
            hasBackgroundLocationPermissions.value = true
        }

        // ask to enable auto boarding
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    text = stringResource(R.string.automatic_board_bus),
                    style = MaterialTheme.typography.headlineLarge,
                )

                Text(
                    text = if (!isAutoBoardingServiceRunning) {
                        stringResource(R.string.automatic_board_bus_rational)
                    } else {
                        stringResource(R.string.automatic_board_bus_enabled)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                if (!isAutoBoardingServiceRunning) {
                    Button(onClick = {
                        context.startForegroundService(Intent(context, BeaconService::class.java))
                    }) {
                        Text(text = stringResource(R.string.enable_automatic_board_bus))
                    }
                }
            }
        }
    }
}

const val DENIED = "denied"
const val EXPLAINED = "explained"

/**
 * Item to ask and update permission states
 * */
@Composable
fun PermissionItem(
    permission: Array<String>,
    state: MutableState<Boolean>,
    title: String,
    description: String,
    deniedIcon: ImageVector = Icons.Outlined.Close,
) {
    val context = LocalContext.current

    state.value = permission.all {
        ContextCompat.checkSelfPermission(
            context,
            it,
        ) == PackageManager.PERMISSION_GRANTED
    }

    val showDialog = remember { mutableStateOf(false) }

    if (showDialog.value) {
        ToSettingsAlertDialog(showDialog)
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { isGranted ->
        val deniedList: List<String> = isGranted.filter {
            !it.value
        }.map {
            it.key
        }

        when {
            deniedList.isNotEmpty() -> {
                val map = deniedList.groupBy { permission ->
                    if (shouldShowRequestPermissionRationale(context as Activity, permission)) {
                        DENIED
                    } else {
                        EXPLAINED
                    }
                }

                // request denied, request again
                map[DENIED]?.let {
                    /* IGNORED */
                }

                // request denied, send to settings
                map[EXPLAINED]?.let {
                    showDialog.value = true
                }
            }
            else -> {
                // All request are permitted
                state.value = true
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
    ) {
        if (!state.value) {
            Icon(deniedIcon, stringResource(R.string.permission_denied))
        }

        Text(text = title, style = MaterialTheme.typography.headlineLarge)

        Text(
            text = if (!state.value) description else stringResource(R.string.permission_granted),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (!state.value) {
            Button(onClick = {
                launcher.launch(permission)
            }) {
                Text(text = stringResource(R.string.grant_permissions))
            }
        }
    }
}

/**
 * Alert dialog that is shown when user denies permissions
 * and has to be redirected to settings
 * */
@Composable
fun ToSettingsAlertDialog(
    showDialog: MutableState<Boolean>,
) {
    val context = LocalContext.current

    if (showDialog.value) {
        AlertDialog(
            title = { Text(text = stringResource(R.string.permissions)) },
            text = { Text(text = stringResource(R.string.to_settings_explanation)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog.value = false

                        val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                        with(intent) {
                            data = Uri.fromParts("package", context.packageName, null)
                            addCategory(CATEGORY_DEFAULT)
                            addFlags(FLAG_ACTIVITY_NEW_TASK)
                            addFlags(FLAG_ACTIVITY_NO_HISTORY)
                            addFlags(FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        }

                        context.startActivity(intent)
                    },
                ) {
                    Text(text = stringResource(R.string.to_settings))
                }
            },
            dismissButton = {
                Button(onClick = { showDialog.value = false }) {
                    Text(text = stringResource(R.string.not_now))
                }
            },
            onDismissRequest = { showDialog.value = false },
        )
    }
}

@Composable
fun SkipSetup(
    showDialog: MutableState<Boolean>,
    navigateToMaps: () -> Unit,
) {
    if (showDialog.value) {
        AlertDialog(
            title = { Text(text = stringResource(R.string.skip_setup)) },
            text = { Text(text = stringResource(R.string.skip_confirmation)) },
            dismissButton = {
                Button(onClick = { showDialog.value = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                Button(onClick = {
                    navigateToMaps()
                    showDialog.value = false
                }) {
                    Text(text = stringResource(R.string.skip))
                }
            },
            onDismissRequest = { showDialog.value = false },
        )
    }
}
