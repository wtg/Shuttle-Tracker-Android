package edu.rpi.shuttletracker.feature.etas.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.data.models.Route
import edu.rpi.shuttletracker.data.models.Vehicle
import edu.rpi.shuttletracker.feature.etas.utils.buildStopsWithEtas
import edu.rpi.shuttletracker.testing.fixtures.testRoute
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class EtaComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun vehicle(
        id: String,
        routeName: String,
        stopTimes: Map<String, String> = emptyMap(),
    ) = Vehicle(
        id = id,
        name = "$routeName Bus",
        latitude = 42.730,
        longitude = -73.680,
        speedMph = 10.0,
        timestamp = Instant.now().toString(),
        headingDegrees = null,
        routeName = routeName,
        isAtStop = false,
        currentStop = null,
        stopTimes = stopTimes,
    )

    @Test
    fun loadingShowsASpinnerBeforeRoutesArrive() {
        setListContent(routes = emptyMap(), routesLoaded = false)

        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun emptyRoutesOnceLoadedShowsTheNoRoutesMessageInsteadOfSpinningForever() {
        setListContent(routes = emptyMap(), routesLoaded = true)

        composeRule.onNodeWithText("No routes are running right now").assertIsDisplayed()
    }

    @Test
    fun routesOutsideTheVisibleAllowlistProduceNoStops() {
        setListContent(routes = mapOf("ACADEMY_SHUTTLE" to testRoute()))

        composeRule.onNodeWithText("No stops found").assertIsDisplayed()
    }

    @Test
    fun stopsFromVisibleRoutesAreListed() {
        val futureEta = Instant.now().plusSeconds(300).toString()
        setListContent(
            routes = mapOf("NORTH" to testRoute()),
            vehicles = listOf(vehicle("bus-1", "NORTH", mapOf("union" to futureEta))),
        )

        composeRule.onNodeWithText("Student Union").assertIsDisplayed()
        composeRule.onNodeWithText("Academy Hall").assertIsDisplayed()
    }

    @Test
    fun stopsWithNoApproachingVehicleShowTheEmptyEtaMessage() {
        setListContent(routes = mapOf("NORTH" to testRoute()), vehicles = emptyList())

        // Both of NORTH's stops (union, academy) lack a live eta here.
        composeRule.onAllNodesWithText("No live ETAs").assertCountEquals(2)
    }

    @Test
    fun etaChipShowsTheShuttleNameAndMinutes() {
        composeRule.setContent {
            ShuttleTrackerTheme(dynamicColor = false) {
                EtaChip(vehicleName = "500", routeName = "NORTH", minutes = 2)
            }
        }

        composeRule.onNodeWithText("500 · 2m").assertIsDisplayed()
    }

    @Test
    fun tappingARouteFilterInvokesTheCallbackWithThatRoute() {
        var selectedFilter: String? = null
        composeRule.setContent {
            ShuttleTrackerTheme(dynamicColor = false) {
                StopEtaList(
                    routes = mapOf("NORTH" to testRoute(), "WEST" to testRoute()),
                    vehicles = emptyList(),
                    routesLoaded = true,
                    selectedRouteFilter = selectedFilter,
                    onRouteFilterChange = { selectedFilter = it },
                    onStopClick = {},
                )
            }
        }

        composeRule.onNodeWithText("West").performClick()

        assertEquals("WEST", selectedFilter)
    }

    @Test
    fun tappingAStopInvokesOnStopClickWithItsKey() {
        var clickedStopKey: String? = null
        composeRule.setContent {
            ShuttleTrackerTheme(dynamicColor = false) {
                StopEtaList(
                    routes = mapOf("NORTH" to testRoute()),
                    vehicles = emptyList(),
                    routesLoaded = true,
                    selectedRouteFilter = null,
                    onRouteFilterChange = {},
                    onStopClick = { clickedStopKey = it },
                )
            }
        }

        composeRule.onNodeWithText("Student Union").performClick()

        assertEquals("union", clickedStopKey)
    }

    @Test
    fun sheetShowsEveryVehicleEtaForTheSelectedStop() {
        val futureEta = Instant.now().plusSeconds(120).toString()
        val stop =
            buildStopsWithEtas(
                routes = mapOf("NORTH" to testRoute()),
                vehicles = listOf(vehicle("bus-1", "NORTH", mapOf("union" to futureEta))),
            ).single { it.stopKey == "union" }

        composeRule.setContent {
            ShuttleTrackerTheme(dynamicColor = false) {
                StopEtaSheet(
                    stop = stop,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    onDismiss = {},
                )
            }
        }

        // The sheet's appear animation runs as a coroutine, so give it a chance to settle.
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Student Union").assertIsDisplayed()
        composeRule.onNodeWithText("NORTH Bus").assertIsDisplayed()
    }

    @Test
    fun sheetShowsTheEmptyMessageWhenNoVehicleIsApproaching() {
        val stop =
            buildStopsWithEtas(
                routes = mapOf("NORTH" to testRoute()),
                vehicles = emptyList(),
            ).single { it.stopKey == "union" }

        composeRule.setContent {
            ShuttleTrackerTheme(dynamicColor = false) {
                StopEtaSheet(
                    stop = stop,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    onDismiss = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("No live ETAs").assertIsDisplayed()
    }

    private fun setListContent(
        routes: Map<String, Route>,
        vehicles: List<Vehicle> = emptyList(),
        routesLoaded: Boolean = true,
    ) {
        composeRule.setContent {
            ShuttleTrackerTheme(dynamicColor = false) {
                StopEtaList(
                    routes = routes,
                    vehicles = vehicles,
                    routesLoaded = routesLoaded,
                    selectedRouteFilter = null,
                    onRouteFilterChange = {},
                    onStopClick = {},
                )
            }
        }
    }
}
