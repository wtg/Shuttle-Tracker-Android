package edu.rpi.shuttletracker.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import edu.rpi.shuttletracker.background.notification.Notifications
import edu.rpi.shuttletracker.background.worker.AnnouncementWorker
import javax.inject.Inject

@HiltAndroidApp
class ShuttleTrackerApplication :
    Application(),
    Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        Notifications.createChannels(this)

        AnnouncementWorker.startWork(this)
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()
}
