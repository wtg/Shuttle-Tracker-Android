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
        val LOCATION = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
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
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestAllPermissions() {
    var permissions = listOf(
        PermissionUtils.NOTIFICATION,
        PermissionUtils.LOCATION,
        PermissionUtils.BLUETOOTH,
    )

    permissions = permissions.filter { !rememberMultiplePermissionsState(it).shouldShowRationale }

    val permissionState = rememberMultiplePermissionsState(
        permissions = permissions.flatten(),
    )

    SideEffect {
        permissionState.launchMultiplePermissionRequest()
    }
}

/**
 * Requests for location permissions and gives a rational if denied
 * */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionsChecker(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {},
) {
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
@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BluetoothPermissionChecker(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {},
) {
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
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionChecker(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {},
) {
    val notificationPermissionState = rememberMultiplePermissionsState(
        PermissionUtils.NOTIFICATION,
    )

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

    SideEffect {
        if (!permissionState.shouldShowRationale) {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    // makes the rational dialog show every recomposition
    LaunchedEffect(false) {
        rationalDialogState = true
    }

    if (permissionState.allPermissionsGranted) {
        onPermissionGranted()
    } else if (permissionState.shouldShowRationale && rationalDialogState) {
        // shows the rational of why permission is needed
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
                }) {
                    Text(text = "I understand")
                }
            },
        )
    } else {
        // permissions not granted (user ignored the rational)
        rationalDialogState = false
        Toast.makeText(LocalContext.current, "Permissions required", Toast.LENGTH_SHORT).show()
        onPermissionDenied()
    }
}
