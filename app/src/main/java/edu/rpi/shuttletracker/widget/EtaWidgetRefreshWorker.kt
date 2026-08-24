package edu.rpi.shuttletracker.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration

private const val UNIQUE_WORK_NAME = "eta_widget_refresh"
private const val UNIQUE_IMMEDIATE_WORK_NAME = "eta_widget_refresh_immediate"

/** Refreshes widgets at WorkManager's 15-minute minimum interval. */
class EtaWidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        EtaWidgetUpdater.refresh(applicationContext)
        return Result.success()
    }
}

fun scheduleEtaWidgetRefresh(context: Context) {
    val request =
        PeriodicWorkRequestBuilder<EtaWidgetRefreshWorker>(Duration.ofMinutes(15))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        UNIQUE_WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        request,
    )
}

fun cancelEtaWidgetRefresh(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
}

/** Fills a new widget before its first periodic refresh. */
fun enqueueImmediateEtaWidgetRefresh(context: Context) {
    val request =
        OneTimeWorkRequestBuilder<EtaWidgetRefreshWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        UNIQUE_IMMEDIATE_WORK_NAME,
        ExistingWorkPolicy.KEEP,
        request,
    )
}
