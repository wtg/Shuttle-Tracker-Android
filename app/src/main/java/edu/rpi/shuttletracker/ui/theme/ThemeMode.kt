package edu.rpi.shuttletracker.ui.theme

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
