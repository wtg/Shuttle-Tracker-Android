package edu.rpi.shuttletracker.widget

import android.content.Context
import androidx.glance.color.ColorProviders
import edu.rpi.shuttletracker.core.ui.theme.shuttleTrackerColorScheme
import androidx.glance.material3.ColorProviders as material3ColorProviders

/** The widget's light/dark colors, built from the app's own [shuttleTrackerColorScheme] instead of Glance's Material defaults. Dynamic color is left off so the widget stays visually stable. */
fun etaWidgetColors(context: Context): ColorProviders =
    material3ColorProviders(
        light = shuttleTrackerColorScheme(context, darkTheme = false, dynamicColor = false),
        dark = shuttleTrackerColorScheme(context, darkTheme = true, dynamicColor = false),
    )
