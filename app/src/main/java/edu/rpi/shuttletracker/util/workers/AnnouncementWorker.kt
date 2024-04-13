package edu.rpi.shuttletracker.util.workers

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
import com.haroldadmin.cnradapter.NetworkResponse
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.repositories.ApiRepository
import edu.rpi.shuttletracker.util.notifications.NotificationReceiver
import edu.rpi.shuttletracker.util.notifications.Notifications
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class AnnouncementWorker
    @AssistedInject
    constructor(
        @Assisted val context: Context,
        @Assisted val workerParams: WorkerParameters,
        private val apiRepository: ApiRepository,
    ) : CoroutineWorker(context, workerParams) {
        override suspend fun doWork(): Result {
            val announcements = apiRepository.getAnnouncements()

            if (announcements !is NetworkResponse.Success) {
                return Result.retry()
            }

            with(announcements.body) {
                if (hasNewAnnouncement(this)) {
                    pushNotification(
                        first().subject,
                        first().body,
                        size,
                    )
                }
            }

            return Result.success()
        }

        /**
         * Determines if there is a new announcement based on:
         * - having the current time be between the announcement's start & end time
         * */
        private fun hasNewAnnouncement(announcements: List<Announcement>): Boolean =
            with(announcements.first()) {
                val now = Calendar.getInstance()
                now.after(startCalendar) && now.before(endCalendar)
            }

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
                NotificationCompat.Builder(
                    context,
                    Notifications.CHANNEL_ANNOUNCEMENT,
                ).setContentTitle(subject)
                    .setContentText(body)
                    .setSmallIcon(R.drawable.ic_stat_default)
                    .addAction(
                        R.drawable.baseline_mark_email_read_24,
                        context.getString(R.string.mark_as_read),
                        NotificationReceiver.markNotificationsRead(context, notificationCount),
                    )
                    .setContentIntent(NotificationReceiver.openAnnouncements(context))
                    .setAutoCancel(true)
                    .build()

            notificationManager.notify(Notifications.ID_ANNOUNCEMENT, notificationBody)
        }

        companion object {
            fun startWork(context: Context) {
                val constraints =
                    Constraints.Builder()
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
