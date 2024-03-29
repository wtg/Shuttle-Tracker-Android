package edu.rpi.shuttletracker.util.notifications

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_HIGH
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
    const val CHANNEL_LEAVE_BUS = "leave_bus_channel"
    const val ID_LEAVE_BUS = 3

    private const val GROUP_ANNOUNCEMENTS = "group_announcements"
    const val CHANNEL_ANNOUNCEMENT = "announcement_channel"
    const val ID_ANNOUNCEMENT = 11

    private const val GROUP_DEPARTURES = "group_departures"
    const val CHANNEL_FIRING_DEPARTURES = "departure_alarm_channel"
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
                    GROUP_TRACKER,
                    context.getString(R.string.bus_tracker),
                ),
                buildNotificationChannelGroup(
                    GROUP_ANNOUNCEMENTS,
                    "Announcements",
                ),
                buildNotificationChannelGroup(
                    GROUP_DEPARTURES,
                    "Departures",
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
                buildNotificationChannel(
                    GROUP_DEPARTURES,
                    CHANNEL_FIRING_DEPARTURES,
                    IMPORTANCE_DEFAULT,
                    "Firing departures",
                ),
                buildNotificationChannel(
                    GROUP_TRACKER,
                    CHANNEL_LEAVE_BUS,
                    IMPORTANCE_HIGH,
                    "Leave bus",
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
