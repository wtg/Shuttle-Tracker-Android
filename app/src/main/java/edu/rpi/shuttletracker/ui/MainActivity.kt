package edu.rpi.shuttletracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.ramcosta.composedestinations.DestinationsNavHost
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.data.models.EmptyEvent
import edu.rpi.shuttletracker.data.models.Event
import edu.rpi.shuttletracker.data.repositories.ApiRepository
import edu.rpi.shuttletracker.data.repositories.UserPreferencesRepository
import edu.rpi.shuttletracker.ui.destinations.MapsScreenDestination
import edu.rpi.shuttletracker.ui.destinations.SetupScreenDestination
import edu.rpi.shuttletracker.ui.setup.TOTAL_PAGES
import edu.rpi.shuttletracker.ui.theme.ShuttleTrackerTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
            ShuttleTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    DestinationsNavHost(
                        navGraph = NavGraphs.root,
                        startRoute =
                            if (runBlocking { userPreferencesRepository.getSetupStartIndex() == TOTAL_PAGES }) {
                                MapsScreenDestination
                            } else {
                                SetupScreenDestination
                            },
                    )
                }
            }
        }
    }
}
