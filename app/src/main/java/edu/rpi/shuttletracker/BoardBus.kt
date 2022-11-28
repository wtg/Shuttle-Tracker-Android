package edu.rpi.shuttletracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews

// Intended functionality: A button that allows the user to board the bus. When the user clicks
// on the button, it opens up the app and lets the user choose their desired bus ID

/**
 * Implementation of App Widget functionality.
 */
class BoardBus : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val views = RemoteViews(context.packageName, R.layout.board_bus)
        val maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)

        if (maxHeight > 50) {
            views.setViewVisibility(R.id.widget_text_view, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_text_view, View.GONE)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val intent = Intent(context, MapsActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.board_bus)
    views.setOnClickPendingIntent(R.id.board_bus_widget_button, pendingIntent)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}