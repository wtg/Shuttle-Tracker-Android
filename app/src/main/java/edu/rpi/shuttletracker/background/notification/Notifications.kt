package edu.rpi.shuttletracker.background.notification

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT
import edu.rpi.shuttletracker.R

/** Creates current notification channels and removes obsolete ones at startup. */
object Notifications {
    private const val GROUP_PUSH = "group_push"
    const val CHANNEL_PUSH = "push_channel"

    private val deprecatedChannels =
        listOf(
            "ShuttleTrackerRPI",
            "announcement_channel",
            "tracking_bus_channel",
            "departure_alarm_channel",
        )

    private val deprecatedGroups =
        listOf(
            "group_tracker",
            "group_departures",
        )

    fun createChannels(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)

        deprecatedChannels.forEach(notificationManager::deleteNotificationChannel)
        deprecatedGroups.forEach(notificationManager::deleteNotificationChannelGroup)

        notificationManager.createNotificationChannelGroupsCompat(
            listOf(
                buildNotificationChannelGroup(
                    GROUP_PUSH,
                    context.getString(R.string.push_notifications),
                ),
            ),
        )

        notificationManager.createNotificationChannelsCompat(
            listOf(
                buildNotificationChannel(
                    GROUP_PUSH,
                    CHANNEL_PUSH,
                    IMPORTANCE_DEFAULT,
                    context.getString(R.string.push_notifications),
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
