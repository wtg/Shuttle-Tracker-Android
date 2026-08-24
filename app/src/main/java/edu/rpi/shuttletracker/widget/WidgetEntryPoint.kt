package edu.rpi.shuttletracker.widget

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import edu.rpi.shuttletracker.data.local.preferences.UserPreferences
import edu.rpi.shuttletracker.data.repository.ShuttleRepository

/** Exposes app dependencies to workers and Glance actions that only receive a [Context]. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun shuttleRepository(): ShuttleRepository

    fun userPreferences(): UserPreferences

    companion object {
        fun from(context: Context): WidgetEntryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java,
            )
    }
}
