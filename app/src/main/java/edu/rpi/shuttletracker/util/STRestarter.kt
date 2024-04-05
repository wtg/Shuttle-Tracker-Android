package edu.rpi.shuttletracker.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.data.repositories.DeparturesRepository
import edu.rpi.shuttletracker.data.repositories.UserPreferencesRepository
import edu.rpi.shuttletracker.util.services.BeaconService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class STRestarter : BroadcastReceiver() {
    @Inject
    lateinit var userPreferencesRepository: Lazy<UserPreferencesRepository>

    @Inject
    lateinit var departuresRepository: DeparturesRepository

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                startAllAlarms(context)
                startBeaconService(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                startAllAlarms(context)
                startBeaconService(context)
            }
        }
    }

    private fun startBeaconService(context: Context) {
        runBlocking(Dispatchers.IO) {
            if (userPreferencesRepository.get().getAutoBoardService().first()) {
                context.startForegroundService(Intent(context, BeaconService::class.java))
            }
        }
    }

    private fun startAllAlarms(context: Context) {
        runBlocking(Dispatchers.IO) {
            departuresRepository.getAllDepartures().first().forEach { it.initiateAlarms(context) }
        }
    }
}
