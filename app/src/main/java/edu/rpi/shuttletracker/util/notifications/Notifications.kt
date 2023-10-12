package edu.rpi.shuttletracker.util.notifications

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW
import edu.rpi.shuttletracker.R

/**
 * Based on the notification generator for Tachiyomi
 * */
object Notifications {

    private const val GROUP_TRACKER = "group_tracker"
    const val CHANNEL_TRACKING_BUS = "tracking_bus_channel"
    const val ID_TRACKING_BUS = 1
    const val CHANNEL_AUTO_BOARD = "auto_board_channel"
    const val ID_AUTO_BOARD = 2

    private val deprecatedChannels = listOf(
        "ShuttleTrackerRPI",
    )

    fun createChannels(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)

        // deletes all the channels
        deprecatedChannels.forEach(notificationManager::deleteNotificationChannel)

        // creates notification groups
        notificationManager.createNotificationChannelGroupsCompat(
            listOf(
                buildNotificationChannelGroup(GROUP_TRACKER, context.getString(R.string.bus_tracker)),
            ),
        )

        // create notification channels
        notificationManager.createNotificationChannelsCompat(
            listOf(
                buildNotificationChannel(GROUP_TRACKER, CHANNEL_TRACKING_BUS, IMPORTANCE_DEFAULT, context.getString(R.string.tracker)),
                buildNotificationChannel(GROUP_TRACKER, CHANNEL_AUTO_BOARD, IMPORTANCE_LOW, context.getString(R.string.auto_boarding)),
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
