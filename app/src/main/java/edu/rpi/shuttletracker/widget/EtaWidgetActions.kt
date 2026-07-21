package edu.rpi.shuttletracker.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import edu.rpi.shuttletracker.feature.etas.utils.ETA_VISIBLE_ROUTES

/** Refetches live data and updates every widget instance. Bound to the widget's refresh icon. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        EtaWidgetUpdater.refresh(context)
    }
}

/** Cycles this instance's route filter through "every route" and each of [ETA_VISIBLE_ROUTES]. Just a display filter, no network call. */
class ToggleRouteFilterAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val options = listOf(null) + ETA_VISIBLE_ROUTES
        updateAppWidgetState(context, glanceId) { prefs: MutablePreferences ->
            val current = prefs[EtaWidgetKeys.ROUTE_FILTER]
            val nextIndex = (options.indexOf(current) + 1).mod(options.size)
            val next = options[nextIndex]
            if (next == null) {
                prefs.remove(EtaWidgetKeys.ROUTE_FILTER)
            } else {
                prefs[EtaWidgetKeys.ROUTE_FILTER] = next
            }
        }
        EtaWidget().update(context, glanceId)
    }
}

/** Flips this instance between the all-routes view and its configured single-stop view. Just a display switch, no network call. */
class ToggleStopModeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { prefs: MutablePreferences ->
            val showingAllRoutes = prefs[EtaWidgetKeys.SHOWING_ALL_ROUTES] ?: true
            prefs[EtaWidgetKeys.SHOWING_ALL_ROUTES] = !showingAllRoutes
        }
        EtaWidget().update(context, glanceId)
    }
}
