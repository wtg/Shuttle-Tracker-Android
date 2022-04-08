package edu.rpi.shuttletracker

import android.R
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate


class Wakelock : Service(){

    val markerTimer = Timer("markerTimer",true)

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        var notification: Notification = Notification.Builder(this, "1")
            .setSmallIcon(R.drawable.arrow_up_float)
            .setContentTitle("My Service Notification")
            .setContentText("Much longer text that cannot fit one line...")
            .setOngoing(true)
            .build()
        println("Service Started")


        markerTimer.scheduleAtFixedRate(0,1000){
            println("Service-BGLocationTracking: Current location is ")
        }

        startForeground(1,notification)
        return super.onStartCommand(intent, flags, startId)

    }

}