package edu.rpi.shuttletracker.util.workers

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.haroldadmin.cnradapter.NetworkResponse
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.repositories.ApiRepository
import edu.rpi.shuttletracker.data.repositories.UserPreferencesRepository
import edu.rpi.shuttletracker.util.notifications.NotificationReceiver
import edu.rpi.shuttletracker.util.notifications.Notifications
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@HiltWorker
class AnnouncementWorker
    @AssistedInject
    constructor(
        @Assisted val context: Context,
        @Assisted val workerParams: WorkerParameters,
        private val apiRepository: ApiRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : CoroutineWorker(context, workerParams) {
        override suspend fun doWork(): Result {
            val announcements = apiRepository.getAnnouncements()

            if (announcements !is NetworkResponse.Success) {
                return Result.retry()
            }

            val announcementsBody = announcements.body

            if (hasNewAnnouncement(announcementsBody)) {
                pushNotification(announcementsBody.first().subject, announcementsBody.first().body)
            }

            return Result.success()
        }

        private fun hasNewAnnouncement(announcements: List<Announcement>): Boolean =
            announcements.size >
                runBlocking {
                    userPreferencesRepository.getNotificationsRead().first()
                } && announcements.isNotEmpty()

        private fun pushNotification(
            subject: String,
            body: String,
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
                    .setContentIntent(NotificationReceiver.openMaps(context))
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
                    OneTimeWorkRequestBuilder<AnnouncementWorker>().setConstraints(constraints)
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build()

                WorkManager
                    .getInstance(context)
                    .enqueueUniqueWork(
                        "pushNotification",
                        ExistingWorkPolicy.REPLACE,
                        announcementWork,
                    )

                // val announcementWork =
                //    PeriodicWorkRequestBuilder<AnnouncementWorker>(
                //        5,
                //        TimeUnit.MINUTES,
                //    ).setConstraints(constraints)
                //        .build()
                //
                // WorkManager
                //    .getInstance(context)
                //    .enqueueUniquePeriodicWork(
                //        "pushNotification",
                //        ExistingPeriodicWorkPolicy.KEEP,
                //        announcementWork,
                //    )
            }
        }
    }
