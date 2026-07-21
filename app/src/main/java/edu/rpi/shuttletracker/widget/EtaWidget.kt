package edu.rpi.shuttletracker.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.app.MainActivity
import edu.rpi.shuttletracker.core.ui.theme.VehicleColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.glance.appwidget.action.actionStartActivity as actionStartActivityIntent

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

private val SMALL = DpSize(110.dp, 110.dp)
private val MEDIUM = DpSize(250.dp, 110.dp)
private val LARGE = DpSize(250.dp, 250.dp)
private val XLARGE = DpSize(320.dp, 250.dp)
private val XXLARGE = DpSize(320.dp, 390.dp)
private val WIDE = DpSize(450.dp, 250.dp)
private val WIDE_TALL = DpSize(450.dp, 390.dp)

/**
 * Home-screen widget with two views: all stops' soonest arrivals, or one configured stop's full
 * live status. [EtaWidgetUpdater] fetches the data and saves it to Glance state; this class only
 * renders whatever's already stored, it never touches the network itself.
 * */
class EtaWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE, XLARGE, XXLARGE, WIDE, WIDE_TALL))

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            GlanceTheme(colors = etaWidgetColors(context)) {
                EtaWidgetContent()
            }
        }
    }
}

/** Backs the manifest `<receiver>` entry; just points the framework at [EtaWidget]. */
class EtaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EtaWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleEtaWidgetRefresh(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        enqueueImmediateEtaWidgetRefresh(context)
    }

    override fun onDisabled(context: Context) {
        cancelEtaWidgetRefresh(context)
        super.onDisabled(context)
    }
}

@Composable
private fun EtaWidgetContent() {
    val context = LocalContext.current
    val prefs = currentState<Preferences>()
    val snapshot = WidgetSnapshot.fromJsonOrEmpty(prefs[EtaWidgetKeys.SNAPSHOT_JSON])
    val routeFilter = prefs[EtaWidgetKeys.ROUTE_FILTER]
    val lastUpdatedMillis = prefs[EtaWidgetKeys.LAST_UPDATED_MILLIS]
    val loadFailed = prefs[EtaWidgetKeys.LOAD_FAILED] ?: false
    val showingAllRoutes = prefs[EtaWidgetKeys.SHOWING_ALL_ROUTES] ?: true
    val configuredStop = prefs[EtaWidgetKeys.CONFIGURED_STOP]
    val singleStop = configuredStop?.let { snapshot.perStop[it] }
    val isCompact = LocalSize.current.height <= SMALL.height
    val configureAction = configureAction()

    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.background)
                .cornerRadius(16.dp)
                .padding(8.dp),
    ) {
        WidgetHeader(
            // Ignored in all-routes mode - WidgetHeader shows the route filter there instead.
            title =
                if (showingAllRoutes) {
                    ""
                } else {
                    singleStop?.stopName
                        ?: context.getString(R.string.widget_configure_title)
                },
            showingAllRoutes = showingAllRoutes,
            routeFilter = routeFilter,
            lastUpdatedMillis = lastUpdatedMillis,
            onTitleClick = if (showingAllRoutes) null else configureAction,
        )

        Divider()

        when {
            !showingAllRoutes && configuredStop == null -> ConfigurePrompt()
            !showingAllRoutes && singleStop == null ->
                EmptyMessage(
                    if (loadFailed) R.string.widget_load_failed else R.string.widget_no_active_shuttles,
                )
            !showingAllRoutes -> SingleStopContent(singleStop = requireNotNull(singleStop), compact = isCompact)
            else ->
                AllRoutesContent(
                    snapshot = snapshot,
                    routeFilter = routeFilter,
                    loadFailed = loadFailed,
                    compact = isCompact,
                )
        }
    }
}

@Composable
private fun AllRoutesContent(
    snapshot: WidgetSnapshot,
    routeFilter: String?,
    loadFailed: Boolean,
    compact: Boolean,
) {
    val stops = snapshot.allRoutesForRoute(routeFilter)

    when {
        stops.isEmpty() && loadFailed -> EmptyMessage(R.string.widget_load_failed)
        stops.isEmpty() -> EmptyMessage(R.string.widget_no_active_shuttles)
        else ->
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(stops) { stop -> StopRow(stop = stop, compact = compact) }
            }
    }
}

@Composable
private fun SingleStopContent(
    singleStop: SingleStopSnapshot,
    compact: Boolean,
) {
    val context = LocalContext.current

    Column(modifier = GlanceModifier.fillMaxSize()) {
        if (singleStop.vehicles.isEmpty()) {
            Text(
                text = context.getString(R.string.widget_no_active_shuttles),
                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.padding(vertical = 8.dp),
            )
        } else {
            LazyColumn(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                items(singleStop.vehicles) { vehicle ->
                    VehicleRow(vehicle = vehicle, targetStopName = singleStop.stopName, compact = compact)
                }
            }
        }

        ScheduleFooter(nextScheduledEpochMillis = singleStop.nextScheduledEpochMillis)
    }
}

@Composable
private fun ConfigurePrompt() {
    val context = LocalContext.current

    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .padding(8.dp)
                .clickable(configureAction()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.widget_no_stop_configured),
            style =
                TextStyle(
                    fontSize = 12.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                ),
        )
    }
}

/** Opens [EtaWidgetConfigureActivity] to change this widget instance's stop. */
@Composable
private fun configureAction(): Action {
    val context = LocalContext.current
    val glanceId = LocalGlanceId.current
    val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
    val configureIntent =
        Intent(context, EtaWidgetConfigureActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

    return actionStartActivityIntent(configureIntent)
}

@Composable
private fun Divider() {
    Spacer(
        modifier =
            GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GlanceTheme.colors.outline)
                .padding(vertical = 4.dp),
    )
}

