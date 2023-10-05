package edu.rpi.shuttletracker.util

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.data.repositories.UserPreferencesRepository
import edu.rpi.shuttletracker.util.services.BeaconService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class Restarter : BroadcastReceiver() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> startBeaconService(context)
            Intent.ACTION_BOOT_COMPLETED -> startBeaconService(context)
        }
    }

    private fun startBeaconService(context: Context) {
        // needs location all the time to run service
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            runBlocking(Dispatchers.IO) {
                if (userPreferencesRepository.getAutoBoardService().first()) {
                    context.startForegroundService(Intent(context, BeaconService::class.java))
                }
            }
        }
    }
}
