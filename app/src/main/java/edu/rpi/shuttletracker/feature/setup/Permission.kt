package edu.rpi.shuttletracker.feature.setup

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import edu.rpi.shuttletracker.R

/**
 * Contains all the permissions we need for the setup process
 * */
sealed class Permission(
    @param:StringRes val nameRes: Int,
    @param:StringRes val descriptionRes: Int,
    val permissions: Array<String>,
) {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    data object Notification : Permission(
        R.string.notifications,
        R.string.setup_notification_description,
        arrayOf(
            Manifest.permission.POST_NOTIFICATIONS,
        ),
    )

    data object Location : Permission(
        R.string.location,
        R.string.setup_location_description,
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ),
    )
}
