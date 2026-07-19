package edu.rpi.shuttletracker.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import edu.rpi.shuttletracker.background.notification.Notifications

@HiltAndroidApp
class ShuttleTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Notifications.createChannels(this)
    }
}
