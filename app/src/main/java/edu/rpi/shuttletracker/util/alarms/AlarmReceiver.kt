package edu.rpi.shuttletracker.util.alarms

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.repositories.ApiRepository
import edu.rpi.shuttletracker.util.notifications.NotificationReceiver
import edu.rpi.shuttletracker.util.notifications.Notifications
import edu.rpi.shuttletracker.util.notifications.Notifications.ID_FIRING_DEPARTURE
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
            intent.getStringExtra("stop") ?: "",
            intent.getIntExtra("id", 0),
        )
    }

    private fun departureFired(
        context: Context,
        name: String,
        id: Int,
    ) {
        val notification =
            NotificationCompat.Builder(
                context,
                Notifications.CHANNEL_FIRING_DEPARTURES,
            ).setContentTitle("$name Placeholder for $id")
                .setSmallIcon(R.drawable.ic_stat_default)
                .setContentIntent(NotificationReceiver.openMaps(context))

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            notify(ID_FIRING_DEPARTURE + id, notification.build())
        }
    }
}
