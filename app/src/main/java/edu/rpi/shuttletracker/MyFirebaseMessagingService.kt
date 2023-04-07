package edu.rpi.shuttletracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)


    }

    fun generateNotification(title: String, body: String){
        val intent = Intent(this, MapsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val builder = Notification.Builder(this)
        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
        builder.setContentTitle("PLACEHOLDER TITLE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "Shuttle Tracker",
                "PLACEHOLDER CONTENT", NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description =
                "Push notifications from Shuttle Tracker"
            val notificationManager = getSystemService(
                NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            builder.setChannelId(channel.id)
            builder.setContentIntent(pendingIntent)
        }
    }
}