package edu.rpi.shuttletracker.util.notifications

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW

object Notifications {

    private const val GROUP_TRACKER = "group_tracker"
    const val CHANNEL_TRACKING_BUS = "tracking_bus_channel"
    const val ID_TRACKING_BUS = 1
    const val CHANNEL_AUTO_BOARD = "auto_board_channel"
    const val ID_AUTO_BOARD = 2

    fun createChannels(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)

        notificationManager.createNotificationChannelGroupsCompat(
            listOf(
                buildNotificationChannelGroup(GROUP_TRACKER, "Bus Tracker"),
            ),
        )

        notificationManager.createNotificationChannelsCompat(
            listOf(
                buildNotificationChannel(GROUP_TRACKER, CHANNEL_TRACKING_BUS, IMPORTANCE_DEFAULT, "Bus Tracker"),
                buildNotificationChannel(GROUP_TRACKER, CHANNEL_AUTO_BOARD, IMPORTANCE_LOW, "Bus auto boarder"),
            ),
        )
    }

    private fun buildNotificationChannelGroup(groupId: String, name: String) =
        NotificationChannelGroupCompat
            .Builder(groupId)
            .setName(name)
            .build()

    private fun buildNotificationChannel(groupId: String, channelId: String, importance: Int, name: String) =
        NotificationChannelCompat
            .Builder(channelId, importance)
            .setGroup(groupId)
            .setName(name)
            .build()
}
