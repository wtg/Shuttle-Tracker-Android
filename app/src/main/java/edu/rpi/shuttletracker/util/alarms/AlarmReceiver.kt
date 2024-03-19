package edu.rpi.shuttletracker.util.alarms

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.haroldadmin.cnradapter.NetworkResponse
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Departure
import edu.rpi.shuttletracker.data.repositories.ApiRepository
import edu.rpi.shuttletracker.util.notifications.NotificationReceiver
import edu.rpi.shuttletracker.util.notifications.Notifications
import edu.rpi.shuttletracker.util.notifications.Notifications.ID_FIRING_DEPARTURE
import edu.rpi.shuttletracker.util.notifications.goAsync
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    @Inject
    lateinit var apiRepository: ApiRepository

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        departureFired(
            context,
            intent.getStringExtra(Departure.INTENT_STOP_NAME) ?: "",
            intent.getDoubleExtra(Departure.INTENT_LATITUDE, 0.0),
            intent.getDoubleExtra(Departure.INTENT_LONGITUDE, 0.0),
            intent.getIntExtra(Departure.INTENT_ID, 0),
        )
    }

    private fun departureFired(
        context: Context,
        name: String,
        latitude: Double,
        longitude: Double,
        id: Int,
    ) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification =
            NotificationCompat.Builder(
                context,
                Notifications.CHANNEL_FIRING_DEPARTURES,
            ).setContentTitle("Fetching buses approaching $name...")
                .setSmallIcon(R.drawable.ic_stat_default)
                .setContentIntent(NotificationReceiver.openMaps(context))

        with(NotificationManagerCompat.from(context)) {
            notify(ID_FIRING_DEPARTURE + id, notification.build())
        }

        goAsync {
            val buses = apiRepository.getApproachingBuses(latitude, longitude)

            notification.apply {
                if (buses is NetworkResponse.Success) {
                    if (buses.body.isEmpty()) {
                        setContentTitle("There are no tracked busses approaching $name")
                        return@apply
                    }

                    setContentTitle("Buses approaching $name")
                    setContentText("${buses.body.size} buses are approaching")
                    setStyle(
                        NotificationCompat
                            .BigTextStyle()
                            .bigText(
                                """
                                    ${buses.body.size} buses are approaching $name
                                    ${buses.body.joinToString("\n")}
                                    """,
                            ),
                    )
                } else {
                    setContentTitle("Unable to get approaching buses")
                }
            }

            with(NotificationManagerCompat.from(context)) {
                notify(ID_FIRING_DEPARTURE + id, notification.build())
            }
        }
    }
}
