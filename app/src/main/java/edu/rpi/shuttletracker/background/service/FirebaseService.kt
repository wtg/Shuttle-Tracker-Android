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

/**
 * Firebase Cloud Messaging is a manually-operated push channel: staff send notifications directly
 * from the Firebase Console, independent of the shuttle API and its announcement banners. This
 * service only has to render what Firebase hands it and route taps back into the app.
 * */
@AndroidEntryPoint
class FirebaseService : FirebaseMessagingService() {
    /**
     * Debug-only: prints the registration token so it can be pasted into the Firebase Console's
     * "Send test message" field, which targets a single device rather than the whole app.
     * */
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        if (BuildConfig.DEBUG) Log.d("FCM_TOKEN", token)
    }

    /**
     * Only fires when the app is in the foreground; Firebase Console notification+data messages
     * are otherwise displayed automatically (using the manifest's default icon/color/channel)
     * when the app is backgrounded or not running, and never reach this callback.
     * */
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
        /**
         * Matches the raw FCM data key so the same extra name works whether we built the
         * PendingIntent ourselves (foreground) or Firebase copied its data payload onto the
         * launcher intent for us (background/terminated).
         * */
        const val EXTRA_URL = "url"

        /**
         * A constant ID would silently replace every previous push; the message ID is stable per
         * notification but unique across them, falling back to the clock only if Firebase omits it.
         * */
        private fun notificationIdFor(message: RemoteMessage): Int =
            message.messageId?.hashCode() ?: System.currentTimeMillis().toInt()
    }
}
