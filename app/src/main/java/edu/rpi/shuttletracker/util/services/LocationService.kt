package edu.rpi.shuttletracker.util.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.location.Location
import android.net.Uri
import android.os.Build
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
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.AnalyticsFactory
import edu.rpi.shuttletracker.data.models.BoardBus
import edu.rpi.shuttletracker.data.repositories.ApiRepository
import edu.rpi.shuttletracker.data.repositories.UserPreferencesRepository
import edu.rpi.shuttletracker.util.notifications.NotificationReceiver
import edu.rpi.shuttletracker.util.notifications.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject


/* reference https://github.com/tachiyomiorg/tachiyomi/blob/d4290f6f596dcafbe354eec51875680eb854d179/app/src/main/java/eu/kanade/tachiyomi/data/updater/AppUpdateService.kt#L33 */

@AndroidEntryPoint
class LocationService : Service() {

    @Inject
    lateinit var apiRepository: ApiRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var analyticsFactory: AnalyticsFactory

    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var request: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {

        private val _startedManual = MutableStateFlow<Boolean?>(null)
        val startedManual = _startedManual.asStateFlow()

        private val _busNum = MutableStateFlow<Int?>(null)
        val busNum = _busNum.asStateFlow()

        private val _permissionError = MutableStateFlow(false)
        val permissionError = _permissionError.asStateFlow()

        const val BUNDLE_BUS_ID = "BUS_ID"
        const val STARTED_MANUAL = "STARTED_MANUAL"
    }

    override fun onCreate() {
        super.onCreate()

        // gets location changes every 5 secs
        request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000,
        ).apply {
            setWaitForAccurateLocation(true)
        }.build()

        locationClient = LocationServices.getFusedLocationProviderClient(this)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    Notifications.ID_TRACKING_BUS,
                    notifyLaunch(),
                    FOREGROUND_SERVICE_TYPE_LOCATION,
                )
            } else {
                startForeground(
                    Notifications.ID_TRACKING_BUS,
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

        val extras: Bundle = intent!!.extras!!

        val busNum = extras.getInt(BUNDLE_BUS_ID)
        val startedManual = extras.getBoolean(STARTED_MANUAL, true)
        val startTime = System.currentTimeMillis()

        _permissionError.update { false }

        // checks for location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // no location permissions
            _permissionError.update { true }
            stopSelf()
            return START_NOT_STICKY
        }

        //Notific
        with(NotificationManagerCompat.from(this)) {
            notify(Notifications.ID_TRACKING_BUS, notify(busNum))

            notify(busNum)
        }

        runBlocking { userPreferencesRepository.incrementBoardBusCount() }

        val analytics = runBlocking { analyticsFactory.build(startedManual) }

        val uuid = UUID.randomUUID().toString()

        serviceScope.launch {
            if (userPreferencesRepository.getAllowAnalytics().first()) {
                apiRepository.addAnalytics(analytics)
            }
        }

        // change in location
        locationCallback = object : LocationCallback() {
            @SuppressLint("MissingPermission")
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val currentLocation = locationResult.lastLocation

                serviceScope.launch {
                    if (currentLocation != null) {
                        updateLocation(busNum, currentLocation, uuid)

                        // been on bus for 10 min
                        if (System.currentTimeMillis() - startTime >=
                            TimeUnit.MINUTES.toMillis(10)
                        ) {
                            var notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.notify(Notifications.ID_LEAVE_BUS, notifyLeaveBus(busNum))
                            notify(busNum)
                        }
                    }
                }
            }
        }

        // starts getting location changes
        locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())

        _busNum.update { busNum }
        _startedManual.update { startedManual }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        _permissionError.update { false }
        _busNum.update { null }

        try {
            // unsubscribes from location updates
            locationClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {}
        var notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(Notifications.ID_LEAVE_BUS)
    }

    /**
     * Sends updated bus location to server
     * */
    private suspend fun updateLocation(
        busNum: Int,
        location: Location,
        uuid: String,
    ) = apiRepository.addBus(
        busNum,
        BoardBus(
            uuid,
            location.latitude,
            location.longitude,
            "user",
        ),
    )

    private fun notifyLaunch() = NotificationCompat.Builder(
        this,
        Notifications.CHANNEL_TRACKING_BUS,
    ).setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setContentTitle(getString(R.string.notification_location_launching))
        .setSmallIcon(R.drawable.ic_stat_default)
        .setContentIntent(NotificationReceiver.openMaps(this))
        .build()

    private fun notify(busNum: Int): Notification {
        return NotificationCompat.Builder(
            this,
            Notifications.CHANNEL_TRACKING_BUS,
        ).setContentTitle(getString(R.string.notification_tracking_bus, busNum))
            .setSmallIcon(R.drawable.ic_stat_default)
            .addAction(
                R.drawable.baseline_location_off_24,
                getString(R.string.notification_stop_tracking),
                NotificationReceiver.stopLocationService(this),
            )
            .setContentIntent(NotificationReceiver.openMaps(this))
            .build()
    }

    private fun notifyLeaveBus(busNum: Int): Notification {
        return NotificationCompat.Builder(
            this,
            Notifications.CHANNEL_LEAVE_BUS,
        ).setContentTitle(String.format("Hey, you're still on bus %d", busNum))
            .setSmallIcon(R.drawable.ic_stat_default)
            .addAction(
                R.drawable.baseline_location_off_24,
                getString(R.string.notification_stop_tracking),
                NotificationReceiver.stopLocationService(this),
            )
            .setAutoCancel(true)
            .build()
    }
}

