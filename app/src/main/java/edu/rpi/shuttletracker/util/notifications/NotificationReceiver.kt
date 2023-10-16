package edu.rpi.shuttletracker.util.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import edu.rpi.shuttletracker.ui.MainActivity
import edu.rpi.shuttletracker.util.services.LocationService

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_STOP_LOCATION_SERVICE -> context.stopService(
                Intent(context, LocationService::class.java),
            )
        }
    }

    companion object {
        const val ACTION_STOP_LOCATION_SERVICE = "STOP_SERVICE"

        /**
         * Creates a pending intent to stop location tracking service
         * */
        internal fun stopLocationService(context: Context): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_STOP_LOCATION_SERVICE
            }

            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /**
         * Creates a pending intent to open an activity
         * */
        internal fun openMaps(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
