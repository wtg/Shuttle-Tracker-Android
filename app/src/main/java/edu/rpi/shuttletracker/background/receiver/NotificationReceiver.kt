package edu.rpi.shuttletracker.background.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.app.MainActivity
import edu.rpi.shuttletracker.background.notification.Notifications
import edu.rpi.shuttletracker.data.local.preferences.UserPreferences
import edu.rpi.shuttletracker.data.repository.ShuttleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {
    @Inject
    lateinit var shuttleRepository: ShuttleRepository

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            ACTION_MARK_NOTIFICATIONS_READ -> {
                val notificationManager: NotificationManager =
                    context.getSystemService(
                        Context.NOTIFICATION_SERVICE,
                    ) as NotificationManager

                notificationManager.cancel(Notifications.ID_ANNOUNCEMENT)
                goAsync {
                    userPreferences.saveNotificationsRead(intent.getIntExtra("count", 0))
                }
            }
        }
    }

    companion object {
        const val ACTION_MARK_NOTIFICATIONS_READ = "MARK_NOTIFICATIONS_READ"

        /**
         * Creates a pending intent to open an activity
         * */
        internal fun openMaps(context: Context): PendingIntent {
            val intent =
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                }

            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        internal fun openAnnouncements(context: Context): PendingIntent {
            val intent =
                Intent(context, MainActivity::class.java).apply {
                    data = "https://shuttletracker.app/analytics/".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        internal fun markNotificationsRead(
            context: Context,
            count: Int,
        ): PendingIntent {
            val intent =
                Intent(context, NotificationReceiver::class.java).apply {
                    action = ACTION_MARK_NOTIFICATIONS_READ
                    putExtra("count", count)
                }

            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}

/**
 * Allows us to call suspend functions in broadcast receiver
 * */
fun BroadcastReceiver.goAsync(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit,
) {
    val pendingResult = goAsync()
    @OptIn(DelicateCoroutinesApi::class)
    GlobalScope.launch(context) {
        try {
            block()
        } finally {
            pendingResult.finish()
        }
    }
}
