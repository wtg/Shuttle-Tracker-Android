package edu.rpi.shuttletracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import com.ramcosta.composedestinations.DestinationsNavHost
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.ui.theme.ShuttleTrackerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appUpdateManager = AppUpdateManagerFactory.create(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        checkForAppUpdate()

        setContent {
            ShuttleTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    DestinationsNavHost(navGraph = NavGraphs.root)
                }
            }
        }
    }

    private fun checkForAppUpdate() {
        val activityLauncher =
            registerForActivityResult(
                ActivityResultContracts
                    .StartIntentSenderForResult(),
            ) { result: ActivityResult ->
                // handle callback
            }

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        // Create a listener to track request state updates.
        val listener = InstallStateUpdatedListener { state ->
            // (Optional) Provide a download progress bar.
            if (state.installStatus() == InstallStatus.DOWNLOADING) {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytesToDownload = state.totalBytesToDownload()
                // Show update progress bar.
            }
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                // After the update is downloaded, show a notification
                // and request user confirmation to restart the app.
                // popupSnackbarForCompleteUpdate()
            }

            // Log state or install the update.
        }
        // Start an update
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val updateAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            val updateAllowed = when (appUpdateInfo.updatePriority()) {
                AppUpdateType.FLEXIBLE -> appUpdateInfo.isFlexibleUpdateAllowed
                AppUpdateType.IMMEDIATE -> appUpdateInfo.isImmediateUpdateAllowed
                else -> false
            }

            // Flexible update
            if (updateAvailable && updateAllowed) {
                // Before starting an update, register a listener for updates.
                appUpdateManager.registerListener(listener)
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    activityLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                )
                // When status updates are no longer needed, unregister the listener.
                appUpdateManager.unregisterListener(listener)
            }
            // Immediate update
            else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.updatePriority() >= 4 && // high priority
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    activityLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                        .setAllowAssetPackDeletion(true)
                        .build(),
                )
            }
        }
    }
}
