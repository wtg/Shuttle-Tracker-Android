package edu.rpi.shuttletracker.core.ui.theme

/** The user's saved theme preference (see [UserPreferences][edu.rpi.shuttletracker.data.local.preferences.UserPreferences]). */
enum class ThemeMode {
    System,
    Light,
    Dark,
    ;

    fun isDarkTheme(systemInDarkTheme: Boolean): Boolean =
        when (this) {
            System -> systemInDarkTheme
            Dark -> true
            Light -> false
        }
}
