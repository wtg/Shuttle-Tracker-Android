package edu.rpi.shuttletracker.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.repository.ShuttleRepository
import edu.rpi.shuttletracker.feature.etas.utils.StopWithEtas
import edu.rpi.shuttletracker.feature.etas.utils.buildStopsWithEtas
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Selects the stop shown by one widget instance during setup or editing. */
@AndroidEntryPoint
class EtaWidgetConfigureActivity : ComponentActivity() {
    @Inject
    lateinit var shuttleRepository: ShuttleRepository

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setResult(Activity.RESULT_CANCELED)

        appWidgetId =
            intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            ShuttleTrackerTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ConfigureContent(
                        loadRoutes = { (shuttleRepository.getRoutes() as? NetworkResult.Success)?.data ?: emptyMap() },
                        onAllRoutesSelected = { finishWithSelection(configuredStop = null) },
                        onStopSelected = { stop -> finishWithSelection(configuredStop = stop.stopKey) },
                    )
                }
            }
        }
    }

    private fun finishWithSelection(configuredStop: String?) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@EtaWidgetConfigureActivity).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(this@EtaWidgetConfigureActivity, glanceId) { prefs ->
                prefs[EtaWidgetKeys.SHOWING_ALL_ROUTES] = configuredStop == null
                if (configuredStop == null) {
                    prefs.remove(EtaWidgetKeys.CONFIGURED_STOP)
                } else {
                    prefs[EtaWidgetKeys.CONFIGURED_STOP] = configuredStop
                }
            }
            EtaWidgetUpdater.refresh(this@EtaWidgetConfigureActivity)

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }
}

@Composable
private fun ConfigureContent(
    loadRoutes: suspend () -> Map<String, Route>,
    onAllRoutesSelected: () -> Unit,
    onStopSelected: (StopWithEtas) -> Unit,
) {
    var stops by remember { mutableStateOf<List<StopWithEtas>?>(null) }

    LaunchedEffect(Unit) {
        stops = buildStopsWithEtas(loadRoutes(), emptyList())
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.widget_configure_title)) }) },
    ) { contentPadding ->
        val currentStops = stops
        if (currentStops == null) {
            CircularProgressIndicator(modifier = Modifier.padding(contentPadding).padding(24.dp))
        } else {
            LazyColumn(modifier = Modifier.padding(contentPadding)) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.etas_route_all)) },
                        modifier = Modifier.clickable(onClick = onAllRoutesSelected),
                    )
                }
                items(currentStops, key = { it.stopKey }) { stop ->
                    ListItem(
                        headlineContent = { Text(stop.stop.name) },
                        supportingContent = { Text(stop.routeNames.joinToString(", ")) },
                        modifier = Modifier.clickable { onStopSelected(stop) },
                    )
                }
            }
        }
    }
}
