package edu.rpi.shuttletracker.background.notification

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT
import edu.rpi.shuttletracker.R

/**
 * Based on the notification generator for Tachiyomi
 * */
object Notifications {
    private const val GROUP_TRACKER = "group_tracker"
    const val CHANNEL_TRACKING_BUS = "tracking_bus_channel"
    const val ID_TRACKING_BUS = 1

    private const val GROUP_PUSH = "group_push"
    const val CHANNEL_PUSH = "push_channel"

    private const val GROUP_DEPARTURES = "group_departures"
    const val CHANNEL_FIRING_DEPARTURES = "departure_alarm_channel"
    const val ID_FIRING_DEPARTURE = 10000

    private val deprecatedChannels =
        listOf(
            "ShuttleTrackerRPI",
            "announcement_channel",
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
                    GROUP_PUSH,
                    context.getString(R.string.push_notifications),
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
                    GROUP_PUSH,
                    CHANNEL_PUSH,
                    IMPORTANCE_DEFAULT,
                    context.getString(R.string.push_notifications),
                ),
                buildNotificationChannel(
                    GROUP_DEPARTURES,
                    CHANNEL_FIRING_DEPARTURES,
                    IMPORTANCE_DEFAULT,
                    "Firing departures",
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
