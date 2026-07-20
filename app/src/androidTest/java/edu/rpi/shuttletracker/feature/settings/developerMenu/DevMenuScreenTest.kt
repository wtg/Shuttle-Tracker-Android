package edu.rpi.shuttletracker.feature.settings.developerMenu

import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.testing.fakes.FakeUserPreferences
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Constructs DevMenuViewModel directly with a fake, bypassing hiltViewModel(), the same way
 * MapsScreenNavigationTest does - DevMenuContent itself is private, so the real screen is
 * exercised instead of trying to reach into it.
 * */
@RunWith(AndroidJUnit4::class)
class DevMenuScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun turningOffDeveloperOptionsNavigatesBack() {
        val preferences = FakeUserPreferences().apply { devOptions.value = true }
        var backInvoked = false
        setContent(preferences, onBack = { backInvoked = true })

        clickSwitch(index = 0)

        assertTrue(backInvoked)
    }

    @Test
    fun togglingSimulateAnnouncementsUpdatesThePreference() {
        val preferences = FakeUserPreferences().apply { devOptions.value = true }
        setContent(preferences)

        clickSwitch(index = 1)

        assertTrue(preferences.simulateAnnouncements.value)
    }

    @Test
    fun togglingFakeShuttlesUpdatesThePreference() {
        val preferences = FakeUserPreferences().apply { devOptions.value = true }
        setContent(preferences)

        clickSwitch(index = 2)

        assertTrue(preferences.fakeShuttlesEnabled.value)
    }

    private fun clickSwitch(index: Int) {
        composeRule.onAllNodes(isToggleable())[index].performClick()
        composeRule.waitForIdle()
    }

    private fun setContent(
        preferences: FakeUserPreferences,
        onBack: () -> Unit = {},
    ) {
        composeRule.setContent {
            ShuttleTrackerTheme(dynamicColor = false) {
                DevMenuScreen(onBack = onBack, viewModel = DevMenuViewModel(preferences))
            }
        }
    }
}
