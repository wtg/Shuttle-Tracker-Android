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

    private const val GROUP_ANNOUNCEMENTS = "group_announcements"
    const val CHANNEL_ANNOUNCEMENT = "announcement_channel"
    const val ID_ANNOUNCEMENT = 11

    private val deprecatedChannels =
        listOf(
            "ShuttleTrackerRPI",
        )

    fun createChannels(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)

        // deletes all the channels
        deprecatedChannels.forEach(notificationManager::deleteNotificationChannel)

        // creates notification groups
        notificationManager.createNotificationChannelGroupsCompat(
            listOf(
                buildNotificationChannelGroup(
                    GROUP_TRACKER,
                    context.getString(R.string.bus_tracker),
                ),
                buildNotificationChannelGroup(
                    GROUP_ANNOUNCEMENTS,
                    "Announcements",
                ),
            ),
        )

        // create notification channels
        notificationManager.createNotificationChannelsCompat(
            listOf(
                buildNotificationChannel(
                    GROUP_TRACKER,
                    CHANNEL_TRACKING_BUS,
                    IMPORTANCE_DEFAULT,
                    context.getString(R.string.tracker),
                ),
                buildNotificationChannel(
                    GROUP_TRACKER,
                    CHANNEL_AUTO_BOARD,
                    IMPORTANCE_LOW,
                    context.getString(R.string.automatic_board_bus),
                ),
                buildNotificationChannel(
                    GROUP_ANNOUNCEMENTS,
                    CHANNEL_ANNOUNCEMENT,
                    IMPORTANCE_DEFAULT,
                    "Announcement",
                ),
            ),
        )
    }

    private fun buildNotificationChannelGroup(
        groupId: String,
        name: String,
    ) = NotificationChannelGroupCompat
        .Builder(groupId)
        .setName(name)
        .build()

    private fun buildNotificationChannel(
        groupId: String,
        channelId: String,
        importance: Int,
        name: String,
    ) = NotificationChannelCompat
        .Builder(channelId, importance)
        .setGroup(groupId)
        .setName(name)
        .build()
}
