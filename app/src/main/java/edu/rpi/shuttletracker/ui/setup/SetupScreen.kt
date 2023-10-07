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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.popUpTo
import edu.rpi.shuttletracker.ui.destinations.MapsScreenDestination
import edu.rpi.shuttletracker.ui.destinations.SetupScreenDestination
import edu.rpi.shuttletracker.util.services.BeaconService
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

const val DENIED = "denied"
const val EXPLAINED = "explained"
const val TOTAL_PAGES = 2

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Destination(start = true)
@Composable
fun SetupScreen(
    navigator: DestinationsNavigator,
) {
    val pagerState = rememberPagerState(pageCount = { TOTAL_PAGES })

    var sectionHeader by remember { mutableStateOf("") }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collect { page ->
            when (page) {
                0 -> sectionHeader = "Permissions"
                1 -> sectionHeader = "Auto boarding"
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()

    /**
     * Navigates to the next page, but if at end it will go to maps
     * */
    fun toNextPage() {
        if (pagerState.targetPage == TOTAL_PAGES - 1) {
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
            BottomAppBar(
                actions = {},
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { toNextPage() },

                    ) {
                        if (pagerState.currentPage == TOTAL_PAGES - 1) {
                            Icon(Icons.Outlined.Done, "Complete setup")
                        } else {
                            Icon(Icons.Outlined.ArrowForward, "Next page")
                        }
                    }
                },
            )
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (it) {
                0 -> PermissionPage { toNextPage() }
                1 -> AutoBoardingPage { toNextPage() }
            }
        }
    }
}

@Composable
fun PermissionPage(
    allPermissionsGranted: () -> Unit,
) {
    val context = LocalContext.current
    val hasLocationPermissions = remember { mutableStateOf(false) }
    val hasNotificationPermissions = remember { mutableStateOf(false) }

    LaunchedEffect(
        key1 = hasLocationPermissions.value,
        key2 = hasNotificationPermissions.value,
    ) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = arrayOf(
                Manifest.permission.POST_NOTIFICATIONS,
            )

            hasNotificationPermissions.value =
                notificationPermission.all {
                    ContextCompat.checkSelfPermission(
                        context,
                        it,
                    ) == PackageManager.PERMISSION_GRANTED
                }

            PermissionItem(
                permission = notificationPermission,
                state = hasNotificationPermissions,
                title = "Notifications",
                description = "Notifications are for you to know whether or not any bus tracking services are running",
                deniedIcon = Icons.Outlined.NotificationsOff,
            )
        } else {
            hasNotificationPermissions.value = true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val locationPermissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )

            hasLocationPermissions.value =
                locationPermissions.all {
                    ContextCompat.checkSelfPermission(
                        context,
                        it,
                    ) == PackageManager.PERMISSION_GRANTED
                }

            PermissionItem(
                permission = locationPermissions,
                state = hasLocationPermissions,
                title = "Location",
                description = "Location is needed to show your current location and to share bus locations with others",
                deniedIcon = Icons.Outlined.LocationOff,
            )
        } else {
            hasNotificationPermissions.value = true
        }
    }
}

@Composable
fun AutoBoardingPage(
    allPermissionsGranted: () -> Unit,
) {
    val context = LocalContext.current
    val hasBluetoothPermissions = remember { mutableStateOf(false) }
    val hasBackgroundLocationPermissions = remember { mutableStateOf(false) }
    val isAutoBoardingServiceRunning = BeaconService.isRunning.collectAsStateWithLifecycle().value

    LaunchedEffect(
        key1 = hasBluetoothPermissions.value,
        key2 = hasBackgroundLocationPermissions.value,
        key3 = isAutoBoardingServiceRunning,
    ) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bluetoothPermissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            )

            hasBluetoothPermissions.value =
                bluetoothPermissions.all {
                    ContextCompat.checkSelfPermission(
                        context,
                        it,
                    ) == PackageManager.PERMISSION_GRANTED
                }

            item {
                PermissionItem(
                    permission = bluetoothPermissions,
                    state = hasBluetoothPermissions,
                    title = "Bluetooth",
                    description = "Bluetooth is needed to find nearby beacons for auto boarding",
                    deniedIcon = Icons.Outlined.NearbyError,
                )
            }
        } else {
            hasBluetoothPermissions.value = true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val backgroundLocationPermissions = arrayOf(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            )

            hasBluetoothPermissions.value =
                backgroundLocationPermissions.all {
                    ContextCompat.checkSelfPermission(
                        context,
                        it,
                    ) == PackageManager.PERMISSION_GRANTED
                }

            item {
                PermissionItem(
                    permission = backgroundLocationPermissions,
                    state = hasBackgroundLocationPermissions,
                    title = "Background Location",
                    description = "Background location is needed auto start auto boarding services on devices restart/app update\n\n" +
                        "Click \"Allow all the time\" in location permission\n\n" +
                        "Location permissions must be granted prior on the previous page",
                    deniedIcon = Icons.Outlined.LocationDisabled,
                )
            }
        } else {
            hasBackgroundLocationPermissions.value = true
        }

        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(text = "Auto boarding", style = MaterialTheme.typography.headlineLarge)

                Text(
                    text = if (!isAutoBoardingServiceRunning) {
                        "Auto boarding lets you share your location to everyone else when a bus beacon is detected nearby\n\n" +
                            "This requires the permissions above"
                    } else {
                        "Auto boarding enabled, Thank you!"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                if (!isAutoBoardingServiceRunning) {
                    Button(onClick = {
                        context.startForegroundService(Intent(context, BeaconService::class.java))
                    }) {
                        Text(text = "Enable auto boarding")
                    }
                }
            }
        }
    }
}

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
            Icon(deniedIcon, "Permission State")
        }

        Text(text = title, style = MaterialTheme.typography.headlineLarge)

        Text(
            text = if (!state.value) description else "Permission granted, Thank you!",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (!state.value) {
            Button(onClick = {
                launcher.launch(permission)
            }) {
                Text(text = "Grant permissions")
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
            title = { Text(text = "Permissions") },
            text = { Text(text = "Go to settings to enable permissions?") },
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
                    Text(text = "To settings")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog.value = false }) {
                    Text(text = "Not now")
                }
            },
            onDismissRequest = { showDialog.value = false },
        )
    }
}
