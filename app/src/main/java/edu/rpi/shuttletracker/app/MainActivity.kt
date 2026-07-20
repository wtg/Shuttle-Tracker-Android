package edu.rpi.shuttletracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.MapsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SetupScreenDestination
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.core.ui.theme.ThemeMode
import edu.rpi.shuttletracker.data.local.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val setupCompletedFlow =
                remember(userPreferencesRepository) {
                    userPreferencesRepository
                        .getSetupCompleted()
                        .map { it as Boolean? }
                }

            val themeMode by userPreferencesRepository
                .getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            val setupCompleted by setupCompletedFlow
                .collectAsStateWithLifecycle(initialValue = null)

            ShuttleTrackerTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (setupCompleted != null) {
                        DestinationsNavHost(
                            navGraph = NavGraphs.root,
                            start =
                                if (setupCompleted == true) {
                                    MapsScreenDestination()
                                } else {
                                    SetupScreenDestination()
                                },
                        )
                    }
                }
            }
        }
    }
}
