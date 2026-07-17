package edu.rpi.shuttletracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.MapsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SetupScreenDestination
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.data.models.EmptyEvent
import edu.rpi.shuttletracker.data.models.Event
import edu.rpi.shuttletracker.data.repository.ApiRepository
import edu.rpi.shuttletracker.data.repository.UserPreferencesRepository
import edu.rpi.shuttletracker.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.ui.theme.ThemeMode
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var apiRepository: ApiRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            apiRepository.sendAnalytics(Event(coldLaunch = EmptyEvent))
        }

        setContent {
            val themeMode by userPreferencesRepository
                .getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            val setupCompleted by userPreferencesRepository
                .getSetupCompleted()
                .map { it as Boolean? }
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
