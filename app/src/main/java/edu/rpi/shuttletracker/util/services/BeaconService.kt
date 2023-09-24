package edu.rpi.shuttletracker.util.services

import android.Manifest
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.util.notifications.Notifications
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.Identifier
import org.altbeacon.beacon.Region
import java.util.Date
import java.util.UUID

class BeaconService : Service() {

    private lateinit var beaconManager: BeaconManager
    private lateinit var region: Region
    private lateinit var rangingObserver: Observer<Collection<Beacon>>

    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()
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

        startForeground(Notifications.ID_AUTO_BOARD, notify())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // checks for bluetooth & location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN,
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // no bluetooth & location permissions
            stopSelf()
        }
        beaconManager.beaconParsers.apply {
            clear()
            add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))
        }

        rangingObserver = Observer { beacons ->
            if (beacons.isEmpty()) {
                // TODO DELETE
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .notify(Notifications.ID_DEBUG, notifyDebug(beacons.size))

                stopService(Intent(this, LocationService::class.java))
                return@Observer
            }

            val closest = beacons.minBy { it.distance }

            // TODO DELETE
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(Notifications.ID_DEBUG, notifyDebug(beacons.size, closest.distance, closest.id2.toString()))

            val serviceIntent = Intent(this, LocationService::class.java).apply {
                putExtra(LocationService.BUNDLE_BUS_ID, closest.id2.toString())
            }

            // startService(serviceIntent)
        }

        beaconManager.apply {
            // lets scanning occur in background for service
            setEnableScheduledScanJobs(false)

            getRegionViewModel(region).rangedBeacons.observeForever(rangingObserver)

            // scans every 30 sec for 1 sec in background
            backgroundBetweenScanPeriod = 30000
            backgroundScanPeriod = 1000

            // scans every 10 sec for 1 sec in background
            foregroundBetweenScanPeriod = 10000
            backgroundScanPeriod = 1000

            startRangingBeacons(region)
        }

        _isRunning.update { true }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        // stops looking for beacons
        beaconManager.getRegionViewModel(region).rangedBeacons.removeObserver(rangingObserver)
        beaconManager.stopRangingBeacons(region)
        _isRunning.update { false }
    }

    private fun notify() = NotificationCompat.Builder(
        this,
        Notifications.CHANNEL_AUTO_BOARD,
    ).setContentTitle("Auto-board service")
        .setSmallIcon(R.mipmap.ic_launcher)
        .build()

    private fun notifyDebug(buses: Int, distance: Double = -1.0, busNum: String = "") = NotificationCompat.Builder(
        this,
        Notifications.CHANNEL_DEBUG,
    ).setContentTitle("DEBUG INFO ${Date()}")
        .setContentText("$buses buses, distance: $distance, #$busNum")
        .setSmallIcon(R.mipmap.ic_launcher)
        .build()
}
