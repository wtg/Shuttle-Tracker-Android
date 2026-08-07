package edu.rpi.shuttletracker.feature.settings.about

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.testing.fakes.FakeUserPreferences
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aboutContentIsShown() {
        setContent()

        composeRule.onNodeWithText("Check out the repository").assertIsDisplayed()
        composeRule.onNodeWithText("Report a problem").assertIsDisplayed()
        composeRule.onNodeWithText("View Privacy Policy").assertIsDisplayed()
        composeRule.onNodeWithText("Libraries used").assertIsDisplayed()
    }

    @Test
    fun tappingTheVersionRowTenTimesUnlocksDeveloperOptions() {
        val preferences = FakeUserPreferences()
        setContent(preferences)

        repeat(10) {
            composeRule.onNodeWithText("Version").performClick()
        }
        // The 10th tap's unlock write goes through viewModelScope.launch, so it isn't guaranteed
        // to have landed in the fake preferences the instant performClick() returns.
        composeRule.waitForIdle()

        assertTrue(preferences.devOptions.value)
    }

    @Test
    fun tappingTheVersionRowFewerThanTenTimesDoesNotUnlockDeveloperOptions() {
        val preferences = FakeUserPreferences()
        setContent(preferences)

        repeat(9) {
            composeRule.onNodeWithText("Version").performClick()
        }
        composeRule.waitForIdle()

        assertTrue(!preferences.devOptions.value)
    }

    private fun setContent(preferences: FakeUserPreferences = FakeUserPreferences()) {
        composeRule.setContent {
            ShuttleTrackerTheme(dynamicColor = false) {
                AboutScreen(
                    onBack = {},
                    viewModel = remember { AboutViewModel(preferences) },
                )
            }
        }
    }
}
