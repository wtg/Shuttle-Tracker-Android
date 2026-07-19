package edu.rpi.shuttletracker.background.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.app.MainActivity
import edu.rpi.shuttletracker.background.notification.Notifications
import edu.rpi.shuttletracker.data.repository.ShuttleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseService : FirebaseMessagingService() {
    @Inject
    lateinit var shuttleRepository: ShuttleRepository

    private val job = SupervisorJob()

    override fun onNewToken(token: String) {
        CoroutineScope(job).launch {
            shuttleRepository.sendRegistrationToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        message.notification?.let {
            it.body?.let { body -> sendNotification(body) }
        }
    }

    private fun sendNotification(body: String) {
        val notificationManager: NotificationManager =
            getSystemService(
                NOTIFICATION_SERVICE,
            ) as NotificationManager

        val notificationBody =
            NotificationCompat
                .Builder(
                    this,
                    Notifications.CHANNEL_ANNOUNCEMENT,
                ).setContentTitle("FCM")
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_stat_default)
                .setContentIntent(openMapPendingIntent())
                .build()

        notificationManager.notify(Notifications.ID_ANNOUNCEMENT, notificationBody)
    }

    private fun openMapPendingIntent(): PendingIntent {
        val intent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }
}
