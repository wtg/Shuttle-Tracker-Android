package edu.rpi.shuttletracker

import android.R
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate


class Wakelock : Service(){

    val markerTimer = Timer("markerTimer",true)

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val res : Resources = getResources()

        var notification: Notification = Notification.Builder(this, "1")
            .setSmallIcon(R.drawable.ic_menu_mylocation)
            .setContentTitle("Shuttle Tracker")//TODO get the string from xml file instead
            .setContentText("Who's the shuttle tracker now 🤔")
            .setOngoing(true)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .build()


        println("Service Started")


        markerTimer.scheduleAtFixedRate(0,1000){
        }

        startForeground(1,notification)
        return super.onStartCommand(intent, flags, startId)

    }

}