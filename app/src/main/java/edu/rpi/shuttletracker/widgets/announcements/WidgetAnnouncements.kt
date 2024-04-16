package edu.rpi.shuttletracker.widgets.announcements

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import androidx.glance.text.Text
import edu.rpi.shuttletracker.ui.theme.LightColors
import edu.rpi.shuttletracker.widgets.announcements.WidgetAnnouncementsReceiver.Companion.allAnnouncements

class WidgetAnnouncements : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            GlanceTheme(colors = ColorScheme.colors) {
                Content()
            }
        }
    }

    @Composable
    fun Content() {
        Box(
            modifier =
                GlanceModifier.fillMaxSize()
                    .background(GlanceTheme.colors.background),
        ) {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Text(text = "ST Announcements", modifier = GlanceModifier.padding(15.dp))
                    Button(
                        text = "⟳",
                        onClick = actionRunCallback<RefreshAnnouncements>(),
                    )
                }

                if (allAnnouncements != null) {
                    LazyColumn {
                        items(allAnnouncements!!) {
                            Column {
                                Text(text = it.subject)
                                Text(text = it.body)
                                Text(text = "Till: ${it.endTime}")
                            }
                        }
                    }
                } else {
                    Text(text = "No data...")
                }
            }
        }
    }

    inner class RefreshAnnouncements : ActionCallback {
        override suspend fun onAction(
            context: Context,
            glanceId: GlanceId,
            parameters: ActionParameters,
        ) {
            update(context, glanceId)
        }
    }

    internal object ColorScheme {
        val colors =
            ColorProviders(
                light = LightColors,
                dark = LightColors,
            )
    }
}
