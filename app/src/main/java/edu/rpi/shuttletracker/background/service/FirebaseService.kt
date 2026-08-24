package edu.rpi.shuttletracker.background.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.BuildConfig
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.app.MainActivity
import edu.rpi.shuttletracker.background.notification.Notifications

/** Renders staff-sent Firebase messages independently of API announcement banners. */
@AndroidEntryPoint
class FirebaseService : FirebaseMessagingService() {
    /** Logs the registration token in debug builds for Firebase test messages. */
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        if (BuildConfig.DEBUG) Log.d("FCM_TOKEN", token)
    }

    /** Handles foreground messages; Firebase renders background messages from manifest defaults. */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val notification = message.notification ?: return
        val title = notification.title ?: return
        val body = notification.body ?: return

        showNotification(
            id = notificationIdFor(message),
            title = title,
            body = body,
            url = message.data[EXTRA_URL],
        )
    }

    private fun showNotification(
        id: Int,
        title: String,
        body: String,
        url: String?,
    ) {
        val notificationManager = getSystemService(NotificationManager::class.java)

        val notificationBody =
            NotificationCompat
                .Builder(this, Notifications.CHANNEL_PUSH)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_stat_default)
                .setContentIntent(mapPendingIntent(id, url))
                .setAutoCancel(true)
                .build()

        notificationManager.notify(id, notificationBody)
    }

    private fun mapPendingIntent(
        id: Int,
        url: String?,
    ): PendingIntent {
        val intent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (url != null) putExtra(EXTRA_URL, url)
            }

        return PendingIntent.getActivity(
            this,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        /** Shared URL key for intents created here or by Firebase. */
        const val EXTRA_URL = "url"

        /** Uses a stable unique ID so new pushes do not replace older ones. */
        private fun notificationIdFor(message: RemoteMessage): Int =
            message.messageId?.hashCode() ?: System.currentTimeMillis().toInt()
    }
}
