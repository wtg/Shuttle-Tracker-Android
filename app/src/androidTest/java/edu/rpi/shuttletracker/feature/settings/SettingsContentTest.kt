package edu.rpi.shuttletracker.feature.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.rpi.shuttletracker.core.ui.theme.ShuttleTrackerTheme
import edu.rpi.shuttletracker.core.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectingDarkThemeSendsDarkMode() {
        var selectedMode: ThemeMode? = null
        setContent(onThemeModeChange = { selectedMode = it })

        composeRule.onNodeWithText("Dark").performClick()

        assertEquals(ThemeMode.Dark, selectedMode)
    }

    @Test
    fun redoSetupInvokesResetAction() {
        var resetRequested = false
        setContent(onRedoSetup = { resetRequested = true })

        composeRule.onNodeWithText("Redo Setup").performClick()

        assertTrue(resetRequested)
    }

    private fun setContent(
        onThemeModeChange: (ThemeMode) -> Unit = {},
        onRedoSetup: () -> Unit = {},
    ) {
        composeRule.setContent {
            ShuttleTrackerTheme(dynamicColor = false) {
                SettingsContent(
                    uiState = SettingsUiState(),
                    onBack = {},
                    onThemeModeChange = onThemeModeChange,
                    onRotationChange = {},
                    onAnimationsChange = {},
                    onRedoSetup = onRedoSetup,
                    onAbout = {},
                    onOpenAppSettings = {},
                    onDeveloperOptions = {},
                )
            }
        }
    }
}
