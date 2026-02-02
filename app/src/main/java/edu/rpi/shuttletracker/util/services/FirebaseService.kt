package edu.rpi.shuttletracker.util.services

import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.repositories.ApiRepository
import edu.rpi.shuttletracker.util.notifications.NotificationReceiver
import edu.rpi.shuttletracker.util.notifications.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseService : FirebaseMessagingService() {
    @Inject
    lateinit var apiRepository: ApiRepository

    private val job = SupervisorJob()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(job).launch {
            apiRepository.sendRegistrationToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        message.notification?.let {
            val title = it.title
            val body = it.body

            if (title != null && body != null) {
                sendNotification(title, body)
            }
        }
    }

    private fun sendNotification(
        title: String,
        body: String,
    ) {
        val notificationManager: NotificationManager =
            getSystemService(
                NOTIFICATION_SERVICE,
            ) as NotificationManager

        val notificationBody =
            NotificationCompat
                .Builder(
                    this,
                    Notifications.CHANNEL_ANNOUNCEMENT,
                ).setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_stat_default)
                .setContentIntent(NotificationReceiver.openMaps(this))
                .build()

        notificationManager.notify(Notifications.ID_ANNOUNCEMENT, notificationBody)
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    companion object {
        fun retrieveToken() = FirebaseMessaging.getInstance().token
    }
}
