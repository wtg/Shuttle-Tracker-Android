package edu.rpi.shuttletracker.background.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.background.notification.Notifications
import edu.rpi.shuttletracker.background.receiver.NotificationReceiver
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.repository.ShuttleRepository
import java.util.concurrent.TimeUnit

@HiltWorker
class AnnouncementWorker
    @AssistedInject
    constructor(
        @Assisted val context: Context,
        @Assisted val workerParams: WorkerParameters,
        private val shuttleRepository: ShuttleRepository,
    ) : CoroutineWorker(context, workerParams) {
        override suspend fun doWork(): Result {
            val announcements = shuttleRepository.getAnnouncements()

            if (announcements !is NetworkResult.Success) {
                return Result.retry()
            }

            with(announcements.data) {
                if (hasNewAnnouncement(this)) {
                    pushNotification(
                        context.getString(R.string.announcements),
                        first().message,
                        size,
                    )
                }
            }

            return Result.success()
        }

        /**
         * Determines if there is a new announcement based on the first (most recent) entry being active.
         * */
        private fun hasNewAnnouncement(announcements: List<Announcement>): Boolean = announcements.first().active

        private fun pushNotification(
            subject: String,
            body: String,
            notificationCount: Int,
        ) {
            val notificationManager: NotificationManager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE,
                ) as NotificationManager

            val notificationBody =
                NotificationCompat
                    .Builder(
                        context,
                        Notifications.CHANNEL_ANNOUNCEMENT,
                    ).setContentTitle(subject)
                    .setContentText(body)
                    .setSmallIcon(R.drawable.ic_stat_default)
                    .addAction(
                        R.drawable.baseline_mark_email_read_24,
                        context.getString(R.string.mark_as_read),
                        NotificationReceiver.markNotificationsRead(context, notificationCount),
                    ).setContentIntent(NotificationReceiver.openAnnouncements(context))
                    .setAutoCancel(true)
                    .build()

            notificationManager.notify(Notifications.ID_ANNOUNCEMENT, notificationBody)
        }

        companion object {
            fun startWork(context: Context) {
                val constraints =
                    Constraints
                        .Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()

                val announcementWork =
                    PeriodicWorkRequestBuilder<AnnouncementWorker>(
                        1,
                        TimeUnit.DAYS,
                    ).setConstraints(constraints)
                        .build()

                WorkManager
                    .getInstance(context)
                    .enqueueUniquePeriodicWork(
                        "pushNotification",
                        ExistingPeriodicWorkPolicy.KEEP,
                        announcementWork,
                    )
            }
        }
    }
