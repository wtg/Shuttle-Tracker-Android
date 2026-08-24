package edu.rpi.shuttletracker.feature.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.rpi.shuttletracker.core.network.NetworkResult
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.data.models.VehicleLocation
import edu.rpi.shuttletracker.data.models.VehicleStopEta
import edu.rpi.shuttletracker.data.models.VehicleVelocities
import edu.rpi.shuttletracker.feature.schedule.ScheduleViewModel
import edu.rpi.shuttletracker.testing.fakes.FakeShuttleRepository
import edu.rpi.shuttletracker.testing.fakes.FakeUserPreferences
import edu.rpi.shuttletracker.testing.fixtures.testRoute
import edu.rpi.shuttletracker.testing.fixtures.testSchedule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Constructs MapsViewModel directly with fakes (bypassing hiltViewModel()) so the bottom
 * navigation can be exercised without a Hilt test runner.
 * */
@RunWith(AndroidJUnit4::class)
class MapsScreenNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun allThreeTabsAreShown() {
        setContent()

        composeRule.onNodeWithText("Map").assertIsDisplayed()
        composeRule.onNodeWithText("ETAs").assertIsDisplayed()
        composeRule.onNodeWithText("Schedule").assertIsDisplayed()
    }

    @Test
    fun mapIsTheStartingTab() {
        setContent()

        composeRule.onNodeWithContentDescription("Open settings").assertIsDisplayed()
    }

    @Test
    fun switchingToScheduleTabShowsScheduleContent() {
        setContent()

        composeRule.onNodeWithText("Schedule").performClick()

        composeRule
            .onNodeWithText("Times are based on departures from the Student Union.", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun switchingToEtasTabShowsEtasContent() {
        setContent()

        composeRule.onNodeWithText("ETAs").performClick()

        composeRule.onNodeWithText("Student Union").assertIsDisplayed()
    }

    @Test
    fun switchingBackToMapRestoresMapContent() {
        setContent()

        composeRule.onNodeWithText("Schedule").performClick()
        composeRule.onNodeWithText("Map").performClick()

        composeRule.onNodeWithContentDescription("Open settings").assertIsDisplayed()
    }

    @Test
    fun tappingAVehicleInTheEtaSheetSwitchesToMap() {
        setContent {
            vehicleLocations.tryEmit(
                NetworkResult.Success(
                    mapOf("bus-1" to VehicleLocation("500", 42.730, -73.680, 12.0, Instant.now().toString(), 90)),
                ),
            )
            vehicleVelocities.tryEmit(NetworkResult.Success(mapOf("bus-1" to VehicleVelocities("NORTH", false, null))))
            vehicleEtas.tryEmit(
                NetworkResult.Success(
                    mapOf("bus-1" to VehicleStopEta(mapOf("union" to Instant.now().plusSeconds(300).toString()))),
                ),
            )
        }

        composeRule.onNodeWithText("ETAs").performClick()
        composeRule.onNodeWithText("Student Union").performClick()
        composeRule.onNodeWithText("500").performClick()

        composeRule.onNodeWithContentDescription("Open settings").assertIsDisplayed()
    }

    private fun setContent(configureRepository: FakeShuttleRepository.() -> Unit = {}) {
        val repository =
            FakeShuttleRepository().apply {
                routesResult = NetworkResult.Success(mapOf("NORTH" to testRoute()))
                scheduleResult = NetworkResult.Success(testSchedule())
                configureRepository()
            }
        val preferences = FakeUserPreferences()
        val viewModel = MapsViewModel(repository, preferences)
        val scheduleViewModel = ScheduleViewModel(repository)

        composeRule.setContent {
            ShuttleTrackerTheme(dynamicColor = false) {
                MapsScreen(
                    onOpenSettings = {},
                    viewModel = viewModel,
                    scheduleViewModel = scheduleViewModel,
                )
            }
        }
    }
}
