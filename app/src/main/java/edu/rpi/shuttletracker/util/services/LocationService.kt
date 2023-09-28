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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.haroldadmin.cnradapter.NetworkResponse
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.BoardBus
import edu.rpi.shuttletracker.data.models.ErrorResponse
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

    companion object {
        private val _busNum = MutableStateFlow<Int?>(null)
        val busNum = _busNum.asStateFlow()

        private val _error = MutableStateFlow<NetworkResponse<*, ErrorResponse>?>(null)
        val error = _error.asStateFlow()

        const val BUNDLE_BUS_ID = "BUS_ID"
        const val BUNDLE_DISPLAY_ERROR = "DISPLAY_ERROR"

        fun dismissError() {
            _error.update { null }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // gets location changes of 2 meters ever 5 secs
        request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000,
        ).apply {
            setWaitForAccurateLocation(true)
            setMinUpdateDistanceMeters(2F)
        }.build()

        locationClient = LocationServices.getFusedLocationProviderClient(this)

        startForeground(Notifications.ID_TRACKING_BUS, notifyLaunch())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val extras: Bundle = intent!!.extras!!

        val busNum = extras.getInt(BUNDLE_BUS_ID)
        val displayError = extras.getBoolean(BUNDLE_DISPLAY_ERROR, true)

        _error.update { null }

        // checks for location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // no location permissions
            stopSelf()
        }

        with(NotificationManagerCompat.from(this)) {
            notify(Notifications.ID_TRACKING_BUS, notify(busNum))

            notify(busNum)
        }

        // change in location
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val currentLocation = locationResult.lastLocation

                serviceScope.launch {
                    if (currentLocation != null) {
                        updateLocation(busNum, currentLocation, displayError)
                    }
                }
            }
        }

        // starts getting location changes
        locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())

        _busNum.update { busNum }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        _busNum.update { null }

        // unsubscribes from location updates
        locationClient.removeLocationUpdates(locationCallback)
    }

    /**
     * Sends updated bus location to server
     * */
    private suspend fun updateLocation(busNum: Int, location: Location, displayError: Boolean) {
        val response = apiRepository.addBus(
            busNum,
            BoardBus(
                UUID.randomUUID().toString(),
                location.latitude,
                location.longitude,
                "user",
            ),
        )

        // report error and end service
        if (
            response is NetworkResponse.NetworkError ||
            response is NetworkResponse.UnknownError ||
            response is NetworkResponse.ServerError
        ) {
            if (displayError) _error.update { response }
            stopSelf()
        }
    }

    private fun notifyLaunch() = NotificationCompat.Builder(
        this,
        Notifications.CHANNEL_TRACKING_BUS,
    ).setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setContentTitle("Launching Location Service")
        .setSmallIcon(R.mipmap.ic_launcher_adaptive_fore)
        .build()

    private fun notify(busNum: Int): Notification {
        return NotificationCompat.Builder(
            this,
            Notifications.CHANNEL_TRACKING_BUS,
        ).setContentTitle("Tracking bus $busNum")
            .setSmallIcon(R.mipmap.ic_launcher_adaptive_fore)
            .setContentText("IM TRYING")
            .addAction(
                R.drawable.baseline_location_off_24,
                "Stop Tracking",
                NotificationReceiver.stopLocationService(this),
            )
            .setContentIntent(NotificationReceiver.openMaps(this))
            .build()
    }
}
