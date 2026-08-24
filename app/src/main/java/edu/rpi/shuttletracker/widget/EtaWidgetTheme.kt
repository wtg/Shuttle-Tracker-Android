package edu.rpi.shuttletracker.widget

import android.content.Context
import androidx.glance.color.ColorProviders
import edu.rpi.shuttletracker.core.ui.theme.shuttleTrackerColorScheme
import androidx.glance.material3.ColorProviders as material3ColorProviders

/** Uses the app palette without dynamic color so widget colors stay stable. */
fun etaWidgetColors(context: Context): ColorProviders =
    material3ColorProviders(
        light = shuttleTrackerColorScheme(context, darkTheme = false, dynamicColor = false),
        dark = shuttleTrackerColorScheme(context, darkTheme = true, dynamicColor = false),
    )
