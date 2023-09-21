package edu.rpi.shuttletracker.util.notifications

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT

object Notifications {

    const val CHANNEL_TRACKER = "tracker_channel"
    const val ID_TRACKING_PROGRESS = 1

    fun createChannels(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)

        notificationManager.createNotificationChannelGroupsCompat(
            listOf(
                buildNotificationChannelGroup(CHANNEL_TRACKER, "Bus Tracker"),
            ),
        )

        notificationManager.createNotificationChannelsCompat(
            listOf(
                buildNotificationChannel(CHANNEL_TRACKER, IMPORTANCE_DEFAULT, "Bus Tracker"),
            ),
        )
    }

    private fun buildNotificationChannelGroup(channelId: String, name: String) =
        NotificationChannelGroupCompat
            .Builder(channelId)
            .setName(name)
            .build()

    private fun buildNotificationChannel(channelId: String, importance: Int, name: String) =
        NotificationChannelCompat
            .Builder(channelId, importance)
            .setName(name)
            .build()
}
