package edu.rpi.shuttletracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import edu.rpi.shuttletracker.util.notifications.Notifications
import edu.rpi.shuttletracker.util.services.FirebaseService

@HiltAndroidApp
class ShuttleTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Notifications.createChannels(this)

        FirebaseService.retrieveToken()
    }
}
