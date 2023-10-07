package edu.rpi.shuttletracker.util.services

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.repositories.UserPreferencesRepository
import edu.rpi.shuttletracker.util.notifications.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.Identifier
import org.altbeacon.beacon.Region
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class BeaconService : Service() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private lateinit var beaconManager: BeaconManager
    private lateinit var region: Region
    private lateinit var rangingObserver: Observer<Collection<Beacon>>

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private val _permissionError = MutableStateFlow(false)
        val permissionError = _permissionError.asStateFlow()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        beaconManager = BeaconManager.getInstanceForApplication(this)
        region = Region(
            "all-beacons-region",
            Identifier.fromUuid(UUID.fromString("3bb7876d-403d-cb84-5e4c-907adc953f9c")),
            null,
            null,
        )

        beaconManager.beaconParsers.apply {
            add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    Notifications.ID_AUTO_BOARD,
                    notifyLaunch(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
                )
            } else {
                startForeground(
                    Notifications.ID_AUTO_BOARD,
                    notifyLaunch(),
                )
            }
        } catch (e: Exception) {
            _permissionError.update { true }
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        _permissionError.update { false }

        // checks for bluetooth & location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN,
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // no bluetooth & location permissions

            _permissionError.update { true }
            stopSelf()
            return START_STICKY
        }

        serviceScope.launch {
            userPreferencesRepository.saveAutoBoardService(true)
        }

        _isRunning.update { true }

        rangingObserver = Observer { beacons ->

            // gets the min distance beacon
            val closest = beacons.minByOrNull { it.distance }

            // no beacons nearby and runs till death
            if (closest == null) {
                stopService(Intent(this, LocationService::class.java))

                return@Observer
            }

            val closestId = closest.id2.toInt()
            if (LocationService.busNum.value == closestId) { return@Observer }

            val serviceIntent = Intent(this, LocationService::class.java).apply {
                putExtra(LocationService.BUNDLE_BUS_ID, closestId)
                putExtra(LocationService.BUNDLE_DISPLAY_ERROR, false)
            }

            startService(serviceIntent)
        }

        beaconManager.apply {
            // lets scanning occur in background for service
            setEnableScheduledScanJobs(false)

            getRegionViewModel(region).rangedBeacons.observeForever(rangingObserver)

            // scans every 30 sec for 10 sec in background
            backgroundBetweenScanPeriod = 30000
            backgroundScanPeriod = 10000

            // scans every 10 sec for 10 sec in background
            foregroundBetweenScanPeriod = 10000
            backgroundScanPeriod = 10000

            startRangingBeacons(region)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        serviceScope.launch {
            userPreferencesRepository.saveAutoBoardService(false)
        }

        _permissionError.update { false }
        _isRunning.update { false }

        try {
            // stops looking for beacons
            beaconManager.getRegionViewModel(region).rangedBeacons.removeObserver(rangingObserver)
            beaconManager.stopRangingBeacons(region)
        } catch (_: Exception) {}
    }

    private fun notifyLaunch() = NotificationCompat.Builder(
        this,
        Notifications.CHANNEL_AUTO_BOARD,
    ).setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setContentTitle("Auto-board service")
        .setSmallIcon(R.mipmap.ic_launcher_adaptive_fore)
        .build()
}
