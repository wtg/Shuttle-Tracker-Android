package edu.rpi.shuttletracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.app.navigation.AppNavigation
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.core.ui.theme.ThemeMode
import edu.rpi.shuttletracker.data.local.preferences.UserPreferences
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val setupCompletedFlow =
                remember(userPreferences) {
                    userPreferences
                        .getSetupCompleted()
                        .map { it as Boolean? }
                }

            val themeMode by userPreferences
                .getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            val setupCompleted by setupCompletedFlow
                .collectAsStateWithLifecycle(initialValue = null)

            LaunchedEffect(themeMode) {
                AppCompatDelegate.setDefaultNightMode(
                    when (themeMode) {
                        ThemeMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        ThemeMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
                        ThemeMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
                    },
                )
            }

            ShuttleTrackerTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (setupCompleted != null) {
                        AppNavigation(setupCompleted = setupCompleted == true)
                    }
                }
            }
        }
    }
}
