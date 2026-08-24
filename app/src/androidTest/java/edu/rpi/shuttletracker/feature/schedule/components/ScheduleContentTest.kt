package edu.rpi.shuttletracker.feature.schedule.components

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.data.models.Schedule
import edu.rpi.shuttletracker.testing.fixtures.testRoute
import edu.rpi.shuttletracker.testing.fixtures.testSchedule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun headerIsAlwaysShown() {
        setContent()

        composeRule.onNodeWithText("Schedule").assertIsDisplayed()
    }

    @Test
    fun loadingShowsASpinner() {
        setContent(schedule = null, isLoading = true)

        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun missingScheduleShowsEmptyState() {
        setContent(schedule = null, isLoading = false)

        composeRule.onNodeWithText("No schedule found").assertIsDisplayed()
    }

    @Test
    fun selectingAWeekdayShowsItsDepartureTimes() {
        setContent()

        composeRule.onNodeWithText("Wed").performClick()

        composeRule.onNodeWithText("North Route").assertIsDisplayed()
        composeRule.onNodeWithText("7:00 AM").assertIsDisplayed()
        composeRule.onNodeWithText("12:30 AM").assertIsDisplayed()
    }

    @Test
    fun expandingADepartureRowShowsItsStopTimes() {
        setContent()

        composeRule.onNodeWithText("Wed").performClick()
        // Explicitly open a row because auto-expand and click recomposition can race.
        composeRule.onNodeWithText("7:00 AM").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Student Union").assertIsDisplayed()
    }

    @Test
    fun aDayWithNoServiceShowsTheNoShuttlesEmptyState() {
        setContent()

        composeRule.onNodeWithText("Sat").performClick()

        composeRule.onNodeWithText("No shuttles running").assertIsDisplayed()
    }

    private fun setContent(
        schedule: Schedule? = testSchedule(),
        isLoading: Boolean = false,
    ) {
        composeRule.setContent {
            ShuttleTrackerTheme(dynamicColor = false) {
                ScheduleContent(
                    schedule = schedule,
                    isLoading = isLoading,
                    routesByName = mapOf("NORTH" to testRoute()),
                    selectedRoute = null,
                    onSelectedRouteChange = {},
                )
            }
        }
    }
}
