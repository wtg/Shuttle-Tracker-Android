package edu.rpi.shuttletracker.widget

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.data.local.preferences.UserPreferences
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.data.models.VehicleMerger
import edu.rpi.shuttletracker.data.repository.ShuttleRepository
import edu.rpi.shuttletracker.feature.etas.utils.buildStopsWithEtas
import edu.rpi.shuttletracker.feature.etas.utils.vehiclesForStop
import edu.rpi.shuttletracker.feature.map.utils.buildFakeVehicles
import edu.rpi.shuttletracker.feature.schedule.utils.nextScheduledArrival
import kotlinx.coroutines.flow.first
import java.time.ZoneId

/** Keys into each [EtaWidget] instance's [androidx.glance.state.PreferencesGlanceStateDefinition] state. */
object EtaWidgetKeys {
    val SNAPSHOT_JSON = stringPreferencesKey("snapshot_json")
    val LAST_UPDATED_MILLIS = longPreferencesKey("last_updated_millis")
    val ROUTE_FILTER = stringPreferencesKey("route_filter")
    val LOAD_FAILED = booleanPreferencesKey("load_failed")

    /** The stop key an instance's single-stop view targets, set by [EtaWidgetConfigureActivity]. */
    val CONFIGURED_STOP = stringPreferencesKey("configured_stop")

    /** Whether this instance shows the all-routes view instead of [CONFIGURED_STOP]. Defaults to `true`. */
    val SHOWING_ALL_ROUTES = booleanPreferencesKey("showing_all_routes")
}

/**
 * Fetches routes + live vehicle etas and refreshes every placed [EtaWidget] instance's state -
 * shared by [EtaWidgetRefreshWorker]'s periodic background refresh and the widget's own manual
 * refresh button ([RefreshAction]), so both paths update state the same way.
 * */
object EtaWidgetUpdater {
    private const val TAG = "EtaWidgetUpdater"

    suspend fun refresh(context: Context) {
        val entryPoint = WidgetEntryPoint.from(context)
        val snapshot = fetchSnapshot(entryPoint.shuttleRepository(), entryPoint.userPreferences())

        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(EtaWidget::class.java)
        for (glanceId in glanceIds) {
            updateAppWidgetState(context, glanceId) { prefs: MutablePreferences ->
                if (snapshot != null) {
                    prefs[EtaWidgetKeys.LOAD_FAILED] = false
                    prefs[EtaWidgetKeys.SNAPSHOT_JSON] = snapshot.toJson()
                    prefs[EtaWidgetKeys.LAST_UPDATED_MILLIS] = System.currentTimeMillis()
                } else {
                    prefs[EtaWidgetKeys.LOAD_FAILED] = true
                }
            }
        }
        if (glanceIds.isNotEmpty()) EtaWidget().updateAll(context)
    }

    /** Null only when the routes fetch itself fails - a vehicle-endpoint failure just yields fewer etas, not a hard error, since the widget has no room to show per-endpoint error detail anyway. */
    private suspend fun fetchSnapshot(
        repository: ShuttleRepository,
        userPreferences: UserPreferences,
    ): WidgetSnapshot? {
        val routes =
            repository.getRoutes().dataOrNull() ?: run {
                Log.w(TAG, "Widget refresh failed: could not load routes")
                return null
            }

        val locations =
            repository
                .observeVehicleLocations()
                .first()
                .dataOrNull()
                .orEmpty()
        val velocities =
            repository
                .observeVehicleVelocities()
                .first()
                .dataOrNull()
                .orEmpty()
        val etas =
            repository
                .observeVehicleEtas()
                .first()
                .dataOrNull()
                .orEmpty()

        val vehicles = VehicleMerger.merge(locations = locations, velocities = velocities, etas = etas)
        val schedule = repository.getSchedule().dataOrNull()

        // Same dev-mode fallback as the map and ETAs tab.
        val fakeShuttlesActive =
            userPreferences.getDevOptions().first() && userPreferences.getFakeShuttlesEnabled().first()
        val allVehicles =
            if (fakeShuttlesActive) {
                vehicles + buildFakeVehicles(routes, elapsedMs = System.currentTimeMillis())
            } else {
                vehicles
            }

        // Every stop, so any instance can find its configured stop below.
        val stopDirectory = buildStopsWithEtas(routes, emptyList())

        val perStop =
            stopDirectory.associate { stop ->
                stop.stopKey to
                    buildSingleStopSnapshot(
                        stopKey = stop.stopKey,
                        stopName = stop.stop.name,
                        vehicles = vehiclesForStop(allVehicles, stop.stopKey),
                        routesByName = routes,
                        nextScheduledEpochMillis =
                            schedule?.let {
                                nextScheduledArrivalMillis(
                                    stop.stopKey,
                                    it,
                                    routes,
                                )
                            },
                    )
            }

        return WidgetSnapshot(
            allRoutes = buildStopsWithEtas(routes, allVehicles).toWidgetStopSnapshots(),
            perStop = perStop,
        )
    }

    private fun nextScheduledArrivalMillis(
        stopKey: String,
        schedule: Schedule,
        routes: Map<String, Route>,
    ): Long? =
        nextScheduledArrival(stopKey = stopKey, schedule = schedule, routesByName = routes)
            ?.atZone(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()

    private fun <T> NetworkResult<T>.dataOrNull(): T? = (this as? NetworkResult.Success)?.data
}
