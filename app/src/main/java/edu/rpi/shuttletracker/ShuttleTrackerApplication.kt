package edu.rpi.shuttletracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import edu.rpi.shuttletracker.util.notifications.Notifications
import edu.rpi.shuttletracker.util.services.FirebaseService
import edu.rpi.shuttletracker.util.workers.AnnouncementWorker
import javax.inject.Inject

@HiltAndroidApp
class ShuttleTrackerApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        Notifications.createChannels(this)

        FirebaseService.retrieveToken()

        AnnouncementWorker.startWork(this)
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
}
