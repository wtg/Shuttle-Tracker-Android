package edu.rpi.shuttletracker.feature.setup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun acceptingAboutShowsPrivacyPolicy() {
        var state by mutableStateOf(SetupUiState())
        composeRule.setContent {
            ShuttleTrackerTheme(dynamicColor = false) {
                SetupContent(
                    uiState = state,
                    onPreviousPage = {},
                    onCompletePage = { state = state.copy(page = SetupPage.PrivacyPolicy) },
                )
            }
        }

        composeRule.onNodeWithText("I accept").performClick()

        composeRule.onNodeWithText("Privacy Policy").assertIsDisplayed()
    }
}
