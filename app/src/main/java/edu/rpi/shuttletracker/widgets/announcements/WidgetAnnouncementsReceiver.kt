package edu.rpi.shuttletracker.widgets.announcements

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.haroldadmin.cnradapter.NetworkResponse
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.data.models.Announcement
import edu.rpi.shuttletracker.data.repositories.ApiRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WidgetAnnouncementsReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = WidgetAnnouncements()

    @Inject
    lateinit var apiRepository: ApiRepository

    private fun getAnnouncements(context: Context) {
        allAnnouncements = null

        MainScope().launch {
            val curAnnouncements = apiRepository.getAnnouncements()
            val glanceId = GlanceAppWidgetManager(context).getGlanceIds(WidgetAnnouncements::class.java).firstOrNull()

            if (curAnnouncements !is NetworkResponse.Success) {
                return@launch
            }

            if (glanceId != null) {
                allAnnouncements = curAnnouncements.body
                glanceAppWidget.update(context, glanceId)
            }
        }
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        super.onReceive(context, intent)
        getAnnouncements(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        getAnnouncements(context)
    }

    companion object {
        var allAnnouncements: List<Announcement>? = null
    }
}
