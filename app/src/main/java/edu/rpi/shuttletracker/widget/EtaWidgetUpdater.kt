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
import edu.rpi.shuttletracker.feature.schedule.utils.RPI_ZONE_ID
import edu.rpi.shuttletracker.feature.schedule.utils.nextScheduledArrival
import kotlinx.coroutines.flow.first

/** Preference keys stored separately for each widget instance. */
object EtaWidgetKeys {
    val SNAPSHOT_JSON = stringPreferencesKey("snapshot_json")
    val LAST_UPDATED_MILLIS = longPreferencesKey("last_updated_millis")
    val ROUTE_FILTER = stringPreferencesKey("route_filter")
    val LOAD_FAILED = booleanPreferencesKey("load_failed")

    val CONFIGURED_STOP = stringPreferencesKey("configured_stop")

    val SHOWING_ALL_ROUTES = booleanPreferencesKey("showing_all_routes")
}

/** Fetches one snapshot used by both periodic and manual widget refreshes. */
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

    /** Route failure aborts refresh; vehicle endpoint failures produce a partial snapshot. */
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

        val fakeShuttlesActive =
            userPreferences.getDevOptions().first() && userPreferences.getFakeShuttlesEnabled().first()
        val allVehicles =
            if (fakeShuttlesActive) {
                vehicles + buildFakeVehicles(routes, elapsedMs = System.currentTimeMillis())
            } else {
                vehicles
            }

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
            ?.atZone(RPI_ZONE_ID)
            ?.toInstant()
            ?.toEpochMilli()

    private fun <T> NetworkResult<T>.dataOrNull(): T? = (this as? NetworkResult.Success)?.data
}
