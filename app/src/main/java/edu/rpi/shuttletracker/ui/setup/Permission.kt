package edu.rpi.shuttletracker.ui.setup

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Contains all the permissions we need for the setup process
 * */
sealed class Permission(
    val name: String,
    val description: String,
    val permissions: Array<String>,
) {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    data object Notification : Permission(
        "Notifications",
        "Receive announcements and more.",
        arrayOf(
            Manifest.permission.POST_NOTIFICATIONS,
        ),
    )

    data object Location : Permission(
        "Location",
        "See where you are and crowd source bus data.",
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ),
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    data object BackgroundLocation : Permission(
        "Background Location",
        "Crowd source bus data with the app closed.",
        arrayOf(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ),
    )

    @RequiresApi(Build.VERSION_CODES.S)
    data object Bluetooth : Permission(
        "Bluetooth",
        "Detect nearby buses for auto-boarding.",
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
        ),
    )
}
