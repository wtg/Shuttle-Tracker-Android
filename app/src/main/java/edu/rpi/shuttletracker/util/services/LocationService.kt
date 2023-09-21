package edu.rpi.shuttletracker.util.services

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.repositories.ShuttleTrackerRepository
import edu.rpi.shuttletracker.util.notifications.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

    companion object {
        var IS_SERVICE_RUNNING: Boolean = false
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
        val busNum = 1
        val uuid = "KLASJD"

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return START_STICKY
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

        IS_SERVICE_RUNNING = true

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        IS_SERVICE_RUNNING = false

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
    ).setContentTitle("HALLO")
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentText("IM TRYING")
        .build()
}
