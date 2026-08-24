package edu.rpi.shuttletracker.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import edu.rpi.shuttletracker.background.notification.Notifications

/** Enables Hilt and performs process-wide startup work. */
@HiltAndroidApp
class ShuttleTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Notifications.createChannels(this)
    }
}
