package edu.rpi.shuttletracker

import android.R
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat


class BackgroundLocationTracking : Service(){
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
//Null service
    //TODO: https://stackoverflow.com/questions/34573109/how-to-make-an-android-app-to-always-run-in-background

    var notificationIntent = Intent(this, MapsActivity::class.java)

    open fun startForeground() {
    var builder = NotificationCompat.Builder(this, "1")
        .setSmallIcon(R.drawable.arrow_up_float)
        .setContentTitle("My Service Notification")
        .setContentText("Much longer text that cannot fit one line...")
        .setStyle(NotificationCompat.BigTextStyle()
            .bigText("Much longer text that cannot fit one line..."))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setOngoing(true)
    )
}
}