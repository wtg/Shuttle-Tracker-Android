package edu.rpi.shuttletracker.ui.util

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.NearbyError
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class PermissionUtils {
    companion object {
        @RequiresApi(Build.VERSION_CODES.Q)
        val LOCATION = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        @RequiresApi(Build.VERSION_CODES.S)
        val BLUETOOTH = listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
        )

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        val NOTIFICATION = listOf(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/**
 * Checks all of the permissions necessary for the app to run
 * */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestAllPermissions() {
    var permissions = mutableListOf<List<String>>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(PermissionUtils.NOTIFICATION)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        permissions.add(PermissionUtils.LOCATION)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.add(PermissionUtils.BLUETOOTH)
    }

    // gets only those that does not need rationals
    permissions = permissions.filter {
        !rememberMultiplePermissionsState(it).shouldShowRationale
    }.toMutableList()

    // makes the list flat so its no longer 2d
    val permissionState = rememberMultiplePermissionsState(
        permissions = permissions.flatten(),
    )

    SideEffect {
        permissionState.launchMultiplePermissionRequest()
    }
}

/**
 * checks for location, background location, and bluetooth permissions
 * */
@Composable
fun AutoBoardingPermissionsChecker(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {},
) {
    var checkLocationPermissionsState by remember { mutableStateOf(true) }
    var checkBluetoothPermissionsState by remember { mutableStateOf(false) }

    // checks for location permissions first, if they have check for bluetooth
    if (checkLocationPermissionsState) {
        BackgroundLocationPermissionChecker(
            onPermissionGranted = {
                checkBluetoothPermissionsState = true
                checkLocationPermissionsState = false
            },
            onPermissionDenied = {
                onPermissionDenied()
                checkLocationPermissionsState = false
            },
        )
    }

    // if there is bluetooth permissions then start the beacon service
    if (checkBluetoothPermissionsState) {
        BluetoothPermissionChecker(
            onPermissionGranted = {
                onPermissionGranted()
                checkBluetoothPermissionsState = false
            },
            onPermissionDenied = {
                onPermissionDenied()
                checkBluetoothPermissionsState = false
            },
        )
    }
}

/**
 * Requests for location permissions and gives a rational if denied
 * */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BackgroundLocationPermissionChecker(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {},
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        LocationPermissionsChecker(
            onPermissionGranted,
            onPermissionDenied,
        )
        return
    }

    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ),
    )

    var showCheckBackgroundLocationPermissions by remember { mutableStateOf(false) }

    LocationPermissionsChecker(
        onPermissionGranted = {
            showCheckBackgroundLocationPermissions = true
        },
        onPermissionDenied = onPermissionDenied,
    )

    if (showCheckBackgroundLocationPermissions) {
        CheckPermission(
            locationPermissionsState,
            "Location",
            Icons.Outlined.LocationOff,
            "Background Location is needed to enable auto boarding\n\n" +
                "Click \"Allow all the time\" on the proceeding screen to enable",
            onPermissionGranted,
            onPermissionDenied,
        )
    }
}

/**
 * Requests for location permissions and gives a rational if denied
 * */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionsChecker(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {},
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        onPermissionGranted()
        return
    }

    val locationPermissionsState = rememberMultiplePermissionsState(
        PermissionUtils.LOCATION,
    )

    CheckPermission(
        locationPermissionsState,
        "Location",
        Icons.Outlined.LocationOff,
        "Location permissions are for you to know where you currently are and for others to be able to keep track of the buses",
        onPermissionGranted,
        onPermissionDenied,
    )
}

/**
 * Requests for bluetooth permissions and gives a rational if denied
 * */

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BluetoothPermissionChecker(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {},
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        onPermissionGranted()
        return
    }

    val bluetoothPermissionsState = rememberMultiplePermissionsState(
        PermissionUtils.BLUETOOTH,
    )

    CheckPermission(
        bluetoothPermissionsState,
        "Nearby devices",
        Icons.Outlined.NearbyError,
        "Nearby devices are needed to enable auto-boarding",
        onPermissionGranted,
        onPermissionDenied,
    )
}

/**
 * Requests for notification permissions and gives a rational if denied
 * */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionChecker(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {},
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        onPermissionGranted()
        return
    }

    val notificationPermissionState = rememberMultiplePermissionsState(
        PermissionUtils.NOTIFICATION,
    ) { result ->
        if (result.values.all { !it }) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }

    CheckPermission(
        notificationPermissionState,
        "Notifications",
        Icons.Outlined.NotificationsOff,
        "Notification permissions are for you to know which bus tracking services are currently running",
        onPermissionGranted,
        onPermissionDenied,
    )
}

/**
 * Creates an alert for the rational of what permissions would be needed
 * */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CheckPermission(
    permissionState: MultiplePermissionsState,
    permissions: String,
    icon: ImageVector,
    rational: String,
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {},
) {
    var rationalDialogState by remember { mutableStateOf(true) }

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            onPermissionGranted()
        }
    }

    if (permissionState.allPermissionsGranted) {
        onPermissionGranted()
    } else if (permissionState.shouldShowRationale && rationalDialogState) {
        AlertDialog(
            title = { Text(text = permissions) },
            icon = { Icon(icon, "Needed permission") },
            text = { Text(text = rational) },
            onDismissRequest = {
                rationalDialogState = false
                onPermissionDenied()
            },
            confirmButton = {
                Button(onClick = {
                    permissionState.launchMultiplePermissionRequest()
                    rationalDialogState = false
                }) {
                    Text(text = "I understand")
                }
            },
        )
    } else {
        // permissions not granted (user ignored the rational)
        LaunchedEffect(key1 = Unit) { permissionState.launchMultiplePermissionRequest() }
        Toast.makeText(LocalContext.current, "Permissions required", Toast.LENGTH_SHORT).show()
    }
}
