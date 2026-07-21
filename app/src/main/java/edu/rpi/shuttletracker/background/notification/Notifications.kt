package edu.rpi.shuttletracker.background.notification

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT
import edu.rpi.shuttletracker.R

/**
 * Creates the notification channel(s) the app posts to, and cleans up channels from
 * removed/never-shipped features so they don't linger in the user's system settings. Called once
 * from [edu.rpi.shuttletracker.app.ShuttleTrackerApplication] on startup. Based on the
 * notification generator for Tachiyomi.
 * */
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

        // deletes channels/groups from removed or never-shipped features so they don't linger
        deprecatedChannels.forEach(notificationManager::deleteNotificationChannel)
        deprecatedGroups.forEach(notificationManager::deleteNotificationChannelGroup)

        // creates notification groups
        notificationManager.createNotificationChannelGroupsCompat(
            listOf(
                buildNotificationChannelGroup(
                    GROUP_PUSH,
                    context.getString(R.string.push_notifications),
                ),
            ),
        )

        // create notification channels
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
