package edu.rpi.shuttletracker.app

import android.content.Intent
import android.net.Uri
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
import edu.rpi.shuttletracker.background.service.FirebaseService
import edu.rpi.shuttletracker.background.service.NotificationTapDestination
import edu.rpi.shuttletracker.background.service.resolveNotificationTapDestination
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.core.ui.theme.ThemeMode
import edu.rpi.shuttletracker.data.local.preferences.UserPreferences
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * The app's one and only [android.app.Activity]. Every screen you see is Compose content set here
 * via [setContent] and hosted by [AppNavigation] - there is no second Activity to navigate to.
 * Also decides whether to show setup or the map first, based on [UserPreferences], and turns a
 * tapped push notification into either "just open the app" or an external URL.
 * */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleNotificationTap(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationTap(intent)
    }

    /**
     * The map is already the app's home screen, so a tapped push notification needs no
     * navigation of its own - the only special case is an optional safe `url` to open instead.
     * Covers both the foreground PendingIntent we build in [FirebaseService] and the intent FCM
     * builds automatically for background/terminated taps, since both use the same extra name.
     * */
    private fun handleNotificationTap(intent: Intent?) {
        val url = intent?.getStringExtra(FirebaseService.EXTRA_URL) ?: return
        intent.removeExtra(FirebaseService.EXTRA_URL)

        when (val destination = resolveNotificationTapDestination(url)) {
            is NotificationTapDestination.ExternalUrl ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(destination.url)))
            NotificationTapDestination.Map -> Unit
        }
    }
}
