package edu.rpi.shuttletracker.util.services

import android.app.Service
import android.content.Intent
import android.os.IBinder

class BeaconService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
