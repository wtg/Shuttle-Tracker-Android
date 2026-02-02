package edu.rpi.shuttletracker.util.notifications

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT

/**
 * Based on the notification generator for Tachiyomi
 * */
object Notifications {
    // Group
    private const val GROUP_TRACKER = "group_tracker"
    private const val GROUP_ANNOUNCEMENTS = "group_announcements"
    private const val GROUP_DEPARTURES = "group_departures"

    // Channels
    const val CHANNEL_TRACKING_BUS = "tracking_bus_channel"
    const val CHANNEL_ANNOUNCEMENT = "announcement_channel"
    const val CHANNEL_FIRING_DEPARTURES = "departure_alarm_channel"

    // IDs
    const val ID_TRACKING_BUS = 1
    const val ID_ANNOUNCEMENT = 11
    const val ID_FIRING_DEPARTURE = 10000

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
                    GROUP_ANNOUNCEMENTS,
                    "Announcements",
                ),
            ),
        )

        // create notification channels
        notificationManager.createNotificationChannelsCompat(
            listOf(
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
