package edu.rpi.shuttletracker.widget

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import edu.rpi.shuttletracker.data.local.preferences.UserPreferences
import edu.rpi.shuttletracker.data.repository.ShuttleRepository

/** Lets classes that only get a plain [Context] (the refresh worker, Glance actions) reach [ShuttleRepository]/[UserPreferences] without full Hilt injection. */
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