@Composable
private fun WidgetHeader(
    title: String,
    showingAllRoutes: Boolean,
    routeFilter: String?,
    lastUpdatedMillis: Long?,
    onTitleClick: Action?,
) {
    val context = LocalContext.current

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showingAllRoutes) {
            // All-routes mode has no stop to name, so the route filter goes here instead.
            Text(
                text = routeFilter?.lowercaseTitle() ?: context.getString(R.string.etas_route_all),
                maxLines = 1,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.primary),
                modifier =
                    GlanceModifier
                        .defaultWeight()
                        .clickable(actionRunCallback<ToggleRouteFilterAction>()),
            )
        } else {
            Text(
                text = title,
                maxLines = 1,
                style =
                    TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onBackground,
                    ),
                modifier =
                    GlanceModifier.defaultWeight().let { modifier ->
                        if (onTitleClick != null) modifier.clickable(onTitleClick) else modifier
                    },
            )
        }

        Text(
            text = context.getString(if (showingAllRoutes) R.string.widget_toggle_to_stop else R.string.etas_route_all),
            maxLines = 1,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GlanceTheme.colors.secondary),
            modifier =
                GlanceModifier
                    .clickable(actionRunCallback<ToggleStopModeAction>())
                    .padding(horizontal = 6.dp),
        )

        Spacer(modifier = GlanceModifier.width(4.dp))

        Image(
            provider = ImageProvider(R.drawable.ic_restart_alt),
            contentDescription = context.getString(R.string.widget_refresh),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground),
            modifier =
                GlanceModifier
                    .size(18.dp)
                    .clickable(actionRunCallback<RefreshAction>()),
        )
    }

    lastUpdatedMillis?.let { millis ->
        Text(
            text = context.getString(R.string.widget_updated_at, formatTime(millis)),
            maxLines = 1,
            style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant),
        )
    }
}

@Composable
private fun StopRow(
    stop: WidgetStopSnapshot,
    compact: Boolean,
) {
    Column(
        modifier =
            GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity<MainActivity>())
                .padding(vertical = 4.dp),
    ) {
        Text(
            text = stop.stopName,
            maxLines = 1,
            style =
                TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onBackground,
                ),
        )

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            val etasToShow = if (compact) stop.etas.take(1) else stop.etas
            etasToShow.forEach { eta ->
                Text(
                    text = formatTime(eta.etaEpochMillis),
                    maxLines = 1,
                    style =
                        TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = routeGlanceColor(eta.routeName),
                        ),
                    modifier = GlanceModifier.padding(end = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun VehicleRow(
    vehicle: WidgetVehicleSnapshot,
    targetStopName: String,
    compact: Boolean,
) {
    val context = LocalContext.current
    val isNow = vehicle.isAtStop && vehicle.currentStopName == targetStopName

    Row(
        modifier =
            GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity<MainActivity>())
                .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier =
                GlanceModifier
                    .width(4.dp)
                    .height(16.dp)
                    .background(routeGlanceColor(vehicle.routeName))
                    .cornerRadius(2.dp),
        )

        Spacer(modifier = GlanceModifier.width(6.dp))

        Text(
            text = vehicle.name,
            maxLines = 1,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onBackground),
            modifier = GlanceModifier.defaultWeight(),
        )

        if (!compact) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = context.getString(R.string.vehicle_speed, vehicle.speedMph),
                    maxLines = 1,
                    style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant),
                )
                Text(
                    text =
                        if (vehicle.isAtStop) {
                            context.getString(R.string.widget_at_stop_format, vehicle.currentStopName ?: "?")
                        } else {
                            context.getString(R.string.widget_moving)
                        },
                    maxLines = 1,
                    style =
                        TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.tertiary,
                        ),
                )
            }
        } else {
            Spacer(modifier = GlanceModifier.defaultWeight())
        }

        val etaLabel =
            when {
                isNow -> context.getString(R.string.widget_eta_now)
                vehicle.etaEpochMillis != null -> formatTime(vehicle.etaEpochMillis)
                else -> context.getString(R.string.widget_no_eta)
            }
        Text(
            text = etaLabel,
            maxLines = 1,
            style =
                TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isNow) GlanceTheme.colors.tertiary else GlanceTheme.colors.onBackground,
                ),
        )
    }
}

@Composable
private fun ScheduleFooter(nextScheduledEpochMillis: Long?) {
    val context = LocalContext.current

    Text(
        text =
            context.getString(
                R.string.widget_schedule_format,
                nextScheduledEpochMillis?.let { formatTime(it) } ?: context.getString(R.string.widget_schedule_none),
            ),
        maxLines = 1,
        style =
            TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = GlanceTheme.colors.onSurfaceVariant,
            ),
        modifier = GlanceModifier.padding(top = 4.dp),
    )
}

@Composable
private fun EmptyMessage(textRes: Int) {
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(textRes),
            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
        )
    }
}

private fun formatTime(epochMillis: Long): String =
    TIME_FORMATTER.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

private fun String.lowercaseTitle(): String = lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }

private fun routeGlanceColor(routeName: String?): ColorProvider =
    ColorProvider(
        when {
            routeName == null -> VehicleColors.Default
            "north" in routeName.lowercase(Locale.ROOT) -> VehicleColors.North
            "west" in routeName.lowercase(Locale.ROOT) -> VehicleColors.West
            else -> VehicleColors.Default
        },
    )
