package edu.rpi.shuttletracker.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import edu.rpi.shuttletracker.background.notification.Notifications

/**
 * The app's [Application] class. Marking it `@HiltAndroidApp` is what turns on Hilt dependency
 * injection for the whole app - every `@AndroidEntryPoint`/`@HiltViewModel` elsewhere depends on
 * this. Also does the one-time setup that has to happen before any screen shows, like creating
 * notification channels.
 * */
@HiltAndroidApp
class ShuttleTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Notifications.createChannels(this)
    }
}
