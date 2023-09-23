package edu.rpi.shuttletracker.util.services

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.repositories.ShuttleTrackerRepository
import edu.rpi.shuttletracker.util.notifications.NotificationReceiver
import edu.rpi.shuttletracker.util.notifications.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/* reference https://github.com/tachiyomiorg/tachiyomi/blob/d4290f6f596dcafbe354eec51875680eb854d179/app/src/main/java/eu/kanade/tachiyomi/data/updater/AppUpdateService.kt#L33 */

@AndroidEntryPoint
class LocationService : Service() {

    @Inject
    lateinit var apiRepository: ShuttleTrackerRepository

    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var request: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    private lateinit var uuid: String

    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        const val BUNDLE_BUS_ID = "BUS_ID"
    }

    override fun onCreate() {
        // gets location changes of 2 meters ever 5 secs
        request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000,
        ).apply {
            setWaitForAccurateLocation(true)
            setMinUpdateDistanceMeters(2F)
        }.build()

        locationClient = LocationServices.getFusedLocationProviderClient(this)

        startForeground(
            Notifications.ID_TRACKING_PROGRESS,
            notify(),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val extras: Bundle = intent!!.extras!!

        val busNum = extras.getInt(BUNDLE_BUS_ID)
        uuid = UUID.randomUUID().toString()

        // checks for location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // no location permissions
            stopSelf()
        }

        with(NotificationManagerCompat.from(this)) {
            notify(Notifications.ID_TRACKING_PROGRESS, notify(busNum))

            notify(busNum)
        }

        // change in location
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val currentLocation = locationResult.lastLocation

                serviceScope.launch {
                    if (currentLocation != null) {
                        updateLocation(busNum, currentLocation, uuid)
                    }
                }
            }
        }

        // starts getting location changes
        locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())

        _isRunning.update { true }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        _isRunning.update { false }

        // unsubscribes from location updates
        locationClient.removeLocationUpdates(locationCallback)
    }

    private suspend fun updateLocation(busNum: Int, location: Location, uuid: String) {
        Log.d("PIEEE", "updateLocation: LOCATION CHANGED")
        // apiRepository.addBus(
        //    busNum,
        //    BoardBus(
        //        uuid,
        //        location.latitude,
        //        location.longitude,
        //        "User",
        //    ),
        // )
    }

    private fun notify() = NotificationCompat.Builder(
        this,
        Notifications.CHANNEL_TRACKER,
    ).setContentTitle("Launching Location Service")
        .setSmallIcon(R.mipmap.ic_launcher)
        .build()

    private fun notify(busNum: Int): Notification {
        return NotificationCompat.Builder(
            this,
            Notifications.CHANNEL_TRACKER,
        ).setContentTitle("Tracking bus $busNum")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentText("IM TRYING")
            .addAction(
                R.drawable.baseline_location_off_24,
                "Stop Tracking",
                NotificationReceiver.stopLocationService(this),
            )
            .build()
    }
}
