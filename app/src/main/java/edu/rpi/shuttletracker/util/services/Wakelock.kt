package edu.rpi.shuttletracker.util.services

import android.R
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.res.Resources
import android.os.IBinder
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

class Wakelock : Service() {

    val markerTimer = Timer("markerTimer", true)

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val res: Resources = getResources()

        var notification: Notification = Notification.Builder(this, "1")
            .setSmallIcon(R.drawable.ic_menu_mylocation)
            .setContentTitle("Shuttle Tracker") // TODO get the string from xml file instead
            .setContentText("Who's the shuttle tracker now 🤔")
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_MAX)
            .build()

        println("Service Started")

        markerTimer.scheduleAtFixedRate(0, 1000) {
        }

        startForeground(1, notification)
        return super.onStartCommand(intent, flags, startId)
    }
}
