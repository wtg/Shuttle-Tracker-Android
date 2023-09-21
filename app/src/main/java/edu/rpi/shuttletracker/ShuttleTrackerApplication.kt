package edu.rpi.shuttletracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import edu.rpi.shuttletracker.util.notifications.Notifications

@HiltAndroidApp
class ShuttleTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Notifications.createChannels(this)
    }
}
